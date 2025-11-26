package proyecto2so.storage;

/**
 * Modela un bloque individual dentro del disco simulado.
 */
public class DiskBlock {

    private final int index;
    private boolean free = true;
    private int nextIndex = -1;
    private String ownerId = "";
    private String data = "";

    public DiskBlock(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public boolean isFree() {
        return free;
    }

    public void occupy(String ownerId) {
        this.free = false;
        this.ownerId = ownerId;
    }

    public void release() {
        this.free = true;
        this.ownerId = "";
        this.nextIndex = -1;
        this.data = "";
    }

    public int getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(int nextIndex) {
        this.nextIndex = nextIndex;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
