package de.tud.stg.ao4ode.aspectmanager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.dd.DeployAspectDocument;
import org.apache.ode.bpel.dd.TDeployment;
import org.apache.ode.bpel.dd.TDeployment.Process;
import org.apache.ode.bpel.iapi.ContextException;
import org.apache.ode.bpel.o.OAspect;
import org.apache.ode.store.DeploymentUnitDir;
import org.apache.ode.store.DeploymentUnitDir.CBPInfo;
import org.apache.ode.utils.InternPool;
import org.apache.ode.utils.InternPool.InternableBlock;
import org.apache.ode.utils.fs.FileUtils;
import org.apache.xmlbeans.XmlOptions;

import de.tud.stg.ao4ode.compiler.AO4BPEL2AspectCompiler;

public class AspectDeploymentUnitDir {
	
	private static Log log = LogFactory.getLog(AspectDeploymentUnitDir.class);
	private File _duDirectory;
	private volatile DeployAspectDocument _dd;
		
	private static final FileFilter _aspectFilter = new FileFilter() {
        public boolean accept(File path) {
            return path.getName().endsWith(".aspect") && path.isFile();
        }
    };
    
    private static final FileFilter _cbaFilter = new FileFilter() {
        public boolean accept(File path) {
            return path.getName().endsWith(".cba") && path.isFile();
        }
    };
    
    private HashMap<QName, CBAInfo> _aspects = new HashMap<QName, CBAInfo>();
	
	AspectDeploymentUnitDir(File dir) {
		_duDirectory = dir;
	}
	
	/**
     * Compile all aspects in aspect deploy dir
	 * @param scope 
     */
    public void compile(String scope) {
    	
    	
    	List<File> aspects = FileUtils.directoryEntriesInPath(_duDirectory, AspectDeploymentUnitDir._aspectFilter);
        if (aspects.size() == 0)
            throw new IllegalArgumentException("Directory " + _duDirectory.getName() + " does not contain any aspects!");
        for (File aspect : aspects) {
        	String b = aspect.getAbsolutePath();
        	File cba = new File(b.substring(0,b.lastIndexOf(".aspect")) + ".cba"); 
        	if (!cba.exists() || cba.lastModified() < aspect.lastModified()) {
        		log.debug("compiling " + aspect);
        		compile(aspect, scope);
        	} else {
        		log.debug("skipping compilation of " + aspect + " cba found: " + cba);
        	}
        }
        
    }
    
    private void compile(final File aspectFile, final String scope) {
    	
        // Create aspect such that immutable objects are intern'ed.
        InternPool.runBlock(new InternableBlock() {
        	public void run() {
                try {
                    
                    AO4BPEL2AspectCompiler compiler = new AO4BPEL2AspectCompiler();
                    
                    OAspect oaspect = compiler.compileAspect(aspectFile, scope);
                    
                    // TODO: Serialize compiled aspect to .cba
                    AspectSerializer aspectSerializer = new AspectSerializer();
                    String cbaPath = aspectFile.getPath().substring(0, aspectFile.getPath().lastIndexOf(".")) + ".cba";
                    OutputStream cbaOut = new BufferedOutputStream(new FileOutputStream(cbaPath));                    
                    aspectSerializer.writeOAspect(oaspect, cbaOut);
                       
                } catch (IOException e) {
                    log.error("Compile error in " + aspectFile, e);
                } catch (Exception e) {
                	log.error("Compile error in " + aspectFile, e);
					e.printStackTrace();
				}
        	}
        });
    }
    
    protected void scan() {

    	HashMap<QName, CBAInfo> aspects = new HashMap<QName, CBAInfo>();
        List<File> cbas = FileUtils.directoryEntriesInPath(_duDirectory, AspectDeploymentUnitDir._cbaFilter);
        for (File file : cbas) {
            CBAInfo cbainfo = loadCBAInfo(file);
            aspects.put(cbainfo.aspectName, cbainfo);
        }
        _aspects = aspects;
    	
    }
    
    /**
     * Load the parsed and compiled Aspect definition.
     */
    private CBAInfo loadCBAInfo(File f) {
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            AspectSerializer as = new AspectSerializer();
            OAspect oaspect = as.readOAspect(is);
            // FIXME: Versioning
            log.debug("LoadCBAINFO: namespace: " + oaspect.targetNamespace + ", oaspect.aspectNAme: " + oaspect.aspectName);
            QName aspectName = new QName(oaspect.targetNamespace, oaspect.aspectName);
            log.debug("QName: " + aspectName);
            CBAInfo info = new CBAInfo(aspectName, f);
            return info;
        } catch (Exception e) {
            throw new ContextException("Couldn't read compiled Aspect " + f.getAbsolutePath(), e);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (Exception e) {
                ;
            }
        }
    }
	
	public final class CBAInfo {
	        final QName aspectName;
	        final File cba;

	        CBAInfo(QName aspectName, File cba) {
	            this.aspectName = aspectName;
	            this.cba = cba;
	        }
	}
	
	CBAInfo getCBAInfo(QName aspectName) {
		log.debug("Looking for CBAInfo: " + aspectName);
		log.debug("_aspects: " + _aspects);
        return _aspects.get(aspectName);
    }

	public OAspect getAspect(QName aspectName) {
		CBAInfo cbai = _aspects.get(aspectName);
		
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cbai.cba));
			OAspect oaspect = (OAspect)ois.readObject();
			return oaspect;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return null;
		
	}

	public Collection<QName> getAspects() {
		return _aspects.keySet();
	}
	
	public String getName() {
        return _duDirectory.getName();
    }

	public Set<QName> getAspectNames() {
		return _aspects.keySet();
    }
	
	// TODO:
	public long getVersion() {
        return 0;
    }
	
	public DeployAspectDocument getDeploymentDescriptor() {
        if (_dd == null) {
            File ddLocation = new File(_duDirectory, "deploy.xml");
            try {
                XmlOptions options = new XmlOptions();
                HashMap otherNs = new HashMap();

                otherNs.put("http://ode.fivesight.com/schemas/2006/06/27/dd",
                        "http://www.apache.org/ode/schemas/dd/2007/03");
                options.setLoadSubstituteNamespaces(otherNs);
                _dd = DeployAspectDocument.Factory.parse(ddLocation, options);
            } catch (Exception e) {
                throw new ContextException("Couldn't read deployment descriptor at location "
                        + ddLocation.getAbsolutePath(), e);
            }

        }
        return _dd;
    }
	
	public File getDeployDir() {
		return _duDirectory;
	}
    
}
