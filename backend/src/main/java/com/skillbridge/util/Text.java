package com.skillbridge.util;

import com.skillbridge.datastructures.MyLinkedList;

/** Small text helpers shared by the loaders and services. */
public final class Text {
    private Text() {}

    /** Trim and collapse repeated spaces. Never returns null. */
    public static String clean(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", " "); }

    /** Cleaned and lower-case: used for comparing skills, roles and codes. */
    public static String norm(String s) { return clean(s).toLowerCase(); }

    /** Split "a; b; c" into normalized, de-duplicated items (empty items dropped). */
    public static String[] splitList(String s, String separatorRegex) {
        MyLinkedList<String> list = new MyLinkedList<>();
        if (s != null) for (String part : s.split(separatorRegex)) {
            String n = norm(part);
            if (!n.isEmpty() && !list.contains(n)) list.add(n);
        }
        return toArray(list);
    }

    /** Like splitList, but keeps the original capitalization (used for role names shown to the user). Duplicates are ignored regardless of case. */
    public static String[] splitListKeepCase(String s, String separatorRegex) {
        MyLinkedList<String> list = new MyLinkedList<>();
        if (s != null) for (String part : s.split(separatorRegex)) {
            String c = clean(part);
            boolean duplicate = false;
            for (String x : list) if (x.equalsIgnoreCase(c)) duplicate = true;
            if (!c.isEmpty() && !duplicate) list.add(c);
        }
        return toArray(list);
    }

    public static String[] toArray(MyLinkedList<String> list) {
        String[] out = new String[list.size()];
        int i = 0;
        for (String s : list) out[i++] = s;
        return out;
    }

    /** Lower-case word pieces used by keyword search ("c++" and "c#" stay intact). */
    public static String[] tokens(String s) {
        MyLinkedList<String> list = new MyLinkedList<>();
        for (String t : norm(s).split("[^a-z0-9+#]+")) if (!t.isEmpty() && !list.contains(t)) list.add(t);
        return toArray(list);
    }
}
