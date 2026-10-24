package com.skillbridge.datastructures;

/** FIFO queue backed by MyLinkedList: enqueue at the tail, dequeue from the head. Both O(1). */
public class MyQueue<T> {
    private final MyLinkedList<T> list = new MyLinkedList<>();

    public void enqueue(T item) { list.add(item); }
    public T dequeue()          { return list.removeFirst(); }
    public T peek()             { return list.peekFirst(); }
    public boolean isEmpty()    { return list.isEmpty(); }
    public int size()           { return list.size(); }
}
