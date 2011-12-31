package de.tud.stg.ao4ode.runtime.aspectmanager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.File;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.o.OActivity;
import org.apache.ode.bpel.o.OAdvice;
import org.apache.ode.bpel.o.OInvoke;
import org.apache.ode.bpel.o.OPointcut;
import org.apache.ode.bpel.o.OSequence;
import org.apache.ode.bpel.o.OFailureHandling;
import org.apache.ode.bpel.o.OScope;
import org.apache.ode.bpel.o.OFaultHandler;
import org.apache.ode.bpel.o.OCatch;
import org.apache.ode.bpel.runtime.ACTIVITYGUARD;
import org.apache.ode.store.ProcessStoreImpl;

import de.tud.stg.ao4ode.aspectstore.AspectConfImpl;
import de.tud.stg.ao4ode.aspectstore.AspectStore;
import de.tud.stg.ao4ode.facts.BpelFactsManager;
import de.tud.stg.ao4ode.prolog.MalformedQueryException;
import de.tud.stg.ao4ode.prolog.MalformedTheoryException;

/**
 * @author A. Look
 */
public class AspectManager {
	private static final Log log = LogFactory.getLog(AspectManager.class);
	private static AspectManager instance = new AspectManager();
				
	private BpelFactsManager fm = null;
	
	private AspectStore aspectStore;
	private ProcessStoreImpl processStore;
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
			
			// log.debug("AspectStore before PC check: ");
			// log.debug(aspects);
			
			// Get all matching advices
			Set<OAdvice> beforeAdvices = new HashSet<OAdvice>();
			Set<OAdvice> aroundAdvices = new HashSet<OAdvice>();
			Set<OAdvice> afterAdvices = new HashSet<OAdvice>();
			List<File> aspectDeploymentUnits = new ArrayList<File>();
			
			for(AspectConfImpl aspect : aspects) {
				
				// Scoping
				String scope = aspect.getOAspect().getScope();
				
				// Bind ProcessID
				scope = addVariables(oActivity, aspect, scope);
				
				log.debug("Checking scope for aspect " + aspect.getOAspect().getQName() + ": " + scope);
				if(scope == null || fm.solve("Scope for " + aspect.getOAspect().getQName(),
						"Invalid Scope",
						scope, pid)) {

					log.debug("Scope " + scope + " is active, checking pointcuts!");
					
					// Check all pointcuts
					for(OPointcut pointcut : aspect.getOAspect().getPointcuts()) {	
						
						String pc = addVariables(oActivity, aspect, pointcut.getQuery());
						
						if(fm.solve(pointcut.getName(),
								"Invalid Pointcut",
								pc, pid)) {
							
							log.debug("POINTCUT MATCH AT " + xpath + ": " + pointcut);
							
							aspectDeploymentUnits.add(aspect.getDeploymentUnit().getDeployDir());
							
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
							// log.debug("NO POINTCUT MATCH FOR POINTCUT: " + pointcut);
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
				
				// Default fault handler
				OFailureHandling fh = new OFailureHandling();
				fh.retryFor = 0;
				fh.retryDelay = 0;
				fh.faultOnFailure = false;
				osequence.setFailureHandling(fh);
				
				// OCatch ocatch = new OCatch(oActivity.getOwner(), oActivity.getParent());
				OCatch ocatch = new OCatch(null, null);
				// ocatch.faultName = Bpel20QNames.CATCHALL;
				ocatch.activity = osequence;
				ocatch.name = "Advice catchAll block";
				
				OFaultHandler faultHandler = new OFaultHandler(null);
				faultHandler.catchBlocks.add(ocatch);
				// OScope oscope = new OScope(oActivity.getOwner(), oActivity.getParent());
				OScope oscope = new OScope(null, null);
				oscope.name = "defaultAdviceScope";
				oscope.activity = osequence;
				oscope.faultHandler = faultHandler;
				
				AdviceActivity aa = new AdviceActivity();
				aa.setOAdviceActivity(oscope);
				aa.setBeforeAdvices(beforeAdvices);
				aa.setAroundAdvices(aroundAdvices);
				aa.setAfterAdvices(afterAdvices);
				aa.setAspectDeploymentUnits(aspectDeploymentUnits);
				
				log.debug("Got " + aa.getAdvices().size() + " advices: "  + aa.getAdvices());
				
				return aa;
				
			}
			
		}

		return null;
		
	}
	
	private String addVariables(OActivity oActivity, AspectConfImpl aspect, String scope) {		 
		return "ActivityID=" + oActivity.getId() + ","
		+ "AspectDeploymentTime=" + aspect.getDeployDate().getTime() + ","
		+ scope;				
	}

	public AspectConfImpl getAspectConfiguration(QName aid) {
		return aspectStore.getAspectConfiguration(aid);
	}
		
	public void setAspectStore(AspectStore as) {
		this.aspectStore = as;
	}
	
	public void setProcessStore(ProcessStoreImpl ps) {
		this.processStore = ps;
	}
	
	public ProcessStoreImpl getProcessStore() {
		return this.processStore;
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
		else if(partName.equals("xpath")) {
			value = ag.getActivityInfo().getO().getXPath();					
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
	
	public static AspectManager getInstance() {				
		return instance;
	}
	
	public boolean validateQuery(String query) throws MalformedQueryException {
		return fm.solve("Scope test: " + query,
				"Invalid Scope",
				query);
	}

	public void addRule(String ruleId, String rule) throws MalformedTheoryException {
		log.info("Adding rule " + ruleId + ": " + rule);		
		fm.addRule(rule);
	}
	
	public void updateXPathPointcuts() {
		aspectStore.updateXPathPointcuts(processStore);
	}
	
}