package de.tud.stg.ao4ode.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.ode.bpel.o.OActivity;
import org.apache.ode.bpel.o.OAdvice;
import org.apache.ode.bpel.o.OSequence;

public class AdviceActivity {
	
	private OActivity oAdviceActivity;
	private Set<OAdvice> beforeAdvices;
	private Set<OAdvice> aroundAdvices;
	private Set<OAdvice> afterAdvices;

	public void setOAdviceActivity(OActivity oactivity) {
		this.oAdviceActivity = oactivity;
	}

	public void setBeforeAdvices(Set<OAdvice> beforeAdvices) {
		this.beforeAdvices = beforeAdvices;
	}

	public void setAroundAdvices(Set<OAdvice> aroundAdvices) {
		this.aroundAdvices = aroundAdvices;
	}

	public void setAfterAdvices(Set<OAdvice> afterAdvices) {
		this.afterAdvices = afterAdvices;		
	}
	
	public OActivity getOAdviceActivity() {
		return oAdviceActivity;
	}
	
	public List<OAdvice> getAdvices() {
		List<OAdvice> advices = new Vector<OAdvice>();
		advices.addAll(beforeAdvices);
		advices.addAll(aroundAdvices);
		advices.addAll(afterAdvices);
		return advices;
	}

}
