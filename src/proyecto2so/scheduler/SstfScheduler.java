package proyecto2so.scheduler;

/**
 * Shortest Seek Time First selection.
 */
public class SstfScheduler implements DiskScheduler {

    @Override
    public int selectNext(int currentHead, DiskRequestQueue queue) {
        if (queue.size() == 0) {
            return -1;
        }
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < queue.size(); i++) {
            DiskRequest req = queue.get(i);
            int distance = Math.abs(req.getTargetBlock() - currentHead);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    @Override
    public String getName() {
        return "SSTF";
    }
}
