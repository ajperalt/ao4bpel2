package de.tud.stg.ao4ode.facts;

/**
 * This fact should be added when a process has been registered
 * or started
 * 
 * Engines need to provide an implementation that
 * collects static process facts
 * 
 * @author A. Look
 *
 */
public interface BpelProcessFact extends DynamicFact {

	public String getName();

	public Iterable<StaticActivityFact> getStaticActivityFacts();
	
	public Iterable<StaticInvokeFact> getStaticInvokeFacts();

}
