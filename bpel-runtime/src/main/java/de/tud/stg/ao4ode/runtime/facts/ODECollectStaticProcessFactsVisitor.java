package de.tud.stg.ao4ode.runtime.facts;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.o.ASTVisitor;
import org.apache.ode.bpel.o.OActivity;
import org.apache.ode.bpel.o.OAssign;
import org.apache.ode.bpel.o.OBase;
import org.apache.ode.bpel.o.OCatch;
import org.apache.ode.bpel.o.OFaultHandler;
import org.apache.ode.bpel.o.OFlow;
import org.apache.ode.bpel.o.OInvoke;
import org.apache.ode.bpel.o.OProcess;
import org.apache.ode.bpel.o.OScope;
import org.apache.ode.bpel.o.OSequence;
import org.apache.ode.bpel.o.OSwitch;

import de.tud.stg.ao4ode.facts.StaticActivityFact;
import de.tud.stg.ao4ode.facts.StaticInvokeFact;


/**
 * Visitor that collects the static process facts by visiting
 * the ODE-AST.
 * 
 * @author A. Look
 *
 */
public class ODECollectStaticProcessFactsVisitor implements ASTVisitor {
	private static final Log log = LogFactory.getLog(ODECollectStaticProcessFactsVisitor.class);
	
	private Vector<StaticActivityFact> activities = new Vector<StaticActivityFact>();
	private Vector<StaticInvokeFact> invokes = new Vector<StaticInvokeFact>();
	
	
	public ODECollectStaticProcessFactsVisitor() {
		activities = new Vector<StaticActivityFact>();
		invokes = new Vector<StaticInvokeFact>();
	}

	public Iterable<StaticActivityFact> getActivites() {
		return activities;
	}

	public Iterable<StaticInvokeFact> getInvokes() {
		return invokes;
	}

	public void visit(OProcess oprocess) {
		// AST contains DefaultCompensationHandelrs etc.
		// We don't want to handle those!
		oprocess.procesScope.accept(this);
	}

	public void visit(OSequence osequence) {
		visit((OActivity)osequence);
		for(OActivity a : osequence.sequence) {
			a.accept(this);
		}
	}
	
	// In Apache ODE every invoke lives within its own scope
	// see: http://ode.apache.org/atomic-scopes-extension-for-bpel.html
	public void visit(OScope scope) {
		scope.activity.accept(this);
		scope.faultHandler.accept(this);
	}
	
	public void visit(OFaultHandler ofault) {
		for(OCatch c : ofault.catchBlocks) {
			c.accept(this);
		}
	}
	
	public void visit(OCatch ocatch) {
		ocatch.activity.accept(this);
	}

	public void visit(OActivity oactivity) {	
		String actId = oactivity.getXPath();
		if(actId != null) {
			log.debug("Adding static ACTIVITY fact: " + oactivity.toString() + " (" + oactivity.getXPath() + ")");
			
			// If the activity has no parent, is must be the root element
			String parentId = "process";
			if(oactivity.getParent() != null && oactivity.getParent().getXPath() != null) {
				parentId = oactivity.getParent().getXPath(); 
			}

			activities.add(new ODEStaticActivityFact(actId, parentId));
			
		}
		else {
			log.debug("Found OActivity without xpath: " + oactivity.getClass().getSimpleName());
		}
	}
	
	public void visit(OAssign oassign) {
		visit((OActivity)oassign);
		for(OAssign.Copy c : oassign.copy) {
			activities.add(new ODEStaticActivityFact(c.getXPath(), oassign.getXPath()));
		}
	}
			
	public void visit(OInvoke oinvoke) {

		log.debug("Adding static INVOKE fact: " + oinvoke.toString() + " (" + oinvoke.getXPath() + ")");
						
		invokes.add(new ODEStaticInvokeFact(oinvoke.getXPath(),
					// ODE wraps a Scope implicitly around Invokes, use parents parent.
					oinvoke.getParent().getParent().getXPath(),
					oinvoke.partnerLink.getName(),
					oinvoke.partnerLink.partnerRolePortType.getQName().toString(),
					oinvoke.operation.getName(),
					oinvoke.inputVar.name,
					(oinvoke.outputVar != null) ? oinvoke.outputVar.name : null));
	}
	
	// If statements sind als switch OSwitch implementiert
	public void visit(OSwitch oswitch) {
		log.debug("Adding static OSwitch fact: " + oswitch.toString() + " (" + oswitch.getXPath() + ")");
		visit((OActivity)oswitch);
		for(OSwitch.OCase c : oswitch.getCases()) {
			c.activity.accept(this);
		}
	}
	
	public void visit(OFlow oflow) {
		log.debug("Adding static OFlow fact: " + oflow.toString() + " (" + oflow.getXPath() + ")");
		visit((OActivity)oflow);
		for(OActivity a : oflow.parallelActivities) {
			a.accept(this);
		}
	}

	public void visit(OBase base) {
		log.debug("Unhandled OBase: " + base.getClass().getSimpleName());		
	}

	



}
