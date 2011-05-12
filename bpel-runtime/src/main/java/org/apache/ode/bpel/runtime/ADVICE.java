package org.apache.ode.bpel.runtime;

import org.apache.ode.bpel.o.OProcess;

public class ADVICE extends PROCESS {
	
	private ACTIVITY joinPointActivity;

	public ADVICE(OProcess process, ACTIVITY jointPointActivity) {		
		super(process);
		this.joinPointActivity = jointPointActivity;
	}

	private static final long serialVersionUID = -3792083256538970881L;
	
	@Override
	public void run() {
		
	}

}
