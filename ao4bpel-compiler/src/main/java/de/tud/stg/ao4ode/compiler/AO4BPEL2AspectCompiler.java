package de.tud.stg.ao4ode.compiler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.ode.bpel.compiler.BpelCompiler;
import org.apache.ode.bpel.compiler.BpelCompiler20;
import org.apache.ode.bpel.compiler.DefaultResourceFinder;
import org.apache.ode.bpel.compiler.ResourceFinder;
import org.apache.ode.bpel.compiler.api.CompilationException;
import org.apache.ode.bpel.compiler.api.CompilationMessage;
import org.apache.ode.bpel.compiler.bom.Bpel11QNames;
import org.apache.ode.bpel.compiler.bom.Bpel20QNames;
import org.apache.ode.bpel.compiler.bom.Import;
import org.apache.ode.bpel.compiler.bom.Property;
import org.apache.ode.bpel.compiler.bom.PropertyAlias;
import org.apache.ode.bpel.compiler.wsdl.Definition4BPEL;
import org.apache.ode.bpel.o.OAdvice;
import org.apache.ode.bpel.o.OAspect;
import org.apache.ode.bpel.o.OConstantVarType;
import org.apache.ode.bpel.o.OExpressionLanguage;
import org.apache.ode.bpel.o.OJPVarType;
import org.apache.ode.bpel.o.OPointcut;
import org.apache.ode.bpel.o.OScope;
import org.apache.ode.bpel.o.OVarType;
import org.apache.ode.store.DeploymentUnitDir;
import org.apache.ode.store.ProcessStoreImpl;
import org.apache.ode.utils.GUID;
import org.apache.ode.utils.StreamUtils;

import org.apache.ode.utils.fs.FileUtils;
import org.apache.ode.utils.xsl.XslTransformHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.utils.PrefixResolver;
import com.sun.org.apache.xml.internal.utils.PrefixResolverDefault;

import de.tud.stg.ao4ode.compiler.aom.Advice;
import de.tud.stg.ao4ode.compiler.aom.Aspect;
import de.tud.stg.ao4ode.compiler.aom.AspectObjectFactory;
import de.tud.stg.ao4ode.compiler.aom.Pointcut;

/**
 * AO4BPEL 2.0 aspect compiler
 * 
 * @author A. Look
 *
 */
public class AO4BPEL2AspectCompiler extends BpelCompiler20 {
	
	private ProcessStoreImpl processStore;

	public AO4BPEL2AspectCompiler(ProcessStoreImpl processStore) throws Exception {
		super();
		this.processStore = processStore;
	}

	public OAspect compileAspect(URL aspectURL, String scope) throws CompilationException, IOException {
		File aspectFile = new File(aspectURL.getFile());
		return compileAspect(aspectFile, scope);
	}
	
	public OAspect compileAspect(File aspectFile, String scope) throws CompilationException, IOException {
		
		Aspect aspect = null;
        try {
            InputSource isrc = new InputSource(new ByteArrayInputStream(StreamUtils.read(aspectFile.toURL())));
            isrc.setSystemId(aspectFile.getAbsolutePath());

            aspect = AspectObjectFactory.getInstance().parseAspect(isrc,aspectFile.toURI());
        } catch (Exception e) {
        	e.printStackTrace();
        }

        assert aspect != null;
        
        assert aspect.getAdvice() != null;
        
        ResourceFinder wf;
        File suDir = aspectFile.getParentFile(); 
        wf = new DefaultResourceFinder(
        		aspectFile.getAbsoluteFile().getParentFile(),
        		suDir.getAbsoluteFile());        	     
        this.setResourceFinder(wf);
 
        OAspect oaspect;
        try {
        	oaspect = this.compile(aspect,wf,scope,aspectFile.getAbsoluteFile());
        }
        catch (CompilationException cex) {
            throw cex;
        }
        
        return oaspect;
        
	}
	
