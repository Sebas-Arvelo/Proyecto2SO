package proyecto2so.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField; // Added import for JTextField
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import proyecto2so.core.SystemConfig;
import proyecto2so.filesystem.FileSystemNode;
import proyecto2so.filesystem.FileSystemService;
import proyecto2so.filesystem.FileEntry; // Added import for FileEntry
import proyecto2so.persistence.PersistenceService;
import proyecto2so.process.OperationType;
import proyecto2so.process.ProcessControlBlock;
import proyecto2so.process.ProcessQueue;
import proyecto2so.process.ProcessState;
import proyecto2so.scheduler.DiskRequest;
import proyecto2so.scheduler.DiskRequestQueue;
import proyecto2so.scheduler.DiskScheduler;
import proyecto2so.scheduler.FifoScheduler;
import proyecto2so.scheduler.SchedulerFactory;
import proyecto2so.storage.BufferManager;
import proyecto2so.storage.BufferPolicy;
import proyecto2so.storage.Disk;
import proyecto2so.storage.DiskBlock;

/**
 * Main Swing window for the simulator.
 */
public class MainWindow extends JFrame {

    private final FileSystemService fileSystem;
    private final Disk disk;
    private final BufferManager buffer;
    private final ProcessQueue processQueue = new ProcessQueue();
    private final DiskRequestQueue requestQueue = new DiskRequestQueue();
    private final PersistenceService persistence;
    private DiskScheduler scheduler = new FifoScheduler();

    private int pidSequence = 1;

    private final JTree tree;
    private final DiskPanel diskPanel;
    private final FileAllocationTableModel fatModel = new FileAllocationTableModel();
    private final ProcessTableModel processModel = new ProcessTableModel();
    private final BufferTableModel bufferModel = new BufferTableModel();
    private final JComboBox<String> modeSelector = new JComboBox<>(new String[]{"Administrador", "Usuario"});
    private final JComboBox<String> schedulerSelector = new JComboBox<>(new String[]{"FIFO", "SSTF", "SCAN", "C-SCAN"});
    private final JComboBox<String> bufferPolicySelector = new JComboBox<>(new String[]{"FIFO", "LRU", "LFU"});
    private final JLabel diskStatsLabel = new JLabel();
    private final JLabel bufferStatsLabel = new JLabel();
    private final JTextField userField = new JTextField(SystemConfig.ROOT_USER, 10);

