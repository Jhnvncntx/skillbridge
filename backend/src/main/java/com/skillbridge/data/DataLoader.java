package com.skillbridge.data;

import com.skillbridge.datastructures.MyHashTable;
import com.skillbridge.datastructures.MyLinkedList;
import com.skillbridge.model.Job;
import com.skillbridge.model.Program;
import com.skillbridge.util.JobKey;
import com.skillbridge.util.Text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Loads programs.csv, program_keywords.csv and jobs.csv.
 * Invalid job rows are skipped and explained in a LoadReport (shown on the admin page),
 * so whoever collects the data can see exactly what to fix.
 */
public final class DataLoader {
    private DataLoader() {}

    public static class Keywords {
        public final String[] roles, skills;
        Keywords(String[] roles, String[] skills) { this.roles = roles; this.skills = skills; }
    }

    public static class LoadReport {
        public int rowsRead, jobsLoaded;
        public final MyLinkedList<String> problems = new MyLinkedList<>();
    }

    /** Reads a file from the classpath (src/main/resources). */
    public static String readResource(String path) throws IOException {
        try (InputStream in = DataLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IOException("Missing resource: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Optional override: if the environment variable points to a file on disk, read that instead. */
    public static String readJobsText() throws IOException {
        String override = System.getenv("JOBS_CSV_PATH");
        if (override != null && !override.isBlank())
            return new String(Files.readAllBytes(Paths.get(override)), StandardCharsets.UTF_8);
        return readResource("data/jobs.csv");
    }

    private static String col(String[] row, int i) { return i < row.length ? Text.clean(row[i]) : ""; }

    // ---------------------------------------------------------------- programs
    public static MyLinkedList<Program> loadPrograms(String csv) {
        MyLinkedList<Program> programs = new MyLinkedList<>();
        MyHashTable<String, Boolean> seen = new MyHashTable<>();
        boolean header = true;
        for (String[] r : CsvParser.parse(csv)) {
            if (header) { header = false; continue; }
            String code = col(r, 0).toUpperCase();
            if (code.isEmpty() || seen.get(code) != null) continue;
            seen.put(code, true);
            programs.add(new Program(programs.size(), code, col(r, 1), col(r, 2), col(r, 3),
                    col(r, 4), col(r, 5), col(r, 6)));
        }
        return programs;
    }

    // ---------------------------------------------------------------- keywords (roles + skills per program)
    public static MyHashTable<String, Keywords> loadKeywords(String csv) {
        MyHashTable<String, Keywords> map = new MyHashTable<>();
        boolean header = true;
        for (String[] r : CsvParser.parse(csv)) {
            if (header) { header = false; continue; }
            String code = col(r, 0).toUpperCase();
            if (!code.isEmpty()) map.put(code, new Keywords(Text.splitListKeepCase(col(r, 1), ";"), Text.splitList(col(r, 2), ";")));
        }
        return map;
    }

    // ---------------------------------------------------------------- jobs
    /**
     * Columns (same order as the Excel template):
     * 0 program code | 1 other program codes | 2 role | 3 title | 4 company | 5 location | 6 description |
     * 7 required skills | 8 licensure | 9 link | 10 source | 11 date collected | 12 status
     */
    public static MyLinkedList<Job> loadJobs(String csv, MyHashTable<String, Program> programByCode, LoadReport report) {
        MyLinkedList<Job> jobs = new MyLinkedList<>();
        MyHashTable<String, Boolean> seenLinks = new MyHashTable<>();
        int rowNumber = 0;
        boolean header = true;

        for (String[] r : CsvParser.parse(csv)) {
            rowNumber++;
            if (header) { header = false; continue; }

            boolean blank = true;
            for (int i = 0; i < 12; i++) if (!col(r, i).isEmpty()) blank = false;
            if (blank) continue;
            String title = col(r, 3);
            if (title.toLowerCase().startsWith("example") || col(r, 12).toLowerCase().startsWith("example")) continue;

            report.rowsRead++;
            String label = "Row " + rowNumber + (title.isEmpty() ? "" : " (" + title + ")");

            String primary = col(r, 0).toUpperCase();
            String role = col(r, 2), company = col(r, 4), location = col(r, 5), description = col(r, 6);
            String link = col(r, 9), source = col(r, 10), date = col(r, 11);
            String[] skills = Text.splitList(col(r, 7), "[;,\\n]");

            String missing = "";
            if (primary.isEmpty()) missing += " program code,";
            if (role.isEmpty()) missing += " job role,";
            if (title.isEmpty()) missing += " job title,";
            if (company.isEmpty()) missing += " company,";
            if (location.isEmpty()) missing += " location,";
            if (description.isEmpty()) missing += " description,";
            if (skills.length == 0) missing += " required skills,";
            if (link.isEmpty()) missing += " link,";
            if (source.isEmpty()) missing += " source,";
            if (date.isEmpty()) missing += " date collected,";
            if (!missing.isEmpty()) { report.problems.add(label + ": missing" + missing.substring(0, missing.length() - 1)); continue; }

            if (!link.toLowerCase().startsWith("http")) { report.problems.add(label + ": link must start with http"); continue; }
            String linkKey = JobKey.of(link);          // stable id, ignores tracking parameters
            if (seenLinks.get(linkKey) != null) { report.problems.add(label + ": duplicate link"); continue; }

            MyLinkedList<String> codes = new MyLinkedList<>();
            String badCode = null;
            if (programByCode.get(primary) == null) badCode = primary; else codes.add(primary);
            for (String other : col(r, 1).toUpperCase().split("[;,]")) {
                String c = other.trim();
                if (c.isEmpty() || codes.contains(c)) continue;
                if (programByCode.get(c) == null) { badCode = c; break; }
                codes.add(c);
            }
            if (badCode != null) { report.problems.add(label + ": unknown program code " + badCode); continue; }

            seenLinks.put(linkKey, true);
            jobs.add(new Job(linkKey, title, role, company, location, description,
                    col(r, 8), link, source, date, Text.toArray(codes), skills));
        }
        report.jobsLoaded = jobs.size();
        return jobs;
    }
}
