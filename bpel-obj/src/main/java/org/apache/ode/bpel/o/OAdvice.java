package org.apache.ode.bpel.o;

import java.util.List;

public class OAdvice extends OProcess {
	
	private static final long serialVersionUID = 5258360738412039062L;
	
	public static enum TYPE {UNKOWN, BEFORE, AFTER, AROUND}
	private TYPE type = TYPE.UNKOWN;
	private OAspect oaspect;
	
	public OAdvice(String bpelVersion) {
		super(bpelVersion);
	}
	
	public void setType(TYPE type) {
		this.type = type;
	}

	public TYPE getType() {
		return type;
	}
	
	public String toString() {
		return "AO4BPEL ADVICE [" + getType() + ", " + this.getName() + ", " + this.getId() + "]";
	}
	
	public OAspect getOAspect() {
		return oaspect;
	}

	public void setAspect(OAspect oaspect) {
		this.oaspect = oaspect;
	}

	public OProceed getOProceed() {
		List<OBase> children = this.getChildren();
		for(OBase child : children) {
			if(child instanceof OProceed)
				return (OProceed)child;
		}
		return null;
	}

}
