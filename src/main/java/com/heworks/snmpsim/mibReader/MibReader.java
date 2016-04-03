package com.heworks.snmpsim.mibReader;

import org.snmp4j.smi.OID;

import java.io.*;
import java.util.*;

/**
 * Created by m2c2 on 3/21/16.
 */
public class MibReader {

    public static final List<String> MIB_FILE = Arrays.asList(
            "/mibs/SNMPv2-SMI.my",
            "/mibs/CISCO-SMI.my",
            "/mibs/RFC1213-MIB.my",
            "/mibs/SNMPv2-MIB.my",
            "/mibs/IP-FORWARD-MIB.my",
            "/mibs/IP-MIB.my",
            "/mibs/CISCO-PROCESS-MIB.my");
    private Map<String, OID> nameToOIDMap = new HashMap<>();
    private Set<OID> rootOIDs = new HashSet<>();
    //use the first 7 ints as the root OID
    private static final int ROOT_OID_LEVEL = 7;

    public MibReader() throws IOException {
        System.out.println("Starting loading mib files...");
        load(MIB_FILE);
        System.out.println("Finished loading mib files.");
        addRootOIDs();
        System.out.println("Root OIDs: " + this.rootOIDs);
    }

    private void addRootOIDs() {
        for(OID oid : nameToOIDMap.values())  {
            if(oid.size() <= ROOT_OID_LEVEL) continue;
            OID rootOID = new OID(oid.getValue(), 0, ROOT_OID_LEVEL);
            this.rootOIDs.add(rootOID);
        }
    }

    private void load(List<String> mibFiles) throws IOException {
        this.nameToOIDMap.put("iso", new OID("1"));
        for(String mibFile : mibFiles) {
            InputStream inputStream  = getClass().getResourceAsStream(mibFile);
            System.out.println("Loading mib: " + mibFile);
            load(inputStream);
        }

    }

    private void load(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        String fullLine = "";
        while( (line = reader.readLine()) != null) {
            if(doWeCare(line)) {
                fullLine += line;
            }
            if(isCompleteLine(fullLine)) {
                String trimedString = fullLine.trim();
                String oidName = trimedString.substring(0, trimedString.indexOf(" "));
                String oidValueString = trimedString.substring(trimedString.lastIndexOf('{') + 1, trimedString.lastIndexOf('}')).trim();
                String[] oidValueSubStrings = oidValueString.split(" ");
//                System.out.println(oidName + " : " + nameToOIDMap.get(oidValueSubStrings[0]) + "." + oidValueSubStrings[1]);
                OID oid = new OID(nameToOIDMap.get(oidValueSubStrings[0]));
                oid.append(Integer.parseInt(oidValueSubStrings[1]));
                nameToOIDMap.put(oidName, oid);
                fullLine = "";
            }
        }
    }

    private boolean doWeCare(String line) {
        String trimLine = line.trim();
        if(trimLine.startsWith("-- ")) {
            return false;
        }
        if(trimLine.contains(",")) {
            return false;
        }
        if(trimLine.contains(" OBJECT") || trimLine.contains(" MODULE")|| trimLine.contains("::= {")
                || trimLine.contains(" NOTIFICATION")) {
            return true;
        }
        return false;
    }

    private boolean isCompleteLine(String fullLine) {
        if(fullLine.contains("::= {")) {
            return true;
        }
        return false;
    }

    public boolean isScalarOID(OID oid) {
        if(getRootTableOID(oid) == null) {
            return true;
        }
        return false;
    }

    private OID getRootOID(OID oid) {
        return new OID(oid.getValue(), 0, ROOT_OID_LEVEL);
    }

    public boolean accept(OID oid) {
        OID rootOID = getRootOID(oid);
        return this.rootOIDs.contains(rootOID);
    }

    public OID getRootTableOID(OID oid) {
        for(String name : nameToOIDMap.keySet()) {
            if(name.endsWith("Table")) {
//                System.out.println(name + " : " + nameToOIDMap.get(name));
                if(oid.startsWith(nameToOIDMap.get(name))) {
                    //add one for the entry
                    int size = nameToOIDMap.get(name).size() + 1;
                    OID result = new OID(oid);
                    while(result.size() > size) {
                        result.removeLast();
                    }
                    return result;
                }
            }
        }
//        OID rootOid = new OID(oid);
//        rootOid.removeLast();
//        rootOid.removeLast();
        return null;
    }

    public int getColumnIndex(OID oid) {
        OID rootTableOid = getRootTableOID(oid);
        return oid.get(rootTableOid.size());
    }

    public OID getRowIndexOID(OID oid) {
        OID rootOid = getRootTableOID(oid);
        int[] ints = oid.toIntArray();
        int[] tableInts = rootOid.toIntArray();
        List<Integer> results = new ArrayList<>();
        //add one for the column
        for(int i = (tableInts.length + 1); i < ints.length; i++) {
           results.add(ints[i]);
        }
        int[] oidArray = new int[results.size()];
        for(int i = 0; i < results.size(); i++) {
           oidArray[i] = results.get(i);
        }
        return new OID(oidArray);
    }
}
