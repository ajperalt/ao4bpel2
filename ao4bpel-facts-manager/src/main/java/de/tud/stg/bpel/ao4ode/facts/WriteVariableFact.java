package de.tud.stg.bpel.ao4ode.facts;

/**
 * Use this fact whenever a variable is set!
 * 
 * @author A. Look
 *
 */
public interface WriteVariableFact extends DynamicFact {

	String getVarName();

	String getNewValue();

}
