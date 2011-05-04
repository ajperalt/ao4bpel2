package de.tud.stg.ao4ode.runtime.facts;

import org.apache.ode.bpel.o.OInvoke;
import org.apache.ode.bpel.runtime.INVOKE;
import org.w3c.dom.Element;

import de.tud.stg.ao4ode.facts.InvokeFact;


public class ODEInvokeFact extends ODEDynamicFact implements InvokeFact {

	private static final long serialVersionUID = 6155235595482152933L;
	private OInvoke oinvoke;
	private Element response = null;
	
	public ODEInvokeFact(OInvoke oinvoke, Element response) {
		this(oinvoke);
		this.response = response;
	}
	
	public ODEInvokeFact(OInvoke oinvoke) {
		super(oinvoke);
		this.oinvoke = oinvoke;
	}
	
	public String[] getParameters() {		
		return new String[] { oinvoke.inputVar.name };
	}

	public String getResponse() {
		if(response == null)
			return null;		
		return response.getTextContent();
	}

	public boolean isTwoWay() {
		return oinvoke.outputVar != null;		
	}

}
