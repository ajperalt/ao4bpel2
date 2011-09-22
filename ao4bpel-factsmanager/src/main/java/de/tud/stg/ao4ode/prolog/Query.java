package de.tud.stg.ao4ode.prolog;

public class Query {
	
	private String name;
	private String faultName;
	private String query;
	
	public Query(String name, String faultName, String query) {
		this.name = name;
		this.faultName = faultName;
		this.query = query;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getFaultName() {
		return faultName;
	}
	public void setFaultName(String faultName) {
		this.faultName = faultName;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	
	@Override
	public String toString() {
		return "(" + name + ", " + faultName + ", " + query + ")";
	}

}
