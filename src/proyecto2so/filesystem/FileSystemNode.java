package proyecto2so.filesystem;

/**
 * Base class with first-child/next-sibling representation to avoid Java collections.
 */
public abstract class FileSystemNode {

    private final String id;
    private String name;
    private DirectoryNode parent;
    private FileSystemNode firstChild;
    private FileSystemNode nextSibling;

    protected FileSystemNode(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public DirectoryNode getParent() {
        return parent;
    }

    void setParent(DirectoryNode parent) {
        this.parent = parent;
    }

    public FileSystemNode getFirstChild() {
        return firstChild;
    }

    void setFirstChild(FileSystemNode firstChild) {
        this.firstChild = firstChild;
    }

    public FileSystemNode getNextSibling() {
        return nextSibling;
    }

    void setNextSibling(FileSystemNode nextSibling) {
        this.nextSibling = nextSibling;
    }

    public abstract boolean isDirectory();
}
