package campusnav.dsa;

/** A small linked-list FIFO queue used by breadth-first routing and report processing. */
public class SimpleQueue<T> {
    private static final class Node<E> {
        private final E value;
        private Node<E> next;

        private Node(E value) {
            this.value = value;
        }
    }

    private Node<T> front;
    private Node<T> rear;
    private int size;

    public void enqueue(T value) {
        Node<T> node = new Node<T>(value);
        if (rear == null) {
            front = node;
            rear = node;
        } else {
            rear.next = node;
            rear = node;
        }
        size++;
    }

    /** Returns null when the queue is empty. */
    public T dequeue() {
        if (front == null) {
            return null;
        }
        T value = front.value;
        front = front.next;
        if (front == null) {
            rear = null;
        }
        size--;
        return value;
    }

    /** Returns null when the queue is empty. */
    public T peek() {
        return front == null ? null : front.value;
    }

    public boolean isEmpty() {
        return front == null;
    }

    public int size() {
        return size;
    }
}
