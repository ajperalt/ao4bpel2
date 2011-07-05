package de.tud.stg.ao4ode.aspectmanager;

import java.util.Date;

import org.apache.ode.bpel.o.OAspect;

public class AspectInfo {
	
	// Compiled representation of the aspect
	private OAspect oAspect;
	
	// Additional information
	private final Date deployDate;
	
	public AspectInfo(OAspect oAspect, Date deployDate) {
		this.oAspect = oAspect;
		this.deployDate = deployDate;
	}
	
	public OAspect getOAspect() {
		return oAspect;
	}
	
	public String toString() {
		return "Aspect deployed at " + deployDate + ": " + oAspect;
	}
	 

}