    public MainWindow(FileSystemService fs, Disk disk, BufferManager buffer) {
        super("Simulador Sistema de Archivos - Proyecto 2 SO");
        this.fileSystem = fs;
        this.disk = disk;
        this.buffer = buffer;
        this.persistence = new PersistenceService(fileSystem, disk, processQueue, requestQueue, buffer);

        this.tree = new JTree(buildTreeModel());
        this.diskPanel = new DiskPanel(disk);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1200, 720));
        setLayout(new BorderLayout());

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        updateDiskStats();
        updateUserFieldState();
    }

    private JPanel buildTopBar() {
        JPanel panel = new JPanel();
        panel.add(new JLabel("Modo:"));
        panel.add(modeSelector);
        panel.add(new JLabel("Usuario:"));
        panel.add(userField);
        panel.add(new JLabel("Planificador:"));
        panel.add(schedulerSelector);
        panel.add(new JLabel("Buffer:"));
        panel.add(bufferPolicySelector);
        JButton btnDir = new JButton("Crear Directorio");
        JButton btnFile = new JButton("Crear Archivo");
        JButton btnDelete = new JButton("Eliminar");
        JButton btnRead = new JButton("Leer Archivo");
        JButton btnRename = new JButton("Renombrar");
        JButton btnSave = new JButton("Guardar");
        JButton btnLoad = new JButton("Cargar");
        panel.add(btnDir);
        panel.add(btnFile);
        panel.add(btnDelete);
        panel.add(btnRead);
        panel.add(btnRename);
        panel.add(btnSave);
        panel.add(btnLoad);
        panel.add(diskStatsLabel);
        panel.add(bufferStatsLabel);

        schedulerSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scheduler = SchedulerFactory.create((String) schedulerSelector.getSelectedItem());
            }
        });

        bufferPolicySelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buffer.setPolicy(BufferPolicy.valueOf((String) bufferPolicySelector.getSelectedItem()));
                updateBufferStats();
            }
        });

        modeSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUserFieldState();
            }
        });

        btnDir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCreateDirectory();
            }
        });

        btnFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCreateFile();
            }
        });

        btnDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleDelete();
            }
        });

        btnRead.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleReadFile();
            }
        });

        btnRename.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRename();
            }
        });

        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSaveSnapshot();
            }
        });

        btnLoad.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLoadSnapshot();
            }
        });

        return panel;
    }

    private JPanel buildCenter() {
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSplit.setTopComponent(new JScrollPane(tree));
        leftSplit.setBottomComponent(new JScrollPane(buildFatTable()));
        leftSplit.setDividerLocation(300);

        JPanel rightPanel = new JPanel(new BorderLayout());
        diskPanel.setPreferredSize(new Dimension(400, 300));
        rightPanel.add(diskPanel, BorderLayout.CENTER);
        JSplitPane bufferSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        bufferSplit.setTopComponent(new JScrollPane(buildBufferTable()));
        bufferSplit.setBottomComponent(new JScrollPane(buildProcessTable()));
        bufferSplit.setDividerLocation(150);
        bufferSplit.setPreferredSize(new Dimension(400, 280));
        rightPanel.add(bufferSplit, BorderLayout.SOUTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightPanel);
        mainSplit.setDividerLocation(500);

        JPanel container = new JPanel(new BorderLayout());
        container.add(mainSplit, BorderLayout.CENTER);
        return container;
    }

    private javax.swing.JTable buildFatTable() {
        javax.swing.JTable table = new javax.swing.JTable(fatModel);
        table.setFillsViewportHeight(true);
        return table;
    }

    private javax.swing.JTable buildProcessTable() {
        javax.swing.JTable table = new javax.swing.JTable(processModel);
        table.setPreferredScrollableViewportSize(new Dimension(400, 200));
        table.setFillsViewportHeight(true);
        return table;
    }

    private javax.swing.JTable buildBufferTable() {
        javax.swing.JTable table = new javax.swing.JTable(bufferModel);
        table.setPreferredScrollableViewportSize(new Dimension(400, 150));
        table.setFillsViewportHeight(true);
        return table;
    }

    private DefaultTreeModel buildTreeModel() {
        DefaultMutableTreeNode root = buildTreeNodes(fileSystem.getRoot());
        return new DefaultTreeModel(root);
    }

    private DefaultMutableTreeNode buildTreeNodes(FileSystemNode node) {
        DefaultMutableTreeNode uiNode = new DefaultMutableTreeNode(node.getName());
        FileSystemNode child = node.getFirstChild();
        while (child != null) {
            uiNode.add(buildTreeNodes(child));
            child = child.getNextSibling();
        }
        return uiNode;
    }

    private void refreshTree() {
        tree.setModel(buildTreeModel());
    }

    public void refreshAll() {
        refreshTree();
        fatModel.updateFrom(fileSystem.getRoot());
        processModel.updateFrom(processQueue);
        bufferModel.updateFrom(buffer);
        diskPanel.repaint();
        updateDiskStats();
        updateBufferStats();
    }

    private void updateDiskStats() {
        String text = "Bloques libres: " + disk.getFreeBlockCount() + " / " + disk.capacity()
                + " | Cabezal: " + disk.getHeadPosition();
        diskStatsLabel.setText(text);
    }

    private void updateBufferStats() {
        String text = "Buffer Hits: " + buffer.getHits() + " | Misses: " + buffer.getMisses();
        bufferStatsLabel.setText(text);
    }

    private void updateUserFieldState() {
        if (isAdmin()) {
            userField.setText(SystemConfig.ROOT_USER);
            userField.setEditable(false);
        } else {
            if (SystemConfig.ROOT_USER.equals(userField.getText())) {
                userField.setText("usuario");
            }
            userField.setEditable(true);
        }
    }

    private void handleCreateDirectory() {
        if (!isAdmin()) {
            showRestricted();
            return;
        }
        String parent = prompt("Ruta del directorio padre", "/");
        if (parent == null) {
            return;
        }
        String name = prompt("Nombre del nuevo directorio", "nuevo_dir");
        if (name == null) {
            return;
        }
        submitProcess(OperationType.MKDIR, joinPath(parent, name), 0, "");
    }

    private void handleCreateFile() {
        if (!isAdmin()) {
            showRestricted();
            return;
        }
        String parent = prompt("Ruta del directorio contenedor", "/");
        if (parent == null) {
            return;
        }
        String name = prompt("Nombre del archivo", "archivo.txt");
        if (name == null) {
            return;
        }
        String sizeText = prompt("Tamano en bloques", "1");
        if (sizeText == null) {
            return;
        }
        int blocks;
        try {
            blocks = Integer.parseInt(sizeText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Tamano invalido");
            return;
        }
        boolean isPublic = JOptionPane.showConfirmDialog(this, "Archivo publico?", "Visibilidad",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        String content = prompt("Contenido del archivo", "Datos de " + name);
        String payload = (isPublic ? "PUBLIC:" : "PRIVATE:") + (content == null ? "" : content);
        submitProcess(OperationType.CREATE, joinPath(parent, name), blocks, payload);
    }

    private void handleDelete() {
        if (!isAdmin()) {
            showRestricted();
            return;
        }
        String path = prompt("Ruta a eliminar", "/archivo.txt");
        if (path == null) {
            return;
        }
        submitProcess(OperationType.DELETE, path, 0, "");
    }

    private void handleReadFile() {
        String path = prompt("Ruta del archivo a leer", "/archivo.txt");
        if (path == null) {
            return;
        }
        FileEntry file = fileSystem.getFile(path);
        if (file == null) {
            JOptionPane.showMessageDialog(this, "El archivo no existe");
            return;
        }
        if (!canRead(file)) {
            JOptionPane.showMessageDialog(this, "No tiene permisos para leer este archivo");
            return;
        }
        submitProcess(OperationType.READ, path, 0, "");
    }

    private void handleRename() {
        if (!isAdmin()) {
            showRestricted();
            return;
        }
        String path = prompt("Ruta a renombrar", "/archivo.txt");
        if (path == null) {
            return;
        }
        String newName = prompt("Nuevo nombre", "nuevo.txt");
        if (newName == null) {
            return;
        }
        submitProcess(OperationType.UPDATE, path, 0, newName);
    }

    private void handleSaveSnapshot() {
        String path = prompt("Ruta de guardado", "snapshot.txt");
        if (path == null) {
            return;
        }
        boolean ok = persistence.save(path);
        JOptionPane.showMessageDialog(this, ok ? "Estado guardado" : "No se pudo guardar el archivo");
    }

    private void handleLoadSnapshot() {
        String path = prompt("Ruta del archivo a cargar", "snapshot.txt");
        if (path == null) {
            return;
        }
        int maxPid = persistence.load(path);
        if (maxPid == -1) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el archivo");
            return;
        }
        pidSequence = Math.max(pidSequence, maxPid + 1);
        refreshAll();
        JOptionPane.showMessageDialog(this, "Estado cargado correctamente");
    }

    private void submitProcess(OperationType op, String path, int blocks, String payload) {
        ProcessControlBlock pcb = new ProcessControlBlock(pidSequence++, op, path, getCurrentUser());
        pcb.setRequestedBlocks(blocks);
        pcb.setState(ProcessState.READY);
        pcb.setPayload(payload == null ? "" : payload);
        if (!processQueue.enqueue(pcb)) {
            JOptionPane.showMessageDialog(this, "La cola de procesos esta llena");
            return;
        }
        processModel.updateFrom(processQueue);
        if (!enqueueRequest(pcb)) {
            processQueue.removeByPid(pcb.getPid());
            processModel.updateFrom(processQueue);
            return;
        }
        processModel.updateFrom(processQueue);
        dispatchRequests();
    }

    private boolean enqueueRequest(ProcessControlBlock pcb) {
        int targetBlock = estimateTargetBlock(pcb.getTargetPath());
        DiskRequest request = new DiskRequest(pcb, targetBlock);
        if (!requestQueue.enqueue(request)) {
            JOptionPane.showMessageDialog(this, "La cola de E/S esta llena");
            return false;
        }
        pcb.setState(ProcessState.BLOCKED);
        return true;
    }

    private void dispatchRequests() {
        while (requestQueue.size() > 0) {
            int index = scheduler.selectNext(disk.getHeadPosition(), requestQueue);
            if (index == -1) {
                break;
            }
            DiskRequest request = requestQueue.dequeueAt(index);
            ProcessControlBlock pcb = request.getPcb();
            disk.setHeadPosition(request.getTargetBlock());
            pcb.setState(ProcessState.RUNNING);
            processModel.updateFrom(processQueue);
            boolean success = executeOperation(pcb);
            pcb.setState(ProcessState.TERMINATED);
            processModel.updateFrom(processQueue);
            String message = (success ? "Operacion completada" : "Operacion fallo")
                    + "\nPlanificador: " + scheduler.getName()
                    + "\nBloque atendido: " + request.getTargetBlock();
            JOptionPane.showMessageDialog(this, message);
            processQueue.removeByPid(pcb.getPid());
            refreshAll();
        }
    }

    private boolean executeOperation(ProcessControlBlock pcb) {
        switch (pcb.getOperation()) {
            case CREATE:
                return performCreate(pcb);
            case MKDIR:
                return performMkdir(pcb);
            case DELETE:
                return fileSystem.deleteNode(pcb.getTargetPath());
            case READ:
                return performRead(pcb);
            case UPDATE:
                return performRename(pcb);
            default:
                return true;
        }
    }

    private int estimateTargetBlock(String path) {
        FileEntry file = fileSystem.getFile(path);
        if (file != null && file.getFirstBlockIndex() != -1) {
            return file.getFirstBlockIndex();
        }
        int capacity = Math.max(1, disk.capacity());
        int hash = Math.abs(path.hashCode());
        return hash % capacity;
    }

    private boolean performCreate(ProcessControlBlock pcb) {
        String fullPath = pcb.getTargetPath();
        String parent = parentOf(fullPath);
        String name = nameOf(fullPath);
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        if (name.isEmpty()) {
            return false;
        }
        String payload = pcb.getPayload() == null ? "" : pcb.getPayload();
        boolean isPublic = true;
        String content = null;
        if (payload.startsWith("PRIVATE:")) {
            isPublic = false;
            content = payload.substring("PRIVATE:".length());
        } else if (payload.startsWith("PUBLIC:")) {
            isPublic = true;
            content = payload.substring("PUBLIC:".length());
        } else {
            content = payload;
        }
        return fileSystem.createFile(parent, name, Math.max(1, pcb.getRequestedBlocks()), pcb.getOwner(), isPublic, content) != null;
    }

    private boolean performRead(ProcessControlBlock pcb) {
        FileEntry file = fileSystem.getFile(pcb.getTargetPath());
        if (file == null) {
            return false;
        }
        int hitsBefore = buffer.getHits();
        int missesBefore = buffer.getMisses();
        StringBuilder builder = new StringBuilder();
        int cursor = file.getFirstBlockIndex();
        while (cursor != -1) {
            DiskBlock block = disk.getBlock(cursor);
            if (block == null || block.isFree()) {
                break;
            }
            buffer.fetchBlock(cursor, block.getData());
            builder.append(block.getData()).append('\n');
            cursor = block.getNextIndex();
        }
        int hitDelta = buffer.getHits() - hitsBefore;
        int missDelta = buffer.getMisses() - missesBefore;
        JOptionPane.showMessageDialog(this, "Lectura completada:\n" + builder.toString().trim()
                + "\nBuffer hits: " + hitDelta + " | miss: " + missDelta);
        return true;
    }

    private boolean performMkdir(ProcessControlBlock pcb) {
        String fullPath = pcb.getTargetPath();
        String parent = parentOf(fullPath);
        String name = nameOf(fullPath);
        return fileSystem.createDirectory(parent, name) != null;
    }

    private boolean performRename(ProcessControlBlock pcb) {
        String newName = pcb.getPayload();
        if (newName == null || newName.trim().isEmpty()) {
            return false;
        }
        return fileSystem.renameNode(pcb.getTargetPath(), newName.trim());
    }

    private String parentOf(String fullPath) {
        int lastSlash = fullPath.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return fullPath.substring(0, lastSlash);
    }

    private String nameOf(String fullPath) {
        int lastSlash = fullPath.lastIndexOf('/');
        if (lastSlash == -1) {
            return fullPath;
        }
        return fullPath.substring(lastSlash + 1);
    }

    private String joinPath(String parent, String name) {
        if (parent.endsWith("/")) {
            return parent + name;
        }
        if (parent.isEmpty()) {
            return "/" + name;
        }
        return parent + "/" + name;
    }

    private boolean isAdmin() {
        return modeSelector.getSelectedIndex() == 0;
    }

    private String getCurrentUser() {
        if (isAdmin()) {
            userField.setText(SystemConfig.ROOT_USER);
            return SystemConfig.ROOT_USER;
        }
        String text = userField.getText() == null ? "" : userField.getText().trim();
        if (text.isEmpty()) {
            text = "usuario";
            userField.setText(text);
        }
        return text;
    }

    private void showRestricted() {
        JOptionPane.showMessageDialog(this, "La operacion no esta disponible en modo usuario.");
    }

    private String prompt(String message, String defaultValue) {
        return JOptionPane.showInputDialog(this, message, defaultValue);
    }

    private boolean canRead(FileEntry file) {
        if (file == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        String currentUser = getCurrentUser();
        return file.isPublicReadable() || file.getOwner().equalsIgnoreCase(currentUser);
    }
}
