package org.apache.ode.bpel.o;

import java.util.List;

import org.apache.ode.bpel.o.OScope.Variable;

public class OAdvice extends OProcess {
	
	private static final long serialVersionUID = 5258360738412039062L;
	
	public static enum TYPE {UNKOWN, BEFORE, AFTER, AROUND}
	private TYPE type = TYPE.UNKOWN;
	private OAspect oaspect;
	private Variable inputVar;
	private Variable outputVar;
	private OActivity jpActivity;
	private Long processId;
	
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
	
	public OJPVarType getOJPVarType( OJPVarType.Type type) {
		List<OBase> children = this.getChildren();
		for(OBase child : children) {
			if(child instanceof OJPVarType
					&& ((OJPVarType)child).getType() == type)
				return (OJPVarType)child;
		}
		return null;
	}

	public void setJPActivity(OActivity oactivity) {
		this.jpActivity = oactivity;
	}
	
	public OActivity getJPActivity() {
		return this.jpActivity;
	}
	
	public void setJPInVariable(Variable inputVar) {
		this.inputVar = inputVar;
	}
	
	public Variable getInputVar() {
		return inputVar;
	}

	public void setJPOutVariable(Variable outputVar) {
		this.outputVar = outputVar;
		
		// Link ThisJPOutVariable to outputVar
		OJPVarType thisjpout = this.getOJPVarType(OJPVarType.Type.OUT);
		// FIXME: What about other types?
		thisjpout.setVar((OMessageVarType)this.outputVar.type);		
	}
	
	public Variable getOutputVar() {
		return outputVar;
	}

	public void setProcessId(Long pid) {
		this.processId = pid;
	}
	
	public long getProcessId() {
		return this.processId;
	}

}
