package de.tud.stg.ao4ode.compiler.aom;

import org.w3c.dom.Element;

/** 
 * @author A. Look
 */
public class Aspect extends AspectObject {

	public Aspect(Element el) {
		super(el);
	}

	public String getName() {
		return getAttribute("name");
	}
	
    public String getTargetNamespace() {
        return getAttribute("targetNamespace", null);
    }
	
	public Advice getAdvice() {
		return getFirstChild(Advice.class);
	}
	
	public Pointcuts getPointcuts() {		
		return getFirstChild(Pointcuts.class);
	}

}
