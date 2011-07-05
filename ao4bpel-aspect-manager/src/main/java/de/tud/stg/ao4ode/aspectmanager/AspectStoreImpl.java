package de.tud.stg.ao4ode.aspectmanager;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.compiler.api.CompilationException;
import org.apache.ode.bpel.iapi.ContextException;
import org.apache.ode.bpel.iapi.EndpointReferenceContext;
import org.apache.ode.bpel.iapi.ProcessStoreListener;
import org.apache.ode.bpel.o.OAspect;
import org.apache.ode.il.config.OdeConfigProperties;
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
		UUID uuid = UUID.randomUUID();
		return uuid.getMostSignificantBits();
    }

	public Collection<QName> deployAspect(File deploymentUnitDirectory) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<QName> undeployAspect(File file) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<String> getAspectPackages() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<QName> listAspects(String packageName) {
		// TODO Auto-generated method stub
		return null;
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

}
