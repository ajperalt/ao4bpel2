package de.tud.stg.bpel.ao4ode.prolog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;


/**
 * This class is the heart of the BPEL Prolog Engine. 
 * Should be accessed through the interface.
 * @author Philipp Zuehlke, A. Look
 * 
 * AO4ODE: Removed policy specific code
 * 
 */
public class BPELPrologEngine implements IBPELPrologEngine {
	/**a reference to the tuProlog engine*/
	private Prolog engine = new Prolog();
	private final Log log = LogFactory.getLog(BPELPrologEngine.class);
			
//	Example for defining new clauses:
//	private final static String theories = 
//		"invoke(ProcessID,Token,Timestamp,Params):-invoke(ProcessID,Token,Timestamp,Params,false).\n" +
//		"invoke_req(ProcessID,Token,Timestamp,Params):-invoke(ProcessID,Token,Timestamp,Params,true).\n" +
//		"hasbeen_invoked(ProcessID,Token,Timestamp):-invoke(ProcessID,Token,Timestamp,_),invoke_req(ProcessID,Token,Timestamp,_).\n"
//	;
	
	/**
	 * Constructs an instance of this class
	 * @param addConstistencyPolicies When true, policies concerning the consistency are added to the list of policies
	 */
	public BPELPrologEngine()
	{
//		Example for defining new clauses:
//		try {
//			engine.addTheory(new Theory(theories));
//		} catch (InvalidTheoryException e) {
//			e.printStackTrace();
//		}
		
	}
	
	/**
	 * This method adds a single fact to the prolog database
	 * @param fact the fact to add
	 */
	private void addFact(Term fact)
	{
		try {
			Struct struct = new Struct();
			struct.append(fact);
			engine.addTheory(new Theory(struct));
		} catch (InvalidTheoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NullPointerException npe) {
			log.error("NULL POINTER EXCEPTION: ADD FACT");
			npe.printStackTrace();
		}
	}

	/**
	 * Adds all static process facts to the BPEL Prolog Engine. It is possible to add several 
	 * StaticProcessFacts as long as their DefinitionID differs
	 * @param spf the instance which holds the static process facts
	 */
	// @Override
	public void addStaticProcessFacts(IStaticProcessFactGenerator spf) {
		Struct struct = spf.getStaticProcessStruct();
		if(struct != null)
			addFact(struct);
		else
			log.error("STATIC PROCESS STRUCT IS NULL!");
	}


	/**
	 * Will be called when a new BPEL process is created
	 * @param definitionID The unique ID of the corresponding static process facts
	 * @param processID unique ID of this process. All activities of this process share this id
	 * @param timestamp The creation time
	 * @param request True, activity is requested but not yet checked, false, activity will be executed and granted.
	 */
	// @Override
	public void addCreateProcessInstance(String definitionID, String processID, long timestamp, boolean request) {
		Term createprocess = new Struct(
				"create_process", 
				new Struct(definitionID),
				new Struct(processID),
				new alice.tuprolog.Long(timestamp),
				new Struct(new Boolean(request).toString())
		);
		
		addFact(createprocess);
		
	}

	/**
	 * Will be called when a BPEL process terminates/ends
	 * @param processID unique ID of this process. All activities of this process share this id
	 * @param timestamp The destruction time
	 * @param request True, activity is requested but not yet checked, false, activity will be executed and granted.
	 */
	// @Override
	public void addDestroyProcessInstance(String processID, long timestamp, boolean request) {
		Term destroyprocess = new Struct(
				"destroy_process", 
				new Struct(processID),
				new alice.tuprolog.Long(timestamp),
				new Struct(new Boolean(request).toString())
		);
		
		addFact(destroyprocess);
	}

	/**
	 * Will be invoked when an Invoke activity response is received.
	 * @param processID unique ID of the process
	 * @param token unique id of BPEL activity
	 * @param timestamp The invocation time
	 * @param returnVal the returned value
	 * @param request True, response is requested but not yet checked, false, response is granted.
	 * */
	// @Override
	public void addEndInvoke(String processID, String token, long timestamp,
			String returnVal, boolean request) {
		Term end_invoke = new Struct(
				"end_invoke", 
				new Struct(processID),
				new Struct(token), 
				new alice.tuprolog.Long(timestamp),
				new Struct(returnVal),
				new Struct(new Boolean(request).toString())
			);
			
		
		addFact(end_invoke);
	}

