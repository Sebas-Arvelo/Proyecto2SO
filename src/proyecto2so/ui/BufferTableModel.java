package proyecto2so.ui;

import javax.swing.table.AbstractTableModel;
import proyecto2so.storage.BufferManager;
import proyecto2so.storage.BufferSlot;

/**
 * Shows the current contents of the buffer cache.
 */
public class BufferTableModel extends AbstractTableModel {

    private BufferSlot[] snapshot;

    public void updateFrom(BufferManager manager) {
        this.snapshot = manager.snapshot();
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return snapshot == null ? 0 : snapshot.length;
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (snapshot == null || rowIndex >= snapshot.length) {
            return "";
        }
        BufferSlot slot = snapshot[rowIndex];
        if (slot == null) {
            return "";
        }
        switch (columnIndex) {
            case 0:
                return slot.isEmpty() ? "-" : slot.getBlockIndex();
            case 1:
                return slot.getData();
            case 2:
                return slot.getLastAccessTick();
            default:
                return slot.getHitCount();
        }
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Bloque";
            case 1:
                return "Datos";
            case 2:
                return "Ultimo";
            default:
                return "Hits";
        }
    }
}
