package de.tud.stg.ao4ode.runtime;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.compiler.bom.CreateInstanceActivity;
import org.apache.ode.bpel.o.OActivity;
import org.apache.ode.bpel.o.OAdvice;
import org.apache.ode.bpel.o.OAssign;
import org.apache.ode.bpel.o.OInvoke;
import org.apache.ode.bpel.o.OPointcut;
import org.apache.ode.bpel.o.OProceed;
import org.apache.ode.bpel.o.OMessageVarType.Part;
import org.apache.ode.bpel.o.OScope.Variable;
import org.apache.ode.bpel.o.OSequence;
import org.apache.ode.bpel.runtime.ACTIVITY;
import org.apache.ode.bpel.runtime.ACTIVITYGUARD;
import org.apache.ode.bpel.runtime.ScopeFrame;
import org.apache.ode.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import de.tud.stg.ao4ode.aspectmanager.AspectConfImpl;
import de.tud.stg.ao4ode.aspectmanager.AspectStore;
import de.tud.stg.ao4ode.facts.BpelFactsManager;

public class AspectManager {
	private static final Log log = LogFactory.getLog(AspectManager.class);
	private static AspectManager instance = new AspectManager();
		
	// TODO: REMOVE: move to AspectStore
	private File deployDir = null;
		
	private BpelFactsManager fm = null;
	
	private AspectStore aspectStore;
	private Map<Long,ACTIVITYGUARD> jpActivities = new HashMap<Long,ACTIVITYGUARD>();
	
	private AspectManager() {	
		fm = BpelFactsManager.getInstance();
	}
	
	public AdviceActivity getAdvice(Long pid, OActivity oActivity) {
				
		// Only handle activities with XPath
		String xpath = oActivity.getXPath();		
		if(xpath != null) {

			// Return (composite) advice for current join point
			Collection<AspectConfImpl> aspects = aspectStore.getAspects();
			
			log.debug("AspectStore before PC check: ");
			log.debug(aspects);
			
			// Get all matching advices
			Set<OAdvice> beforeAdvices = new HashSet<OAdvice>();
			Set<OAdvice> aroundAdvices = new HashSet<OAdvice>();
			Set<OAdvice> afterAdvices = new HashSet<OAdvice>();
			
			for(AspectConfImpl aspect : aspects) {
				
				// Scoping
				String scope = aspect.getOAspect().getScope();
				log.debug("Checking scope for aspect " + aspect.getOAspect().getQName() + ": " + scope);
				if(scope == null || fm.solve("Scope for " + aspect.getOAspect().getQName(),
						"Invalid Scope",
						scope, pid)) {

					log.debug("Scope " + scope + " is active, checking pointcuts!");
					
					// Check all pointcuts
					for(OPointcut pointcut : aspect.getOAspect().getPointcuts()) {					
						if(fm.solve(pointcut.getName(),
								"Invalid Pointcut",
								pointcut.getQuery(), pid)) {
							
							log.debug("POINTCUT MATCH AT " + xpath + ": " + pointcut);
							
							OAdvice oAdvice = aspect.getOAspect().getOAdvice();
							oAdvice.setProcessId(pid);
							oAdvice.setJPActivity(oActivity);
							oAdvice.setPointcut(pointcut);
							if(oActivity instanceof OInvoke) {
								OInvoke oinvoke = (OInvoke)oActivity;
								oAdvice.setJPInVariable(oinvoke.inputVar);
								oAdvice.setJPOutVariable(oinvoke.outputVar);
							}
							
							
							if(oAdvice.getType().equals(OAdvice.TYPE.BEFORE)) {
								beforeAdvices.add(aspect.getOAspect().getOAdvice());
							}
							if(oAdvice.getType().equals(OAdvice.TYPE.AROUND)) {
								aroundAdvices.add(aspect.getOAspect().getOAdvice());
							}
							if(oAdvice.getType().equals(OAdvice.TYPE.AFTER)) {
								afterAdvices.add(aspect.getOAspect().getOAdvice());
							}
						}
						else {
							log.debug("NO POINTCUT MATCH FOR POINTCUT: " + pointcut);
						}
					}
				}
			}
			
			// Create composite ACTIVITY from JP-ACTIVITY and Advice ACTIVITIES
			if(beforeAdvices.size() > 0 ||
					aroundAdvices.size() > 0 ||
					afterAdvices.size() > 0) {
				
				OSequence osequence = new OSequence(oActivity.getOwner(), oActivity.getParent());
			
				// Before advice
				for(OAdvice oadvice : beforeAdvices) {
					osequence.sequence.add(oadvice.procesScope);
				}
				
				// Around advice
				if(aroundAdvices.size() > 0) {
					for(OAdvice oadvice : aroundAdvices) {
						osequence.sequence.add(oadvice.procesScope);
					}					
				}
				else  {
					osequence.sequence.add(oActivity);					
				}
				
				// After advice
				for(OAdvice oadvice : afterAdvices) {
					osequence.sequence.add(oadvice.procesScope);
				}
				
				AdviceActivity aa = new AdviceActivity();
				aa.setOAdviceActivity(osequence);
				aa.setBeforeAdvices(beforeAdvices);
				aa.setAroundAdvices(aroundAdvices);
				aa.setAfterAdvices(afterAdvices);
				
				log.error("Got " + aa.getAdvices().size() + " advices: "  + aa.getAdvices());
				
				return aa;
				
			}
			
		}

		return null;
		
	}
	
	public AspectConfImpl getAspectConfiguration(QName aid) {
		return aspectStore.getAspectConfiguration(aid);
	}
	
	// TODO: Remove?
	public void setDepoloymentDir(File deployDir) {
		this.deployDir = deployDir;
	}
	
	public File getDeploymentDir() {
		return deployDir;
	}
	
	public void setAspectStore(AspectStore as) {
		this.aspectStore = as;
	}
	
	// TODO: Avoid singleton pattern
	public static AspectManager getInstance() {				
		return instance;
	}

	public void addJPActivity(long pid, ACTIVITYGUARD oactivity) {		
		jpActivities.put(pid, oactivity);
	}
	
	public ACTIVITYGUARD getJPActivity(long pid) {		
		return jpActivities.get(pid);
	}

	public static String getThisJPActivityValue(OAdvice advice, ACTIVITYGUARD ag,
			String partName) {
		
		String value = "Unknown part \"" + partName + "\" for ThisJPAcitivity \"" + ag.getActivityInfo().getO().name + "\"";
		if(partName.equals("name")) {
			value = ag.getActivityInfo().getO().name;					
		}
		else if(partName.equals("process")) {
			value = ag.getActivityInfo().getO().getOwner().processName;
		}
		else if(partName.equals("type")) {
			value = ag.getActivityInfo().getO().getType();
		}
		else if(partName.equals("partnerlink")) {
			if(ag.getActivityInfo().getO() instanceof OInvoke) {        				
				value = ((OInvoke)ag.getActivityInfo().getO()).partnerLink.name;
			}
		}
		else if(partName.equals("porttype")) {
			if(ag.getActivityInfo().getO() instanceof OInvoke) {
				value = ((OInvoke)ag.getActivityInfo().getO()).partnerLink.partnerRolePortType.getQName().toString();
			}
		}
		else if(partName.equals("operation")) {
			if(ag.getActivityInfo().getO() instanceof OInvoke) {        				
				value = ((OInvoke)ag.getActivityInfo().getO()).operation.getName();
			}
		}
		else if(partName.equals("pointcut")) {
			return advice.getPointcut().getQuery();
		}
		
		return value;
	}
	
}