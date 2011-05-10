package de.tud.stg.ao4ode.aspectmanager;

import java.net.URL;

import junit.framework.TestCase;

import org.apache.ode.bpel.o.OProcess;

import de.tud.stg.ao4ode.compiler.AO4BPEL2AspectCompiler;

public class AspectCompilerTest extends TestCase  {

	public void testCompileAspect() {
		try {
			
			URL local = getClass().getResource(".");
			System.out.println(local.toString());
			
			URL aspectURL = getClass().getResource("IncreaseCounter.bpel");
			AO4BPEL2AspectCompiler compiler = new AO4BPEL2AspectCompiler();
			OProcess oprocess = compiler.compileAspect(aspectURL);
            
			System.out.println(oprocess.processName);
			assertTrue(true);	
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
