package de.tud.stg.ao4ode.aspectstore;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.activityRecovery.FailureHandlingDocument.FailureHandling;
import org.apache.ode.bpel.dd.TDeploymentAspect;
import org.apache.ode.bpel.dd.TInvoke;
import org.apache.ode.bpel.dd.TService;
import org.apache.ode.bpel.iapi.Endpoint;
import org.apache.ode.bpel.iapi.EndpointReferenceContext;
import org.apache.ode.bpel.iapi.ProcessConf.PartnerRoleConfig;
import org.apache.ode.bpel.iapi.ProcessState;
import org.apache.ode.bpel.o.OAspect;
import org.apache.ode.bpel.o.OFailureHandling;

/**
 * Aspect Configuration
 * Based on/Stripped down version of ProcessConfImpl
 * 
 * @author A. Look
 */
public class AspectConfImpl {
    private static final Log __log = LogFactory.getLog(AspectConfImpl.class);

    private final Date _deployDate;
    private final HashMap<String, Endpoint> _partnerRoleInitialValues = new HashMap<String, Endpoint>();
    private final HashMap<String, PartnerRoleConfig> _partnerRoleConfig = new HashMap<String, PartnerRoleConfig>();

    ProcessState _state;
    final TDeploymentAspect.Aspect _ainfo;
    final AspectDeploymentUnitDir _du;
    private QName _aid;
    private QName _name;
    private OAspect oaspect;

    AspectConfImpl(QName pid, QName name, AspectDeploymentUnitDir du,
    		TDeploymentAspect.Aspect ainfo, Date deployDate,
                    EndpointReferenceContext eprContext,
                    File configDir, OAspect oaspect) {
        _aid = pid;
        _du = du;
        _ainfo = ainfo;
        _deployDate = deployDate;
        _name = name;
        this.oaspect = oaspect;

        initLinks();

    }

    private void initLinks() {
        if (_ainfo.getInvokeList() != null) {
            for (TInvoke invoke : _ainfo.getInvokeList()) {
                String plinkName = invoke.getPartnerLink();
                TService service = invoke.getService();
                // NOTE: service can be null for partner links
                if (service == null)
                    continue;
                __log.debug("Processing <invoke> element for aspect " + _ainfo.getName() + ": partnerlink " + plinkName + " --> "
                        + service);
                _partnerRoleInitialValues.put(plinkName, new Endpoint(service.getName(), service.getPort()));
                
                {
                    OFailureHandling g = null;
                    
                    if (invoke.isSetFailureHandling()) {
                        FailureHandling f = invoke.getFailureHandling();
                        g = new OFailureHandling();
                        if (f.isSetFaultOnFailure()) g.faultOnFailure = f.getFaultOnFailure();
                        if (f.isSetRetryDelay()) g.retryDelay = f.getRetryDelay();
                        if (f.isSetRetryFor()) g.retryFor = f.getRetryFor();
                    }
                    
                    PartnerRoleConfig c = new PartnerRoleConfig(g, invoke.getUsePeer2Peer());
                    
                    _partnerRoleConfig.put(plinkName, c);
                }
            }
        }
       
    }

    public Date getDeployDate() {
        return _deployDate;
    }

    public QName getAspectId() {
        return _aid;
    }
    
    public QName getName() {
        return _name;
    }
    
    public Map<String, Endpoint> getInvokeEndpoints() {
        return Collections.unmodifiableMap(_partnerRoleInitialValues);
    }

	public OAspect getOAspect() {
		return oaspect;
	}
 
	public String toString() {
		return "AspectContImpl: " + oaspect.toString();
	}
	
}