package com.heworks.snmpsim.agent;


import com.heworks.snmpsim.manager.SNMPManager;
import org.snmp4j.smi.OID;

import java.io.IOException;

public class TestSNMPAgent {

    static final OID sysDescr = new OID(".1.3.6.1.2.1.1.1.0");

    public static void main(String[] args) throws IOException {
        TestSNMPAgent client = new TestSNMPAgent("udp:127.0.0.1/161");
        client.init();
    }

    SNMPAgent agent = null;
    /**
     * This is the client which we have created earlier
     */
    SNMPManager client = null;

    String address = null;

    /**
     * Constructor
     *
     * @param add
     */
    public TestSNMPAgent(String add) {
        address = add;
    }

    private void init() throws IOException {
        agent = new SNMPAgent("172.17.1.102/2001");
        agent.start();

        // Since BaseAgent registers some MIBs by default we need to unregister
        // one before we register our own sysDescr. Normally you would
        // override that method and register the MIBs that you need
        agent.unregisterManagedObject(agent.getSnmpv2MIB());

        // Register a system description, use one from you product environment
        // to test with
//        agent.registerManagedObject(MOCreator.createReadOnly(sysDescr,
//                "This Description is set By ShivaSoft"));

        // Setup the client to use our newly started agent
        client = new SNMPManager("udp:172.17.1.102/2001");
        client.start();
        // Get back Value which is set
        System.out.println(client.getAsString(sysDescr));
    }

}