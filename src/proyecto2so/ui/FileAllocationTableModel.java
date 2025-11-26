package proyecto2so.ui;

import javax.swing.table.AbstractTableModel;
import proyecto2so.core.SystemConfig;
import proyecto2so.filesystem.DirectoryNode;
import proyecto2so.filesystem.FileEntry;
import proyecto2so.filesystem.FileSystemNode;

/**
 * Modelo de la tabla FAT: nombre, bloques, primer bloque, dueño, color y proceso.
 */
public class FileAllocationTableModel extends AbstractTableModel {

    private final String[][] rows = new String[SystemConfig.MAX_BLOCKS][6];
    private int rowCount;

    public FileAllocationTableModel() {
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new String[6];
        }
    }

    public void updateFrom(FileSystemNode root) {
        rowCount = 0;
        traverse(root);
        fireTableDataChanged();
    }

    private void traverse(FileSystemNode node) {
        if (node == null) {
            return;
        }
        if (!node.isDirectory()) {
            FileEntry file = (FileEntry) node;
            rows[rowCount][0] = file.getName();
            rows[rowCount][1] = String.valueOf(file.getBlockCount());
            rows[rowCount][2] = String.valueOf(file.getFirstBlockIndex());
            rows[rowCount][3] = file.getOwner();
            rows[rowCount][4] = file.getColorHex();
            rows[rowCount][5] = file.getCreatedByPid() == -1 ? "-" : String.valueOf(file.getCreatedByPid());
            rowCount++;
        }
        FileSystemNode child = node.getFirstChild();
        while (child != null) {
            traverse(child);
            child = child.getNextSibling();
        }
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows[rowIndex][columnIndex];
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Archivo";
            case 1:
                return "Bloques";
            case 2:
                return "Primer Bloque";
            case 3:
                return "Propietario";
            case 4:
                return "Color";
            default:
                return "Proceso";
        }
    }
}
