package com.heworks.snmpsim.agent;

import com.heworks.snmpsim.helper.IPAddressManager;

import java.io.IOException;
import java.util.List;

/**
 * Created by m2c2 on 3/9/16.
 */
public class SimulatorMain {


    public static void main(String[] args) throws IOException {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        IPAddressManager addressManager = new IPAddressManager(applicationProperties.getIpRanges());
        addressManager.addAllIPsAsSecondaryIPs(applicationProperties.getInterfaceName());

        List<String> ipAddresses = addressManager.getAllIPs();
        Simulator simulator = new Simulator(ipAddresses, applicationProperties.getSnmpPort());

        while(true) {}
    }
}
