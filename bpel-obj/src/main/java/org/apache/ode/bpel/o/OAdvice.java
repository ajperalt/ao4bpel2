package org.apache.ode.bpel.o;

public class OAdvice extends OProcess {

	public static enum TYPE {UNKOWN, BEFORE, AFTER, AROUND}
	private static final long serialVersionUID = 5258360738412039062L;
	
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

}
