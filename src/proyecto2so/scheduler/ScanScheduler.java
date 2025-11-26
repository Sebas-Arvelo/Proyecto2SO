package proyecto2so.scheduler;

import proyecto2so.core.SystemConfig;

/**
 * SCAN (Elevator) sweeps from one end to the other.
 */
public class ScanScheduler implements DiskScheduler {

    private boolean movingUp = true;

    @Override
    public int selectNext(int currentHead, DiskRequestQueue queue) {
        if (queue.size() == 0) {
            return -1;
        }
        int candidate = findInDirection(currentHead, queue, movingUp);
        if (candidate == -1) {
            movingUp = !movingUp;
            candidate = findInDirection(currentHead, queue, movingUp);
        }
        if (candidate == -1) {
            candidate = 0;
        }
        return candidate;
    }

    private int findInDirection(int currentHead, DiskRequestQueue queue, boolean upwards) {
        int bestIndex = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < queue.size(); i++) {
            DiskRequest req = queue.get(i);
            int target = req.getTargetBlock();
            int delta = target - currentHead;
            if (upwards && delta >= 0) {
                if (delta < bestDistance) {
                    bestDistance = delta;
                    bestIndex = i;
                }
            } else if (!upwards && delta <= 0) {
                int abs = Math.abs(delta);
                if (abs < bestDistance) {
                    bestDistance = abs;
                    bestIndex = i;
                }
            }
        }
        return bestIndex;
    }

    @Override
    public String getName() {
        return "SCAN";
    }
}
