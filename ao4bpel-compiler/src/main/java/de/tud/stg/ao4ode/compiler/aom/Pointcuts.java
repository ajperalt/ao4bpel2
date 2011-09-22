package de.tud.stg.ao4ode.compiler.aom;

import java.util.List;

import org.w3c.dom.Element;

/**
 * @author A. Look
 */
public class Pointcuts extends AspectObject {
	
	public Pointcuts(Element el) {
		super(el);
	}

	public List<Pointcut> getPointcuts() {
		return getChildren(Pointcut.class);
	}
	
}
