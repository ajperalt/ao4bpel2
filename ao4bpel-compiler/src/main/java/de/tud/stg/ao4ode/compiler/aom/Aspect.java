package de.tud.stg.ao4ode.compiler.aom;

import org.apache.ode.bpel.compiler.bom.BpelObject;
import org.w3c.dom.Element;

import sun.tools.tree.ThisExpression;

// FIXME: Aspect is not a BpelObject
public class Aspect extends AspectObject {

	public Aspect(Element el) {
		super(el);
	}

	public String getName() {
		return getAttribute("name");
	}
	
	public Advice getAdvice() {
		return getFirstChild(Advice.class);
	}
	
	public Pointcuts getPointcuts() {		
		return getFirstChild(Pointcuts.class);
	}

}
