package com.skillbridge.datastructures;

public class SearchUtil {
    /** Binary search on an ASCENDING sorted String array. Returns index or -1. */
    public static int binarySearch(String[] sorted, String target) {
        int lo = 0, hi = sorted.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            int c = sorted[mid].compareTo(target);
            if (c == 0) return mid;
            if (c < 0) lo = mid + 1; else hi = mid - 1;
        }
        return -1;
    }
}
