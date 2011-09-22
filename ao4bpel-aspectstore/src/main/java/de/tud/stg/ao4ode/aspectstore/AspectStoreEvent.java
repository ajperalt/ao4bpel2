package de.tud.stg.ao4ode.aspectstore;

import javax.xml.namespace.QName;

/**
 * AspectStoreEvent
 * 
 * @author A. Look
 */
public class AspectStoreEvent {
	private static final long serialVersionUID = 1L;

    public enum Type {
        DEPLOYED,
        UNDEPLOYED
    }

    public final Type type;

    public final QName aid; 
    
    public final String deploymentUnit;
    
    public AspectStoreEvent(Type type, QName aid, String aspectDeploymentUnit) {
        this.type = type;
        this.aid = aid;
        this.deploymentUnit = aspectDeploymentUnit;
    }
    
    @Override
    public String toString() {
        return "{AspectStoreEvent#" + type + ":" + aid +"}";
    }
}
