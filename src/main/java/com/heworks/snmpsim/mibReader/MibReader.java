package com.heworks.snmpsim.mibReader;

import org.snmp4j.log.LogAdapter;
import org.snmp4j.log.LogFactory;
import org.snmp4j.smi.OID;

import java.io.*;
import java.util.*;

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
    private Map<String, MibFileInfo> mibNameToInfoMap = new HashMap<>();
    private Set<String> loadedMibs = new HashSet<>();
    private static final Set<String> MIB_KEYS = new HashSet<>(Arrays.asList("OBJECT-IDENTITY", "OBJECT IDENTIFIER", 
            "MODULE-IDENTITY", "OBJECT-TYPE", "MODULE-COMPLIANCE", "OBJECT-GROUP", "NOTIFICATION-TYPE", "NOTIFICATION-GROUP"));
    /**
     * Some mibs that are required in the IMPORTS section are not really needed in order to decode the mib correctly. Add them to here
     * to exclude them from the required.
     */
    private static final Set<String> EXCLUDED_REQUIRED_MIB = new HashSet<>(Arrays.asList("RFC1155-SMI", "RFC-1155", "RFC-1212", "RFC-1215"));

    /**
     * Create a new mib reader.
     * @throws IOException
     */
    public MibReader(File mibFolder) throws IOException {
        LOGGER.warn("Loading mib files...");
        if (!mibFolder.exists() || !mibFolder.isDirectory()) {
            throw new IllegalArgumentException(mibFolder + " doesn't exist or is not a folder.");
        }
        loadMibFileInfo(mibFolder);
        this.nameToOIDMap.put("iso", new OID("1"));
        loadMibs();
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
     * @throws IOException
     */
    private void loadMibs() throws IOException {
        Set<String> onDiskMibs = new HashSet<>(mibNameToInfoMap.keySet());
        for (MibFileInfo fileInfo : mibNameToInfoMap.values()) {
            loadMib(fileInfo, onDiskMibs); 
        }
    }

    /**
     * Loads a single mib. This method will load the required mib first. If any of the required mib cannot be found, 
     * then it will skip loading this mib.
     * @param fileInfo the mib file info
     * @param onDiskMibs the existing no disk mibs
     * @throws IOException
     */
    private void loadMib(MibFileInfo fileInfo, Set<String> onDiskMibs) throws IOException {
        Set<String> requiredMibs = fileInfo.getRequiredMibNames();
        if (onDiskMibs.containsAll(requiredMibs)) {
            for (String requiredMib : requiredMibs) {
                if (!loadedMibs.contains(requiredMib)) {
                    MibFileInfo requiredMibInfo = mibNameToInfoMap.get(requiredMib);
                    if (requiredMibInfo.getRequiredMibNames().isEmpty()) {
                        FileInputStream inputStream = new FileInputStream(requiredMibInfo.getMibFile());
                        LOGGER.warn("Loading mib: " + requiredMibInfo.getMibName() + " at: " + requiredMibInfo.getMibFile());
                        loadMib2(inputStream);
                        loadedMibs.add(requiredMibInfo.getMibName());
                    }
                    else {
                        loadMib(requiredMibInfo, onDiskMibs);
                    }
                }
            }
            if (loadedMibs.containsAll(requiredMibs)) {
                //all required mibs are loaded, now we can loadMib this mib
                FileInputStream inputStream = new FileInputStream(fileInfo.getMibFile());
                LOGGER.warn("Loading mib: " + fileInfo.getMibName() + " at: " + fileInfo.getMibFile());
                loadMib2(inputStream);
                loadedMibs.add(fileInfo.getMibName());
            }
            else {
                Set<String> missingMibs = new HashSet<>(requiredMibs);
                missingMibs.removeAll(loadedMibs);
                LOGGER.warn("==== Ignore mib: " + fileInfo.getMibName() + " located at: " + fileInfo.getMibFile() +
                        " due to missing nested dependee mibs: " + missingMibs);
            }
        }
        else {
            Set<String> missingMibs = new HashSet<>(requiredMibs);
            missingMibs.removeAll(onDiskMibs);
            LOGGER.warn("==== Ignore mib: " + fileInfo.getMibName() + " located at: " + fileInfo.getMibFile() +
                    " due to missing required mibs: " + missingMibs);
        }     
    }

    /**
     * Reads all the mibs in the specified folder to cache basic information about all the mib files.
     * @param mibFolder the mib folder
     * @throws IOException
     */
    private void loadMibFileInfo(File mibFolder) throws IOException {
        for (File mibFile : mibFolder.listFiles()) {
            if (mibFile.isDirectory()) {
                loadMibFileInfo(mibFile);
            }
            else {
                MibFileInfo fileInfo = getMibFileInfo(mibFile);
                MibFileInfo existingInfo = mibNameToInfoMap.put(fileInfo.getMibName(), fileInfo);
                if (existingInfo != null) {
                    LOGGER.warn("==== Duplicated mib file detected: " + existingInfo.getMibFile() + " and " + mibFile);
                }
            }
        }
    }

    /**
     * Gets the basic information about the mib file.
     * @param mibFile the mib file
     * @return the basic mib info
     * @throws IOException
     */
    private MibFileInfo getMibFileInfo(File mibFile) throws  IOException {
        FileInputStream inputStream = new FileInputStream(mibFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)); 
        String line = null;
        String mibName = null;
        Set<String> requiredMibs = new HashSet<>();
        while ((line = reader.readLine()) != null) {
           if (line.contains("DEFINITIONS ::=")) {
              mibName = line.split(" DEFINITIONS ::=")[0].trim();
           }
           else if (line.contains("FROM ")) {
              String requiredMib = line.split("FROM ")[1]; 
              requiredMib = requiredMib.split(" ")[0];
              if (requiredMib.endsWith(";")) {
                  requiredMib = requiredMib.substring(0, requiredMib.length() - 1);
                  requiredMibs.add(requiredMib);
                  break;
              }
              else {
                  requiredMibs.add(requiredMib);
              }
           }
        }
        if (mibName == null) {
            throw new IllegalArgumentException("Unable to determine mib name for " + mibFile);
        }
        requiredMibs.removeAll(EXCLUDED_REQUIRED_MIB);
        LOGGER.warn(mibFile + ", name: " + mibName + ", requires: " + requiredMibs);
        return new MibFileInfo(mibFile, mibName, requiredMibs);
    }

    /**
     * Loads a single file into the in memory map.
     * @param inputStream the input file stream
     * @throws IOException
     */
    private void loadMib2(FileInputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String previousLine = null;
        String line = null;
        String fullLine = null;
        boolean inImportSection = false;
        boolean inDescriptionSection = false;
        boolean inValueSection = false;
        List<String> unresolvedLines = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.trim().startsWith("--")) {
                continue;
            }
            if (inImportSection) {
                if (line.contains(";")) {
                    inImportSection = false;
                }
                continue;
            }
            if (line.trim().startsWith("IMPORTS")) {
                inImportSection = true;
                continue;
            }
            if (inDescriptionSection) {
                if (line.trim().endsWith("\"")) {
                    inDescriptionSection = false;
                }
                continue;
            }
            if (line.trim().startsWith("DESCRIPTION")) {
                if (!line.trim().endsWith("\"")) {
                    inDescriptionSection = true; 
                }
                continue;
            }
            else{
                if (fullLine != null) { 
                    if (!inValueSection && (fullLine.contains("::=") || line.contains("::="))) {
                        inValueSection = true;
                    }
                    if (inValueSection) {
                        fullLine = fullLine + line;
                    }
                    if (isCompleteLine(fullLine)) {
                        processLine(fullLine, unresolvedLines);
                        fullLine = null;
                        inValueSection = false;
                        continue;
                    }
                    continue;
                }
                String mibKey = containsMibKey(line);
                if (mibKey != null) {
                    if (fullLine != null) {
                        throw new IllegalArgumentException("Error");
                    }
                    if (line.trim().startsWith(mibKey)) {
                        fullLine = previousLine + line;
                    } else if (line.trim().matches("^[a-z].*$") ){
                        fullLine = line;
                    }
                    if (isCompleteLine(fullLine)) {
                        processLine(fullLine, unresolvedLines);
                        fullLine = null;
                    }
                }
            }
            previousLine = line; 
        }
    }
    
    /**
     * Checks if the line contains the mib key. If true, the key is returned, otherwise null.
     * @param line the line
     * @return the key, could be null
     */
    private String containsMibKey(String line) {
        for (String key : MIB_KEYS) {
            if (line.contains(key)) {
                return key;
            }
        }
        return null;
    }

    /**
     * Process the full line, if cannot resolve the line, put it in the unresolvedLines for later processing.
     * @param fullLine the full line
     * @param unresolvedLines a list of lines that cannot resolved due to the key is not seen yet
     */
    private void processLine(String fullLine, List<String> unresolvedLines) {
        boolean success = processLine(fullLine);
        if (!success) {
            unresolvedLines.add(fullLine);
            return;
        }
        //reprocess unresolved lines again with new information.
        for (String line : unresolvedLines) {
            processLine(line);
        }
    }

    /**
     * Process one full line. True if it can be processed, false otherwise.
     * @param fullLine the full line
     * @return true if can be processed, false otherwise
     */
    private boolean processLine(String fullLine) {
        String trimedString = fullLine.trim();
        String oidName = trimedString.substring(0, trimedString.indexOf(" "));
        //Exmaple: ieee8021paeMIB MODULE-IDENTITY    ::= { iso std(0) iso8802(8802) ieee802dot1(1)          ieee802dot1mibs(1) 1 }
        String oidValueString = trimedString.substring(trimedString.lastIndexOf('{') + 1, trimedString.lastIndexOf('}')).trim();
        String[] oidValueSubStrings = oidValueString.split("\\s+");
//        LOGGER.warn("" + Arrays.asList(oidValueSubStrings));
        OID tempOid = nameToOIDMap.get(oidValueSubStrings[0]);
        if (tempOid == null) {
            return false;
        }
        OID oid = new OID(tempOid);
        for (int i = 1; i < oidValueSubStrings.length; i++) {
            String oidValue = oidValueSubStrings[i];
            if (oidValue.contains("(")) {
                oidValue = oidValue.substring(oidValue.lastIndexOf('(') + 1, oidValue.lastIndexOf(')')).trim();
            }
            oid.append(Integer.parseInt(oidValue));
        }
        nameToOIDMap.put(oidName, oid);
        if (oidName.endsWith("Table")) {
            tableNameToOidMap.put(oidName, oid);
        }
        return true;
    }

    /**
     * Checks if the given line is an complete line which has enough information to be processed.
     * @param fullLine the line
     * @return true if line is ready to be processed, false otherwise
     */
    private boolean isCompleteLine(String fullLine) {
        if(fullLine != null && fullLine.matches("^.*::=\\s*\\{.*\\}.*$")) {
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

    /**
     * Information about the the mib file.
     */
    private static class MibFileInfo  {
        private final File mibFile;
        private final String mibName;
        private final Set<String> requiredMibNames;

        /**
         * Create a new mib file info.
         * @param mibFile the mib file
         * @param mibName the mib file name
         * @param requiredMibNames the required mibs
         */
        public MibFileInfo(File mibFile, String mibName, Set<String> requiredMibNames) {
            this.mibFile = mibFile;
            this.mibName = mibName;
            this.requiredMibNames = requiredMibNames;
        }

        /**
         * Gets the mib file.
         * @return the mib file
         */
        public File getMibFile() {
            return mibFile;
        }

        /**
         * Gets the mib name.
         * @return the mib name
         */
        public String getMibName() {
            return mibName;
        }

        /**
         * Gets the required mib names.
         * @return the mib names
         */
        public Set<String> getRequiredMibNames() {
            return new HashSet<>(requiredMibNames);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MibFileInfo fileInfo = (MibFileInfo) o;
            return Objects.equals(mibFile, fileInfo.mibFile) &&
                    Objects.equals(mibName, fileInfo.mibName) &&
                    Objects.equals(requiredMibNames, fileInfo.requiredMibNames);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mibFile, mibName, requiredMibNames);
        }
    }
}
