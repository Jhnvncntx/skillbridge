package com.skillbridge.datastructures;

/** LIFO stack backed by MyLinkedList (top = front of list). */
public class MyStack<T> {
    private final MyLinkedList<T> list = new MyLinkedList<>();

    public void push(T item) { list.addFirst(item); }
    public T pop()           { return list.removeFirst(); }
    public T peek()          { return list.peekFirst(); }
    public boolean isEmpty() { return list.isEmpty(); }
    public int size()        { return list.size(); }
}
