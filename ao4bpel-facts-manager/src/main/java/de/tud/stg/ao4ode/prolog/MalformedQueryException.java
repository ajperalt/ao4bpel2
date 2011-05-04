package de.tud.stg.ao4ode.prolog;

/**
 * This exception is thrown when a query is malformed/invalid
 * @author Philipp Zuehlke
 *
 */
public class MalformedQueryException extends Exception {
	private static final long serialVersionUID = 1L;
	/**the malformed query*/
	private Query query;

	/**
	 * Constructs an exception for a query that is malformed
	 * @param query the invalid exception
	 */
	public MalformedQueryException( Query query )
	{
		this.query = query;
	}
	
	/**
	 * Use to determine the malformed query
	 * @return the malformed query
	 */
	public Query getMalformedQuery()
	{
		return query;
	}
}
