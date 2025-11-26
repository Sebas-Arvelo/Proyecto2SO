package proyecto2so.filesystem;

import proyecto2so.core.SystemConfig;
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
        if (path == null) {
            return new String[0];
        }
        String trimmed = path.trim().replace('\\', '/');
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/") && trimmed.length() > 0) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        String[] raw = trimmed.split("/");
        int nonEmpty = 0;
        for (int i = 0; i < raw.length; i++) {
            if (!raw[i].isEmpty()) {
                nonEmpty++;
            }
        }
        String[] cleaned = new String[nonEmpty];
        int index = 0;
        for (int i = 0; i < raw.length; i++) {
            if (!raw[i].isEmpty()) {
                cleaned[index++] = raw[i];
            }
        }
        return cleaned;
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
            boolean publicReadable, String content, int createdByPid) {
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
        file.setCreatedByPid(createdByPid);
        file.setColorHex(colorFromId(file.getId()));
        writeContentToBlocks(file, content != null ? content : defaultContent(fileName));
        parent.addChild(file);
        return file;
    }

    public boolean updateFileContent(String path, String newContent, boolean publicReadable) {
        FileEntry file = getFile(path);
        if (file == null) {
            return false;
        }
        String content = newContent == null ? "" : newContent;
        int requiredBlocks = Math.max(1,
                (int) Math.ceil((double) Math.max(1, content.length()) / SystemConfig.BLOCK_SIZE_BYTES));
        if (!ensureBlockCount(file, requiredBlocks)) {
            return false;
        }
        writeContentToBlocks(file, content);
        file.setPublicReadable(publicReadable);
        return true;
    }

    public FileEntry addFileFromSnapshot(String parentPath, String fileName, String owner, int firstBlock,
            int blockCount, boolean publicReadable, String colorHex, int createdByPid) {
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
        file.setCreatedByPid(createdByPid);
        if (colorHex != null && !colorHex.isEmpty()) {
            file.setColorHex(colorHex);
        } else {
            file.setColorHex(colorFromId(file.getId()));
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
        if (file == null || file.getFirstBlockIndex() == -1) {
            return;
        }
        String safeContent = content == null ? "" : content;
        int chunkSize = Math.max(1, SystemConfig.BLOCK_SIZE_BYTES);
        int cursor = file.getFirstBlockIndex();
        int offset = 0;
        while (cursor != -1) {
            DiskBlock block = allocator.getDisk().getBlock(cursor);
            if (block == null) {
                break;
            }
            String segment;
            if (offset >= safeContent.length()) {
                segment = "";
            } else {
                int end = Math.min(safeContent.length(), offset + chunkSize);
                segment = safeContent.substring(offset, end);
            }
            block.setData(segment);
            offset += chunkSize;
            cursor = block.getNextIndex();
        }
    }

    private boolean ensureBlockCount(FileEntry file, int newBlockCount) {
        if (file.getFirstBlockIndex() == -1) {
            int head = allocator.allocateChain(file.getId(), newBlockCount);
            if (head == -1) {
                return false;
            }
            file.setFirstBlockIndex(head);
            file.setBlockCount(newBlockCount);
            return true;
        }
        int current = Math.max(1, file.getBlockCount());
        if (newBlockCount == current) {
            return true;
        }
        if (newBlockCount < current) {
            truncateChain(file, newBlockCount);
            file.setBlockCount(newBlockCount);
            return true;
        }
        int extraNeeded = newBlockCount - current;
        int extensionHead = allocator.allocateChain(file.getId(), extraNeeded);
        if (extensionHead == -1) {
            return false;
        }
        appendChain(file, extensionHead);
        file.setBlockCount(newBlockCount);
        return true;
    }

    private void truncateChain(FileEntry file, int keepBlocks) {
        int cursor = file.getFirstBlockIndex();
        int remaining = keepBlocks;
        DiskBlock lastKept = null;
        while (cursor != -1 && remaining > 0) {
            lastKept = allocator.getDisk().getBlock(cursor);
            cursor = lastKept != null ? lastKept.getNextIndex() : -1;
            remaining--;
        }
        if (lastKept == null) {
            return;
        }
        int tailHead = lastKept.getNextIndex();
        lastKept.setNextIndex(-1);
        allocator.releaseChain(tailHead);
    }

    private void appendChain(FileEntry file, int newChainHead) {
        if (file.getFirstBlockIndex() == -1) {
            file.setFirstBlockIndex(newChainHead);
            return;
        }
        int cursor = file.getFirstBlockIndex();
        DiskBlock last = null;
        while (cursor != -1) {
            last = allocator.getDisk().getBlock(cursor);
            cursor = (last != null) ? last.getNextIndex() : -1;
        }
        if (last != null) {
            last.setNextIndex(newChainHead);
        }
    }

    private String defaultContent(String fileName) {
        return "Contenido de " + fileName;
    }

    private String colorFromId(String value) {
        int hash = Math.abs(value.hashCode());
        int r = (hash >> 16) & 0xFF;
        int g = (hash >> 8) & 0xFF;
        int b = hash & 0xFF;
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
