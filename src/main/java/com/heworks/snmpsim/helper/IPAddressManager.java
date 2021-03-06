package com.heworks.snmpsim.helper;

import com.heworks.snmpsim.agent.ApplicationProperties;
import org.apache.commons.lang3.SystemUtils;
import org.snmp4j.log.LogAdapter;
import org.snmp4j.log.LogFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by m2c2 on 3/9/16.
 */
public class IPAddressManager {
    private static final LogAdapter LOGGER = LogFactory.getLogger(IPAddressManager.class);
    private final List<IPAddressRange> ipAddressRanges;

    public IPAddressManager(List<String> ipRangeStrings) throws IOException {
        this.ipAddressRanges = convertToAddressRanges(ipRangeStrings);
    }

    private List<IPAddressRange> convertToAddressRanges(List<String> ipRangeStrings) throws IOException {
        List<IPAddressRange> ipRanges = new ArrayList<>();
        //example: 172.17.1.102/32
        //example: 172.17.101.0/24
        for (String ipRangeString : ipRangeStrings) {
            IPAddressRange IPAddressRange = new IPAddressRange(ipRangeString);
            ipRanges.add(IPAddressRange);
        }
        return ipRanges;
    }

    public void addAllIPsAsSecondaryIPs(String interfaceName) throws IOException {
        LOGGER.warn("Adding secondary ip addresses to interface " + interfaceName + "...");
        for (IPAddressRange ipRanges : ipAddressRanges) {
            for (String ip : ipRanges.getIps()) {
                LOGGER.warn(ip);
                addSecondaryIP(interfaceName, ip, ipRanges.getNetmask(), ipRanges.getBitmask());
            }
//            System.out.println("Adding static route to " + ipRanges.getNetwork() + "/" + ipRanges.getBitmask());
//            addStaticRoute(ipRanges.getNetwork(), ipRanges.getBitmask());
        }
    }

    public List<String> getAllIPs() {
        List<String> ipAddresses = new ArrayList<>();
        for (IPAddressRange ipRange : ipAddressRanges) {
            ipAddresses.addAll(ipRange.getIps());
        }
        return ipAddresses;
    }

    private void addSecondaryIP(String interfaceName, String ip, String netmask, int bitmask) throws IOException {
        if (SystemUtils.IS_OS_MAC) {
            //MacOS doesn't work if adding same network more than once. Hardcoding 32 bit mask to force each ip to be in a
            //unique network.
            Runtime.getRuntime().exec("sudo ifconfig " + interfaceName + " alias " + ip + "/32"  + " up");
        }
        else if (SystemUtils.IS_OS_LINUX) {
            Runtime.getRuntime().exec("sudo ip address add " + ip + "/" + bitmask + " dev " + interfaceName);
        }
        else if (SystemUtils.IS_OS_WINDOWS){
            Runtime.getRuntime().exec("netsh interface ipv4 add address \"" + interfaceName + "\" " + ip + " " + netmask);
        }
        else {
            LOGGER.error("OS not supported. " + SystemUtils.OS_NAME);
        }
    }

    private void addStaticRoute(String network, int netmask) throws IOException {
        if (SystemUtils.IS_OS_MAC) {
            Runtime.getRuntime().exec("sudo route -n add " + network + "/" + netmask + " 127.0.0.1");
        }
        else if (SystemUtils.IS_OS_LINUX) {

        }
        else {
            LOGGER.error("OS not supported.");
        }
    }
}
