package proyecto2so.storage;

import proyecto2so.core.SystemConfig;

/**
 * Simulated disk that stores blocks and handles chained allocation.
 */
public class Disk {

    private final DiskBlock[] blocks;
    private int headPosition;

    public Disk() {
        this.blocks = new DiskBlock[SystemConfig.MAX_BLOCKS];
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = new DiskBlock(i);
        }
        this.headPosition = 0;
    }

    public DiskBlock[] getBlocks() {
        return blocks;
    }

    public int getHeadPosition() {
        return headPosition;
    }

    public void setHeadPosition(int headPosition) {
        if (headPosition >= 0 && headPosition < blocks.length) {
            this.headPosition = headPosition;
        }
    }

    public DiskBlock getBlock(int index) {
        if (index < 0 || index >= blocks.length) {
            return null;
        }
        return blocks[index];
    }

    public int getFreeBlockCount() {
        int free = 0;
        for (int i = 0; i < blocks.length; i++) {
            if (blocks[i].isFree()) {
                free++;
            }
        }
        return free;
    }

    public int capacity() {
        return blocks.length;
    }

    public void reset() {
        for (int i = 0; i < blocks.length; i++) {
            blocks[i].release();
        }
        headPosition = 0;
    }
}
