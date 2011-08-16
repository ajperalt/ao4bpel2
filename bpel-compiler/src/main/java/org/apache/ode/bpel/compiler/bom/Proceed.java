package org.apache.ode.bpel.compiler.bom;

import org.apache.ode.bpel.compiler.bom.Activity;
import org.w3c.dom.Element;

/**
 * AO4ODE: BOM representation of the AO4BPEL <code>&lt;proceed&gt;</code> activity.
 */
public class Proceed extends Activity {

    public Proceed(Element el) {
        super(el);
    }
}