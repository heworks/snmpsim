package com.heworks.snmpsim.agent;


import com.heworks.snmpsim.manager.SNMPManager;
import com.heworks.snmpsim.mibReader.MibReader;
import org.snmp4j.agent.mo.MOTable;
import org.snmp4j.log.LogAdapter;
import org.snmp4j.log.LogFactory;
import org.snmp4j.smi.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Simulator {
    private static final LogAdapter LOGGER = LogFactory.getLogger(Simulator.class);
    private static final OID DEVICE_NAME_OID = new OID("1.3.6.1.2.1.1.5.0");
    private static final OID DEVICE_SERIAL_CISCO_OID = new OID("1.3.6.1.4.1.9.3.6.3.0");
    private static final OID DEVICE_SERIAL_JUNIPER_OID = new OID("1.3.6.1.4.1.2636.3.1.3.0");
    private static final String PREFIX = "SNMP4J-";

    private List<SNMPAgent> agents = new ArrayList<SNMPAgent>();
    private SNMPManager client = null;
    private MibReader mibReader;

    /**
     * Create a new simulator.
     * @param ips the ips to simulate
     * @param port the snmp port to use
     * @param walkFiles the walk files to simulate
     * @param mibFolder the mib folder that contains the mibs to simulate                 
     * @throws IOException
     */
    public Simulator(List<String> ips, int port, File walkFiles, File mibFolder) throws IOException {
        this.mibReader = new MibReader(mibFolder);
        initAllAgent(ips, port, walkFiles);
    }

    private void initAllAgent(List<String> ips, int port, File walkFile) throws IOException {

        for (String ip : ips) {
            SNMPAgent agent = new SNMPAgent(ip, port);
            this.agents.add(agent);
            agent.start();
        }
        LOGGER.warn("agents started");

        // Since BaseAgent registers some MIBs by default we need to unregister
        // one before we register our own sysDescr. Normally you would
        // override that method and register the MIBs that you need
//        agent.unregisterManagedObject(agent.getSnmpFrameworkMIB());
//        agent.unregisterManagedObject(agent.getSnmpCommunityMIB());
//        agent.unregisterManagedObject(agent.getUsmMIB());
//        agent.unregisterManagedObject(agent.getSnmp4jConfigMIB());
//        agent.unregisterManagedObject(agent.getSnmpNotificationMIB());
//        agent.unregisterManagedObject(agent.getSnmp4jLogMIB());
//        agent.unregisterManagedObject(agent.getSnmpProxyMIB());
//        agent.unregisterManagedObject(agent.getSnmpTargetMIB());

        loadWalkFile(walkFile);
        registerSpecialOIDs();
        testAgents(); 
    }

    /**
     * Register certain OIDs to the agents.
     */
    private void registerSpecialOIDs() {
        LOGGER.warn("Register special OIDs.");
        for (SNMPAgent agent : this.agents) {
            String ipString = agent.getAddress().replace(".", "-");
            agent.registerManagedObject(MOCreator.createReadOnly(DEVICE_NAME_OID, new OctetString(PREFIX + ipString)));
            agent.registerManagedObject(MOCreator.createReadOnly(DEVICE_SERIAL_CISCO_OID, new OctetString(PREFIX + ipString)));
            agent.registerManagedObject(MOCreator.createReadOnly(DEVICE_SERIAL_JUNIPER_OID, new OctetString(PREFIX + ipString)));
        }
        LOGGER.warn("Done");
    }

    /**
     * Test if device returns certain OID as expected.
     * @throws IOException
     */
    private void testAgents() throws IOException {
        LOGGER.warn("Testing agents...");
        for (SNMPAgent agent : this.agents) {
            client = new SNMPManager("udp:" + agent.getAddressAndPort());
            client.start();
            // Get back Value which is set
            LOGGER.warn("Testing device: " + agent.getAddressAndPort());
            LOGGER.warn("Name: " + client.getAsString(DEVICE_NAME_OID));
            LOGGER.warn("Serial: " + client.getAsString(DEVICE_SERIAL_CISCO_OID)); 
        }
        LOGGER.warn("Done");
    }

    /**
     * Iterate through the walk file and loads in the OIDs.
     * @throws IOException
     */
    private void loadWalkFile(File walkFile) throws IOException {
        LOGGER.warn("Loading walk file...");
        FileInputStream inputStream = new FileInputStream(walkFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        String fullLine = "";

        List<VariableBinding> tableData = new ArrayList<>();
        while((line = reader.readLine()) != null) {
            if(!fullLine.isEmpty() && line.startsWith(".1.3.6.1")) {
                VariableBinding variableBinding = processOidString(fullLine);
                fullLine = line;

                if(mibReader.accept(variableBinding.getOid())) {
                    if (variableBinding.getOid().startsWith(new OID(".1.3.6.1.2.1.2.2.1.1.1"))) {
                        LOGGER.info("Stop");
                    }
//                    this.agents.forEach(agent -> agent.registerManagedObject(MOCreator.createReadOnly(variableBinding)));
                    if (isScalar(variableBinding)) {
                        //if table is not empty, means table data is complete, register the table
                        if (!tableData.isEmpty()) {
                            MOTable table = createTable(tableData);
                            this.agents.forEach(agent -> agent.registerManagedObject(table));
                            tableData.clear();
                        }
                        this.agents.forEach(agent -> agent.registerManagedObject(MOCreator.createReadOnly(variableBinding)));
                    } else {
                        //if table is not empty and is currently at a new table, means the old table data is complete
                        //regitster the table
                        if (!tableData.isEmpty() && !isSameTable(variableBinding.getOid(), tableData.get(0).getOid())) {
                            MOTable table = createTable(tableData);
                            this.agents.forEach(agent -> agent.registerManagedObject(table));
                            tableData.clear();
                        }
                        tableData.add(variableBinding);
                    }
                }
            }
            else {
                fullLine += line;
            }
        }
        if(!tableData.isEmpty()) {
            MOTable table = createTable(tableData);
            this.agents.forEach(agent -> agent.registerManagedObject(table));
            tableData.clear();
        }
        LOGGER.warn("Done.");
    }

    private MOTable createTable(List<VariableBinding> tableData) throws  IOException {

        MOTableBuilder builder = new MOTableBuilder();
        return builder.addAllData(tableData, mibReader).build();
    }


    private boolean isSameTable(OID oid1, OID oid2) {
        OID rootTable1 = this.mibReader.getRootTableOID(oid1);
        OID rootTable2 = this.mibReader.getRootTableOID(oid2);
        if(rootTable1.equals(rootTable2)){
            return true;
        }
        return false;
    }

    private boolean isScalar(VariableBinding variableBinding) {
        if(this.mibReader.isScalarOID(variableBinding.getOid())) return true;
        return false;
    }

    private VariableBinding processOidString(String fullLine) {
        String oid = null;
        String type = null;
        String value = null;
        String[] strings = fullLine.split("=", 2);
        oid = strings[0].trim();
        Variable variable = null;

        if (strings[1].equals(" \"\"")) {
            variable = new OctetString("");
            return new VariableBinding(new OID(oid), variable);
        } else {
            String[] strings2 = strings[1].split(": ", 2);
            type = strings2[0].trim();
            value = strings2[1].trim();
            variable = MOCreator.convertToVariable(type, value);
            return new VariableBinding(new OID(oid), variable);
        }
    }


}