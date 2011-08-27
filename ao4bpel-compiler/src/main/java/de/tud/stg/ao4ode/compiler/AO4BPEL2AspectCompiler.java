package de.tud.stg.ao4ode.compiler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ode.bpel.compiler.BpelCompiler20;
import org.apache.ode.bpel.compiler.DefaultResourceFinder;
import org.apache.ode.bpel.compiler.ResourceFinder;
import org.apache.ode.bpel.compiler.api.CompilationException;
import org.apache.ode.bpel.compiler.api.CompilationMessage;
import org.apache.ode.bpel.compiler.bom.Activity;
import org.apache.ode.bpel.compiler.bom.Bpel11QNames;
import org.apache.ode.bpel.compiler.bom.Bpel20QNames;
import org.apache.ode.bpel.compiler.bom.BpelObject;
import org.apache.ode.bpel.compiler.bom.Import;
import org.apache.ode.bpel.compiler.bom.Property;
import org.apache.ode.bpel.compiler.bom.PropertyAlias;
import org.apache.ode.bpel.compiler.wsdl.Definition4BPEL;
import org.apache.ode.bpel.o.DebugInfo;
import org.apache.ode.bpel.o.OActivity;
import org.apache.ode.bpel.o.OAdvice;
import org.apache.ode.bpel.o.OAspect;
import org.apache.ode.bpel.o.OConstantVarType;
import org.apache.ode.bpel.o.OExpressionLanguage;
import org.apache.ode.bpel.o.OJPVarType;
import org.apache.ode.bpel.o.OPointcut;
import org.apache.ode.bpel.o.OScope;
import org.apache.ode.bpel.o.OVarType;
import org.apache.ode.utils.GUID;
import org.apache.ode.utils.StreamUtils;
import org.apache.ode.utils.SystemUtils;
import org.apache.ode.utils.xsl.XslTransformHandler;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import de.tud.stg.ao4ode.compiler.aom.Advice;
import de.tud.stg.ao4ode.compiler.aom.Aspect;
import de.tud.stg.ao4ode.compiler.aom.AspectObjectFactory;
import de.tud.stg.ao4ode.compiler.aom.Pointcut;

public class AO4BPEL2AspectCompiler extends BpelCompiler20 {
	
	// protected OAdvice _oprocess;
		
	public AO4BPEL2AspectCompiler() throws Exception {
		super();
	}

	public OAspect compileAspect(URL aspectURL, String scope) throws CompilationException, IOException {
		// Load aspect from file
		File aspectFile = new File(aspectURL.getFile());
		return compileAspect(aspectFile, scope);
	}
	
	public OAspect compileAspect(File aspectFile, String scope) throws CompilationException, IOException {
		
		// TODO: Parse aspect
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
        
        
        // TODO: Compile aspect
        ResourceFinder wf;
        File suDir = aspectFile.getParentFile(); 
        wf = new DefaultResourceFinder(
        		aspectFile.getAbsoluteFile().getParentFile(),
        		suDir.getAbsoluteFile());        	     
        this.setResourceFinder(wf);
 
        OAspect oaspect;
        try {
        	File _outputDir = new File(SystemUtils.userDirectory());
        	oaspect = this.compile(aspect,wf,scope);
        }
        catch (CompilationException cex) {
            throw cex;
        }
        
        return oaspect;
        
	}
	
	public OAspect compile(final Aspect aspect, ResourceFinder rf, String scope) throws CompilationException {
		
		OAspect oaspect = new OAspect();
		
		oaspect.aspectName = aspect.getName();
		
		if (aspect.getTargetNamespace() == null) {
            oaspect.targetNamespace = "--UNSPECIFIED--";
        } else {
            oaspect.targetNamespace = aspect.getTargetNamespace();
        }
		
		// TODO: Versioning of aspects?
		OAdvice oadvice = compile(aspect.getAdvice(), rf);
		
		oaspect.setScope(scope);
		
		// Compile pointcuts
        List<Pointcut> pointcuts = aspect.getPointcuts().getPointcuts();
        for(Pointcut pointcut: pointcuts) {
        	OPointcut oPointcut = new OPointcut(_oprocess, pointcut.getName(),
        			pointcut.getQuery());
        	oaspect.addPointcut(oPointcut);
        }
        
        oaspect.setAdvice(oadvice);
        
        return oaspect;
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
                
                // AO4ODE: ThisJPOutVariable has no declaration, so we need
                // to add it here                
                final OScope oscope = _structureStack.topScope();
                OVarType varType = new OJPVarType(_oprocess, OJPVarType.Type.OUT);
                OScope.Variable ovar = new OScope.Variable(_oprocess, varType);
                ovar.name = "ThisJPOutVariable";
                ovar.declaringScope = oscope;
                ovar.debugInfo = createDebugInfo(null, "Advice context variable");
                oscope.addLocalVariable(ovar);
                
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
