package de.tud.stg.ao4ode.prolog;

import alice.tuprolog.Theory;

/**
 * This exception is thrown when a rule is malformed/invalid
 * @author Philipp Zuehlke
 *
 */
public class MalformedTheoryException extends Exception {
	private static final long serialVersionUID = 1L;
	/**the malformed query*/
	private String theory;

	/**
	 * Constructs an exception for a query that is malformed
	 * @param query the invalid exception
	 */
	public MalformedTheoryException( String theory )
	{
		this.theory = theory;
	}
	
	/**
	 * Use to determine the malformed query
	 * @return the malformed query
	 */
	public String getMalformedTheory()
	{
		return theory;
	}
	
}