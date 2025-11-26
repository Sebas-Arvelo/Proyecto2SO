package proyecto2so.scheduler;

/**
 * FIFO simply serves the oldest request.
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
