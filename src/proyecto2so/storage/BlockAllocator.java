package proyecto2so.storage;

/**
 * Handles chained allocation and release of disk blocks.
 */
public class BlockAllocator {

    private final Disk disk;

    public BlockAllocator(Disk disk) {
        this.disk = disk;
    }

    public Disk getDisk() {
        return disk;
    }

    public int allocateChain(String fileId, int blocksNeeded) {
        int previousIndex = -1;
        int headIndex = -1;
        for (int i = 0; i < disk.capacity() && blocksNeeded > 0; i++) {
            DiskBlock block = disk.getBlock(i);
            if (block.isFree()) {
                block.occupy(fileId);
                if (previousIndex != -1) {
                    disk.getBlock(previousIndex).setNextIndex(block.getIndex());
                }
                if (headIndex == -1) {
                    headIndex = block.getIndex();
                }
                previousIndex = block.getIndex();
                blocksNeeded--;
            }
        }
        if (blocksNeeded > 0) {
            // rollback partial allocation
            releaseChain(headIndex);
            return -1;
        }
        return headIndex;
    }

    public void releaseChain(int startIndex) {
        int cursor = startIndex;
        while (cursor != -1) {
            DiskBlock block = disk.getBlock(cursor);
            if (block == null || block.isFree()) {
                break;
            }
            int next = block.getNextIndex();
            block.release();
            cursor = next;
        }
    }
}
