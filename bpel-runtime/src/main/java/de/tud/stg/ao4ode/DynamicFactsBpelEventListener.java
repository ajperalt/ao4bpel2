package de.tud.stg.ao4ode;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.evt.ActivityEvent;
import org.apache.ode.bpel.evt.BpelEvent;
import org.apache.ode.bpel.evt.InvokeExecEndEvent;
import org.apache.ode.bpel.evt.InvokeExecStartEvent;
import org.apache.ode.bpel.evt.ProcessCompletionEvent;
import org.apache.ode.bpel.evt.ProcessInstanceStartedEvent;
import org.apache.ode.bpel.evt.VariableModificationEvent;
import org.apache.ode.bpel.evt.VariableReadEvent;
import org.apache.ode.bpel.iapi.BpelEventListener;
import org.apache.ode.bpel.runtime.facts.ODEBpelProcess;
import org.apache.ode.bpel.runtime.facts.ODEDynamicFact;
import org.apache.ode.bpel.runtime.facts.ODEInvokeFact;
import org.apache.ode.bpel.runtime.facts.ODEReadVariableFact;
import org.apache.ode.bpel.runtime.facts.ODEWriteVariableFact;

import de.tud.stg.bpel.ao4ode.BpelFactsManager;
import de.tud.stg.bpel.ao4ode.facts.BpelProcessFact;

public class DynamicFactsBpelEventListener implements BpelEventListener {
	
	private final Log log = LogFactory.getLog(DynamicFactsBpelEventListener.class);
	
	private BpelFactsManager bfm = BpelFactsManager.getInstance();
    
	/*
	 * (non-Javadoc)
	 * @see org.apache.ode.bpel.iapi.BpelEventListener#onEvent(org.apache.ode.bpel.evt.BpelEvent)
	 */
	public void onEvent(BpelEvent bpelEvent) {
		
		// TODO: Collect dynamic facts
		log.debug("AO4ODE_DEBUG_PROLOG_EVENT: " + BpelEvent.eventName(bpelEvent));
		
		// Record all ActivityEvents
		if(bpelEvent instanceof ActivityEvent) {
			ActivityEvent activityEvent = (ActivityEvent)bpelEvent;
			
			// ...but only those with xpath!
			if(activityEvent.getO().getXPath() != null) {

				bfm.dynamicEventFact(activityEvent.getProcessInstanceId(),
						new ODEDynamicFact(activityEvent.getO()),
						activityEvent.getActivityId(),
						activityEvent.getActivityDeclarationId(),
						activityEvent.getActivityName(),
						activityEvent.getActivityType(),
						BpelEvent.eventName(activityEvent),
						activityEvent.getType().toString()
						);
				
			}
		}
		
		// Before Invoke
		if(bpelEvent instanceof InvokeExecStartEvent) {
			InvokeExecStartEvent invokeEvent = (InvokeExecStartEvent)bpelEvent;
			bfm.beforeInvoke(invokeEvent.getProcessInstanceId(),
					new ODEInvokeFact(invokeEvent.getOinvoke()));
		}
		
		// After  Invoke
		else if(bpelEvent instanceof InvokeExecEndEvent) {
			InvokeExecEndEvent invokeEvent = (InvokeExecEndEvent)bpelEvent;
			bfm.afterInvoke(invokeEvent.getProcessInstanceId(),
					new ODEInvokeFact(invokeEvent.getOinvoke()));			
		}

		// Process execution start and completion events
		else if(bpelEvent instanceof ProcessInstanceStartedEvent) {
			ProcessInstanceStartedEvent processEvent = (ProcessInstanceStartedEvent)bpelEvent;			
			bfm.processStarted(processEvent.getProcessInstanceId(),
					new ODEBpelProcess(processEvent.getOProcess()));
			
		}
		
		else if(bpelEvent instanceof ProcessCompletionEvent) {
			ProcessCompletionEvent completionEvent = (ProcessCompletionEvent)bpelEvent;
			
			BpelProcessFact processFact = bfm.getProcess(completionEvent.getProcessInstanceId());
			if(processFact != null) {
				bfm.processEnded(completionEvent.getProcessInstanceId(), processFact);
			}
			
			log.debug("Process Completion Event");
			AO4ODEExecutionQueueImpl.RUN_ADVICE = true;
		}
		
		// Handle Variable read and write events
		else if(bpelEvent instanceof VariableReadEvent) {
			VariableReadEvent varEvent = (VariableReadEvent)bpelEvent;
			
			// Not all VariableReadEvent know their OActivity
			// f.e. see ExprEvaluationContextImpl
			if(varEvent.getO() != null) {
				bfm.getVar(varEvent.getProcessInstanceId(),
					new ODEReadVariableFact(varEvent.getO(),
							varEvent.getVarName()));
			}

		}
		
		else if(bpelEvent instanceof VariableModificationEvent) {
			VariableModificationEvent varEvent = (VariableModificationEvent)bpelEvent;
						
			bfm.setVar(varEvent.getProcessInstanceId(),
					new ODEWriteVariableFact(varEvent.getO(),
							varEvent.getVarName(),
							varEvent.getNewValue()));
		}
		
	}

	public void startup(Properties configProperties) {
		// TODO Auto-generated method stub
		
	}

	public void shutdown() {
		// TODO Auto-generated method stub
		
	}
	

}
