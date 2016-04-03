package com.heworks.snmpsim.agent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by m2c2 on 3/10/16.
 */
public class ApplicationProperties {

    private final File propertiesFile = new File("application.properties");
    private static final String INTERFACE = "interface";
    private static final String SNMP_PORT = "snmpPort";
    private static final String DEVICE_IPS = "deviceIPs";

    private final String interfaceName;
    private final int snmpPort;
    private final List<String> ipRanges;

    public ApplicationProperties() throws IOException {
        FileReader reader = new FileReader(propertiesFile);
        Properties props = new Properties();
        props.load(reader);

        String interfaceName = props.getProperty(INTERFACE);
        if (interfaceName == null || interfaceName.isEmpty()) {
            System.err.println("interface must be specified!");
            System.exit(1);
        }
        this.interfaceName = interfaceName;
        System.out.println("using interface : " + this.interfaceName);

        String snmpPort = props.getProperty(SNMP_PORT);
        if (snmpPort == null || snmpPort.isEmpty()) {
            System.err.println("snmp port must be specified!");
            System.exit(1);
        }
        this.snmpPort = Integer.parseInt(snmpPort);
        System.out.println("snmp port : " + this.snmpPort);

        String ipRangesString = props.getProperty(DEVICE_IPS);
        if (ipRangesString == null || ipRangesString.isEmpty()) {
            System.err.println("device ips must be specified!");
            System.exit(1);
        }
        String[] ipRangeStrings = ipRangesString.split(",");
        List<String> ipRangeList = new ArrayList<>();
        for(int i = 0; i < ipRangeStrings.length; i++) {
           ipRangeList.add(ipRangeStrings[i]);
        }
        this.ipRanges = ipRangeList;
        System.out.println("device ips to simulate: " + this.ipRanges);

        reader.close();
    }

    public String getInterfaceName() {
        return this.interfaceName;
    }

    public int getSnmpPort() {
        return this.snmpPort;
    }

    public List<String> getIpRanges() {
        return this.ipRanges;
    }


}
