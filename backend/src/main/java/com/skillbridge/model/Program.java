package com.skillbridge.model;

/** One program or major, loaded from programs.csv. Every major counts as its own category. */
public class Program {
    public final int index;           // position in the load order (used for per-program count arrays)
    public final String code, group, program, major, level, status, note;

    public Program(int index, String code, String group, String program, String major,
                   String level, String status, String note) {
        this.index = index; this.code = code; this.group = group; this.program = program;
        this.major = major; this.level = level; this.status = status; this.note = note;
    }

    public String label() { return major.isEmpty() ? program : program + " (" + major + ")"; }
}
