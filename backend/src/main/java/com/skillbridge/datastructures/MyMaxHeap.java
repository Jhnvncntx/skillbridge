package com.skillbridge.datastructures;

/** Array-based binary max-heap (priority queue). poll() returns the LARGEST element. */
public class MyMaxHeap<T extends Comparable<T>> {
    private Object[] data = new Object[16];
    private int size;

    public void add(T item) {
        if (size == data.length) {
            Object[] bigger = new Object[size * 2];
            for (int i = 0; i < size; i++) bigger[i] = data[i];
            data = bigger;
        }
        data[size] = item;
        siftUp(size++);
    }

    @SuppressWarnings("unchecked")
    public T poll() {
        if (size == 0) return null;
        T top = (T) data[0];
        data[0] = data[--size];
        data[size] = null;
        if (size > 0) siftDown(0);
        return top;
    }

    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }

    @SuppressWarnings("unchecked")
    private int cmp(int a, int b) { return ((T) data[a]).compareTo((T) data[b]); }

    private void swap(int a, int b) { Object t = data[a]; data[a] = data[b]; data[b] = t; }

    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (cmp(i, parent) <= 0) break;
            swap(i, parent);
            i = parent;
        }
    }

    private void siftDown(int i) {
        while (true) {
            int l = 2 * i + 1, r = l + 1, largest = i;
            if (l < size && cmp(l, largest) > 0) largest = l;
            if (r < size && cmp(r, largest) > 0) largest = r;
            if (largest == i) break;
            swap(i, largest);
            i = largest;
        }
    }
}
