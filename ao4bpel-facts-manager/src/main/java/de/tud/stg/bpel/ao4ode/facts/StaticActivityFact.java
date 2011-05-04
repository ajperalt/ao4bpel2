package de.tud.stg.bpel.ao4ode.facts;

/**
 * Every static fact needs to provide his own and
 * parent's xpath 
 * 
 * @author A. Look
 *
 */
public interface StaticActivityFact {
	
	public String getXPath();
	
	public String getParentXPath();

}
