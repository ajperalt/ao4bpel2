package org.apache.ode.bpel.o;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.InputSource;

// AO4ODE OPointcut
public class OPointcut extends OBase {
	
	private static final long serialVersionUID = 1L;
	
	private String name;
	private String language = "prolog";
	private String query;
	private String originalLanguage;
	private String originalQuery;
	
	public OPointcut(OProcess _owner, String name, String language, String query) {
		super(_owner);
		this.name = name;
		if(language != null)
			this.language = language;
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

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getLanguage() {
		return language;
	}

	public void setOriginalLanguage(String originalLanguage) {
		this.originalLanguage = originalLanguage;
	}

	public String getOriginalLanguage() {
		return originalLanguage;
	}

	public void setOriginalQuery(String originalQuery) {
		this.originalQuery = originalQuery;
	}

	public String getOriginalQuery() {
		return originalQuery;
	}
		

}
