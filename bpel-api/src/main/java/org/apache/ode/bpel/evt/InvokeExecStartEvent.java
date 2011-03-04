package org.apache.ode.bpel.evt;

import org.apache.ode.bpel.o.OInvoke;

// AO4ODE
public class InvokeExecStartEvent extends ActivityExecStartEvent {

	private static final long serialVersionUID = -5624598597564210262L;
	private OInvoke oinvoke;
	
	public InvokeExecStartEvent(OInvoke _oinvoke) {
		this.setOinvoke(_oinvoke);
	}

	public void setOinvoke(OInvoke oinvoke) {
		this.oinvoke = oinvoke;
	}

	public OInvoke getOinvoke() {
		return oinvoke;
	}
	

}
