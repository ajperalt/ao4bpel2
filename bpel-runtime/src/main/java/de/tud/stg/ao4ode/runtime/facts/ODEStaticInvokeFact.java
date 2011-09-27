package de.tud.stg.ao4ode.runtime.facts;

import de.tud.stg.ao4ode.facts.StaticInvokeFact;

/**
 * @author A. Look
 */
public class ODEStaticInvokeFact extends ODEStaticActivityFact implements StaticInvokeFact {

	private String partnerLink,	portType, operation, inputVar, outputVar;
	
	public ODEStaticInvokeFact(String token, String parentToken, String partnerLink,
			String portType, String operation, String inputVar, String outputVar) {

		super(token, parentToken);
		
		this.partnerLink = partnerLink;
		this.portType = portType;
		this.operation = operation;
		this.inputVar = inputVar;
		this.outputVar = outputVar;
		
		
	}

	public String getInputVar() {
		return inputVar;
	}

	public String getOperation() {
		return operation;
	}

	public String getOutputVar() {
		return outputVar;
	}

	public String getPartnerLink() {
		return partnerLink;
	}

	public String getPortType() {
		return portType;
	}

}
