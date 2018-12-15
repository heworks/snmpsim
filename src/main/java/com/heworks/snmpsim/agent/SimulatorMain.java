package com.heworks.snmpsim.agent;

import com.heworks.snmpsim.helper.IPAddressManager;
import org.snmp4j.log.ConsoleLogFactory;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.log.LogFactory;
import org.snmp4j.log.LogLevel;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by m2c2 on 3/9/16.
 */
public class SimulatorMain {

    public static void main(String[] args) throws IOException {
        LogFactory logFactoryToUse = new ConsoleLogFactory();
        logFactoryToUse.getRootLogger().setLogLevel(LogLevel.WARN);
        LogFactory.setLogFactory(logFactoryToUse);
        ApplicationProperties applicationProperties = new ApplicationProperties();
        //add secondary ip addresses
        IPAddressManager addressManager = new IPAddressManager(applicationProperties.getIpRanges());
        addressManager.addAllIPsAsSecondaryIPs(applicationProperties.getInterfaceName());

        List<String> ipAddresses = addressManager.getAllIPs();
        Simulator simulator = new Simulator(
                ipAddresses, 
                applicationProperties.getSnmpPort(), 
                applicationProperties.getWalkFile(),
                applicationProperties.getMibDir()
        );

        while(true) {}
    }
}
