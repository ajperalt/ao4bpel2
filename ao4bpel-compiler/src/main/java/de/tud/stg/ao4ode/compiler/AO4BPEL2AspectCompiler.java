package de.tud.stg.ao4ode.compiler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.compiler.BpelC;
import org.apache.ode.bpel.compiler.BpelCompiler;
import org.apache.ode.bpel.compiler.BpelCompiler20;
import org.apache.ode.bpel.compiler.CommonCompilationMessages;
import org.apache.ode.bpel.compiler.DefaultResourceFinder;
import org.apache.ode.bpel.compiler.ResourceFinder;
import org.apache.ode.bpel.compiler.SourceLocationImpl;
import org.apache.ode.bpel.compiler.api.CompilationException;
import org.apache.ode.bpel.compiler.api.CompilationMessage;
import org.apache.ode.bpel.compiler.bom.Activity;
import org.apache.ode.bpel.compiler.bom.BpelObjectFactory;
import org.apache.ode.bpel.compiler.bom.Process;
import org.apache.ode.bpel.o.OProcess;
import org.apache.ode.utils.StreamUtils;
import org.apache.ode.utils.SystemUtils;
import org.apache.ode.utils.msg.MessageBundle;
import org.xml.sax.InputSource;

public class AO4BPEL2AspectCompiler extends BpelCompiler20 {
	
	BpelC bpelC = BpelC.newBpelCompiler();
	
	public AO4BPEL2AspectCompiler() throws Exception {
		super();
	}

	// TODO: Make this OAspect
	public OProcess compileAspect(URL aspectURL) throws CompilationException, IOException {
		
		File aspectFile = new File(aspectURL.getFile());
		
		Process process = null;
        try {
            InputSource isrc = new InputSource(new ByteArrayInputStream(StreamUtils.read(aspectFile.toURL())));
            isrc.setSystemId(aspectFile.getAbsolutePath());

            process = BpelObjectFactory.getInstance().parse(isrc,aspectFile.toURI());
        } catch (Exception e) {
        	e.printStackTrace();
        }

        assert process != null;
               
        
        // Compile
        ResourceFinder wf;

        File suDir = aspectFile.getParentFile(); 
        wf = new DefaultResourceFinder(aspectFile.getAbsoluteFile().getParentFile(), suDir.getAbsoluteFile());
        	        
        // compiler = new AO4BPEL2AspectCompiler();
        this.setResourceFinder(wf);
                

        // Compile aspect like a normal process
        // FIXME: Compile rootactivity in context of current process instead 
        OProcess oprocess;
        try {
        	File _outputDir = new File(SystemUtils.userDirectory());
            oprocess = this.compile(process,wf,BpelCompiler.getVersion(_outputDir.getAbsolutePath()));
        }
        catch (CompilationException cex) {
            throw cex;
        }
        
        return oprocess;		

        
        
		/*
		String wsdlURI = ("IncreaseCounter.wsdl");
		try {
			bpelC.setProcessWSDL(new URI(wsdlURI));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		bpelC.compile(aspectFile, version);
		*/
	}
	
}
