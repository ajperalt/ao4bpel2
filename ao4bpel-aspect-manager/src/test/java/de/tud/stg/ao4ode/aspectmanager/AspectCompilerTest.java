package de.tud.stg.ao4ode.aspectmanager;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.o.OAspect;

import de.tud.stg.ao4ode.compiler.AO4BPEL2AspectCompiler;

public class AspectCompilerTest extends TestCase  {
	
	private static final Log log = LogFactory.getLog(AspectCompilerTest.class);
	
	public void testCompileAspect() {
		try {
						
			URL aspectURL = getClass().getResource("IncreaseCounter.bpel");
			AO4BPEL2AspectCompiler compiler = new AO4BPEL2AspectCompiler();
			OAspect oaspect = compiler.compileAspect(aspectURL);
			
			// TODO: REMOVE
			Set<String> pointcuts = new HashSet<String>();
			pointcuts.add(compiler.pointcut);			
			oaspect.setPointcuts(pointcuts);
            
			System.out.println("Aspect name: " + oaspect.getOAdvice().getName());			
			System.out.println("Aspect pointcuts: " + oaspect.getPointcuts());
			assertTrue(true);	
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
