package de.tud.stg.ao4ode.runtime.facts;

import org.apache.ode.bpel.o.OProcess;

import de.tud.stg.ao4ode.facts.BpelProcessFact;
import de.tud.stg.ao4ode.facts.StaticActivityFact;
import de.tud.stg.ao4ode.facts.StaticInvokeFact;


/**
 * Object-Adapter for OProcess
 * 
 * @author A. Look
 *
 */
public class ODEBpelProcessFact extends ODEDynamicFact implements BpelProcessFact {
	
	private static final long serialVersionUID = -1094807540931139457L;
	private OProcess oprocess;
	private ODECollectStaticProcessFactsVisitor psv; 
	
	public ODEBpelProcessFact( OProcess oprocess) {
		super(oprocess);
		this.oprocess = oprocess;
		psv = new ODECollectStaticProcessFactsVisitor();
		oprocess.accept(psv);
	}
	
	public String getName() {
		return oprocess.getName();
	}

	public Iterable<StaticActivityFact> getStaticActivityFacts() {
		return psv.getActivites();
	}

	public Iterable<StaticInvokeFact> getStaticInvokeFacts() {
		return psv.getInvokes();
	}
	
}
