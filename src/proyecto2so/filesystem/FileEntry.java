package proyecto2so.filesystem;

/**
 * Stores metadata for a file inside the simulated FS.
 */
public class FileEntry extends FileSystemNode {

    private final String owner;
    private int blockCount;
    private int firstBlockIndex = -1;
    private String colorHex = "#CCCCCC";
    private boolean publicReadable = true;

    public FileEntry(String id, String name, String owner) {
        super(id, name);
        this.owner = owner;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    public String getOwner() {
        return owner;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }

    public int getFirstBlockIndex() {
        return firstBlockIndex;
    }

    public void setFirstBlockIndex(int firstBlockIndex) {
        this.firstBlockIndex = firstBlockIndex;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public boolean isPublicReadable() {
        return publicReadable;
    }

    public void setPublicReadable(boolean publicReadable) {
        this.publicReadable = publicReadable;
    }
}
