/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode.bpel.runtime;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.common.FaultException;
import org.apache.ode.bpel.engine.BpelProcess;
import org.apache.ode.bpel.engine.BpelRuntimeContextImpl;
import org.apache.ode.bpel.evt.ActivityEnabledEvent;
import org.apache.ode.bpel.evt.ActivityExecEndEvent;
import org.apache.ode.bpel.evt.ActivityExecStartEvent;
import org.apache.ode.bpel.evt.ActivityFailureEvent;
import org.apache.ode.bpel.evt.ActivityRecoveryEvent;
import org.apache.ode.bpel.evt.InvokeExecStartEvent;
import org.apache.ode.bpel.explang.EvaluationException;
import org.apache.ode.bpel.iapi.Endpoint;
import org.apache.ode.bpel.o.OActivity;
import org.apache.ode.bpel.o.OAdvice;
import org.apache.ode.bpel.o.OExpression;
import org.apache.ode.bpel.o.OFailureHandling;
import org.apache.ode.bpel.o.OInvoke;
import org.apache.ode.bpel.o.OLink;
import org.apache.ode.bpel.o.OScope;
import org.apache.ode.bpel.runtime.channels.ActivityRecoveryChannel;
import org.apache.ode.bpel.runtime.channels.ActivityRecoveryChannelListener;
import org.apache.ode.bpel.runtime.channels.FaultData;
import org.apache.ode.bpel.runtime.channels.LinkStatusChannelListener;
import org.apache.ode.bpel.runtime.channels.ParentScopeChannel;
import org.apache.ode.bpel.runtime.channels.ParentScopeChannelListener;
import org.apache.ode.bpel.runtime.channels.TerminationChannelListener;
import org.apache.ode.bpel.runtime.channels.TimerResponseChannel;
import org.apache.ode.bpel.runtime.channels.TimerResponseChannelListener;
import org.apache.ode.jacob.ChannelListener;
import org.apache.ode.jacob.SynchChannel;
import org.w3c.dom.Element;

import de.tud.stg.ao4ode.runtime.aspectmanager.AdviceActivity;
import de.tud.stg.ao4ode.runtime.aspectmanager.AspectManager;

//AO4ODE: Changed Visibility to public
public class ACTIVITYGUARD extends ACTIVITY {
    private static final long serialVersionUID = 1L;
    
    private static final Log __log = LogFactory.getLog(ACTIVITYGUARD.class);

    private static final ActivityTemplateFactory __activityTemplateFactory = new ActivityTemplateFactory();
    private OActivity _oactivity;

    /** Link values. */
    private Map<OLink, Boolean> _linkVals = new HashMap<OLink, Boolean>();

    /** Flag to prevent duplicate ActivityEnabledEvents */
    private boolean _firstTime = true;

    private ActivityFailure _failure;
    
    ActivityInfo activity = null;
    ActivityInfo adviceActivity = null;

    public ACTIVITYGUARD(ActivityInfo self, ScopeFrame scopeFrame, LinkFrame linkFrame) {
        super(self, scopeFrame, linkFrame);
        _oactivity = self.o;
    }

