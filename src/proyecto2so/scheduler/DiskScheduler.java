package proyecto2so.scheduler;

/**
 * Strategy interface for disk head scheduling.
 */
public interface DiskScheduler {

    /**
     * @param currentHead current disk head position
     * @param queue queue containing pending requests
     * @return index inside the queue of the next request to serve
     */
    int selectNext(int currentHead, DiskRequestQueue queue);

    String getName();
}
