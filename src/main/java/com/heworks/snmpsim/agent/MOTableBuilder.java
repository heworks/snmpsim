package com.heworks.snmpsim.agent;

/**
 * Created by m2c2 on 3/19/16.
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.heworks.snmpsim.mibReader.MibReader;
import javafx.util.Pair;
import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.*;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

/**
 * <p>
 * Utility class for adding dynamic data into an {@link MOTable}
 * </p>
 *
 */
public class MOTableBuilder {

    private MOTableSubIndex[] subIndexes = new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER) };
    private MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

    private final List<MOColumn> columns = new ArrayList<MOColumn>();
    private final LinkedHashMap<OID, List<Variable>> tableRows = new LinkedHashMap<>();
    private int currentRow = 0;
    private int currentCol = 0;

    private OID tableRootOid;

    public MOTableBuilder() throws IOException {
    }

    /**
     * Adds all column types {@link MOColumn} to this table. Important to
     * understand that you must add all types here before adding any row values
     *
     * @param syntax
     *            use {@link SMIConstants}
     * @param access
     * @return
     */
    private MOTableBuilder addColumnType(int colIndex, int syntax, MOAccess access) {
        columns.add(new MOColumn(colIndex, syntax, access));
        return this;
    }

    private MOTableBuilder addRowValue(VariableBinding variableBinding, MibReader mibReader) {
        OID oid = variableBinding.getOid();
        //TODO: improve performance
        OID rowOidIndex = mibReader.getRowIndexOID(oid);
        if (rowOidIndex != null) {
            List<Variable> valuesInARow = tableRows.computeIfAbsent(rowOidIndex, k -> new ArrayList<>());
            valuesInARow.add(variableBinding.getVariable()); 
        }
        return this;
    }

    public MOTableBuilder addAllData(List<VariableBinding> values, MibReader mibReader) {
        OID firstOid = values.get(0).getOid();
        this.tableRootOid = mibReader.getRootTableOID(firstOid);

        int previousColIndex = 0;
        for(VariableBinding variableBinding : values)  {
            OID oid = variableBinding.getOid();
            int colIndex = mibReader.getColumnIndex(oid);
            if (colIndex != previousColIndex) {
                addColumnType(colIndex, oid.getSyntax(), MOAccessImpl.ACCESS_READ_ONLY);
                previousColIndex = colIndex;
            }
            addRowValue(variableBinding, mibReader);
        }
        return this;
    }

    public MOTable build() {
        DefaultMOTable ifTable = new DefaultMOTable(tableRootOid, indexDef, columns.toArray(new MOColumn[0]));
        MOMutableTableModel model = (MOMutableTableModel) ifTable.getModel();
        tableRows.forEach((oidIndex, values) -> {
            model.addRow(new DefaultMOMutableRow2PC(oidIndex, values.toArray(new Variable[values.size()]))); 
        });
        ifTable.setVolatile(true);
        return ifTable;
    }
}
