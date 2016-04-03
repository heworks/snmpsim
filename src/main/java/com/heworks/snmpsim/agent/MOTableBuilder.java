package com.heworks.snmpsim.agent;

/**
 * Created by m2c2 on 3/19/16.
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.heworks.snmpsim.mibReader.MibReader;
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
    private final List<Variable[]> tableRows = new ArrayList<Variable[]>();
    private final List<OID> rowIndexes = new ArrayList<>();
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

    private MOTableBuilder addRowValue(Variable variable) {
        if (tableRows.size() == currentRow) {
            tableRows.add(new Variable[columns.size()]);
        }
        tableRows.get(currentRow)[currentCol] = variable;
        currentCol++;
        if (currentCol >= columns.size()) {
            currentRow++;
            currentCol = 0;
        }
        return this;
    }

    public MOTableBuilder addAllData(List<VariableBinding> values, MibReader mibReader) {


        OID firstOid = values.get(0).getOid();
        this.tableRootOid = mibReader.getRootTableOID(firstOid);
//        System.out.println("====== Table Root === " + this.tableRootOid);
//        if(this.tableRootOid.equals(new OID("1.3.6.1.2.1.4.24.4.1"))) {
//            System.out.println("STOP");
//        }
        int firstColumnIndex = mibReader.getColumnIndex(firstOid);
        int rowSize = 0;
        for(int i = 0; i < values.size(); i++) {
            if(mibReader.getColumnIndex(values.get(i).getOid()) != firstColumnIndex) {
                rowSize = i;
                break;
            }
        }

        //add columns
        for(int i = 0; i < values.size(); i+=rowSize)  {
            OID oid = values.get(i).getOid();
            int colIndex = mibReader.getColumnIndex(oid);
            addColumnType(colIndex, values.get(i).getSyntax(), MOAccessImpl.ACCESS_READ_ONLY);
        }

        //add the variables in row order.
        for(int i = 0; i < rowSize; i++) {
            int index = i;
            this.rowIndexes.add(mibReader.getRowIndexOID(values.get(index).getOid()));
            while(index < values.size()) {
                addRowValue(values.get(index).getVariable());
                index += rowSize;
            }
        }
        return this;
    }

    public MOTable build() {
        DefaultMOTable ifTable = new DefaultMOTable(tableRootOid, indexDef, columns.toArray(new MOColumn[0]));
        MOMutableTableModel model = (MOMutableTableModel) ifTable.getModel();
        int i = 0;

        for (Variable[] variables : tableRows) {
            model.addRow(new DefaultMOMutableRow2PC(rowIndexes.get(i), variables));
            i++;
        }
        ifTable.setVolatile(true);
        return ifTable;
    }
}
