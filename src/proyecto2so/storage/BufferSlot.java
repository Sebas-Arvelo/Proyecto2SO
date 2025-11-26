package proyecto2so.storage;

/**
 * Entry inside the optional buffer cache.
 */
public class BufferSlot {

    private int blockIndex = -1;
    private String data = "";
    private long lastAccessTick;
    private int hitCount;

    public boolean isEmpty() {
        return blockIndex == -1;
    }

    public void load(int blockIndex, String data, long tick) {
        this.blockIndex = blockIndex;
        this.data = data;
        this.lastAccessTick = tick;
        this.hitCount = 1;
    }

    public void touch(long tick) {
        this.lastAccessTick = tick;
        this.hitCount++;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public String getData() {
        return data;
    }

    public long getLastAccessTick() {
        return lastAccessTick;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void clear() {
        blockIndex = -1;
        data = "";
        lastAccessTick = 0L;
        hitCount = 0;
    }
}
