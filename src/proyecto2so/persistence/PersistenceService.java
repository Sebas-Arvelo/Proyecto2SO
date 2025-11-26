package proyecto2so.persistence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import proyecto2so.filesystem.FileEntry;
import proyecto2so.filesystem.FileSystemNode;
import proyecto2so.filesystem.FileSystemService;
import proyecto2so.process.OperationType;
import proyecto2so.process.ProcessControlBlock;
import proyecto2so.process.ProcessQueue;
import proyecto2so.process.ProcessState;
import proyecto2so.scheduler.DiskRequest;
import proyecto2so.scheduler.DiskRequestQueue;
import proyecto2so.storage.BufferManager;
import proyecto2so.storage.Disk;
import proyecto2so.storage.DiskBlock;

/**
 * Serializa/deserializa el estado completo del simulador sin librerías externas.
 */
public class PersistenceService {

    private static final String HEADER = "P2SO_SNAPSHOT 1";
    private final FileSystemService fileSystem;
    private final Disk disk;
    private final ProcessQueue processQueue;
    private final DiskRequestQueue requestQueue;
    private final BufferManager buffer;

    public PersistenceService(FileSystemService fs, Disk disk, ProcessQueue pq, DiskRequestQueue rq, BufferManager buffer) {
        this.fileSystem = fs;
        this.disk = disk;
        this.processQueue = pq;
        this.requestQueue = rq;
        this.buffer = buffer;
    }

