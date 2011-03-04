package de.tud.stg.ao4ode;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.engine.BpelRuntimeContextImpl;
import org.apache.ode.bpel.runtime.ACTIVITY;
import org.apache.ode.jacob.JacobObject;
import org.apache.ode.jacob.JacobRunnable;
import org.apache.ode.jacob.soup.Continuation;
import org.apache.ode.jacob.vpu.ExecutionQueueImpl;
import org.apache.ode.utils.CollectionUtils;
import org.apache.ode.utils.ObjectPrinter;

/**
 * TODO: This could probably be a good place to check
 * pointcuts and inject advice code. Work in progress..
 * 
 * @author A. Look
 *
 */
public class AO4ODEExecutionQueueImpl extends ExecutionQueueImpl {
	/** Class-level logger. */
    private static final Log __log = LogFactory.getLog(AO4ODEExecutionQueueImpl.class);

	private BpelRuntimeContextImpl bpelRuntimeContextImpl;
    
	public AO4ODEExecutionQueueImpl(ClassLoader classLoader, BpelRuntimeContextImpl bpelRuntimeContextImpl) {
		super(classLoader);
		this.bpelRuntimeContextImpl = bpelRuntimeContextImpl;
	}
	
	public static boolean RUN_ADVICE = true;
	@Override	
	public Continuation dequeueReaction() {
        if (__log.isTraceEnabled()) {
            __log.trace(ObjectPrinter.stringifyMethodEnter("dequeueReaction", CollectionUtils.EMPTY_OBJECT_ARRAY));
        }

        Continuation continuation = null;
        if (!_reactions.isEmpty()) {
            Iterator<Continuation> it = _reactions.iterator();
            continuation = it.next();
            it.remove();
        }
        
        JacobObject obj = continuation.getClosure();
        
        // We only handle activities..        
        if(obj instanceof ACTIVITY) {        	
        	ACTIVITY activity = (ACTIVITY) obj;
        	
        	// Proof of concept after advice weaving        	
       		if(activity.getActivityInfo().getO().getXPath() != null
       				&& activity.getActivityInfo().getO().getXPath().equals("process/sequence[1]/assign[@name='assign1']")
       				&& RUN_ADVICE) {
       			
       			// TODO: Weave advices       			
       			
       			__log.debug("ADDING PROOF OF CONCEPT ADVICE");
       			
       			// Doesn't work as expected!
       			// Sometimes it does work, sometimes not. :(
       			// this.enqueueReaction(continuation);
       			// _reactions.add(continuation);
       			
       			JacobRunnable advice = (JacobRunnable)continuation.getClosure();
       			
       			/* Nope... :( */
       			bpelRuntimeContextImpl.getVPU().inject(advice);
       			       			
       			RUN_ADVICE = false;
       		}
       			
        	
        }
                
        return continuation;
    }

}
