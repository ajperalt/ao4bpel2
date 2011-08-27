package org.apache.ode.bpel.o;

import java.util.ArrayList;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ode.bpel.o.OMessageVarType.Part;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class OJPVarType extends OMessageVarType {

	OMessageVarType var = null;
	
	public enum Type {
	    OUT, IN 
	}
	
	private Type type;

	
	public OJPVarType(OProcess owner, Type type) {
		super(owner, null, new ArrayList<Part>());
		this.type = type;
	}

	private static final long serialVersionUID = 1L;

	@Override
	public Node newInstance(Document doc) {
		if(var != null)
			return var.newInstance(doc);
		else
			return null;
	}
	
	public void setVar(OMessageVarType var) {
		this.var = var;		
		this.docLitType = var.docLitType;
		this.messageType = var.messageType;
		this.parts = var.parts;
		this.xpath = var.xpath;
	}
	
	public Type getType() {
		return type;
	}
	
	public String toString() {
		return "OJPVarType: " + var + " / " + messageType + " / " + parts + " / " + xpath;
	}
	
}
