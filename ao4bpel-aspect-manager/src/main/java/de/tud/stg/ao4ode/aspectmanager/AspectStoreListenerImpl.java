package de.tud.stg.ao4ode.aspectmanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AspectStoreListenerImpl implements AspectStoreListener {
	
	private static final Log __log = LogFactory.getLog(AspectStoreListenerImpl.class);
	
	public void onAspectStoreEvent(AspectStoreEvent event) {
		__log.debug("AspectStoreEvent: " + event.toString());
	}

}
