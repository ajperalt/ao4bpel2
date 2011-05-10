package de.tud.stg.ao4ode.aspectmanager;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.ServletContextCleaner;
import org.apache.ode.bpel.compiler.api.CompilationException;
import org.apache.ode.bpel.o.OActivity;
import org.apache.ode.bpel.o.OProcess;

import de.tud.stg.ao4ode.compiler.AO4BPEL2AspectCompiler;
import de.tud.stg.ao4ode.facts.BpelFactsManager;

public class AspectManager {
	private static final Log log = LogFactory.getLog(AspectManager.class);
	private static AspectManager instance = new AspectManager();
	
	// TODO: REMOVE: compile aspect at deployment time and use some kind of
	// aspect store instead
	AO4BPEL2AspectCompiler compiler = null;
	
	BpelFactsManager fm = null;

	private File deployDir = null;
	
	private AspectManager() {
		try {
			compiler = new AO4BPEL2AspectCompiler();
		} catch (Exception e) {
			e.printStackTrace();
		}
		fm = BpelFactsManager.getInstance();
	}
	public static AspectManager getInstance() {				
		return instance;
	}
	
	// TODO: Return OAdvice
	public OActivity getAdvice() {
		
		assert deployDir != null;
		
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
	    		
		OProcess oprocess = null;
		
		try {
			oprocess = compiler.compileAspect(aspectFile.toURL());
		} catch (CompilationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return oprocess.procesScope;
		
	}
	
	public void setDepoloymentDir(File deployDir) {
		this.deployDir = deployDir;
	}

}
