package proyecto2so.process;

import proyecto2so.core.SystemConfig;

/**
 * Fixed-size buffer without using Java collections.
 */
public class ProcessQueue {

    private final ProcessControlBlock[] buffer = new ProcessControlBlock[SystemConfig.MAX_PROCESSES];
    private int size;

    public boolean enqueue(ProcessControlBlock pcb) {
        if (size == buffer.length) {
            return false;
        }
        buffer[size++] = pcb;
        return true;
    }

    public ProcessControlBlock dequeue() {
        return removeAt(0);
    }

    public boolean removeByPid(int pid) {
        for (int i = 0; i < size; i++) {
            if (buffer[i] != null && buffer[i].getPid() == pid) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    private ProcessControlBlock removeAt(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        ProcessControlBlock removed = buffer[index];
        for (int i = index; i < size - 1; i++) {
            buffer[i] = buffer[i + 1];
        }
        buffer[--size] = null;
        return removed;
    }

    public int size() {
        return size;
    }

    public ProcessControlBlock get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return buffer[index];
    }

    public void clear() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = null;
        }
        size = 0;
    }

    public ProcessControlBlock findByPid(int pid) {
        for (int i = 0; i < size; i++) {
            ProcessControlBlock pcb = buffer[i];
            if (pcb != null && pcb.getPid() == pid) {
                return pcb;
            }
        }
        return null;
    }
}
