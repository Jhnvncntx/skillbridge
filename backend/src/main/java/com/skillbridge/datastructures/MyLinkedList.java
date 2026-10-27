package com.skillbridge.datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Generic singly linked list with head and tail pointers. */
public class MyLinkedList<T> implements Iterable<T> {
    private class Node {
        T data;
        Node next;
        Node(T data) { this.data = data; }
    }

    private Node head, tail;
    private int size;

    public void add(T item) {                 // add at end - O(1)
        Node n = new Node(item);
        if (head == null) head = tail = n;
        else { tail.next = n; tail = n; }
        size++;
    }

    public void addFirst(T item) {            // add at front - O(1)
        Node n = new Node(item);
        n.next = head;
        head = n;
        if (tail == null) tail = n;
        size++;
    }

    public T removeFirst() {                  // O(1)
        if (head == null) throw new NoSuchElementException("List is empty");
        T data = head.data;
        head = head.next;
        if (head == null) tail = null;
        size--;
        return data;
    }

    public T peekFirst() {
        if (head == null) throw new NoSuchElementException("List is empty");
        return head.data;
    }

    public T get(int index) {                 // O(n)
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("Index: " + index);
        Node cur = head;
        for (int i = 0; i < index; i++) cur = cur.next;
        return cur.data;
    }

    public boolean remove(T item) {           // O(n), uses equals()
        Node prev = null, cur = head;
        while (cur != null) {
            if (cur.data.equals(item)) {
                if (prev == null) head = cur.next; else prev.next = cur.next;
                if (cur == tail) tail = prev;
                size--;
                return true;
            }
            prev = cur;
            cur = cur.next;
        }
        return false;
    }

    public boolean contains(T item) {
        for (T t : this) if (t.equals(item)) return true;
        return false;
    }

    /** Removes every element. */
    public void clear() { head = tail = null; size = 0; }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node cur = head;
            public boolean hasNext() { return cur != null; }
            public T next() { T d = cur.data; cur = cur.next; return d; }
        };
    }
}
