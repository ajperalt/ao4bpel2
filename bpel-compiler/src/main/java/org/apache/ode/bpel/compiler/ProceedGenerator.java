package org.apache.ode.bpel.compiler;

import org.apache.ode.bpel.compiler.bom.Activity;
import org.apache.ode.bpel.compiler.DefaultActivityGenerator;
import org.apache.ode.bpel.o.OActivity;
import org.apache.ode.bpel.o.OProceed;

/**
 * AO4ODE: ProceedGenerator
 */
class ProceedGenerator extends DefaultActivityGenerator {

    public void compile(OActivity output, Activity src) {
    }

    public OActivity newInstance(Activity src) {
        return new OProceed(_context.getOProcess(), _context.getCurrent());
    }

}