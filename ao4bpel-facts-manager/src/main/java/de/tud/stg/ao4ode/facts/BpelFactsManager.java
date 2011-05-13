package de.tud.stg.ao4ode.facts;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tud.stg.ao4ode.prolog.BPELPrologEngine;
import de.tud.stg.ao4ode.prolog.IBPELPrologEngine;
import de.tud.stg.ao4ode.prolog.IStaticProcessFactGenerator;
import de.tud.stg.ao4ode.prolog.MalformedQueryException;
import de.tud.stg.ao4ode.prolog.Query;
import de.tud.stg.ao4ode.prolog.StaticProcessFactGenerator;


/**
 * Controller to add static and dynamic facts to the
 * prolog database
 *  
 * @author A. Look
 *
 */
public class BpelFactsManager {
	
	private static BpelFactsManager instance = new BpelFactsManager();
	IBPELPrologEngine engine = new BPELPrologEngine();
	private final Log log = LogFactory.getLog(BpelFactsManager.class);
	private HashMap<Long,BpelProcessFact> processes = new HashMap<Long,BpelProcessFact>();
	
	private BpelFactsManager() {
	}
	
	public static BpelFactsManager getInstance() {				
		return instance;
	}
	
	public void registerProcess(BpelProcessFact process) {
				
		// get static facts from BpelProcess implementation		
		IStaticProcessFactGenerator ps = new StaticProcessFactGenerator(process.getName());
		
		for(StaticActivityFact act : process.getStaticActivityFacts()) {
			ps.addActivity(act.getXPath(), act.getParentXPath());
		}
		for(StaticInvokeFact inv : process.getStaticInvokeFacts()) {
			ps.addInvoke(inv.getXPath(),
					inv.getParentXPath(),
					inv.getPartnerLink(),
					inv.getPortType(),
					inv.getOperation(),
					inv.getInputVar(),
					inv.getOutputVar());
		}
		engine.addStaticProcessFacts(ps);

	}
	
	public void processStarted(Long pid, BpelProcessFact process) {
		log.debug("Adding process started fact: " + process.getName() + "(" + pid + ")");
		
		log.debug("Registering process instance: " + process.getName() + "(" + pid + ")");
		processes.put(pid, process);
		
		engine.addCreateProcessInstance(process.getName(),
				pid+"",
				System.currentTimeMillis(),
				true);
	}

	public void processEnded(Long pid, BpelProcessFact process) {
		log.debug("Adding process ended fact: " + process.getName() + "(" + pid + ")");
		
		log.debug("Removing process instance: " + process.getName() + "(" + pid + ")");
		processes.remove(pid);
		
		engine.addDestroyProcessInstance(pid+"",
				System.currentTimeMillis(),
				true);
				
		// Debug output:
		engine.printFacts();		
	}
	
	public void dynamicEventFact(Long processInstanceId,
			DynamicFact odeDynamicFact, long activityId,
			int activityDeclarationId, String activityName,
			String activityType, String eventName, String eventType) {
		
		engine.addEvent(
				processInstanceId,
				odeDynamicFact.getXPath(),
				System.currentTimeMillis(),
				activityId,
				activityDeclarationId,
				activityName,
				activityType,
				eventName,
				eventType);
		
	}
	
	public void beforeInvoke(Long pid, InvokeFact invoke) {
		log.debug("Adding invoke fact: " + invoke.getXPath() + "(" + pid + ")");
		// FIXME: Remove request parameter!
		engine.addInvoke(pid+"",				
				invoke.getXPath(),
				System.currentTimeMillis(),
				invoke.getParameters(),
				true);

	}
	
	public void afterInvoke(Long pid, InvokeFact invoke) {
		log.debug("Adding invoke_end fact: " + invoke.getXPath() + "(" + pid + ")");
		
		String response = invoke.getResponse();
		
		engine.addEndInvoke(pid+"",
			invoke.getXPath(),				 
			System.currentTimeMillis(),
			(response == null) ? "" : response,			
			true);
		
	}
	
	public void setVar(Long pid, WriteVariableFact writeVar) {
		engine.addSetVar(pid+"",
				writeVar.getXPath(),
				System.currentTimeMillis(),
				writeVar.getVarName(),
				writeVar.getNewValue(),
				true);	
	}
	
	public void getVar(Long pid, ReadVariableFact readVar) {
		
		engine.addGetVar(pid+"",
				readVar.getXPath(),
				System.currentTimeMillis(),
				readVar.getVarName(),
				true);
		
	}

	public BpelProcessFact getProcess(Long processInstanceId) {
		return processes.get(processInstanceId);
	}

	public boolean solve(String name, String faultName, String query, Long pid) {
		try {
		return engine.solve(new Query(name, faultName, query), pid+"");
		} catch (MalformedQueryException e) {		
			e.printStackTrace();
			return false;
		}
	}
	

}
