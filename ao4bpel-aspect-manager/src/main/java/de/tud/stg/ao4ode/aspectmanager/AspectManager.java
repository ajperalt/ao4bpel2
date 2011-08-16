package de.tud.stg.ao4ode.aspectmanager;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.compiler.bom.CreateInstanceActivity;
import org.apache.ode.bpel.o.OActivity;
import org.apache.ode.bpel.o.OAdvice;
import org.apache.ode.bpel.o.OPointcut;
import org.apache.ode.bpel.o.OProceed;

import de.tud.stg.ao4ode.facts.BpelFactsManager;

public class AspectManager {
	private static final Log log = LogFactory.getLog(AspectManager.class);
	private static AspectManager instance = new AspectManager();
		
	// TODO: REMOVE: move to AspectStore
	private File deployDir = null;
		
	private BpelFactsManager fm = null;
	
	private AspectStore aspectStore;
	private Map<Long,OActivity> jpActivities = new HashMap<Long,OActivity>();
	
	private AspectManager() {	
		fm = BpelFactsManager.getInstance();
	}
	
	/* TODO: Remove
	public void loadAspects() {
		
		aspects.clear();
		
		// TODO: Use AspectStore
		
		// Load aspect from file
		File deployRoot = new File(deployDir, "aspects");
		if (!deployRoot.isDirectory())
            throw new IllegalArgumentException(deployRoot + " does not exist or is not a directory");		
	    File aspectFile = new File(deployRoot, "IncreaseCounter.aspect");	    	    
	    log.debug("ASPECT FILE: " + aspectFile.getAbsolutePath());
	    		
		// Compile aspect
	    OAspect oaspect = null;
		try {
			oaspect = compiler.compileAspect(aspectFile.toURL());
		} catch (CompilationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		// REMOVE: Add pointcuts for testing
		// TODO: build aspect compiler
		Set<String> pointcuts = new HashSet<String>();		
		pointcuts.add(compiler.pointcut);
		oaspect.setPointcuts(pointcuts);
				
		this.addAspect(oaspect);
	}
	*/
	
	public OAdvice getAdvice(Long pid, OActivity oActivity) {
				
		// Only handle activities with XPath
		String xpath = oActivity.getXPath();		
		if(xpath != null) {

			// Return (composite) advice for current join point
			Collection<AspectConfImpl> aspects = aspectStore.getAspects();
			
			log.debug("AspectStore before PC check: ");
			log.debug(aspects);
			
			for(AspectConfImpl aspect : aspects) {
				// TODO: Build composite advice, for now, use the first match
				for(OPointcut pointcut : aspect.getOAspect().getPointcuts()) {					
					if(fm.solve(pointcut.getName(),
							"Invalid Pointcut",
							pointcut.getQuery(), pid)) {
						
						log.debug("POINTCUT MATCH AT " + xpath + ": " + pointcut);
						
						// TODO: Merge multiple advices into one based on type
						// and order attribute
						OAdvice oadvice = aspect.getOAspect().getOAdvice();						
						return oadvice;
						
					}
					else {
						log.debug("NO POINTCUT MATCH FOR POINTCUT: " + pointcut);
					}
				}
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

	public void addJPActivity(long pid, OActivity oactivity) {		
		jpActivities.put(pid, oactivity);
	}
	
	public OActivity getJPActivity(long pid) {		
		return jpActivities.get(pid);
	}

}
