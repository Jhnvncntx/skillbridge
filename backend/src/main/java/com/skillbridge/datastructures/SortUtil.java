package com.skillbridge.datastructures;

/** Merge sort - O(n log n), stable. */
public class SortUtil {
    public static <T extends Comparable<T>> void mergeSort(T[] arr, boolean descending) {
        if (arr.length < 2) return;
        T[] temp = arr.clone();
        sort(arr, temp, 0, arr.length - 1, descending);
    }

    private static <T extends Comparable<T>> void sort(T[] a, T[] t, int lo, int hi, boolean desc) {
        if (lo >= hi) return;
        int mid = (lo + hi) / 2;
        sort(a, t, lo, mid, desc);
        sort(a, t, mid + 1, hi, desc);
        int i = lo, j = mid + 1, k = lo;
        while (i <= mid && j <= hi) {
            int c = a[i].compareTo(a[j]);
            boolean takeLeft = desc ? c >= 0 : c <= 0;
            t[k++] = takeLeft ? a[i++] : a[j++];
        }
        while (i <= mid) t[k++] = a[i++];
        while (j <= hi)  t[k++] = a[j++];
        for (k = lo; k <= hi; k++) a[k] = t[k];
    }
}