	public OAspect compile(final Aspect aspect, ResourceFinder rf, String scope, File aspectFile) throws CompilationException {
		
		OAspect oaspect = new OAspect();
		
		oaspect.aspectName = aspect.getName();
		
		if (aspect.getTargetNamespace() == null) {
            oaspect.targetNamespace = "--UNSPECIFIED--";
        } else {
            oaspect.targetNamespace = aspect.getTargetNamespace();
        }
		
		OAdvice oadvice = compile(aspect.getAdvice(), rf);
		
		oaspect.setScope(scope);
		
		// Compile pointcuts
        List<Pointcut> pointcuts = aspect.getPointcuts().getPointcuts();
        for(Pointcut pointcut: pointcuts) {
        	OPointcut oPointcut = new OPointcut(_oprocess,
        			pointcut.getName(),
        			pointcut.getLanguage(),
        			pointcut.getQuery());
        	oaspect.addPointcut(oPointcut);
        	

        	// Replace xpath with prolog pointcuts
        	if(oPointcut.getLanguage().equals("xpath")) {
        		List<File> bpelFiles = getBpelFiles();
        		replaceXPathPointcuts(oPointcut, bpelFiles);
        	}
        }
        
        oaspect.setAdvice(oadvice);
        
        return oaspect;
	}
	
	
	private List<File> getBpelFiles() {		
		List<File> allProcesses = new ArrayList<File>();
		Collection<DeploymentUnitDir> processDeploymentUnits = processStore._deploymentUnits.values();
		for(DeploymentUnitDir du : processDeploymentUnits) {
			List<File> bpels = FileUtils.directoryEntriesInPath(du.getDeployDir(), DeploymentUnitDir._bpelFilter);
			allProcesses.addAll(bpels);
		}
		
		return allProcesses;
	}

