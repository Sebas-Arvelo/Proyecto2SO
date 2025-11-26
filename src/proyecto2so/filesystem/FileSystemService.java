package proyecto2so.filesystem;

import proyecto2so.storage.BlockAllocator;
import proyecto2so.storage.DiskBlock;

/**
 * Provee las operaciones CRUD del sistema de archivos y conversa con el disco.
 */
public class FileSystemService {

    private final DirectoryNode root;
    private final BlockAllocator allocator;
    private int idSequence = 1;

    public FileSystemService(BlockAllocator allocator) {
        this.allocator = allocator;
        this.root = new DirectoryNode(nextId(), "/");
    }

    private String nextId() {
        return "node-" + (idSequence++);
    }

    public DirectoryNode getRoot() {
        return root;
    }

    public void reset() {
        FileSystemNode child = root.getFirstChild();
        while (child != null) {
            FileSystemNode next = child.getNextSibling();
            removeNodeRecursive(child);
            child = next;
        }
        root.setFirstChild(null);
    }

    public FileSystemNode findNode(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return root;
        }
        String[] parts = normalize(path);
        FileSystemNode cursor = root;
        for (int i = 0; i < parts.length && cursor != null; i++) {
            if (!cursor.isDirectory()) {
                return null;
            }
            cursor = ((DirectoryNode) cursor).findChildByName(parts[i]);
        }
        return cursor;
    }

    private String[] normalize(String path) {
        String trimmed = path.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        return trimmed.split("/");
    }

    public DirectoryNode createDirectory(String parentPath, String dirName) {
        DirectoryNode parent = asDirectory(findNode(parentPath));
        if (parent == null || parent.findChildByName(dirName) != null) {
            return null;
        }
        DirectoryNode dir = new DirectoryNode(nextId(), dirName);
        parent.addChild(dir);
        return dir;
    }

    public FileEntry createFile(String parentPath, String fileName, int blocksNeeded, String owner,
            boolean publicReadable, String content) {
        DirectoryNode parent = asDirectory(findNode(parentPath));
        if (parent == null || parent.findChildByName(fileName) != null) {
            return null;
        }
        FileEntry file = new FileEntry(nextId(), fileName, owner);
        int headIndex = allocator.allocateChain(file.getId(), blocksNeeded);
        if (headIndex == -1) {
            return null;
        }
        file.setBlockCount(blocksNeeded);
        file.setFirstBlockIndex(headIndex);
        file.setPublicReadable(publicReadable);
        writeContentToBlocks(file, content != null ? content : defaultContent(fileName));
        parent.addChild(file);
        return file;
    }

    public FileEntry addFileFromSnapshot(String parentPath, String fileName, String owner, int firstBlock,
            int blockCount, boolean publicReadable, String colorHex) {
        DirectoryNode parent = asDirectory(findNode(parentPath));
        if (parent == null) {
            return null;
        }
        if (parent.findChildByName(fileName) != null) {
            return null;
        }
        FileEntry file = new FileEntry(nextId(), fileName, owner);
        file.setFirstBlockIndex(firstBlock);
        file.setBlockCount(blockCount);
        file.setPublicReadable(publicReadable);
        if (colorHex != null && !colorHex.isEmpty()) {
            file.setColorHex(colorHex);
        }
        parent.addChild(file);
        return file;
    }

    public boolean deleteNode(String path) {
        if (path == null || path.equals("/")) {
            return false;
        }
        String[] parts = normalize(path);
        if (parts.length == 0) {
            return false;
        }
        DirectoryNode parent = root;
        for (int i = 0; i < parts.length - 1; i++) {
            FileSystemNode child = parent.findChildByName(parts[i]);
            if (!(child instanceof DirectoryNode)) {
                return false;
            }
            parent = (DirectoryNode) child;
        }
        FileSystemNode target = parent.findChildByName(parts[parts.length - 1]);
        if (target == null) {
            return false;
        }
        removeNodeRecursive(target);
        parent.removeChild(target.getName());
        return true;
    }

    private void removeNodeRecursive(FileSystemNode node) {
        if (node.isDirectory()) {
            FileSystemNode child = node.getFirstChild();
            while (child != null) {
                FileSystemNode next = child.getNextSibling();
                removeNodeRecursive(child);
                child = next;
            }
        } else {
            FileEntry file = (FileEntry) node;
            allocator.releaseChain(file.getFirstBlockIndex());
        }
    }

    private DirectoryNode asDirectory(FileSystemNode node) {
        if (node instanceof DirectoryNode) {
            return (DirectoryNode) node;
        }
        return null;
    }

    public FileEntry getFile(String path) {
        FileSystemNode node = findNode(path);
        if (node instanceof FileEntry) {
            return (FileEntry) node;
        }
        return null;
    }

    public String readFileData(FileEntry file) {
        if (file == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int cursor = file.getFirstBlockIndex();
        while (cursor != -1) {
            DiskBlock block = allocator.getDisk().getBlock(cursor);
            if (block == null || block.isFree()) {
                break;
            }
            builder.append(block.getData());
            builder.append('\n');
            cursor = block.getNextIndex();
        }
        return builder.toString().trim();
    }

    public boolean renameNode(String path, String newName) {
        if (newName == null || newName.isEmpty()) {
            return false;
        }
        FileSystemNode node = findNode(path);
        if (node == null || node == root) {
            return false;
        }
        if (node.getParent() != null && node.getParent().findChildByName(newName) != null) {
            return false;
        }
        node.rename(newName);
        return true;
    }

    private void writeContentToBlocks(FileEntry file, String content) {
        int cursor = file.getFirstBlockIndex();
        int blockNumber = 1;
        while (cursor != -1) {
            DiskBlock block = allocator.getDisk().getBlock(cursor);
            if (block == null) {
                break;
            }
            block.setData(content + "#" + blockNumber);
            cursor = block.getNextIndex();
            blockNumber++;
        }
    }

    private String defaultContent(String fileName) {
        return "Contenido de " + fileName;
    }
}