    public boolean save(String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println(HEADER);
            writeFileSystem(writer);
            writeDisk(writer);
            writeProcesses(writer);
            writeRequests(writer);
            writeBufferStats(writer);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Loads snapshot data. Returns the highest PID found (to adjust pid sequence) or -1 on failure.
     */
    public int load(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String first = reader.readLine();
            if (first == null || !HEADER.equals(first.trim())) {
                return -1;
            }
            fileSystem.reset();
            disk.reset();
            processQueue.clear();
            requestQueue.clear();
            buffer.reset();

            Section section = Section.NONE;
            String line;
            int maxPid = 0;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if ("FS".equals(trimmed)) {
                    section = Section.FS;
                    continue;
                }
                if ("END_FS".equals(trimmed)) {
                    section = Section.NONE;
                    continue;
                }
                if ("DISK".equals(trimmed)) {
                    section = Section.DISK;
                    continue;
                }
                if ("END_DISK".equals(trimmed)) {
                    section = Section.NONE;
                    continue;
                }
                if ("PROC".equals(trimmed)) {
                    section = Section.PROC;
                    continue;
                }
                if ("END_PROC".equals(trimmed)) {
                    section = Section.NONE;
                    continue;
                }
                if ("REQ".equals(trimmed)) {
                    section = Section.REQ;
                    continue;
                }
                if ("END_REQ".equals(trimmed)) {
                    section = Section.NONE;
                    continue;
                }
                if ("BUFFER".equals(trimmed)) {
                    section = Section.BUFFER;
                    continue;
                }
                if ("END_BUFFER".equals(trimmed)) {
                    section = Section.NONE;
                    continue;
                }

                switch (section) {
                    case FS:
                        parseFileSystemLine(trimmed);
                        break;
                    case DISK:
                        parseDiskLine(trimmed);
                        break;
                    case PROC:
                        int pid = parseProcessLine(trimmed);
                        if (pid > maxPid) {
                            maxPid = pid;
                        }
                        break;
                    case REQ:
                        parseRequestLine(trimmed);
                        break;
                    case BUFFER:
                        parseBufferLine(trimmed);
                        break;
                    default:
                        break;
                }
            }
            return maxPid;
        } catch (IOException ex) {
            return -1;
        }
    }

    private void writeFileSystem(PrintWriter writer) {
        writer.println("FS");
        writeChildren(fileSystem.getRoot(), "/", writer);
        writer.println("END_FS");
    }

    private void writeChildren(FileSystemNode parent, String currentPath, PrintWriter writer) {
        FileSystemNode child = parent.getFirstChild();
        while (child != null) {
            String childPath = "/".equals(currentPath) ? currentPath + child.getName() : currentPath + "/" + child.getName();
            if (child.isDirectory()) {
                writer.println("DIR|" + encode(childPath));
                writeChildren(child, childPath, writer);
            } else {
                FileEntry file = (FileEntry) child;
                writer.println("FILE|" + encode(childPath) + "|" + encode(file.getOwner()) + "|" + file.getBlockCount()
                        + "|" + file.getFirstBlockIndex() + "|" + (file.isPublicReadable() ? 1 : 0) + "|" + encode(file.getColorHex()));
            }
            child = child.getNextSibling();
        }
    }

    private void writeDisk(PrintWriter writer) {
        writer.println("DISK");
        writer.println("HEAD|" + disk.getHeadPosition());
        DiskBlock[] blocks = disk.getBlocks();
        for (int i = 0; i < blocks.length; i++) {
            DiskBlock block = blocks[i];
            writer.println("BLOCK|" + i + "|" + (block.isFree() ? 1 : 0) + "|" + encode(block.getOwnerId())
                    + "|" + block.getNextIndex() + "|" + encode(block.getData()));
        }
        writer.println("END_DISK");
    }

    private void writeProcesses(PrintWriter writer) {
        writer.println("PROC");
        for (int i = 0; i < processQueue.size(); i++) {
            ProcessControlBlock pcb = processQueue.get(i);
            writer.println("PCB|" + pcb.getPid() + "|" + pcb.getOperation().name() + "|" + pcb.getState().name()
                    + "|" + encode(pcb.getTargetPath()) + "|" + encode(pcb.getOwner()) + "|" + pcb.getRequestedBlocks()
                    + "|" + encode(pcb.getPayload()));
        }
        writer.println("END_PROC");
    }

    private void writeRequests(PrintWriter writer) {
        writer.println("REQ");
        for (int i = 0; i < requestQueue.size(); i++) {
            DiskRequest req = requestQueue.get(i);
            if (req == null || req.getPcb() == null) {
                continue;
            }
            writer.println("REQ|" + req.getPcb().getPid() + "|" + req.getTargetBlock());
        }
        writer.println("END_REQ");
    }

    private void writeBufferStats(PrintWriter writer) {
        writer.println("BUFFER");
        writer.println("STATS|" + buffer.getHits() + "|" + buffer.getMisses());
        writer.println("END_BUFFER");
    }

    private void parseFileSystemLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length == 0) {
            return;
        }
        if ("DIR".equals(parts[0]) && parts.length >= 2) {
            String path = decode(parts[1]);
            ensureDirectory(path);
        } else if ("FILE".equals(parts[0]) && parts.length >= 7) {
            String path = decode(parts[1]);
            String owner = decode(parts[2]);
            int blocks = parseInt(parts[3]);
            int first = parseInt(parts[4]);
            boolean publicReadable = "1".equals(parts[5]);
            String color = decode(parts[6]);
            String parentPath = parentOf(path);
            String name = nameOf(path);
            fileSystem.addFileFromSnapshot(parentPath, name, owner, first, blocks, publicReadable, color);
        }
    }

    private void parseDiskLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length == 0) {
            return;
        }
        if ("HEAD".equals(parts[0]) && parts.length >= 2) {
            disk.setHeadPosition(parseInt(parts[1]));
            return;
        }
        if ("BLOCK".equals(parts[0]) && parts.length >= 6) {
            int index = parseInt(parts[1]);
            boolean free = "1".equals(parts[2]);
            String owner = decode(parts[3]);
            int next = parseInt(parts[4]);
            String data = decode(parts[5]);
            DiskBlock block = disk.getBlock(index);
            if (block == null) {
                return;
            }
            block.release();
            if (!free) {
                block.occupy(owner);
                block.setNextIndex(next);
                block.setData(data);
            }
        }
    }

    private int parseProcessLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 8) {
            return 0;
        }
        int pid = parseInt(parts[1]);
        OperationType type = safeOperation(parts[2]);
        ProcessState state = safeState(parts[3]);
        String target = decode(parts[4]);
        String owner = decode(parts[5]);
        int blocks = parseInt(parts[6]);
        String payload = decode(parts[7]);
        ProcessControlBlock pcb = new ProcessControlBlock(pid, type, target, owner);
        pcb.setRequestedBlocks(blocks);
        pcb.setPayload(payload);
        pcb.setState(state);
        processQueue.enqueue(pcb);
        return pid;
    }

    private void parseRequestLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 3) {
            return;
        }
        int pid = parseInt(parts[1]);
        int target = parseInt(parts[2]);
        ProcessControlBlock pcb = processQueue.findByPid(pid);
        if (pcb == null) {
            return;
        }
        requestQueue.enqueue(new DiskRequest(pcb, target));
    }

    private void parseBufferLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 3) {
            return;
        }
        if ("STATS".equals(parts[0])) {
            buffer.restoreStats(parseInt(parts[1]), parseInt(parts[2]));
        }
    }

    private OperationType safeOperation(String text) {
        try {
            return OperationType.valueOf(text);
        } catch (IllegalArgumentException ex) {
            return OperationType.READ;
        }
    }

    private ProcessState safeState(String text) {
        try {
            return ProcessState.valueOf(text);
        } catch (IllegalArgumentException ex) {
            return ProcessState.NEW;
        }
    }

    private void ensureDirectory(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return;
        }
        String[] segments = path.substring(1).split("/");
        String current = "/";
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (segment.isEmpty()) {
                continue;
            }
            String nextPath = current.equals("/") ? current + segment : current + "/" + segment;
            if (fileSystem.findNode(nextPath) == null) {
                fileSystem.createDirectory(current, segment);
            }
            current = nextPath;
        }
    }

    private String parentOf(String path) {
        int idx = path.lastIndexOf('/') ;
        if (idx <= 0) {
            return "/";
        }
        return path.substring(0, idx);
    }

    private String nameOf(String path) {
        int idx = path.lastIndexOf('/');
        if (idx == -1) {
            return path;
        }
        return path.substring(idx + 1);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String encode(String text) {
        if (text == null) {
            return "";
        }
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        byte[] bytes = Base64.getDecoder().decode(encoded);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private enum Section {
        NONE,
        FS,
        DISK,
        PROC,
        REQ,
        BUFFER
    }
}
