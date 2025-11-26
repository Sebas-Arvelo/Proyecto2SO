package proyecto2so.scheduler;

import proyecto2so.process.ProcessControlBlock;

/**
 * Wraps a PCB with the target block index to serve.
 */
public class DiskRequest {

    private final ProcessControlBlock pcb;
    private final int targetBlock;

    public DiskRequest(ProcessControlBlock pcb, int targetBlock) {
        this.pcb = pcb;
        this.targetBlock = targetBlock;
    }

    public ProcessControlBlock getPcb() {
        return pcb;
    }

    public int getTargetBlock() {
        return targetBlock;
    }
}
