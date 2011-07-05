package org.apache.ode.bpel.o;

import org.apache.ode.bpel.o.OAdvice.TYPE;

public class OAdvice extends OProcess {
	
	private static final long serialVersionUID = 1L;
	
	public static enum TYPE {UNKOWN, BEFORE, AFTER, AROUND}
	private TYPE type = TYPE.UNKOWN;
	
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

}
