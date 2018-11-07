package com.heworks.snmpsim.agent;

import org.snmp4j.log.LogAdapter;
import org.snmp4j.log.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Created by m2c2 on 3/10/16.
 */
public class ApplicationProperties {
    private static final LogAdapter LOGGER = LogFactory.getLogger(ApplicationProperties.class);
    private final File propertiesFile = new File("application.properties");
    private static final String INTERFACE = "interface";
    private static final String SNMP_PORT = "snmpPort";
    private static final String DEVICE_IPS = "deviceIPs";
    private static final String DEVICE_TO_SIMULATE = "deviceToSimulate";
    private static final String MIBS_TO_SIMULATE = "mibsToSimulate";
    private static final String DATA_DIR = "dataDir";
    private static final String WALK_FILE_FOLDER = "walkFiles";
    private static final String MIB_FOLDER = "mibs";

    private final String interfaceName;
    private final int snmpPort;
    private final List<String> ipRanges;
    private final List<File> mibFiles;
    private final File walkFile;
    private final File dataDir;

    public ApplicationProperties() throws IOException {
        FileReader reader = new FileReader(propertiesFile);
        Properties props = new Properties();
        props.load(reader);

        String interfaceName = props.getProperty(INTERFACE);
        if (interfaceName == null || interfaceName.isEmpty()) {
            LOGGER.error("interface must be specified!");
            System.exit(1);
        }
        this.interfaceName = interfaceName;
        LOGGER.info("using interface : " + this.interfaceName);

        String snmpPort = props.getProperty(SNMP_PORT);
        if (snmpPort == null || snmpPort.isEmpty()) {
            LOGGER.error("snmp port must be specified!");
            System.exit(1);
        }
        this.snmpPort = Integer.parseInt(snmpPort);
        LOGGER.info("snmp port : " + this.snmpPort);

        String dataDir = props.getProperty(DATA_DIR);
        if (dataDir == null || dataDir.isEmpty()) {
            LOGGER.info("must specify the data directory!");
        }
        this.dataDir = new File(dataDir);

        String walkFile = props.getProperty(DEVICE_TO_SIMULATE);
        if (walkFile == null || walkFile.isEmpty()) {
            LOGGER.error("must specify a walk file to simulate!");
            System.exit(1);
        }
        File walkFileFolder = new File(this.dataDir, WALK_FILE_FOLDER);
        this.walkFile = new File(walkFileFolder, walkFile);
        LOGGER.info("walk file to use: " + this.walkFile);

        String ipRangesString = props.getProperty(DEVICE_IPS);
        if (ipRangesString == null || ipRangesString.isEmpty()) {
            LOGGER.error("device ips must be specified!");
            System.exit(1);
        }
        String[] ipRangeStrings = ipRangesString.split(",");
        this.ipRanges = Arrays.asList(ipRangeStrings); 
        LOGGER.info("device ips to simulate: " + this.ipRanges);

        String mibFilesString = props.getProperty(MIBS_TO_SIMULATE);
        if (mibFilesString == null || mibFilesString.isEmpty()) {
            System.err.println("must specify the mib files to use!");
            System.exit(1);
        }
        String[] mibFiles = mibFilesString.split(",");
        File mibFolder = new File(dataDir, MIB_FOLDER);
        this.mibFiles = Arrays.asList(mibFiles).stream().map(fileName -> new File(mibFolder, fileName)).collect(Collectors.toList());
        LOGGER.info("mibs to simulate: " + this.mibFiles);

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

    public List<File> getMibFiles() {
        return mibFiles;
    }

    public File getWalkFile() {
        return walkFile;
    }

    public File getDataDir() {
        return dataDir;
    }
}
