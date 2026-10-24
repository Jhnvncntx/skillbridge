package com.skillbridge.data;

import com.skillbridge.datastructures.MyLinkedList;

/** Minimal CSV reader: handles quoted fields, commas and line breaks inside quotes, and "" escapes. */
public final class CsvParser {
    private CsvParser() {}

    public static MyLinkedList<String[]> parse(String text) {
        MyLinkedList<String[]> rows = new MyLinkedList<>();
        if (text == null) return rows;
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') text = text.substring(1);   // strip BOM

        MyLinkedList<String> row = new MyLinkedList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false, fieldStarted = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else inQuotes = false;
                } else field.append(c);
            } else if (c == '"') { inQuotes = true; fieldStarted = true; }
            else if (c == ',') { row.add(field.toString()); field.setLength(0); fieldStarted = true; }
            else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                if (fieldStarted || field.length() > 0) { row.add(field.toString()); addRow(rows, row); }
                row = new MyLinkedList<>(); field.setLength(0); fieldStarted = false;
            } else { field.append(c); fieldStarted = true; }
        }
        if (fieldStarted || field.length() > 0) { row.add(field.toString()); addRow(rows, row); }
        return rows;
    }

    private static void addRow(MyLinkedList<String[]> rows, MyLinkedList<String> row) {
        String[] arr = new String[row.size()];
        int i = 0;
        for (String s : row) arr[i++] = s;
        rows.add(arr);
    }
}
