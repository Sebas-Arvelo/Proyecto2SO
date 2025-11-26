package proyecto2so.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField; // Added import for JTextField
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
 * Ventana principal de Swing: orquesta la interfaz y los eventos de usuario.
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
        JPanel container = new JPanel(new BorderLayout());
        JPanel selectors = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        selectors.add(new JLabel("Modo:"));
        selectors.add(modeSelector);
        selectors.add(new JLabel("Usuario:"));
        selectors.add(userField);
        selectors.add(new JLabel("Planificador:"));
        selectors.add(schedulerSelector);
        selectors.add(new JLabel("Buffer:"));
        selectors.add(bufferPolicySelector);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton btnDir = new JButton("Crear Directorio");
        JButton btnFile = new JButton("Crear Archivo");
        JButton btnDelete = new JButton("Eliminar");
        JButton btnRead = new JButton("Leer Archivo");
        JButton btnEdit = new JButton("Editar Archivo");
        JButton btnRename = new JButton("Renombrar");
        JButton btnSave = new JButton("Guardar");
        JButton btnLoad = new JButton("Cargar");
        JButton btnDataset = new JButton("Dataset CSV");
        buttons.add(btnDir);
        buttons.add(btnFile);
        buttons.add(btnDelete);
        buttons.add(btnRead);
        buttons.add(btnEdit);
        buttons.add(btnRename);
        buttons.add(btnSave);
        buttons.add(btnLoad);
        buttons.add(btnDataset);
        buttons.add(diskStatsLabel);
        buttons.add(bufferStatsLabel);

        container.add(selectors, BorderLayout.NORTH);
        container.add(buttons, BorderLayout.SOUTH);

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

        btnEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleEditFile();
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

        btnDataset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLoadDataset();
            }
        });

        return container;
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
        String previousSelection = getSelectedPath();
        DefaultTreeModel model = buildTreeModel();
        tree.setModel(model);
        expandAll(new TreePath(model.getRoot()));
        selectPath(previousSelection);
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
        JTextField parentField = new JTextField(defaultParentPath(), 20);
        JTextField nameField = new JTextField("nuevo_dir", 20);
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(new JLabel("Ruta del directorio padre"));
        panel.add(parentField);
        panel.add(new JLabel("Nombre del nuevo directorio"));
        panel.add(nameField);
        int result = JOptionPane.showConfirmDialog(this, panel, "Crear directorio",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String parent = sanitizeParent(parentField.getText());
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un nombre");
            return;
        }
        submitProcess(OperationType.MKDIR, joinPath(parent, name), 0, "");
    }

    private void handleCreateFile() {
        if (!isAdmin()) {
            showRestricted();
            return;
        }
        JTextField parentField = new JTextField(defaultParentPath(), 20);
        JTextField nameField = new JTextField("archivo.json", 20);
        JTextField blocksField = new JTextField("1", 5);
        JCheckBox publicCheck = new JCheckBox("Lectura publica", true);
        JTextArea contentArea = new JTextArea("{\n  \"campo\": \"valor\"\n}", 5, 20);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 4));
        fields.add(new JLabel("Ruta del directorio contenedor"));
        fields.add(parentField);
        fields.add(new JLabel("Nombre del archivo"));
        fields.add(nameField);
        fields.add(new JLabel("Tamano en bloques"));
        fields.add(blocksField);
        fields.add(publicCheck);
        panel.add(fields, BorderLayout.NORTH);
        panel.add(new JScrollPane(contentArea), BorderLayout.CENTER);
        int result = JOptionPane.showConfirmDialog(this, panel, "Crear archivo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        int blocks;
        try {
            blocks = Integer.parseInt(blocksField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Tamano invalido");
            return;
        }
        String parent = sanitizeParent(parentField.getText());
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un nombre");
            return;
        }
        String payload = (publicCheck.isSelected() ? "PUBLIC:" : "PRIVATE:") + contentArea.getText();
        submitProcess(OperationType.CREATE, joinPath(parent, name), Math.max(1, blocks), payload);
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

    private void handleEditFile() {
        String path = prompt("Ruta del archivo a editar", "/archivo.txt");
        if (path == null) {
            return;
        }
        FileEntry file = fileSystem.getFile(path);
        if (file == null) {
            JOptionPane.showMessageDialog(this, "El archivo no existe");
            return;
        }
        if (!canEdit(file)) {
            showRestricted();
            return;
        }
        JTextArea area = new JTextArea(fileSystem.readFileData(file), 10, 40);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JCheckBox publicCheck = new JCheckBox("Permitir lectura publica", file.isPublicReadable());
        JPanel meta = new JPanel(new GridLayout(0, 1, 0, 2));
        meta.add(new JLabel("Propietario: " + file.getOwner()));
        meta.add(new JLabel("PID creador: " + file.getCreatedByPid()));
        meta.add(new JLabel("Color FAT: " + file.getColorHex()));
        meta.add(new JLabel("Bloques asignados: " + file.getBlockCount()));
        JPanel editor = new JPanel(new BorderLayout(0, 8));
        editor.add(meta, BorderLayout.NORTH);
        editor.add(new JScrollPane(area), BorderLayout.CENTER);
        editor.add(publicCheck, BorderLayout.SOUTH);
        int result = JOptionPane.showConfirmDialog(this, editor, "Editar archivo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        boolean ok = fileSystem.updateFileContent(path, area.getText(), publicCheck.isSelected());
        JOptionPane.showMessageDialog(this, ok ? "Archivo actualizado" : "No se pudo actualizar el archivo");
        if (ok) {
            refreshAll();
        }
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

    private void handleLoadDataset() {
        File dataset = new File(System.getProperty("user.dir"), "datasets/demo.csv");
        if (!dataset.exists()) {
            JOptionPane.showMessageDialog(this, "No se encontro datasets/demo.csv");
            return;
        }
        int[] result = loadDatasetFromCsv(dataset);
        StringBuilder message = new StringBuilder();
        message.append("Directorios creados: ").append(result[0])
                .append("\nArchivos creados: ").append(result[1]);
        if (result[2] > 0) {
            message.append("\nLineas con error: ").append(result[2]);
        }
        JOptionPane.showMessageDialog(this, message.toString());
        refreshAll();
    }

    private int[] loadDatasetFromCsv(File file) {
        int createdDirs = 0;
        int createdFiles = 0;
        int failures = 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            boolean headerConsumed = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (!headerConsumed) {
                    headerConsumed = true;
                    continue;
                }
                String[] columns = line.split(",", 6);
                if (columns.length < 3) {
                    failures++;
                    continue;
                }
                String type = columns[0].trim().toUpperCase();
                String parent = columns[1].trim();
                String name = columns[2].trim();
                if (type.equals("DIR")) {
                    if (fileSystem.createDirectory(parent, name) != null) {
                        createdDirs++;
                    } else {
                        failures++;
                    }
                } else if (type.equals("FILE")) {
                    int blocks = 1;
                    boolean publicReadable = true;
                    String content = "";
                    if (columns.length > 3 && !columns[3].trim().isEmpty()) {
                        try {
                            blocks = Integer.parseInt(columns[3].trim());
                        } catch (NumberFormatException ex) {
                            blocks = 1;
                        }
                    }
                    if (columns.length > 4 && !columns[4].trim().isEmpty()) {
                        publicReadable = Boolean.parseBoolean(columns[4].trim());
                    }
                    if (columns.length > 5) {
                        content = columns[5];
                    }
                    if (fileSystem.createFile(parent, name, Math.max(1, blocks), SystemConfig.ROOT_USER,
                            publicReadable, content, 0) != null) {
                        createdFiles++;
                    } else {
                        failures++;
                    }
                } else {
                    failures++;
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error leyendo CSV: " + ex.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
        return new int[]{createdDirs, createdFiles, failures};
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
        return fileSystem.createFile(parent, name, Math.max(1, pcb.getRequestedBlocks()), pcb.getOwner(), isPublic, content, pcb.getPid()) != null;
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

    private String sanitizeParent(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "/";
        }
        String cleaned = raw.trim().replace('\\', '/');
        if (!cleaned.startsWith("/")) {
            cleaned = "/" + cleaned;
        }
        if (cleaned.length() > 1 && cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.isEmpty() ? "/" : cleaned;
    }

    private String defaultParentPath() {
        String selected = getSelectedPath();
        return selected == null ? "/" : selected;
    }

    private String getSelectedPath() {
        TreePath selection = tree.getSelectionPath();
        if (selection == null) {
            return "/";
        }
        Object[] nodes = selection.getPath();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < nodes.length; i++) {
            String segment = nodes[i].toString();
            if ("/".equals(segment)) {
                if (builder.length() == 0) {
                    builder.append('/');
                }
            } else {
                if (builder.length() > 1) {
                    builder.append('/');
                } else if (builder.length() == 0) {
                    builder.append('/');
                }
                builder.append(segment);
            }
        }
        return builder.length() == 0 ? "/" : builder.toString();
    }

    private void expandAll(TreePath parent) {
        if (parent == null) {
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        for (int i = 0; i < node.getChildCount(); i++) {
            expandAll(parent.pathByAddingChild(node.getChildAt(i)));
        }
        tree.expandPath(parent);
    }

    private void selectPath(String logicalPath) {
        if (logicalPath == null || logicalPath.isEmpty()) {
            tree.setSelectionRow(0);
            return;
        }
        String normalized = sanitizeParent(logicalPath);
        if ("/".equals(normalized)) {
            tree.setSelectionRow(0);
            return;
        }
        String[] parts = normalized.substring(1).split("/");
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getModel().getRoot();
        TreePath path = new TreePath(node);
        for (int i = 0; i < parts.length; i++) {
            node = findChild(node, parts[i]);
            if (node == null) {
                break;
            }
            path = path.pathByAddingChild(node);
        }
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    private DefaultMutableTreeNode findChild(DefaultMutableTreeNode parent, String name) {
        if (parent == null) {
            return null;
        }
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (name.equalsIgnoreCase(child.getUserObject().toString())) {
                return child;
            }
        }
        return null;
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

    private boolean canEdit(FileEntry file) {
        if (file == null) {
            return false;
        }
        return isAdmin() || file.getOwner().equalsIgnoreCase(getCurrentUser());
    }
}