    public void run() {
        // Send a notification of the activity being enabled,
        if (_firstTime) {
            sendEvent(new ActivityEnabledEvent());
            _firstTime = false;
        }

        /* we know all our links statuses */
        if (_linkVals.keySet().containsAll(_oactivity.targetLinks)) {
            if (evaluateJoinCondition()) {
            	
                // intercept completion channel in order to execute transition conditions.
                activity = new ActivityInfo(genMonotonic(),
                		_self.o,
                		_self.self,
                		newChannel(ParentScopeChannel.class));
                
                // AO4ODE: Scope & Pointcut matching
            	AspectManager am = AspectManager.getInstance();

            	AdviceActivity aa = am.getAdvice(
            			getBpelRuntimeContext().getPid(), _self.o);
            	
            	// TODO: advice level            	
                if(aa != null && !(_self.o.getOwner() instanceof OAdvice)) {
                	
                	// AO4ODE: Add PartnerLinks of Advices to Process
                	BpelRuntimeContextImpl rt = (BpelRuntimeContextImpl)getBpelRuntimeContext();
                	BpelProcess bpelProcess = rt._bpelProcess;
                	for(OAdvice oAdvice : aa.getAdvices()) {
                		__log.debug("Adding advice " + oAdvice.getName() + " to bpelProcess: " + bpelProcess.getOProcess().getName());
                		Map<String, Endpoint> invokeEndpoints = am.getAspectConfiguration(oAdvice.getOAspect().getQName()).getInvokeEndpoints();
                		List<File> aspectDeploymentUnits = aa.getAspectDeploymentUnits();
                		if(invokeEndpoints.size() > 0) {
                			bpelProcess.addAdvice(oAdvice, invokeEndpoints, aspectDeploymentUnits);                		
                		}
                	}
                	                	
                	adviceActivity = new ActivityInfo(genMonotonic(),
                    		aa.getOAdviceActivity(),
                    		_self.self,
                    		newChannel(ParentScopeChannel.class));
                	
                	AspectManager.getInstance().addJPActivity(getBpelRuntimeContext().getPid(),
            				this);
                	                	
                	runActivity(adviceActivity);
                	
                }
                else {
                	
                	// No matching pointcuts, go ahead
                	runActivity(activity);
                	
                }
                
            } else {
                if (_oactivity.suppressJoinFailure) {
                    _self.parent.completed(null, CompensationHandler.emptySet());
                    if (__log.isDebugEnabled())
                        __log.debug("Join condition false, suppress join failure on activity " + _self.aId);
                } else {
                    FaultData fault = null;
                    fault = createFault(_oactivity.getOwner().constants.qnJoinFailure,_oactivity);
                    _self.parent.completed(fault, CompensationHandler.emptySet());
                }

                // Dead path activity.
                dpe(_oactivity);
            }
        } else /* don't know all our links statuses */ {
            Set<ChannelListener> mlset = new HashSet<ChannelListener>();
            mlset.add(new TerminationChannelListener(_self.self) {
                private static final long serialVersionUID = 5094153128476008961L;

                public void terminate() {
                    // Complete immediately, without faulting or registering any comps.
                    _self.parent.completed(null, CompensationHandler.emptySet());
                    // Dead-path activity
                    dpe(_oactivity);
                }
            });
            for (final OLink link : _oactivity.targetLinks) {
                mlset.add(new LinkStatusChannelListener(_linkFrame.resolve(link).sub) {
                    private static final long serialVersionUID = 1024137371118887935L;

                    public void linkStatus(boolean value) {
                        _linkVals.put(link, Boolean.valueOf(value));
                        instance(ACTIVITYGUARD.this);
                    }
                });
            }

            object(false, mlset);
        }
    }
    
    // Run the JP activity and continue advice sequence afterwards
    public void proceed(final PROCEED proceedActivity) {
    	// this.runActivity(activity);
    	ActivityExecStartEvent aese = new ActivityExecStartEvent();
        sendEvent(aese);
        
        // AO4ODE: Send BeforeInvoke Event
        if(_oactivity.getType().equals("OInvoke")) {                	
        	InvokeExecStartEvent iese = new InvokeExecStartEvent((OInvoke)_oactivity);
        	sendEvent(iese);
        }
        
        // Run JP activity
    	instance(createActivity(activity));
    	
    	// Listen for completion of JP activity and continue sequence
    	object(new ParentScopeChannelListener(activity.parent) {
            private static final long serialVersionUID = -8564969265471906493L;

            public void compensate(OScope scope, SynchChannel ret) {
            	__log.info("PROCEED COMPENSATE");
                assert false;
            }

            public void completed(FaultData fault, Set<CompensationHandler> compensations) {
                BpelRuntimeContext nativeAPI = (BpelRuntimeContext)getExtension(BpelRuntimeContext.class);
                if (fault == null) {
                    // nativeAPI.completedOk();
                	__log.debug("PROCEED COMPLETED - OK");                	
                } else {
                    // nativeAPI.completedFault(fault);
                	__log.error("PROCEED COMPLETED - FAULT");
               	 	__log.error("PROCEED FAILED: " + fault.getExplanation());               	 	
                }
                
                // Continue advice sequence
                continueAdviceSequence(proceedActivity);
            }

            public void cancelled() {
            	__log.debug("PROCEED cancelled");
            	// Continue advice sequence
                continueAdviceSequence(proceedActivity);
            }

            public void failure(String reason, Element data) {
                // FaultData faultData = createFault(OFailureHandling.FAILURE_FAULT_NAME, oAdvice, reason);
                // this.completed(faultData, CompensationHandler.emptySet());
            	__log.error("PROCEED FAILURE");
            	
            	// Continue advice sequence
                continueAdviceSequence(proceedActivity);
            }
        });
    	
    }
    
