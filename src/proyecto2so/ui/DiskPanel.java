package proyecto2so.ui;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;
import proyecto2so.storage.Disk;
import proyecto2so.storage.DiskBlock;

/**
 * Dibuja los bloques del disco en un panel tipo rejilla.
 */
public class DiskPanel extends JPanel {

    private final Disk disk;

    public DiskPanel(Disk disk) {
        this.disk = disk;
        setBackground(Color.DARK_GRAY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        DiskBlock[] blocks = disk.getBlocks();
        int columns = 16;
        int rows = (int) Math.ceil(blocks.length / (double) columns);
        int width = getWidth();
        int height = getHeight();
        int cellWidth = Math.max(10, width / columns);
        int cellHeight = Math.max(10, height / rows);
        for (int i = 0; i < blocks.length; i++) {
            int row = i / columns;
            int col = i % columns;
            int x = col * cellWidth;
            int y = row * cellHeight;
            Color color = colorFor(blocks[i]);
            g.setColor(color);
            g.fillRect(x + 1, y + 1, cellWidth - 2, cellHeight - 2);
        }
    }

    private Color colorFor(DiskBlock block) {
        if (block.isFree()) {
            return Color.LIGHT_GRAY;
        }
        int hash = Math.abs(block.getOwnerId().hashCode());
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = (hash & 0x0000FF);
        return new Color(r, g, b);
    }
}
