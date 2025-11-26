package proyecto2so.filesystem;

/**
 * Directorio que administra hijos con punteros entre hermanos.
 */
public class DirectoryNode extends FileSystemNode {

    public DirectoryNode(String id, String name) {
        super(id, name);
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    public void addChild(FileSystemNode child) {
        if (child == null) {
            return;
        }
        child.setParent(this);
        if (getFirstChild() == null) {
            setFirstChild(child);
            return;
        }
        FileSystemNode cursor = getFirstChild();
        while (cursor.getNextSibling() != null) {
            cursor = cursor.getNextSibling();
        }
        cursor.setNextSibling(child);
    }

    public FileSystemNode findChildByName(String name) {
        FileSystemNode cursor = getFirstChild();
        while (cursor != null) {
            if (cursor.getName().equalsIgnoreCase(name)) {
                return cursor;
            }
            cursor = cursor.getNextSibling();
        }
        return null;
    }

    public void removeChild(String name) {
        FileSystemNode prev = null;
        FileSystemNode cursor = getFirstChild();
        while (cursor != null) {
            if (cursor.getName().equalsIgnoreCase(name)) {
                if (prev == null) {
                    setFirstChild(cursor.getNextSibling());
                } else {
                    prev.setNextSibling(cursor.getNextSibling());
                }
                cursor.setNextSibling(null);
                cursor.setParent(null);
                return;
            }
            prev = cursor;
            cursor = cursor.getNextSibling();
        }
    }
}
