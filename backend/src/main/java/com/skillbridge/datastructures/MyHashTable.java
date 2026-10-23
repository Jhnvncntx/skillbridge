package com.skillbridge.datastructures;

/** Hash table using separate chaining. Each bucket is a MyLinkedList of entries. */
public class MyHashTable<K, V> {
    private static class Entry<K, V> {
        final K key;
        V value;
        Entry(K key, V value) { this.key = key; this.value = value; }
    }

    private MyLinkedList<Entry<K, V>>[] buckets;
    private int size;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public MyHashTable() { buckets = new MyLinkedList[17]; }

    private int indexFor(K key, int capacity) {
        return (key.hashCode() & 0x7fffffff) % capacity;
    }

    public void put(K key, V value) {
        if (size >= buckets.length * 0.75) resize();
        int i = indexFor(key, buckets.length);
        if (buckets[i] == null) buckets[i] = new MyLinkedList<>();
        for (Entry<K, V> e : buckets[i]) {
            if (e.key.equals(key)) { e.value = value; return; }   // update existing
        }
        buckets[i].add(new Entry<>(key, value));
        size++;
    }

    public V get(K key) {
        int i = indexFor(key, buckets.length);
        if (buckets[i] == null) return null;
        for (Entry<K, V> e : buckets[i]) if (e.key.equals(key)) return e.value;
        return null;
    }

    public boolean containsKey(K key) { return get(key) != null; }

    public MyLinkedList<K> keys() {
        MyLinkedList<K> result = new MyLinkedList<>();
        for (MyLinkedList<Entry<K, V>> bucket : buckets) {
            if (bucket == null) continue;
            for (Entry<K, V> e : bucket) result.add(e.key);
        }
        return result;
    }

    public int size() { return size; }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void resize() {
        MyLinkedList<Entry<K, V>>[] old = buckets;
        buckets = new MyLinkedList[old.length * 2 + 1];
        for (MyLinkedList<Entry<K, V>> bucket : old) {
            if (bucket == null) continue;
            for (Entry<K, V> e : bucket) {
                int i = indexFor(e.key, buckets.length);
                if (buckets[i] == null) buckets[i] = new MyLinkedList<>();
                buckets[i].add(e);
            }
        }
    }
}
