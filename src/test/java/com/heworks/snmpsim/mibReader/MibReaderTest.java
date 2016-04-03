package com.heworks.snmpsim.mibReader;

import org.junit.Before;
import org.junit.Test;
import org.snmp4j.smi.OID;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by m2c2 on 3/21/16.
 */
public class MibReaderTest {
    private MibReader reader = null;

    @Before
    public void setup() throws IOException {
        reader = new MibReader();
    }

    @Test
    public void testRootTableOid() {
        OID tableRootOid = reader.getRootTableOID(new OID(".1.3.6.1.2.1.3.1.1.1.4.1.172.17.1.1"));
        OID expectedOid = new OID(".1.3.6.1.2.1.3.1.1");
        assertTrue(expectedOid.equals(tableRootOid));
        tableRootOid = reader.getRootTableOID(new OID(".1.3.6.1.2.1.1.9.1.2.1"));
        expectedOid = new OID(".1.3.6.1.2.1.1.9.1");
        assertTrue(expectedOid.equals(tableRootOid));
        tableRootOid = reader.getRootTableOID(new OID(".1.3.6.1.2.1.4.24.4.1.1.10.0.1.0.255.255.255.0.0.0.0.0.0"));
        expectedOid = new OID(".1.3.6.1.2.1.4.24.4.1");
        assertTrue(expectedOid.equals(tableRootOid));
    }

    @Test
    public void testColumnIndex() {
        int colIndex = reader.getColumnIndex(new OID(".1.3.6.1.2.1.3.1.1.1.4.1.172.17.1.1"));
        assertEquals(1, colIndex);
        colIndex = reader.getColumnIndex(new OID(".1.3.6.1.2.1.1.9.1.2.1"));
        assertEquals(2, colIndex);
    }


    @Test
    public void testRowIndexOid() {
        OID oidIndex = reader.getRowIndexOID(new OID(".1.3.6.1.2.1.3.1.1.1.4.1.172.17.1.1"));
        OID resultOid = new OID("4.1.172.17.1.1");
        assertTrue(oidIndex.equals(resultOid));
        oidIndex = reader.getRowIndexOID(new OID(".1.3.6.1.2.1.1.9.1.2.1"));
        resultOid = new OID("1");
        assertTrue(oidIndex.equals(resultOid));
    }
}
