package de.tud.stg.ao4ode.compiler.aom;

import javax.xml.namespace.QName;

public abstract class AO4BPEL20QNames {
    
    // AO4ODE: AO4BPEL Aspect Namespace
    public static final String NS_AO4VPEL2_0_ASPECT = "http://stg.tu-darmstadt.de/ao4bpel/2.0/aspect";

    /** Some BPEL 2.0 Final Elements **/
    public static final QName ASPECT = newFinalQName("aspect");
    public static final QName ADVICE = newFinalQName("advice");
    public static final QName POINTCUT = newFinalQName("pointcut");
    public static final QName POINTCUTS = newFinalQName("pointcuts");
    
    private static QName newFinalQName(String localname) {
        return new QName(NS_AO4VPEL2_0_ASPECT, localname);
    }

}