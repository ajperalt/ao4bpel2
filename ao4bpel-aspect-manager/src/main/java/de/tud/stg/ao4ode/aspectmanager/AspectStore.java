package de.tud.stg.ao4ode.aspectmanager;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.ode.bpel.iapi.ProcessConf;
import org.apache.ode.bpel.iapi.ProcessStoreListener;

public interface AspectStore {
    /**
     * Deploys an aspect from the filesystem.
     * @param deploymentUnitDirectory directory containing all deployment files
     * @return a collection of aspect ids (deployed aspect)
     */
    Collection<QName> deployAspect(File deploymentUnitDirectory, String scope);

    /**
     * Undeploys a package.
     * @param file package
     * @return collection of successfully deployed aspect names
     */
    Collection<QName> undeployAspect(File file);

    /**
     * Lists the names of all the packages that have been deployed (corresponds
     * to a directory name on the file system).
     * @return an array of package names
     */
    Collection<String> getAspectPackages();

    /**
     * Lists all aspect ids in a given package.
     * @return an array of process id QNames
     */
    List<QName> listAspects(String packageName);

    /**
     * Get the list of aspects known to the store.
     * @return list of  aspect qnames
     */
    List<QName> getAspectList();
    
    /**
     * Register a listener
     * @param asl  {@link AspectStoreListener} 
     */
    void registerListener(AspectStoreListener asl);
    
    /**
     * Unregister listener
     * @param psl {@link AspectStoreListener} 
     */
    void unregisterListener(ProcessStoreListener asl);
    
    long getCurrentVersion();

	Collection<AspectConfImpl> getAspects();
	
	AspectConfImpl getAspectConfiguration(QName aspectId);

}