package org.apache.ode.bpel.o;

import java.util.HashSet;
import java.util.Set;

// TODO: Compile aspect
public class OAspect {
	
	private static final long serialVersionUID = -8267184529496027966L;
	
	private OAdvice oAdvice = null;
	private Set<String> pointcuts = new HashSet<String>();
	
	public void setoAdvice(OAdvice oAdvice) {
		this.oAdvice = oAdvice;
	}

	public OAdvice getOAdvice() {
		return oAdvice;
	}

	public void setPointcuts(Set<String> pointcuts) {
		this.pointcuts = pointcuts;
	}

	public Set<String> getPointcuts() {
		return pointcuts;
	}
	
}
