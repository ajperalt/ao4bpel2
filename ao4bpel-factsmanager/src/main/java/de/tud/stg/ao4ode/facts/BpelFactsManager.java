package de.tud.stg.ao4ode.facts;

import java.util.HashMap;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import alice.tuprolog.InvalidTheoryException;

import de.tud.stg.ao4ode.prolog.BPELPrologEngine;
import de.tud.stg.ao4ode.prolog.IBPELPrologEngine;
import de.tud.stg.ao4ode.prolog.IStaticProcessFactGenerator;
import de.tud.stg.ao4ode.prolog.MalformedQueryException;
import de.tud.stg.ao4ode.prolog.MalformedTheoryException;
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
	private HashMap<QName,BpelProcessFact> processes = new HashMap<QName,BpelProcessFact>();
	private HashMap<Long,QName> processInstances = new HashMap<Long,QName>();
	
	private BpelFactsManager() {
		// TODO: Load rules from file
		try {
			engine.addRule("activity(X):-event(ProcessID,_,_,_,_,X,_,'ActivityEnabledEvent',_),not(event(ProcessID,_,_,_,_,X,_,'ActivityExecStartEvent',_)).");		
			engine.addRule("activity_id(X):-event(ProcessID,X,_,_,_,_,_,'ActivityEnabledEvent',_),not(event(ProcessID,X,_,_,_,_,_,'ActivityExecStartEvent',_)).");
			engine.addRule("variable_read(X):-get_var(_,ID,_,X,_),activity_id(ID).");
			engine.addRule("variable_write(X):-set_var(_,ID,_,X,_,_),activity_id(ID).");
			engine.addRule("s_invoke(ID,PARENTID,PARTNERLINK,PORTTYPE,OPERATION,INPUT,OUTPUT):-s_process(_,_,Invokes),member(s_invoke(ID,PARENTID,PARTNERLINK,PORTTYPE,OPERATION,INPUT,OUTPUT),Invokes).");
			engine.addRule("partner(X):-s_invoke(ID,_,X,_,_,_,_),activity_id(ID).");			
			engine.addRule("created_after(PID, T):-create_process(_,PID,CreateT,_),CreateT>T.");
		} catch (MalformedTheoryException e) {
			log.error("Invalid rule: " + e.getMessage());
			e.printStackTrace();
		}		
	}
		
	public static BpelFactsManager getInstance() {				
		return instance;
	}
	
	public void registerProcess(QName processId, BpelProcessFact process) {
				
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
		processes.put(processId, process);

	}
	
	public void processStarted(Long pid, QName processId) {
		
		BpelProcessFact process = processes.get(processId);
		log.debug("Adding process started fact: " + process.getName() + "(" + pid + ")");
				
		log.debug("Registering process instance: " + process.getName() + "(" + pid + ")");
		processInstances.put(pid, processId);
		
		engine.addCreateProcessInstance(process.getName(),
				pid+"",
				System.currentTimeMillis(),
				true);
	}

	public void processEnded(Long pid) {
		log.debug("Adding process ended fact: " + pid );
		
		log.debug("Removing process instance: " + pid );
		processInstances.remove(pid);
		
		engine.addDestroyProcessInstance(pid+"",
				System.currentTimeMillis(),
				true);
		
		// Debug output:
		engine.printFacts();
		
		engine.removeFactsForProcess(pid);
		
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
		return processes.get(processInstances.get(processInstanceId));
	}
	
	public boolean solve(String name, String faultName, String query) throws MalformedQueryException {
		return engine.solve(new Query(name, faultName, query), "-1");
	}

	public boolean solve(String name, String faultName, String query, Long pid) {
		
		log.debug("Solving " + name + ", " + faultName + ", " + pid + ": " + query);
		
		try {
		return engine.solve(new Query(name, faultName, query), pid+"");
		} catch (MalformedQueryException e) {		
			e.printStackTrace();
			return false;
		}
	}

	public void addRule(String rule) throws MalformedTheoryException {		
		engine.addRule(rule);
	}
	
	public String getTheory() {
		return engine.getTheory();
	}
	

}
