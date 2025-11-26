/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package proyecto2so;

import javax.swing.SwingUtilities;
import proyecto2so.filesystem.FileSystemService;
import proyecto2so.storage.BlockAllocator;
import proyecto2so.storage.BufferManager;
import proyecto2so.storage.Disk;
import proyecto2so.ui.MainWindow;

/**
 * Punto de entrada: inicializa servicios y lanza la interfaz Swing.
 */
public class Proyecto2SO {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Disk disk = new Disk();
                BlockAllocator allocator = new BlockAllocator(disk);
                FileSystemService fileSystem = new FileSystemService(allocator);
                BufferManager buffer = new BufferManager();
                MainWindow window = new MainWindow(fileSystem, disk, buffer);
                window.setVisible(true);
                window.refreshAll();
            }
        });
    }
}
