package de.tud.stg.ao4ode.facts;

/**
 * The fact for invoke activities
 * 
 * @author A. Look
 *
 */
public interface InvokeFact extends DynamicFact {
	
	public String[] getParameters();

	/**
	 * @return null for one-way invokes!
	 */
	public String getResponse();

}
