package proyecto2so.storage;

/**
 * Describe la cadena (lista enlazada) de bloques asignados a un archivo.
 */
public class BlockChain {

    private int headIndex = -1;
    private int tailIndex = -1;
    private int length;

    public void append(int blockIndex) {
        if (headIndex == -1) {
            headIndex = blockIndex;
        } else {
            // Caller must ensure linking on disk blocks; this class just tracks positions
            tailIndex = blockIndex;
        }
        if (tailIndex == -1) {
            tailIndex = blockIndex;
        }
        length++;
    }

    public int getHeadIndex() {
        return headIndex;
    }

    public int getTailIndex() {
        return tailIndex;
    }

    public int getLength() {
        return length;
    }

    public boolean isEmpty() {
        return length == 0;
    }
}