    public void continueAdviceSequence(PROCEED proceedActivity) {
    	proceedActivity._self.parent.completed(null, CompensationHandler.emptySet());
    }
    
    
    // AO4ODE: Run the adviceActivity and continue the process
    protected void runAdviceActivity(ActivityInfo adviceActivity) {
    	ActivityExecStartEvent aese = new ActivityExecStartEvent();
        sendEvent(aese);
        
    	instance(createActivity(adviceActivity));
    	// Listen for completion of JP activity and continue sequence
    	object(new ParentScopeChannelListener(adviceActivity.parent) {
            private static final long serialVersionUID = -8583659265471906493L;

            public void compensate(OScope scope, SynchChannel ret) {
            	__log.info("ADVICE SEQUENCE COMPENSATE");
                assert false;
            }

            public void completed(FaultData fault, Set<CompensationHandler> compensations) {
                BpelRuntimeContext nativeAPI = (BpelRuntimeContext)getExtension(BpelRuntimeContext.class);
                if (fault == null) {
                    // nativeAPI.completedOk();
                	__log.debug("ADVICE SEQUENCE COMPLETED - OK");                	
                } else {
                    // nativeAPI.completedFault(fault);
                	__log.error("ADVICE SEQUENCE COMPLETED - FAULT");
               	 	__log.error("ADVICE SEQUENCE FAILED: " + fault.getExplanation());               	 	
                }
                
                // Continue process
                instance(new TCONDINTERCEPT(activity.parent));
            }

            public void cancelled() {
            	__log.debug("ADVICE SEQUENCE cancelled");
            	
            	// Continue process
            	instance(new TCONDINTERCEPT(activity.parent));
            }

            public void failure(String reason, Element data) {
                // FaultData faultData = createFault(OFailureHandling.FAILURE_FAULT_NAME, oAdvice, reason);
                // this.completed(faultData, CompensationHandler.emptySet());
            	__log.error("ADVICE SEQUENCE FAILURE");
            	
            	// Continue process
            	instance(new TCONDINTERCEPT(activity.parent));
            }
        });
    	
    }

    // AO4ODE: Run the activity and continue the process
    protected void runActivity(ActivityInfo activity) {
    	ActivityExecStartEvent aese = new ActivityExecStartEvent();
        sendEvent(aese);
        
        // AO4ODE: Send BeforeInvoke Event
        if(_oactivity.getType().equals("OInvoke")) {                	
        	InvokeExecStartEvent iese = new InvokeExecStartEvent((OInvoke)_oactivity);
        	sendEvent(iese);
        }
        
    	instance(createActivity(activity));
    	
    	instance(new TCONDINTERCEPT(activity.parent));
    }

    private boolean evaluateTransitionCondition(OExpression transitionCondition)
            throws FaultException {
        if (transitionCondition == null)
            return true;

        try {
            return getBpelRuntimeContext().getExpLangRuntime().evaluateAsBoolean(transitionCondition,
                    new ExprEvaluationContextImpl(_scopeFrame, getBpelRuntimeContext()));
        } catch (EvaluationException e) {
            String msg = "Error in transition condition detected at runtime; condition=" + transitionCondition;
            __log.error(msg,e);
            throw new InvalidProcessException(msg, e);
        }
    }

