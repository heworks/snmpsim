package com.heworks.snmpsim.helper;

import org.apache.commons.net.util.SubnetUtils;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by m2c2 on 3/9/16.
 */
public class IPAddressRange {
    private final String network;
    private final String netmask;
    private final int bitmask;
    private final List<String> ips;

    //172.17.101.0,255.255.255.0
    public IPAddressRange(String ipCIDR) throws UnknownHostException {
        SubnetUtils utils = new SubnetUtils(ipCIDR);
        List<String> ipAddresses = new ArrayList<>();
        String[] allAddresses = utils.getInfo().getAllAddresses();
        for (int i = 0; i < allAddresses.length; i++) {
            ipAddresses.add(allAddresses[i]);
        }
        String cidrSinature = utils.getInfo().getCidrSignature();
        int bitmask = Integer.parseInt(cidrSinature.split("/")[1]);

        this.network = utils.getInfo().getNetworkAddress();
        this.netmask = utils.getInfo().getNetmask();
        this.ips = ipAddresses;
        this.bitmask = bitmask;
    }

    public List<String> getIps() {
        //172.17.101.1/32
        if (this.bitmask == 32) {
            return Arrays.asList(this.network);
        }
        return this.ips;
    }

    public String getNetmask() {
        return this.netmask;
    }

    public String getNetwork() {
        return this.network;
    }

    public int getBitmask() {
        return this.bitmask;
    }
}

