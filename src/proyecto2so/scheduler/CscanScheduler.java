package proyecto2so.scheduler;

import proyecto2so.core.SystemConfig;

/**
 * Circular SCAN always moves upward and wraps around to block 0.
 */
public class CscanScheduler implements DiskScheduler {

    @Override
    public int selectNext(int currentHead, DiskRequestQueue queue) {
        if (queue.size() == 0) {
            return -1;
        }
        int bestIndex = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < queue.size(); i++) {
            DiskRequest req = queue.get(i);
            int target = req.getTargetBlock();
            int distance;
            if (target >= currentHead) {
                distance = target - currentHead;
            } else {
                distance = (SystemConfig.MAX_BLOCKS - currentHead) + target;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    @Override
    public String getName() {
        return "C-SCAN";
    }
}
