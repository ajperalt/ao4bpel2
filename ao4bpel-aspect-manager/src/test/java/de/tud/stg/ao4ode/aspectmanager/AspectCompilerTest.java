package de.tud.stg.ao4ode.aspectmanager;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.o.OAdvice;
import org.apache.ode.bpel.o.OAspect;
import org.apache.ode.bpel.o.OInvoke;
import org.apache.ode.bpel.o.OProcess;

import de.tud.stg.ao4ode.compiler.AO4BPEL2AspectCompiler;
import de.tud.stg.ao4ode.facts.BpelFactsManager;
import de.tud.stg.ao4ode.facts.DynamicFact;

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
	
	public void testAspectManager() {
		try {
			AspectManager am = AspectManager.getInstance();
			BpelFactsManager fm = BpelFactsManager.getInstance();
					
			String pc = "activity('invokeTest').";
			
			// Create Aspect
			OAdvice oadvice = new OAdvice("2.0");
			oadvice.setType(OAdvice.TYPE.BEFORE);
			oadvice.processName = "TestAdvice";
			
			OAspect oaspect = new OAspect();			
			oaspect.setoAdvice(oadvice);			
			oaspect.addPointcut(pc);
			            
			System.out.println("Advice name: " + oaspect.getOAdvice().getName());			
			System.out.println("Aspect pointcuts: " + oaspect.getPointcuts());
			
			// Create activity	
			OProcess oprocess = new OProcess("TestProcess");
			final OInvoke oinvoke = new OInvoke(oprocess, null);
			oinvoke.setXPath("process/sequence[1]/invoke[@name='invokeTest']");
			oinvoke.name = "invokeTest";
			
			// Simulate process execution, add some facts
			
			fm.dynamicEventFact(1l,
					new DynamicFact() {						
						public String getXPath() {
							return oinvoke.getXPath();
						}
					},
					oinvoke.getId(),
					-1,
					oinvoke.name,
					oinvoke.getType(),
					"ActivityEnabledEvent",
					"ScopeEvent");
			
			// Debug output
			System.out.println("-- THEORY --");
			System.out.println(fm.getTheory());
			System.out.println("------------");
			
			// Test aspect manager			
			am.addAspect(oaspect);
			
			OAdvice pcadvice = am.getAdvice(1l, oinvoke);
			
			System.out.println("Advice for pointcut " + pc + ": ");
						
			assertNotNull("No advice found!", pcadvice);
			System.out.println(pcadvice.getName());
			
			assertTrue(true);	
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
