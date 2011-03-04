package org.apache.ode.bpel.runtime.facts;

import org.apache.ode.bpel.o.OBase;
import org.apache.ode.bpel.runtime.VariableInstance;
import org.apache.ode.utils.DOMUtils;
import org.w3c.dom.Node;

import de.tud.stg.bpel.ao4ode.facts.WriteVariableFact;


public class ODEWriteVariableFact extends ODEDynamicFact implements WriteVariableFact {
	
	private static final long serialVersionUID = -4074578630498222854L;
	private String varName;
	private Node response;

	public ODEWriteVariableFact(OBase src, String varName, Node response) {
		super(src);
		this.varName = varName;
		this.response = response;
	}

	public String getVarName() {
		return varName;
	}

	public String getNewValue() {
		return DOMUtils.domToString(response);
	}

}
