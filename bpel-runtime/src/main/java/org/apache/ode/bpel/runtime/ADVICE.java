package org.apache.ode.bpel.runtime;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.o.OAdvice;
import org.apache.ode.bpel.o.OAspect;
import org.apache.ode.bpel.o.OProcess;
import org.apache.ode.bpel.o.OScope;
import org.apache.ode.bpel.runtime.channels.FaultData;
import org.apache.ode.bpel.runtime.channels.ParentScopeChannel;
import org.apache.ode.bpel.runtime.channels.ParentScopeChannelListener;
import org.apache.ode.bpel.runtime.channels.TerminationChannel;
import org.apache.ode.jacob.SynchChannel;
import org.w3c.dom.Element;

import de.tud.stg.ao4ode.runtime.AspectManager;

public class ADVICE extends PROCESS {
	
	private static final Log __log = LogFactory.getLog(ADVICE.class);
	
	private ACTIVITYGUARD activityGuard;
	private ACTIVITY joinPointActivity;
	private OAdvice oAdvice;
	private ScopeFrame scopeFrame;
	private LinkFrame linkFrame;	

	public ADVICE(ACTIVITYGUARD activityguard, OAdvice oAdvice, ACTIVITY jointPointActivity, ScopeFrame _scopeFrame, LinkFrame _linkFrame) {		
		super(oAdvice);
		this.activityGuard = activityguard;
		this.joinPointActivity = jointPointActivity;
		this.oAdvice = oAdvice;
		this.scopeFrame = scopeFrame;
		this.linkFrame = linkFrame;
	}

	private static final long serialVersionUID = -3792083256538970881L;
	
	@Override
	public void run() {
		
		__log.debug("Running ADVICE: " + this.toString());		
		
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
        SCOPE scope = new SCOPE(child, adviceScopeFrame, adviceLinkFrame);
        
        __log.debug("Creating instance of new Scope");
        __log.debug(scope.toString());
        
        // RUN ADVICE
        instance(scope);
        
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
                
                // If this advice is a before advice, proceed
                /*
                if(oAdvice.getType() == OAdvice.TYPE.BEFORE)
                	proceed();
                */
                
            }

            public void cancelled() {
                // this.completed(null, CompensationHandler.emptySet());
            	__log.debug("ADVICE cancelled");
            	// proceed();
            }

            public void failure(String reason, Element data) {
                // FaultData faultData = createFault(OFailureHandling.FAILURE_FAULT_NAME, oAdvice, reason);
                // this.completed(faultData, CompensationHandler.emptySet());
            	__log.error("ADVICE FAILURE");
            	// proceed();
            }
        });
    }
	
	/*
	public void proceed() {
		__log.debug("PROCEEDING!");
		activityGuard.runActivity(joinPointActivity._self);
	}
	*/

	public ACTIVITY getJoinPointActivity() {
		return joinPointActivity;
	}

	public OProcess getoAdvice() {
		return oAdvice;
	}
	
	@Override
	public String toString() {
		return "ADVICE: ["			
			+ this.getJoinPointActivity()._self.getO()
			+ this.getoAdvice()
			+ "]";
	}

}
