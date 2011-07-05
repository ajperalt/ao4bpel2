package de.tud.stg.ao4ode.aspectmanager;

import org.apache.ode.bpel.iapi.ProcessStoreEvent;

/** 
 * AO4ODE: Aspect store listener interface. 
 */
public interface AspectStoreListener {
    
    public void onAspectStoreEvent(ProcessStoreEvent event);

}
