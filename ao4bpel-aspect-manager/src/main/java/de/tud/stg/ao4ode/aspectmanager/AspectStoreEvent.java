package de.tud.stg.ao4ode.aspectmanager;

import javax.xml.namespace.QName;

public class AspectStoreEvent {
	private static final long serialVersionUID = 1L;

    public enum Type {
        /** A process was deployed to the store. */
        DEPLOYED,
        
        /** A process was undeployed to from the store. */
        UNDEPLOYED,
                
        /** 
         * A process that was previously disabled or retired has become activated. This
         * event is also sent whenver an active process is "discovered" 
         */
        ACTVIATED,
        
        /** A process has been disabled: it should no longer execute for new or old instances. */
        DISABLED,

    }

    /**
     * Event type. 
     * @see Type
     */
    public final Type type;
    
    /**
     * Process identifier.
     */
    public final QName pid; 
    
    public final String deploymentUnit;
    
    public AspectStoreEvent(Type type, QName pid, String deploymentUnit) {
        this.type = type;
        this.pid = pid;
        this.deploymentUnit = deploymentUnit;
    }
    
    @Override
    public String toString() {
        return "{AspectStoreEvent#" + type + ":" + pid +"}";
    }
}
