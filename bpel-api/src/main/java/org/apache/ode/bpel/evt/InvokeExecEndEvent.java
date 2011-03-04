package org.apache.ode.bpel.evt;

import org.apache.ode.bpel.o.OInvoke;
import org.w3c.dom.Element;

public class InvokeExecEndEvent extends ActivityExecEndEvent {

	private static final long serialVersionUID = -5624598597564210262L;
	private OInvoke oinvoke;
	private Element response;
	
	public InvokeExecEndEvent(OInvoke _oinvoke, Element response) {
		this.setOinvoke(_oinvoke);
		this.setResponse(response);
	}

	public void setOinvoke(OInvoke oinvoke) {
		this.oinvoke = oinvoke;
	}

	public OInvoke getOinvoke() {
		return oinvoke;
	}

	public void setResponse(Element response) {
		this.response = response;
	}

	public Element getResponse() {
		return response;
	}
	

}