	/**
	 * Will be invoked when an Invoke activity is requested or executed.
	 * @param processID unique ID of the process
	 * @param token unique id of BPEL activity
	 * @param timestamp The invocation time
	 * @param params Array of string parameters
	 * @param request True, activity is requested but not yet checked, false, activity will be executed and granted.
	 * */
	// @Override
	public void addInvoke(String processID, String token, long timestamp,
			String[] params, boolean request) {
		Struct list = new Struct();
		for( String s : params )
			list.append(new Struct(s));
		
		Term invoke = new Struct(
				"invoke", 
				new Struct(processID),
				new Struct(token), 
				new alice.tuprolog.Long(timestamp),
				list,
				new Struct(new Boolean(request).toString())
			);
			
		
		addFact(invoke);
	}
	
	
	// AO4ODE: Generic activity event fact
	public void addEvent(Long processInstanceId, String token, long timestamp,
				long activityId, int activityDeclarationId, String activityName,
				String activityType, String eventName, String eventType) {
		
		Term event = new Struct("event", new Term[]{
				
				new Struct(processInstanceId+""),
				new Struct(token), 
				new alice.tuprolog.Long(timestamp),
				new alice.tuprolog.Long(activityId),
				new alice.tuprolog.Int(activityDeclarationId),
				new Struct(activityName),
				new Struct(activityType),
				new Struct(eventName),
				new Struct(eventType)
				
		});
			
		
		addFact(event);
	}

	/**
	 * Will be invoked whenever a variable is read.
	 * @param processID unique ID of the process
	 * @param token unique id of BPEL activity
	 * @param timestamp The invocation time
	 * @param varName The name of the variable which is read
	 * @param request True, reading is requested but not yet checked, false, reading of the variable is granted.
	 */
	// @Override
	public void addGetVar(String processID, String token, long timestamp,
			String varName, boolean request) {
		Term get_var = new Struct(
				"get_var", 
				new Struct(processID),
				new Struct(token), 
				new alice.tuprolog.Long(timestamp),
				new Struct(varName),
				new Struct(new Boolean(request).toString())
			);
			
		addFact(get_var);
	}


	/**
	 * Will be invoked whenever a variable has changed. 
	 * @param processID unique ID of the process
	 * @param token unique id of BPEL activity
	 * @param timestamp The invocation time
	 * @param varName The name of the variable which is set
	 * @param value the new value of the variable
	 * @param request True, setting is requested but not yet checked, false, setting of the variable is granted.
	 */
	// @Override
	public void addSetVar(String processID, String token, long timestamp,
			String varName, String value, boolean request) {
		Term end_invoke = new Struct(
				"set_var", 
				new Struct(processID),
				new Struct(token), 
				new alice.tuprolog.Long(timestamp),
				new Struct(varName),
				new Struct(value),
				new Struct(new Boolean(request).toString())
			);
			
		addFact(end_invoke);
	}
	
	public void solve(Query query, String pid) throws MalformedQueryException {
		
		String q = query.getQuery();
		
		// Replace ProcessID with pid
		if(pid != null) {
			q = q.replaceAll("ProcessID", "'" + pid + "'");
		}
		
		try {
			SolveInfo si = engine.solve(q);
			if(si.isSuccess()) { // pointcut matches 
				// TODO: do something...
			}
		} catch (MalformedGoalException e) {
			//just throw our exception class
			throw new MalformedQueryException(query);
		}
	}
	
	/**
	 * Debug method
	 * @param str The query to solve
	 * @return The result of the query
	 * @throws MalformedGoalException Exception, when the query was invalid
	 */
	public SolveInfo solve(String str) throws MalformedGoalException {
		return engine.solve(str);
	}
	
	/**
	 * DEBUG METHOD
	 * Prints all facts in the database to the screen
	 */
	public void printFacts()
	{
		log.info(engine.getTheory());
	}

	// @Override
	public void addRule(String rule) {
		try {
			engine.addTheory(new Theory(rule));
		} catch (InvalidTheoryException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see de.tud.stg.bpel.ao4ode.prolog.IBPELPrologEngine#removeFactsForProcess(java.lang.Long)
	 */
	public void removeFactsForProcess(Long pid) {
		// engine.clearTheory();
		try {
			log.debug("Removing process facts for process: " + pid);
			engine.solve("retractall(get_var('"+pid+"',_,_,_,_)).");
			engine.solve("retractall(set_var('"+pid+"',_,_,_,_,_)).");
			engine.solve("retractall(invoke('"+pid+"',_,_,_,_)).");
			engine.solve("retractall(end_invoke('"+pid+"',_,_,_,_)).");
			engine.solve("retractall(create_process(_,'"+pid+"',_,_)).");
			engine.solve("retractall(destroy_process('"+pid+"',_,_)).");
		} catch (MalformedGoalException e) {
			e.printStackTrace();
		}
	}

	

}