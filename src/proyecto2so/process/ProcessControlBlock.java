package proyecto2so.process;

/**
 * Minimal representation of a simulated process requesting I/O.
 */
public class ProcessControlBlock {

    private final int pid;
    private final OperationType operation;
    private final String targetPath;
    private final String owner;
    private ProcessState state = ProcessState.NEW;
    private int requestedBlocks;
    private String payload = "";

    public ProcessControlBlock(int pid, OperationType operation, String targetPath, String owner) {
        this.pid = pid;
        this.operation = operation;
        this.targetPath = targetPath;
        this.owner = owner;
    }

    public int getPid() {
        return pid;
    }

    public OperationType getOperation() {
        return operation;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getOwner() {
        return owner;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public int getRequestedBlocks() {
        return requestedBlocks;
    }

    public void setRequestedBlocks(int requestedBlocks) {
        this.requestedBlocks = requestedBlocks;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
