package proyecto2so.scheduler;

/**
 * Política FIFO: atiende siempre la solicitud más antigua.
 */
public class FifoScheduler implements DiskScheduler {

    @Override
    public int selectNext(int currentHead, DiskRequestQueue queue) {
        if (queue.size() == 0) {
            return -1;
        }
        return 0;
    }

    @Override
    public String getName() {
        return "FIFO";
    }
}
