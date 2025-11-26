package proyecto2so.core;

/**
 * Centralized configuration knobs for the simulator.
 */
public final class SystemConfig {

    private SystemConfig() {
        // Utility class
    }

    public static final int MAX_BLOCKS = 240; // Disk blocks available
    public static final int BLOCK_SIZE_BYTES = 4096; // Simulated block size
    public static final int MAX_BUFFER_SLOTS = 16; // Buffer entries when enabled
    public static final int MAX_PROCESSES = 64; // Limit for PCB pool

    public static final String ROOT_USER = "admin";
}
