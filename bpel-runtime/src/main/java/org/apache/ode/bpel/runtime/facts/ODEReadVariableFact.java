package org.apache.ode.bpel.runtime.facts;

import org.apache.ode.bpel.o.OBase;
import org.apache.ode.bpel.runtime.VariableInstance;

import de.tud.stg.bpel.ao4ode.facts.ReadVariableFact;


public class ODEReadVariableFact extends ODEDynamicFact implements ReadVariableFact {

	private static final long serialVersionUID = 5477212048248084442L;
	private OBase src;
	private String varName;

	public ODEReadVariableFact(OBase src, String varName) {
		super(src);
		this.src = src;
		this.varName = varName;
	}

	public String getVarName() {
		return varName;
	}

	public String getXPath() {
		return src.getXPath();
	}

}
