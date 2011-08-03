package de.tud.stg.ao4ode.aspectmanager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.compiler.api.CompilationException;
import org.apache.ode.bpel.iapi.ContextException;
import org.apache.ode.bpel.iapi.EndpointReferenceContext;
import org.apache.ode.bpel.iapi.ProcessStoreEvent;
import org.apache.ode.bpel.iapi.ProcessStoreListener;
import org.apache.ode.bpel.o.OAspect;
import org.apache.ode.il.config.OdeConfigProperties;
import org.apache.ode.store.ConfStoreConnection;
import org.apache.ode.store.DeploymentUnitDAO;
import org.apache.ode.store.DeploymentUnitDir;
import org.apache.ode.store.Messages;
import org.apache.ode.utils.msg.MessageBundle;

public class AspectStoreImpl implements AspectStore {
	
	private static final Log __log = LogFactory.getLog(AspectStoreImpl.class);

	private static final Messages __msgs = MessageBundle.getMessages(Messages.class);

    private Map<QName, AspectInfo> _aspects = new HashMap<QName, AspectInfo>();

    private Map<String, AspectDeploymentUnitDir> _deploymentUnits = new HashMap<String, AspectDeploymentUnitDir>();

    private EndpointReferenceContext eprContext;

    protected File _deployDir;

    protected File _configDir;

	private OdeConfigProperties props;
		
	private final CopyOnWriteArrayList<AspectStoreListener> _listeners = new CopyOnWriteArrayList<AspectStoreListener>();
    
	public AspectStoreImpl(EndpointReferenceContext eprContext, OdeConfigProperties props) {
        this.eprContext = eprContext;
        this.props = props;
    }
	
	public Collection<QName> deployAspect(File deploymentUnitDirectory, String scope) {
		final Date deployDate = new Date();
		final AspectDeploymentUnitDir du = new AspectDeploymentUnitDir(deploymentUnitDirectory);

		// Compile all aspects
		try {
            du.compile(scope);
        } catch (CompilationException ce) {
            String errmsg = __msgs.msgDeployFailCompileErrors(ce);
            __log.error(errmsg, ce);
            throw new ContextException(errmsg, ce);
        }
        
        // Add compiled aspects to aspect manager
        du.scan();
        
        // Add compiled DU to store
        _deploymentUnits.put(du.getName(), du);
        
        // TODO: Foreach aspect defined in DD...
        Collection<QName> aspectIds = du.getAspects();
        for(QName aspectId : aspectIds) {
        	OAspect oaspect = du.getAspect(aspectId);
        	AspectInfo aspect = new AspectInfo(oaspect, deployDate);
        	_aspects.put(aspectId, aspect);
        }
        
        return _aspects.keySet();
 
        
	}
	
	public Collection<AspectInfo> getAspects() {
		return _aspects.values();
	}

	// FIXME: return a version number
	public long getCurrentVersion() {
		// UUID uuid = UUID.randomUUID();
		// return uuid.getMostSignificantBits();
		return 0;
    }
	
	public Collection<QName> undeployAspect(final File dir) {
		return undeploy(dir.getName());
	}
	
	public Collection<QName> undeploy(final String duName) {
        
		__log.debug("AspectStore before undeployment: " + _aspects);
		
        Collection<QName> undeployed = Collections.emptyList();
        AspectDeploymentUnitDir du;
        // _rw.writeLock().lock();
        try {
            du = _deploymentUnits.remove(duName);
            if (du != null) {
                undeployed = toAids(du.getAspectNames(), du.getVersion());
            }
            
            for (QName pn : undeployed) {
                fireEvent(new AspectStoreEvent(AspectStoreEvent.Type.UNDEPLOYED, pn, du.getName()));
                __log.info("Aspect " + pn.toString() + " has been undeployed!");
            }
            
            __log.debug("Undeployed: " + undeployed);
            
            _aspects.keySet().removeAll(undeployed);
        } finally {
            // _rw.writeLock().unlock();
        }

        __log.debug("AspectStore after undeployment: " + _aspects);
        return undeployed;
    }
	
	private List<QName> toAids(Collection<QName> aspectTypes, long version) {
        ArrayList<QName> result = new ArrayList<QName>();
        for (QName pqName : aspectTypes) {
            result.add(toAid(pqName, version));
        }
        return result;
    }

    private QName toAid(QName aspectType, long version) {
        // return new QName(aspectType.getNamespaceURI(), aspectType.getLocalPart() + "-" + version);
    	return new QName(aspectType.getNamespaceURI(), aspectType.getLocalPart());
    }
	

	public Collection<String> getAspectPackages() {
		return new ArrayList<String>(_deploymentUnits.keySet());
	}

	public List<QName> listAspects(String packageName) {
		AspectDeploymentUnitDir du = _deploymentUnits.get(packageName);
        if (du == null)
            return null;
        return toAids(du.getAspectNames(), du.getVersion());
	}

	public List<QName> getAspectList() {
		// TODO Auto-generated method stub
		return null;
	}

	public void registerListener(AspectStoreListener psl) {
		// TODO Auto-generated method stub
		
	}

	public void unregisterListener(ProcessStoreListener psl) {
		// TODO Auto-generated method stub
		
	}
	
	protected void fireEvent(AspectStoreEvent ase) {
        __log.debug("firing event: " + ase);
        for (AspectStoreListener psl : _listeners)
            psl.onAspectStoreEvent(ase);
    }

}
