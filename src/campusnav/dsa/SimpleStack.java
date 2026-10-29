package campusnav.dsa;

import java.util.ArrayList;

/** A small linked-list stack used for route reconstruction, DFS and route history. */
public class SimpleStack<T> {
    private static final class Node<E> {
        private final E value;
        private Node<E> next;

        private Node(E value, Node<E> next) {
            this.value = value;
            this.next = next;
        }
    }

    private Node<T> top;
    private int size;

    public void push(T value) {
        top = new Node<T>(value, top);
        size++;
    }

    /** Returns null when the stack is empty. */
    public T pop() {
        if (top == null) {
            return null;
        }
        T value = top.value;
        top = top.next;
        size--;
        return value;
    }

    /** Returns null when the stack is empty. */
    public T peek() {
        return top == null ? null : top.value;
    }

    public boolean isEmpty() {
        return top == null;
    }

    public int size() {
        return size;
    }

    public void clear() {
        top = null;
        size = 0;
    }

    /** Items from newest to oldest. */
    public ArrayList<T> toList() {
        ArrayList<T> result = new ArrayList<T>();
        for (Node<T> node = top; node != null; node = node.next) {
            result.add(node.value);
        }
        return result;
    }
}
