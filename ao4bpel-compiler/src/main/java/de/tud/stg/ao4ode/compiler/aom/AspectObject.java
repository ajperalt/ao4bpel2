package de.tud.stg.ao4ode.compiler.aom;

import org.apache.ode.bpel.compiler.bom.BpelObject;
import org.w3c.dom.Element;

public class AspectObject extends BpelObject {

	public AspectObject(Element el) {
		super(el);
	}
	
	@Override
	protected BpelObject createBpelObject(Element element) {
        return AspectObjectFactory.getInstance().createBpelObject(element,_docURI);
    }
}
