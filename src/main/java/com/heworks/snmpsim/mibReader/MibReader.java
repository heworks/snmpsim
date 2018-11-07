package com.heworks.snmpsim.mibReader;

import com.heworks.snmpsim.agent.Simulator;
import org.snmp4j.log.LogAdapter;
import org.snmp4j.log.LogFactory;
import org.snmp4j.smi.OID;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by m2c2 on 3/21/16.
 */
public class MibReader {
    private static final LogAdapter LOGGER = LogFactory.getLogger(MibReader.class);
    private Map<String, OID> nameToOIDMap = new HashMap<>();
    private Map<String, OID> tableNameToOidMap = new HashMap<>();
    private Set<OID> rootOIDs = new HashSet<>();
    //use the first 7 ints as the root OID
    private static final int ROOT_OID_LEVEL = 7;

    /**
     * Create a new mib reader.
     * @param mibFiles the mib files to be used
     * @throws IOException
     */
    public MibReader(List<File> mibFiles) throws IOException {
        LOGGER.warn("Loading mib files...");
        load(mibFiles);
        LOGGER.warn("Finished loading mib files.");
        addRootOIDs();
        LOGGER.warn("Root OIDs: " + this.rootOIDs);
    }

    private void addRootOIDs() {
        for(OID oid : nameToOIDMap.values())  {
            if(oid.size() <= ROOT_OID_LEVEL) continue;
            OID rootOID = new OID(oid.getValue(), 0, ROOT_OID_LEVEL);
            this.rootOIDs.add(rootOID);
        }
    }

    /**
     * Load mib files into mib reader.
     * @param mibFiles the mib files to load
     * @throws IOException
     */
    private void load(List<File> mibFiles) throws IOException {
        this.nameToOIDMap.put("iso", new OID("1"));
        for(File mibFile : mibFiles) {
            FileInputStream inputStream = new FileInputStream(mibFile);
            LOGGER.warn("Loading mib: " + mibFile);
            load(inputStream);
        }

    }

    private void load(FileInputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        String fullLine = "";
        boolean previousLineIsEmpty = false;
        boolean previousLineIsCloseure = false;
        while( (line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                previousLineIsEmpty = true;
                continue;
            }
            //ignore comments
            if (line.trim().startsWith("--")) {
                continue;
            }
            else {
                if (fullLine.isEmpty() && line.trim().matches("^[a-z].*$") && (previousLineIsEmpty || previousLineIsCloseure)) {
                    fullLine += line; 
                    if (processLine(fullLine)) {
                        fullLine = "";
                    }
                    continue;
                }
                if (!fullLine.isEmpty() && line.trim().contains("::= {")) {
                    fullLine += line;
                    if (processLine(fullLine)) {
                        fullLine = "";
                    }
                }
                previousLineIsEmpty = false;
                if (line.trim().contains("::= {")) {
                    previousLineIsCloseure = true;
                }
                else {
                    previousLineIsCloseure = false;
                }
            }
        }
    }

    /**
     * Process the full line, if it's full line return true, false otherwise.
     * @param fullLine the full line
     */
    private boolean processLine(String fullLine) {
        if(isCompleteLine(fullLine)) {
            String trimedString = fullLine.trim();
            String oidName = trimedString.substring(0, trimedString.indexOf(" "));
            String oidValueString = trimedString.substring(trimedString.lastIndexOf('{') + 1, trimedString.lastIndexOf('}')).trim();
            String[] oidValueSubStrings = oidValueString.split(" ");
//            System.out.println(oidName + " : " + nameToOIDMap.get(oidValueSubStrings[0]) + "." + oidValueSubStrings[1]);
            OID oid = new OID(nameToOIDMap.get(oidValueSubStrings[0]));
            oid.append(Integer.parseInt(oidValueSubStrings[1]));
            nameToOIDMap.put(oidName, oid);
            if (oidName.endsWith("Table")) {
                tableNameToOidMap.put(oidName, oid);
            }
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
        for (OID rootTableOid : tableNameToOidMap.values()) {
            if (oid.startsWith(rootTableOid)) {
                OID result = new OID(rootTableOid);
                result.append(oid.get(rootTableOid.size()));
                return result;
            }
        }
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
