package org.apache.ode.bpel.o;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class OAspect implements Serializable {
	
	private static final long serialVersionUID = 5258360738412039062L;

    public String targetNamespace;
    
    public String aspectName;
    
	private String scope = null;
	
	private Set<OPointcut> pointcuts = new HashSet<OPointcut>();
	
	private OAdvice advice;
		
	public void addPointcut(OPointcut pointcut) {
		this.pointcuts.add(pointcut);
	}
	
	public Set<OPointcut> getPointcuts() {
		return pointcuts;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getScope() {
		return scope;
	}
	
	public OAdvice getOAdvice() {
		return advice;
	}

	public void setAdvice(OAdvice advice) {
		this.advice = advice;
	}

	public String toString() {
		return "[" + aspectName + ", " + scope + ", " +  pointcuts + "]";
	}
	
}