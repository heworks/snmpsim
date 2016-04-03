package com.heworks.snmpsim.agent;


import com.heworks.snmpsim.manager.SNMPManager;
import com.heworks.snmpsim.mibReader.MibReader;
import org.snmp4j.agent.mo.MOTable;
import org.snmp4j.smi.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Simulator {
    private static final OID DEVICE_NAME_OID = new OID("1.3.6.1.2.1.1.5.0");
    private static final OID DEVICE_SERIAL_OID = new OID("1.3.6.1.4.1.9.3.6.3.0");
    private static final OID SYS_DESCRIPTION = new OID("1.3.6.1.2.1.1.1.0");
    private static final File WALK_FILE = new File("device.walk");
    private static final String PREFIX = "SNMP4J-";

    private List<SNMPAgent> agents = new ArrayList<SNMPAgent>();
    private SNMPManager client = null;
    private MibReader mibReader;

    public Simulator(List<String> ips, int port) throws IOException {
        this.mibReader = new MibReader();
        for (String ip : ips) {
            System.out.println("Initializing device: " + ip);
            initSingleAgent(ip, port);
            System.out.println("Finish loading device: " + ip);
        }
    }

    private void initSingleAgent(String ip, int port) throws IOException {

        SNMPAgent agent = new SNMPAgent(ip + "/" + port);
        agents.add(agent);
        agent.start();
        System.out.println("agent started");

        // Since BaseAgent registers some MIBs by default we need to unregister
        // one before we register our own sysDescr. Normally you would
        // override that method and register the MIBs that you need
        agent.unregisterManagedObject(agent.getSnmpv2MIB());
//        agent.unregisterManagedObject(agent.getSnmpFrameworkMIB());
//        agent.unregisterManagedObject(agent.getSnmpCommunityMIB());
//        agent.unregisterManagedObject(agent.getUsmMIB());
//        agent.unregisterManagedObject(agent.getSnmp4jConfigMIB());
//        agent.unregisterManagedObject(agent.getSnmpNotificationMIB());
//        agent.unregisterManagedObject(agent.getSnmp4jLogMIB());
//        agent.unregisterManagedObject(agent.getSnmpProxyMIB());
//        agent.unregisterManagedObject(agent.getSnmpTargetMIB());

        loadWalkFile(agent);
        System.out.println("Loading walk file done.");
        registerSpecialOIDs(agent, ip);

        // Setup the client to use our newly started agent
        client = new SNMPManager("udp:" + ip + "/" + port);
        client.start();
        // Get back Value which is set
        System.out.println("Testing device: " + ip + " on port: " + port + " ....");
        System.out.println("Name: " + client.getAsString(DEVICE_NAME_OID));
        System.out.println("Serial: " + client.getAsString(DEVICE_SERIAL_OID));
    }

    private void registerSpecialOIDs(SNMPAgent agent, String ip) {
        String ipString = ip.replace(".", "-");
        agent.registerManagedObject(MOCreator.createReadOnly(DEVICE_NAME_OID, new OctetString(PREFIX + ipString)));
        agent.registerManagedObject(MOCreator.createReadOnly(DEVICE_SERIAL_OID, new OctetString(PREFIX + ipString)));
    }

    private void loadWalkFile(SNMPAgent agent) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(WALK_FILE)));
        String line = null;
        String fullLine = "";

        List<VariableBinding> tableData = new ArrayList<>();
        while((line = reader.readLine()) != null) {
            fullLine += line;
            if(isCompleteLine(fullLine)) {
                VariableBinding variableBinding = processOidString(fullLine);
                fullLine = "";

                if(mibReader.accept(variableBinding.getOid())) {
                    if (isScalar(variableBinding)) {
                        //if table is not empty, means table data is complete, register the table
                        if (!tableData.isEmpty()) {
                            MOTable table = createTable(tableData);
                            agent.registerManagedObject(table);
                            tableData.clear();
                        }
                        agent.registerManagedObject(MOCreator.createReadOnly(variableBinding));
                    } else {
                        //if table is not empty and is currently at a new table, means the old table data is complete
                        //regitster the table
                        if (!tableData.isEmpty() && !isSameTable(variableBinding.getOid(), tableData.get(0).getOid())) {
                            MOTable table = createTable(tableData);
                            agent.registerManagedObject(table);
                            tableData.clear();
                        }
                        tableData.add(variableBinding);
                    }
                }
            }
        }
        if(!tableData.isEmpty()) {
            MOTable table = createTable(tableData);
            agent.registerManagedObject(table);
            tableData.clear();
        }
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

    private boolean isCompleteLine(String line) {
        if(line.startsWith(".1.3.6.1")) {
            if (line.contains(" STRING: ")) {
                if(line.endsWith("\"") && line.substring(0, line.length() - 2).contains("\"")) {
                    return true;
                }
                return false;
            }
            return true;
        }
        return false;
    }

    private VariableBinding processOidString(String fullLine) {
        String oid = null;
        String type = null;
        String value = null;
        String[] strings = fullLine.split("=");
        oid = strings[0].trim();
        Variable variable = null;

        if (!strings[1].contains(": ") && strings[1].equals(" \"\"")) {
            variable = new OctetString("");
//            System.out.println("oid: " + oid + ", type: STRING" + ", value:  ");
            return new VariableBinding(new OID(oid), variable);
        } else {
            String[] strings2 = strings[1].split(": ");
            type = strings2[0].trim();
            value = strings[1].substring(strings2[0].length() + 2);
//            System.out.println("oid: " + oid + ", type: " + type + ", value: " + value);

            variable = MOCreator.convertToVariable(type, value);
            return new VariableBinding(new OID(oid), variable);
        }
    }


}