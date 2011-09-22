package de.tud.stg.ao4ode.facts;

/**
 * Static fact for Invoke activity 
 * 
 * @author A. Look
 *
 */
public interface StaticInvokeFact extends StaticActivityFact {
	
	public String getPartnerLink();
	
	public String getPortType();
	
	public String getOperation();
	
	public String getInputVar();
	
	public String getOutputVar();
	
}