    /**
     * Evaluate an activity's join condition.
     * @return <code>true</code> if join condition evaluates to true.
     */
    private boolean evaluateJoinCondition() {
        // For activities with no link targets, the join condition is always satisfied.
        if (_oactivity.targetLinks.size() == 0)
            return true;

        // For activities with no join condition, an OR condition is assumed.
        if (_oactivity.joinCondition == null)
            return _linkVals.values().contains(Boolean.TRUE);

        try {
            return getBpelRuntimeContext().getExpLangRuntime().evaluateAsBoolean(_oactivity.joinCondition,
                    new ExprEvaluationContextImpl(null, null,_linkVals));
        } catch (Exception e) {
            String msg = "Unexpected error evaluating a join condition: " + _oactivity.joinCondition;
            __log.error(msg,e);
            throw new InvalidProcessException(msg,e);
        }
    }

    private static ACTIVITY createActivity(ActivityInfo activity, ScopeFrame scopeFrame, LinkFrame linkFrame) {
    	return __activityTemplateFactory.createInstance(activity.o,activity, scopeFrame, linkFrame);    	
    }

    private ACTIVITY createActivity(ActivityInfo activity) {
    	return createActivity(activity,_scopeFrame, _linkFrame);
    }

    private void startGuardedActivity() {
        ActivityInfo activity = new ActivityInfo(genMonotonic(),_self.o,_self.self, newChannel(ParentScopeChannel.class));
        instance(createActivity(activity));
        instance(new TCONDINTERCEPT(activity.parent));
    }
    
    /**
     * Intercepts the
     * {@link ParentScopeChannel#completed(org.apache.ode.bpel.runtime.channels.FaultData, java.util.Set<org.apache.ode.bpel.runtime.CompensationHandler>)}
     * call, to evaluate transition conditions before returning to the parent.
     */
    private class TCONDINTERCEPT extends BpelJacobRunnable {
        private static final long serialVersionUID = 4014873396828400441L;
        ParentScopeChannel _in;

        public TCONDINTERCEPT(ParentScopeChannel in) {
            _in = in;
        }

