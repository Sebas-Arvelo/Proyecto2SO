package proyecto2so.ui;

import javax.swing.table.AbstractTableModel;
import proyecto2so.core.SystemConfig;
import proyecto2so.process.ProcessControlBlock;
import proyecto2so.process.ProcessQueue;

/**
 * Tabla que refleja el estado actual de la cola de procesos.
 */
public class ProcessTableModel extends AbstractTableModel {

    private final String[][] rows = new String[SystemConfig.MAX_PROCESSES][5];
    private int rowCount;

    public ProcessTableModel() {
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new String[4];
        }
    }

    public void updateFrom(ProcessQueue queue) {
        rowCount = 0;
        for (int i = 0; i < queue.size(); i++) {
            ProcessControlBlock pcb = queue.get(i);
            rows[i][0] = String.valueOf(pcb.getPid());
            rows[i][1] = pcb.getOperation().name();
            rows[i][2] = pcb.getState().name();
            rows[i][3] = pcb.getTargetPath();
            rows[i][4] = pcb.getPayload();
            rowCount++;
        }
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows[rowIndex][columnIndex];
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "PID";
            case 1:
                return "Operacion";
            case 2:
                return "Estado";
            case 3:
                return "Objetivo";
            default:
                return "Detalle";
        }
    }
}
