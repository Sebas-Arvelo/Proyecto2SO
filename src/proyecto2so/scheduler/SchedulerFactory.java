package proyecto2so.scheduler;

/**
 * Simple factory to instantiate supported policies.
 */
public final class SchedulerFactory {

    private SchedulerFactory() {
    }

    public static DiskScheduler create(String name) {
        if ("SSTF".equalsIgnoreCase(name)) {
            return new SstfScheduler();
        }
        if ("SCAN".equalsIgnoreCase(name)) {
            return new ScanScheduler();
        }
        if ("C-SCAN".equalsIgnoreCase(name) || "CSCAN".equalsIgnoreCase(name)) {
            return new CscanScheduler();
        }
        return new FifoScheduler();
    }
}
