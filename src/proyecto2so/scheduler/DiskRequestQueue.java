package proyecto2so.scheduler;

import proyecto2so.core.SystemConfig;

/**
 * Cola fija que mantiene las solicitudes pendientes para el planificador de disco.
 */
public class DiskRequestQueue {

    private final DiskRequest[] buffer = new DiskRequest[SystemConfig.MAX_PROCESSES];
    private int size;

    public boolean enqueue(DiskRequest request) {
        if (size == buffer.length) {
            return false;
        }
        buffer[size++] = request;
        return true;
    }

    public DiskRequest dequeueAt(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        DiskRequest selected = buffer[index];
        for (int i = index; i < size - 1; i++) {
            buffer[i] = buffer[i + 1];
        }
        buffer[--size] = null;
        return selected;
    }

    public DiskRequest get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return buffer[index];
    }

    public int size() {
        return size;
    }

    public void clear() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = null;
        }
        size = 0;
    }
}
