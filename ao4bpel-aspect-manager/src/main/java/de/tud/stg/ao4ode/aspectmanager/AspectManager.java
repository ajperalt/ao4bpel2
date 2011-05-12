package de.tud.stg.ao4ode.aspectmanager;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.compiler.api.CompilationException;
import org.apache.ode.bpel.o.OActivity;
import org.apache.ode.bpel.o.OAdvice;
import org.apache.ode.bpel.o.OAspect;

import de.tud.stg.ao4ode.compiler.AO4BPEL2AspectCompiler;
import de.tud.stg.ao4ode.facts.BpelFactsManager;

public class AspectManager {
	private static final Log log = LogFactory.getLog(AspectManager.class);
	private static AspectManager instance = new AspectManager();
	
	// TODO: REMOVE: compile aspect at deployment time and use some kind of
	// aspect store instead
	AO4BPEL2AspectCompiler compiler = null;
	
	// TODO: REMOVE: move to AspectStore
	private File deployDir = null;
		
	private BpelFactsManager fm = null;
	private Set<OAspect> aspects = new HashSet<OAspect>();
	
	private AspectManager() {
		try {
			compiler = new AO4BPEL2AspectCompiler();
		} catch (Exception e) {
			e.printStackTrace();
		}
		fm = BpelFactsManager.getInstance();
		aspects = new HashSet<OAspect>();
		
	}
	
	public static AspectManager getInstance() {				
		return instance;
	}
	
	private void loadAspects() {
		// TODO: Use AspectStore
		File deployRoot = new File(deployDir, "aspects");
		if (!deployRoot.isDirectory())
            throw new IllegalArgumentException(deployRoot + " does not exist or is not a directory");
		
		File f;
	    f=new File(deployRoot, "myfile.txt");
	    if(!f.exists()){
	    	try {
				f.createNewFile();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    
	    File aspectFile = new File(deployRoot, "IncreaseCounter.bpel");
	    	    
	    log.debug("ASPECT FILE: " + aspectFile.getAbsolutePath());
	    		
		OAdvice oadvice = null;
		
		try {
			oadvice = compiler.compileAspect(aspectFile.toURL());
		} catch (CompilationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		OAspect oaspect = new OAspect();
		Set<String> pointcuts = new HashSet<String>();
		// pointcuts.add("process/sequence[1]/invoke[@name='invokeConcatAdvice']");
		/*
		pointcuts.add("event(_,\"process/sequence[1]/invoke[@name='invokeConcatAdvice']\",_,_,_,_,_,'ActivityEnabledEvent',_)," +
				"not(event(_,\"process/sequence[1]/invoke[@name='invokeConcatAdvice']\",_,_,_,_,_,'ActivityExecStartEvent',_)).");		
		*/
		pointcuts.add(compiler.pointcut);

		oaspect.setPointcuts(pointcuts);
		oaspect.setoAdvice(oadvice);
		
		aspects.add(oaspect);		
	}
	
	
	public OAdvice getAdvice(Long pid, OActivity oActivity) {
		loadAspects();
		String xpath = oActivity.getXPath();		
		if(xpath != null) {

			for(OAspect aspect : aspects) {
				// TODO: Build composite advice, for now, use the first match
				for(String pointcut : aspect.getPointcuts()) {					
					if(fm.solve(aspect.getOAdvice().getName(), "TODO", pointcut, pid+"")) {
						return aspect.getOAdvice();
					}
				}
			}			
			
		}

		return null;
		
	}

	public void setDepoloymentDir(File deployDir) {
		this.deployDir = deployDir;
		// loadAspects();
	}

}