	public void replaceXPathPointcuts(OPointcut oPointcut, List<File> bpelFiles) {
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
    	Map<String,NodeList> nodeLists = new HashMap<String,NodeList>();
    	
    	// Evaluate XPath and get all matching nodes from all deployed processes
		for(File bpelFile : bpelFiles) {
			
        	DocumentBuilder builder;
        	Document doc = null;
        	
			try {
				builder = factory.newDocumentBuilder();
				try {					
					doc = builder.parse(bpelFile);
					
					// Resolve the namespace prefixes using the
					// declarations from the bpel document
					final PrefixResolver resolver =
		    			new PrefixResolverDefault(doc.getDocumentElement());
					
		    		NamespaceContext ctx = new NamespaceContext() {
		    			public String getNamespaceURI(String prefix) {		    				
		    				String resolved = resolver.getNamespaceForPrefix(prefix); 
		    				return resolved;
		    			}
		    			// not used
		    			@SuppressWarnings("rawtypes")
						public Iterator getPrefixes(String val) {
		    				return null;
		    			}
		    			// not used
		    			public String getPrefix(String uri) {
		    				return null;
		    			}
		    		};
					
					doc.normalizeDocument();
										
					XPathFactory xfactory = XPathFactory.newInstance();										
		    		XPath xpath = xfactory.newXPath();
		    		xpath.setNamespaceContext(ctx);	    		
		    		String xpathString = oPointcut.getQuery();
		    		try {
						XPathExpression expr = xpath.compile(xpathString);
						Object result = expr.evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
						NodeList nodes = (NodeList) result;
						if(nodes.getLength() > 0)
							nodeLists.put(bpelFile.getName(), nodes);				
					} catch (XPathExpressionException e) {
						e.printStackTrace();
					}
					
				} catch (MalformedURLException e1) {					
					e1.printStackTrace();
				} catch (SAXException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch(Exception e) {
					e.printStackTrace();
				}
				
			} catch (ParserConfigurationException e1) {
				e1.printStackTrace();
			}			        		
    		
		}
		
		// Create prolog pointcut for nodes matched by xpath
		StringBuffer buf = new StringBuffer("(");
		int p = 0;
		for(String process : nodeLists.keySet()) {		
			NodeList nodes = nodeLists.get(process);
			if(nodes.getLength() > 0) {
				__log.debug("Found " + nodes.getLength() + " matching Elements for process " + process);
			
				for (int i = 0; i < nodes.getLength(); i++) {
					Node n = nodes.item(i);
			    	buf.append("xpath(\"" + BpelCompiler.createXPath(n) + "\")");
			    	if(i < nodes.getLength()-1)
			    		buf.append(";");
				}
				
				if(p < nodeLists.size()-1)
		    		buf.append(";");
				else
					buf.append(").");
			}			
			p++;
		}
		
		__log.debug("Replacing xpath pointcut " + oPointcut.getQuery()
				+ " with prolog pointcut " + buf.toString());
		
		if(buf.length() > 1) {
			oPointcut.setLanguage("prolog");
			oPointcut.setQuery(buf.toString());
		}
		else {
			oPointcut.setQuery("false.");
		}
		
	}
	
	/**
     * Compile advice (based on BPEL compiler)
     */
    public OAdvice compile(final Advice advice, ResourceFinder rf) throws CompilationException {
    	    	
        if (advice == null)
            throw new NullPointerException("Null process parameter");
        
        setResourceFinder(rf);
        _processURI = advice.getURI();
        _processDef = advice;
        _generatedDate = new Date();
        _structureStack.clear();

        String bpelVersionUri = null;
        switch (advice.getBpelVersion()) {
        case BPEL11:
            bpelVersionUri = Bpel11QNames.NS_BPEL4WS_2003_03;
            break;
        case BPEL20_DRAFT:
            bpelVersionUri = Bpel20QNames.NS_WSBPEL2_0;
            break;
        case BPEL20:
            bpelVersionUri = Bpel20QNames.NS_WSBPEL2_0_FINAL_EXEC;
            break;
        default:
            throw new IllegalStateException("Bad bpel version: " + advice.getBpelVersion());
        }

        _oprocess = new OAdvice(bpelVersionUri);
        OAdvice oadvice = (OAdvice) _oprocess;
        _oprocess.guid = null;
        _oprocess.constants = makeConstants();
        _oprocess.debugInfo = createDebugInfo(advice, "process");
        
        if (advice.getTargetNamespace() == null) {
            _oprocess.targetNamespace = "--UNSPECIFIED--";
            recoveredFromError(advice, new CompilationException(__cmsgs.errProcessNamespaceNotSpecified()));
        } else {
            _oprocess.targetNamespace = _processDef.getTargetNamespace();
        }
        
        if (advice.getName() == null) {
            _oprocess.processName = "--UNSPECIFIED--";
            recoveredFromError(advice, new CompilationException(__cmsgs.errProcessNameNotSpecified()));
        } else {
            _oprocess.processName = _processDef.getName();
        }
        
        String type = advice.getAdviceType();
        if (type != null) {
        	if(type.equals("before"))
        		oadvice.setType(OAdvice.TYPE.BEFORE);
        	else if(type.equals("after"))
        		oadvice.setType(OAdvice.TYPE.AFTER);
        	else if(type.equals("around"))
        		oadvice.setType(OAdvice.TYPE.AROUND);
        }
                
        _oprocess.compileDate = _generatedDate;

        _konstExprLang = new OExpressionLanguage(_oprocess, null);
        _konstExprLang.debugInfo = createDebugInfo(_processDef, "Constant Value Expression Language");
        _konstExprLang.expressionLanguageUri = "uri:www.fivesight.com/konstExpression";
        _konstExprLang.properties.put("runtime-class",
                "org.apache.ode.bpel.runtime.explang.konst.KonstExpressionLanguageRuntimeImpl");
        _oprocess.expressionLanguages.add(_konstExprLang);

        // Process the imports. Note, we expect all processes (Event BPEL 1.1)
        // to have an import declaration. This should be automatically generated
        // by the 1.1 parser.
        for (Import imprt : _processDef.getImports()) {
            try {
                compile(_processURI, imprt);
            } catch (CompilationException bce) {
                // We try to recover from import problems by continuing
                recoveredFromError(imprt, bce);
            }
        }

        _expressionValidatorFactory.getValidator().bpelImportsLoaded(_processDef, this);
        
        switch (_processDef.getSuppressJoinFailure()) {
        case NO:
        case NOTSET:
            _supressJoinFailure = false;
            break;
        case YES:
            _supressJoinFailure = true;
            break;
        }
        // compile ALL wsdl properties; needed for property extraction
        Definition4BPEL[] defs = _wsdlRegistry.getDefinitions();
        for (Definition4BPEL def : defs) {
            for (Property property : def.getProperties()) {
                compile(property);
            }
        }
        // compile ALL wsdl property aliases
        for (Definition4BPEL def1 : defs) {
            for (PropertyAlias propertyAlias : def1.getPropertyAliases()) {
                compile(propertyAlias);
            }
        }

        OScope procesScope = new OScope(_oprocess, null);
        procesScope.name = "__PROCESS_SCOPE:" + advice.getName();
        procesScope.debugInfo = createDebugInfo(advice, null);
        _oprocess.procesScope = compileScope(procesScope, advice, new Runnable() {
            public void run() {
                if (advice.getRootActivity() == null) {
                    throw new CompilationException(__cmsgs.errNoRootActivity());
                }
                // Process custom properties are created as variables associated
                // with the top scope
                if (_customProcessProperties != null) {
                    for (Map.Entry<QName, Node> customVar : _customProcessProperties.entrySet()) {
                        final OScope oscope = _structureStack.topScope();
                        OVarType varType = new OConstantVarType(_oprocess, customVar.getValue());
                        OScope.Variable ovar = new OScope.Variable(_oprocess, varType);
                        ovar.name = customVar.getKey().getLocalPart();
                        ovar.declaringScope = oscope;
                        ovar.debugInfo = createDebugInfo(null, "Process custom property variable");
                        oscope.addLocalVariable(ovar);
                        if (__log.isDebugEnabled())
                            __log.debug("Compiled custom property variable " + ovar);
                    }
                }
                
                // AO4ODE: ThisJP variables have no declaration, so we need
                // to add them here
                
                // ThisJPOutVariable
                final OScope oscope = _structureStack.topScope();
                OVarType varTypeOut = new OJPVarType(_oprocess, OJPVarType.Type.OUT);
                OScope.Variable ovarOut = new OScope.Variable(_oprocess, varTypeOut);
                ovarOut.name = "ThisJPOutVariable";
                ovarOut.declaringScope = oscope;
                ovarOut.debugInfo = createDebugInfo(null, "Advice context variable ThisJPOutVariable");
                oscope.addLocalVariable(ovarOut);
                
                // ThisJPInVariable
                OVarType varTypeIn = new OJPVarType(_oprocess, OJPVarType.Type.IN);
                OScope.Variable ovarIn = new OScope.Variable(_oprocess, varTypeIn);
                ovarIn.name = "ThisJPInVariable";
                ovarIn.declaringScope = oscope;
                ovarIn.debugInfo = createDebugInfo(null, "Advice context variable ThisJPInVariable");
                oscope.addLocalVariable(ovarIn);
                
                // ThisJPActivity
                OVarType varTypeJPActivity = new OJPVarType(_oprocess, OJPVarType.Type.META);
                OScope.Variable ovarJPActivity = new OScope.Variable(_oprocess, varTypeJPActivity);
                ovarJPActivity.name = "ThisJPActivity";
                ovarJPActivity.declaringScope = oscope;
                ovarJPActivity.debugInfo = createDebugInfo(null, "Advice context variable ThisJPActivity");
                oscope.addLocalVariable(ovarJPActivity);
                
                _structureStack.topScope().activity = compile(advice.getRootActivity());
            }
        });

        assert _structureStack.size() == 0;

        boolean hasErrors = false;
        StringBuffer sb = new StringBuffer();
        for (CompilationMessage msg : _errors) {
            if (msg.severity >= CompilationMessage.ERROR) {
                hasErrors = true;
                sb.append('\t');
                sb.append(msg.toErrorString());
                sb.append('\n');
            }
        }
        
        XslTransformHandler.getInstance().clearXSLSheets(_oprocess.getQName());

        _expressionValidatorFactory.getValidator().bpelCompilationCompleted(_processDef);
        
        if (hasErrors) {
            throw new CompilationException(__cmsgs.errCompilationErrors(_errors.size(), sb.toString()));
        }
        
        {
            String digest = "version:" + 1L + ";" + _oprocess.digest();
            _oprocess.guid = GUID.makeGUID(digest);
            if (__log.isDebugEnabled()) {
                __log.debug("Compiled process digest: " + digest + "\nguid: " + _oprocess.guid);
            }
        }
        return (OAdvice)_oprocess;
    }
	
}
