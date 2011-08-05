/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package de.tud.stg.ao4ode.aspectmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.activityRecovery.FailureHandlingDocument.FailureHandling;
import org.apache.ode.bpel.dd.TDeployment;
import org.apache.ode.bpel.dd.TDeploymentAspect;
import org.apache.ode.bpel.dd.TInvoke;
import org.apache.ode.bpel.dd.TMexInterceptor;
import org.apache.ode.bpel.dd.TProvide;
import org.apache.ode.bpel.dd.TService;
import org.apache.ode.bpel.iapi.ContextException;
import org.apache.ode.bpel.iapi.Endpoint;
import org.apache.ode.bpel.iapi.EndpointReferenceContext;
import org.apache.ode.bpel.iapi.ProcessConf.PartnerRoleConfig;
import org.apache.ode.bpel.iapi.ProcessState;
import org.apache.ode.bpel.o.OAspect;
import org.apache.ode.bpel.o.OFailureHandling;
import org.apache.ode.utils.DOMUtils;
import org.w3c.dom.Element;

import de.tud.stg.ao4ode.aspectmanager.AspectDeploymentUnitDir.CBAInfo;

/**
 * Aspect Configuration. Based on ProcessConfImpl.
 * TODO: Remove obsolete code
 */
public class AspectConfImpl {
    private static final Log __log = LogFactory.getLog(AspectConfImpl.class);

    private final Date _deployDate;
    private final HashMap<String, Endpoint> _partnerRoleInitialValues = new HashMap<String, Endpoint>();
    private final HashMap<String, PartnerRoleConfig> _partnerRoleConfig = new HashMap<String, PartnerRoleConfig>();

    private final HashMap<String, Endpoint> _myRoleEndpoints = new HashMap<String, Endpoint>();
    private final ArrayList<QName> _sharedServices = new ArrayList<QName>();
    private final ArrayList<String> _mexi = new ArrayList<String>();
    ProcessState _state;
    final TDeploymentAspect.Aspect _ainfo;
    final AspectDeploymentUnitDir _du;
    private long _version = 0;
    private QName _aid;
    private QName _type;
    private OAspect oaspect;

