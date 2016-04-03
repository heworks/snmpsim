package com.heworks.snmpsim.agent;

import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.VariableBinding;

/**
 * Created by m2c2 on 3/12/16.
 */
public class MOWrapper {

    MOScalar mo = null;

    public MOWrapper(MOScalar mo) {
        this.mo = mo;
    }

    public boolean isSameRange(MOScalar other) {
        if(other.getLowerBound().equals(mo.getLowerBound()) && other.getUpperBound().equals(mo.getUpperBound())) {
            return true;
        }
        return false;
    }

    public MOScalar getMO() {
        return this.mo;
    }

    public boolean setValue(VariableBinding variableBinding) {
        return mo.setValue(variableBinding);
    }

}
