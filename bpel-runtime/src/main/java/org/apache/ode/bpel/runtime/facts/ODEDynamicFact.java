package org.apache.ode.bpel.runtime.facts;
import java.io.Serializable;

import org.apache.ode.bpel.o.OBase;

import de.tud.stg.bpel.ao4ode.facts.DynamicFact;


public class ODEDynamicFact implements DynamicFact, Serializable {
	
	private static final long serialVersionUID = -1178719062670275682L;
	private OBase obase;

	public ODEDynamicFact(OBase obase) {
		this.obase = obase;
	}

	public String getXPath() {
		// FIXME: obase should never be null!
		if(obase != null)
			return obase.getXPath();
		else
			return "UNKNOWN XPATH!";
	}
	
	@Override
	public String toString() {
		return "(ODEDynamicFact: " + getXPath() + ", " + obase.getId()  + ", " + obase.getClass().getName() + ")";
	}

}
