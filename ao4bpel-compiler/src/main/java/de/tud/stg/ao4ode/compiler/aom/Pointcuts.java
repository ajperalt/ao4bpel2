package de.tud.stg.ao4ode.compiler.aom;

import java.util.List;
import java.util.Set;

import org.apache.ode.bpel.compiler.bom.BpelObject;
import org.w3c.dom.Element;

public class Pointcuts extends AspectObject {
	
	public Pointcuts(Element el) {
		super(el);
	}

	public List<Pointcut> getPointcuts() {
		return getChildren(Pointcut.class);
	}
	
}
