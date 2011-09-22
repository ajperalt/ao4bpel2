package de.tud.stg.ao4ode.compiler.aom;

import org.apache.ode.bpel.compiler.bom.BpelObject;
import org.w3c.dom.Element;

/**
 * @author A. Look
 */
public class Pointcut extends AspectObject {
	
    public Pointcut(Element el) {
        super(el);
    }
    
    public String getName() {
        return getAttribute("name", null);
    }
    
    public String getLanguage() {
        return getAttribute("language", null);
    }
    
    public String getQuery() {
    	return this.getTextValue();
    }
    
}

