package com.heworks.snmpsim.agent;


import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.*;

/**
 * This class creates and returns ManagedObjects
 *
 * @author Shiva
 */
public class MOCreator {
    public static MOScalar createReadOnly(VariableBinding variableBinding) {
        return new MOScalar(variableBinding.getOid(),
                MOAccessImpl.ACCESS_READ_ONLY,
                variableBinding.getVariable());
    }

    public static MOScalar createReadOnly(OID oid, Variable value) {
        return new MOScalar(oid,
                MOAccessImpl.ACCESS_READ_ONLY,
                value);
    }

    public static Variable convertToVariable(String typeString, String valueString) {
        Variable value = null;
        switch(typeString) {
            case "STRING":
                value = new OctetString(valueString);
                break;
            case "Gauge32":
                //example: 14400000 milliseconds
                String longString = valueString;
                if (valueString.contains(" ")) {
                    longString = valueString.split(" ", 2)[0];
                }
                value = new Gauge32(Long.parseLong(longString));
                break;
            case "Counter32":
                String longValueString = valueString;
                if (valueString.contains(" ")) {
                    longValueString = valueString.split(" ", 2)[0];
                }
                value = new Counter32(Long.parseLong(longValueString));
                break;
            case "INTEGER":
                String integerString = valueString;
                //example: forwarding(1)
                if (valueString.contains("(") & valueString.contains(")")) {
                    integerString = valueString.substring(valueString.indexOf("(") + 1, valueString.indexOf(")"));
                }
                //example: 30 seconds
                else if (valueString.contains(" ")){
                    integerString = valueString.split(" ", 2)[0]; 
                }
                value = new Integer32(Integer.parseInt(integerString));
                break;
            case "Timeticks":
                String intString = valueString.substring(valueString.indexOf("(") + 1, valueString.indexOf(")"));
                value = new TimeTicks(Long.parseLong(intString));
                break;
            case "OID":
                value = new OID(valueString);
                break;
            case "Hex-STRING":
                value = OctetString.fromHexString(valueString.trim().replace(' ', ':'));
                break;
            case "IpAddress":
                value = new IpAddress(valueString.trim());
                break;
            case "Counter64":
                value = new Counter64(Long.parseLong(valueString));
                break;
            case "Network Address":
                value = OctetString.fromHexString(valueString.trim());
                break;
            default:
                throw new IllegalArgumentException("Unrecognized SNMP Type: " + typeString);
        }
        return value;
    }
//    private static Variable getVariable(Object value) {
//        if (value instanceof String) {
//            return new OctetString((String) value);
//        }
//        throw new IllegalArgumentException("Unmanaged Type: " + value.getClass());
//    }

}
