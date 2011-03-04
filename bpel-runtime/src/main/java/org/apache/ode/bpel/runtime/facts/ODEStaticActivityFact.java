package org.apache.ode.bpel.runtime.facts;

import de.tud.stg.bpel.ao4ode.facts.StaticActivityFact;

public class ODEStaticActivityFact implements StaticActivityFact {
	
	private String token;
	private String parentToken;
	
	public ODEStaticActivityFact(String token, String parentToken) {
		this.token = token;
		this.parentToken = parentToken;
	}

	public String getParentXPath() {
		return parentToken;
	}

	public String getXPath() {
		return token;		
	}

}
