package com.heworks.snmpsim.helper;

import org.junit.Test;

import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;

/**
 * Created by m2c2 on 4/2/16.
 */
public class IPAddressRangeTest {

    @Test
    public void test1() throws UnknownHostException {
        IPAddressRange range = new IPAddressRange("172.17.1.0/24");
        assertEquals(range.getNetmask(), "255.255.255.0");
        assertEquals(range.getBitmask(), 24);
        assertEquals(range.getIps().get(0), "172.17.1.1");
        assertEquals(range.getIps().get(253), "172.17.1.254");
        assertEquals(range.getIps().size(), 254);
    }

    @Test
    public void test2() throws UnknownHostException {
        IPAddressRange range = new IPAddressRange("172.17.1.1/32");
        assertEquals(range.getNetmask(), "255.255.255.255");
        assertEquals(range.getBitmask(), 32);
        assertEquals(range.getIps().get(0), "172.17.1.1");
        assertEquals(range.getIps().size(), 1);
    }

    @Test
    public void test3() throws UnknownHostException {
        IPAddressRange range = new IPAddressRange("172.17.1.0/24");
        assertEquals(range.getBitmask(), 24);
        assertEquals(range.getNetmask(), "255.255.255.0");
        assertEquals(range.getNetwork(), "172.17.1.0");
    }
}
