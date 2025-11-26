package proyecto2so.storage;

import proyecto2so.core.SystemConfig;

/**
 * Simple buffer cache abstraction supporting FIFO/LRU/LFU policies.
 */
public class BufferManager {

    private final BufferSlot[] slots = new BufferSlot[SystemConfig.MAX_BUFFER_SLOTS];
    private BufferPolicy policy = BufferPolicy.FIFO;
    private long clock;
    private int fifoPointer;
    private int hits;
    private int misses;

    public BufferManager() {
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new BufferSlot();
        }
    }

    public void setPolicy(BufferPolicy policy) {
        this.policy = policy;
    }

    public BufferSlot fetchBlock(int blockIndex, String diskPayload) {
        clock++;
        BufferSlot slot = find(blockIndex);
        if (slot != null) {
            slot.touch(clock);
            hits++;
            return slot;
        }
        BufferSlot target = selectVictim();
        target.load(blockIndex, diskPayload, clock);
        misses++;
        return target;
    }

    private BufferSlot find(int blockIndex) {
        for (int i = 0; i < slots.length; i++) {
            if (!slots[i].isEmpty() && slots[i].getBlockIndex() == blockIndex) {
                return slots[i];
            }
        }
        return null;
    }

    private BufferSlot selectVictim() {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].isEmpty()) {
                return slots[i];
            }
        }
        if (policy == BufferPolicy.FIFO) {
            BufferSlot victim = slots[fifoPointer];
            fifoPointer = (fifoPointer + 1) % slots.length;
            return victim;
        }
        if (policy == BufferPolicy.LRU) {
            return findOldest();
        }
        return findLeastFrequentlyUsed();
    }

    private BufferSlot findOldest() {
        BufferSlot candidate = slots[0];
        for (int i = 1; i < slots.length; i++) {
            if (slots[i].getLastAccessTick() < candidate.getLastAccessTick()) {
                candidate = slots[i];
            }
        }
        return candidate;
    }

    private BufferSlot findLeastFrequentlyUsed() {
        BufferSlot candidate = slots[0];
        for (int i = 1; i < slots.length; i++) {
            if (slots[i].getHitCount() < candidate.getHitCount()) {
                candidate = slots[i];
            }
        }
        return candidate;
    }

    public BufferSlot[] snapshot() {
        return slots;
    }

    public void reset() {
        for (int i = 0; i < slots.length; i++) {
            slots[i].clear();
        }
        clock = 0L;
        fifoPointer = 0;
        hits = 0;
        misses = 0;
    }

    public int getHits() {
        return hits;
    }

    public int getMisses() {
        return misses;
    }

    public void restoreStats(int hits, int misses) {
        this.hits = Math.max(0, hits);
        this.misses = Math.max(0, misses);
    }
}
