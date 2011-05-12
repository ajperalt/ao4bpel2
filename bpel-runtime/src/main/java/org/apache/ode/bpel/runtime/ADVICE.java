package org.apache.ode.bpel.runtime;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.o.OAdvice;
import org.apache.ode.bpel.o.OProcess;
import org.apache.ode.bpel.o.OScope;
import org.apache.ode.bpel.runtime.channels.FaultData;
import org.apache.ode.bpel.runtime.channels.ParentScopeChannel;
import org.apache.ode.bpel.runtime.channels.ParentScopeChannelListener;
import org.apache.ode.bpel.runtime.channels.TerminationChannel;
import org.apache.ode.jacob.SynchChannel;
import org.w3c.dom.Element;

public class ADVICE extends PROCESS {
	
	private static final Log __log = LogFactory.getLog(ADVICE.class);
	
	private ACTIVITYGUARD activityGuard;
	private ActivityInfo joinPointActivity;
	private OAdvice oAdvice;

	public ADVICE(ACTIVITYGUARD activityguard, OAdvice oAdvice, ActivityInfo jointPointActivity) {		
		super(oAdvice);
		this.activityGuard = activityguard;
		this.joinPointActivity = jointPointActivity;
		this.oAdvice = oAdvice;
	}

	private static final long serialVersionUID = -3792083256538970881L;
	
	@Override
	public void run() {		
        BpelRuntimeContext ntive = getBpelRuntimeContext();
        Long scopeInstanceId = ntive.createScopeInstance(null, oAdvice.procesScope);
        
        /* TODO: Send AdviceExecutionStartedEvent
        AdviceExecutionStartedEvent evt = new AdviceExecutionStartedEvent();
        evt.setRootScopeId(scopeInstanceId);
        evt.setScopeDeclarationId(oAdvice.procesScope.getId());
        ntive.sendEvent(evt);
        */
                
        ActivityInfo child = new ActivityInfo(genMonotonic(),
        		oAdvice.procesScope,
            newChannel(TerminationChannel.class),
            newChannel(ParentScopeChannel.class));
        
        ScopeFrame adviceScopeFrame = new ScopeFrame(oAdvice.procesScope, scopeInstanceId, null, null);
        LinkFrame adviceLinkFrame = new LinkFrame(null);
        
        // Run advice
        __log.info("RUNNING BEFORE ADVICE");
        instance(new SCOPE(child, adviceScopeFrame, adviceLinkFrame));
        
        object(new ParentScopeChannelListener(child.parent) {
            private static final long serialVersionUID = -8564969578471906493L;

            public void compensate(OScope scope, SynchChannel ret) {
            	__log.info("ADVICE COMPENSATE");
                assert false;
            }

            public void completed(FaultData fault, Set<CompensationHandler> compensations) {
                BpelRuntimeContext nativeAPI = (BpelRuntimeContext)getExtension(BpelRuntimeContext.class);
                if (fault == null) {
                    // nativeAPI.completedOk();
                	__log.error("ADVICE COMPLETED - OK");                	
                } else {
                    // nativeAPI.completedFault(fault);
                	__log.error("ADVICE COMPLETED - FAULT");
               	 	__log.error("ADVICE FAILED: " + fault.getExplanation());
                }
                
                proceed();
                
            }

            public void cancelled() {
                // this.completed(null, CompensationHandler.emptySet());
            	__log.debug("ADVICE cancelled");
            	proceed();
            }

            public void failure(String reason, Element data) {
                // FaultData faultData = createFault(OFailureHandling.FAILURE_FAULT_NAME, oAdvice, reason);
                // this.completed(faultData, CompensationHandler.emptySet());
            	__log.error("ADVICE FAILURE");
            	proceed();
            }
        });
    }
	
	private void proceed() {
		activityGuard.runActivity(joinPointActivity);
	}

	public ActivityInfo getJoinPointActivity() {
		return joinPointActivity;
	}

	public OProcess getoAdvice() {
		return oAdvice;
	}

}