        public void run() {
            object(new ParentScopeChannelListener(_in) {
                private static final long serialVersionUID = 2667359535900385952L;

                public void compensate(OScope scope, SynchChannel ret) {
                    _self.parent.compensate(scope,ret);
                    instance(TCONDINTERCEPT.this);
                }

                public void completed(FaultData faultData, Set<CompensationHandler> compensations) {
                    sendEvent(new ActivityExecEndEvent());
                    
                    if (faultData != null) {
                        dpe(_oactivity.sourceLinks);
                        _self.parent.completed(faultData, compensations);
                    } else {
                        FaultData fault = null;
                        for (Iterator<OLink> i = _oactivity.sourceLinks.iterator();i.hasNext();) {
                            OLink olink = i.next();
                            LinkInfo linfo = _linkFrame.resolve(olink);
                            try {
                                boolean val = evaluateTransitionCondition(olink.transitionCondition);
                                linfo.pub.linkStatus(val);
                            } catch (FaultException e) {
                                linfo.pub.linkStatus(false);
                                __log.error(e);
                                if (fault == null)
                                    fault = createFault(e.getQName(),olink.transitionCondition);
                            }
                        }
                        _self.parent.completed(fault, compensations);
                    }
                }

                public void cancelled() {
                    sendEvent(new ActivityExecEndEvent());
                    dpe(_oactivity.outgoingLinks);
                    dpe(_oactivity.sourceLinks);
                    // Implicit scope can tell the difference between cancelled and completed.
                    _self.parent.cancelled();
                }
                
                private OFailureHandling getFailureHandling() {
                    if (_oactivity instanceof OInvoke) {
                        OInvoke _oinvoke = (OInvoke) _oactivity;
                        OFailureHandling f = getBpelRuntimeContext().getConfigForPartnerLink(_oinvoke.partnerLink).failureHandling;
                        if (f != null) return f;
                    }
                    return _oactivity.getFailureHandling();
                }

                public void failure(String reason, Element data) {
                    if (_failure == null)
                        _failure = new ActivityFailure();
                    _failure.dateTime = new Date();
                    _failure.reason = reason;
                    _failure.data = data;

                    OFailureHandling failureHandling = getFailureHandling();
                    if (failureHandling != null && failureHandling.faultOnFailure && _failure.retryCount >= failureHandling.retryFor) {
                        //Fault after retries (may be 0)
                        if (__log.isDebugEnabled())
                            __log.debug("ActivityRecovery: Activity " + _self.aId + " faulting on failure");
                        FaultData faultData = createFault(OFailureHandling.FAILURE_FAULT_NAME, _oactivity, reason);
                        completed(faultData, CompensationHandler.emptySet());
                        return;
                    }
                    if (failureHandling == null || _failure.retryCount >= failureHandling.retryFor) {
                        requireRecovery();
                        return;
                    }
        
                    if (__log.isDebugEnabled())
                        __log.debug("ActivityRecovery: Retrying activity " + _self.aId);
                    Date future = new Date(new Date().getTime() + 
                        (failureHandling == null ? 0L : failureHandling.retryDelay * 1000));
                    final TimerResponseChannel timerChannel = newChannel(TimerResponseChannel.class);
                    getBpelRuntimeContext().registerTimer(timerChannel, future);
                    object(false, new TimerResponseChannelListener(timerChannel) {
                        private static final long serialVersionUID = -261911108068231376L;
                            public void onTimeout() {
                                ++_failure.retryCount;
                                startGuardedActivity();
                            }
                            public void onCancel() {
                                requireRecovery();
                            }
                    });
                }

                private void requireRecovery() {
                    if (__log.isDebugEnabled())
                        __log.debug("ActivityRecovery: Activity " + _self.aId + " requires recovery");
                    sendEvent(new ActivityFailureEvent(_failure.reason));
                    final ActivityRecoveryChannel recoveryChannel = newChannel(ActivityRecoveryChannel.class);
                    getBpelRuntimeContext().registerActivityForRecovery(
                        recoveryChannel, _self.aId, _failure.reason, _failure.dateTime, _failure.data,
                        new String[] { "retry", "cancel", "fault" }, _failure.retryCount);
                    object(false, new ActivityRecoveryChannelListener(recoveryChannel) {
                        private static final long serialVersionUID = 8397883882810521685L;
                        public void retry() {
                            if (__log.isDebugEnabled())
                                __log.debug("ActivityRecovery: Retrying activity " + _self.aId + " (user initiated)");
                            sendEvent(new ActivityRecoveryEvent("retry"));
                            getBpelRuntimeContext().unregisterActivityForRecovery(recoveryChannel);
                            ++_failure.retryCount;
                            startGuardedActivity();
                        }
                        public void cancel() {
                            if (__log.isDebugEnabled())
                                __log.debug("ActivityRecovery: Cancelling activity " + _self.aId + " (user initiated)");
                            sendEvent(new ActivityRecoveryEvent("cancel"));
                            getBpelRuntimeContext().unregisterActivityForRecovery(recoveryChannel);
                            cancelled();
                        }
                        public void fault(FaultData faultData) {
                            if (__log.isDebugEnabled())
                                __log.debug("ActivityRecovery: Faulting activity " + _self.aId + " (user initiated)");
                            sendEvent(new ActivityRecoveryEvent("fault"));
                            getBpelRuntimeContext().unregisterActivityForRecovery(recoveryChannel);
                            if (faultData == null)
                                faultData = createFault(OFailureHandling.FAILURE_FAULT_NAME, _self.o, _failure.reason);
                            completed(faultData, CompensationHandler.emptySet());
                        }
                    }.or(new TerminationChannelListener(_self.self) {
                        private static final long serialVersionUID = 2148587381204858397L;

                        public void terminate() {
                            if (__log.isDebugEnabled())
                                __log.debug("ActivityRecovery: Cancelling activity " + _self.aId + " (terminated by scope)");
                            getBpelRuntimeContext().unregisterActivityForRecovery(recoveryChannel);
                            cancelled();
                        }
                    }));
                }
            });

        }
    }

    static class ActivityFailure implements Serializable {
        private static final long serialVersionUID = 1L;

        Date    dateTime;
        String  reason;
        Element data;
        int     retryCount;
    }
    
}