    AspectConfImpl(QName pid, QName type, long version, AspectDeploymentUnitDir du, TDeploymentAspect.Aspect ainfo, Date deployDate,
                    EndpointReferenceContext eprContext, File configDir, OAspect oaspect) {
        _aid = pid;
        _version = version;
        _du = du;
        _ainfo = ainfo;
        _deployDate = deployDate;
        _type = type;
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
                    __log.debug("PartnerRoleConfig for " + plinkName + " " + c.failureHandling + " usePeer2Peer: " + c.usePeer2Peer);
                    _partnerRoleConfig.put(plinkName, c);
                }
            }
        }
       
    }

    public Date getDeployDate() {
        return _deployDate;
    }

    public String getDeployer() {
        return "";
    }

    public List<File> getFiles() {
        // TODO: return _du.allFiles();
    	return null;
    }

    public QName getAspectId() {
        return _aid;
    }

    public String getPackage() {
        return _du.getName();
    }

    public long getVersion() {
        return _version;
    }

    public InputStream getCBAInputStream() {
        CBAInfo cbaInfo = _du.getCBAInfo(_ainfo.getName());
        if (cbaInfo == null)
            throw new ContextException("CBA record not found for " + _ainfo.getName());
        try {
            return new FileInputStream(cbaInfo.cba);
        } catch (FileNotFoundException e) {
            throw new ContextException("File Not Found: " + cbaInfo.cba, e);
        }
    }

    public long getCBPFileSize() {
        CBAInfo cbpInfo = _du.getCBAInfo(_ainfo.getName());
        if (cbpInfo == null)
            throw new ContextException("CBA record not found for " + _ainfo.getName());
        return cbpInfo.cba.length();
    }
    
    public String getAspectDocument() {
        CBAInfo cbaInfo = _du.getCBAInfo(_ainfo.getName());
        if (cbaInfo == null)
            throw new ContextException("CBA record not found for " + _ainfo.getName());
        try {
            String relative = getRelativePath(_du.getDeployDir(), cbaInfo.cba).replaceAll("\\\\", "/");
            if (!relative.endsWith(".cbp"))
                throw new ContextException("CBA file must end with .cba suffix: " + cbaInfo.cba);
            relative = relative.replace(".cba", ".aspect");
            File aspectFile = new File(_du.getDeployDir(), relative);
            if (!aspectFile.exists()) __log.warn("Aspect file does not exist: " + aspectFile);
            return relative;
        } catch (IOException e) {
            throw new ContextException("IOException in getAspectDocument: " + cbaInfo.cba, e);
        }
    }

    public URI getBaseURI() {
        return _du.getDeployDir().toURI();
    }

    public ProcessState getState() {
        return _state;
    }

    void setState(ProcessState state) {
        _state = state;
    }

    public List<String> getMexInterceptors(QName processId) {
        return Collections.unmodifiableList(_mexi);
    }

    public Map<String, Endpoint> getInvokeEndpoints() {
        return Collections.unmodifiableMap(_partnerRoleInitialValues);
    }

    public Map<String, PartnerRoleConfig> getPartnerRoleConfig() {
        return Collections.unmodifiableMap(_partnerRoleConfig);
    }

    public Map<String, Endpoint> getProvideEndpoints() {
        return Collections.unmodifiableMap(_myRoleEndpoints);
    }

    public boolean isSharedService(QName serviceName) {
        return _sharedServices.contains(serviceName);
    }

    @SuppressWarnings("unused")
    private void handleEndpoints() {
        // for (TProvide provide : _pinfo.getProvideList()) {
        // OPartnerLink pLink = _oprocess.getPartnerLink(provide.getPartnerLink());
        // if (pLink == null) {
        // String msg = __msgs.msgDDPartnerLinkNotFound(provide.getPartnerLink());
        // __log.error(msg);
        // throw new BpelEngineException(msg);
        // }
        // if (!pLink.hasMyRole()) {
        // String msg = __msgs.msgDDMyRoleNotFound(provide.getPartnerLink());
        // __log.error(msg);
        // throw new BpelEngineException(msg);
        // }
        // }
        // for (TInvoke invoke : _pinfo.getInvokeList()) {
        // OPartnerLink pLink = _oprocess.getPartnerLink(invoke.getPartnerLink());
        // if (pLink == null) {
        // String msg = __msgs.msgDDPartnerLinkNotFound(invoke.getPartnerLink());
        // __log.error(msg);
        // throw new BpelEngineException(msg);
        // }
        // if (!pLink.hasPartnerRole()) {
        // String msg = __msgs.msgDDPartnerRoleNotFound(invoke.getPartnerLink());
        // __log.error(msg);
        // throw new BpelEngineException(msg);
        // }
        // TODO Handle non initialize partner roles that just provide a binding
        // if (!pLink.initializePartnerRole && _oprocess.version.equals(Namespaces.WS_BPEL_20_NS)) {
        // String msg = ProcessDDInitializer.__msgs.msgDDNoInitiliazePartnerRole(invoke.getPartnerLink());
        // ProcessDDInitializer.__log.error(msg);
        // throw new BpelEngineException(msg);
        // }
        // }
    }

    AspectDeploymentUnitDir getDeploymentUnit() {
        return _du;
    }

    private String getRelativePath(File base, File path) throws IOException {
        String basePath = base.getCanonicalPath();
        String cbpPath = path.getCanonicalPath();
        if (!cbpPath.startsWith(basePath))
            throw new IOException("Invalid relative path: base=" + base + " path=" + path);
        String relative = cbpPath.substring(basePath.length());
        if (relative.startsWith(File.separator)) relative = relative.substring(1);
        return relative;
    }

    public List<Element> getExtensionElement(QName qname) {
        try {
            return DOMUtils.findChildrenByName(DOMUtils.stringToDOM(_ainfo.toString()), qname);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

	public OAspect getOAspect() {
		return oaspect;
	}
 
	public String toString() {
		return "AspectContImpl: " + oaspect.toString();
	}
	
}