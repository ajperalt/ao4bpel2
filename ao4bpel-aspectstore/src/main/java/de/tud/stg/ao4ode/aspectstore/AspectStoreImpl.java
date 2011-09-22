package de.tud.stg.ao4ode.aspectstore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.compiler.api.CompilationException;
import org.apache.ode.bpel.dd.DeployAspectDocument;
import org.apache.ode.bpel.dd.TDeploymentAspect;
import org.apache.ode.bpel.iapi.ContextException;
import org.apache.ode.bpel.iapi.EndpointReferenceContext;
import org.apache.ode.bpel.o.OAspect;
import org.apache.ode.il.config.OdeConfigProperties;
import org.apache.ode.store.Messages;
import org.apache.ode.store.ProcessStoreImpl;
import org.apache.ode.utils.msg.MessageBundle;

import de.tud.stg.ao4ode.aspectstore.AspectDeploymentUnitDir.CBAInfo;

/**
 * AspectStore, based on/stripped down version of ProcessStore
 * 
 * @author A. Look
 *
 */
public class AspectStoreImpl implements AspectStore {
	
	private static final Log __log = LogFactory.getLog(AspectStoreImpl.class);

	private static final Messages __msgs = MessageBundle.getMessages(Messages.class);

    private Map<QName, AspectConfImpl> _aspects = new HashMap<QName, AspectConfImpl>();

    private Map<String, AspectDeploymentUnitDir> _deploymentUnits = new HashMap<String, AspectDeploymentUnitDir>();

    private EndpointReferenceContext eprContext;
    
    protected File _deployDir;

    protected File _configDir;

	@SuppressWarnings("unused")
	private OdeConfigProperties props;
		
	private final CopyOnWriteArrayList<AspectStoreListener> _listeners = new CopyOnWriteArrayList<AspectStoreListener>();
    
	public AspectStoreImpl(EndpointReferenceContext eprContext, OdeConfigProperties props) {
        this.eprContext = eprContext;
        this.props = props;
    }
	
	public Collection<QName> deployAspect(File deploymentUnitDirectory, String scope, ProcessStoreImpl processStore) {
		
		__log.debug("Deploying Aspect package: " + deploymentUnitDirectory.getName());
		
		final Date deployDate = new Date();
		final AspectDeploymentUnitDir du = new AspectDeploymentUnitDir(deploymentUnitDirectory);

		// Compile all aspects
		try {
			__log.debug("Compiling deployment unit");
            du.compile(scope, processStore);
        } catch (CompilationException ce) {
            String errmsg = __msgs.msgDeployFailCompileErrors(ce);
            __log.error(errmsg, ce);
            throw new ContextException(errmsg, ce);
        }
        
        __log.debug("Scanning for compiled aspects");
        du.scan();
             
        final DeployAspectDocument dd = du.getDeploymentDescriptor();
        final ArrayList<AspectConfImpl> aspects = new ArrayList<AspectConfImpl>();


        if (_deploymentUnits.containsKey(du.getName())) {
            String errmsg = __msgs.msgDeployFailDuplicateDU(du.getName());
            __log.error(errmsg);
            throw new ContextException(errmsg);
        }

        // retirePreviousPackageVersions(du);
        __log.debug("Deploying aspects defined in DD: " + dd.getDeployAspect().getAspectList());
        
        // TODO: Deploy all aspects if there is no DD
        for (TDeploymentAspect.Aspect aspectDD : dd.getDeployAspect().getAspectList()) {
            QName aid = toAid(aspectDD.getName(), 0);

            if (_aspects.containsKey(aid)) {
                String errmsg = __msgs.msgDeployFailDuplicatePID(aspectDD.getName(), du.getName());
                __log.error(errmsg);
                throw new ContextException(errmsg);
            }

            CBAInfo cbaInfo = du.getCBAInfo(aspectDD.getName());
            if (cbaInfo == null) {
                String errmsg = "Aspect " + aspectDD.getName() + " not found in deployment unit " + du.getName();
                __log.error(errmsg);
                throw new ContextException(errmsg);
            }

            OAspect oaspect = du.getAspect(aid);
            AspectConfImpl aconf = new AspectConfImpl(aid, aspectDD.getName(), du, aspectDD, deployDate,
                    eprContext,
                    _configDir,
                    oaspect);
            aspects.add(aconf);
        }

        _deploymentUnits.put(du.getName(), du);

        for (AspectConfImpl aspect : aspects) {
            __log.info("Aspect deployed successfully: " + du.getDeployDir() + "," +  aspect.getAspectId());
            _aspects.put(aspect.getAspectId(), aspect);

        }

        return _aspects.keySet();
        
	}
	
	public Collection<QName> undeploy(final String duName) {
        
		__log.debug("AspectStore before undeployment: " + _aspects);
		
        Collection<QName> undeployed = Collections.emptyList();
        AspectDeploymentUnitDir du;
        
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
        
        __log.debug("AspectStore after undeployment: " + _aspects);
        
        return undeployed;
        
    }
	
	public Collection<AspectConfImpl> getAspects() {
		return _aspects.values();
	}

	public long getCurrentVersion() {
		// No versioning support for aspects
		return 0;
    }
	
	public Collection<QName> undeployAspect(final File dir) {
		return undeploy(dir.getName());
	}
	
	private List<QName> toAids(Collection<QName> aspectTypes, long version) {
        ArrayList<QName> result = new ArrayList<QName>();
        for (QName pqName : aspectTypes) {
            result.add(toAid(pqName, version));
        }
        return result;
    }

    private QName toAid(QName aspectType, long version) {
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
		return null;
	}

	public void registerListener(AspectStoreListener asl) {
		_listeners.add(asl);		
	}

	public void unregisterListener(AspectStoreListener asl) {
		_listeners.remove(asl);
	}
	
	protected void fireEvent(AspectStoreEvent ase) {
        __log.debug("firing event: " + ase);
        for (AspectStoreListener psl : _listeners)
            psl.onAspectStoreEvent(ase);
    }

	public AspectConfImpl getAspectConfiguration(QName aspectId) {
		return _aspects.get(aspectId);
	}

}
