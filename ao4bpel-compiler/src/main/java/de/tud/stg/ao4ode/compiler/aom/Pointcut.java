package de.tud.stg.ao4ode.compiler.aom;

import org.apache.ode.bpel.compiler.bom.BpelObject;
import org.w3c.dom.Element;

public class Pointcut extends BpelObject {
	
    public Pointcut(Element el) {
        super(el);
    }
    
    public String getName() {
        return getAttribute("name", null);
    }
    
    public String getQuery() {
    	return this.getTextValue();
    }
    
}

