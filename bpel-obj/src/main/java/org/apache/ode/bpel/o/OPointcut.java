package org.apache.ode.bpel.o;

// AO4ODE OPointcut
public class OPointcut extends OBase {
	
	private static final long serialVersionUID = 1L;
	
	private String name;
	private String query;
	
	public OPointcut(OProcess _owner, String name, String query) {
		super(_owner);
		this.name = name;
		this.query = query;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	protected OPointcut(OProcess owner) {
		super(owner);
	}
	
	public String toString() {
		return "[" + name + ", " + query + "]";
	}
		

}
