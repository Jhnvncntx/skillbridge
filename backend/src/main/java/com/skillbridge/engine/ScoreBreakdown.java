package com.skillbridge.engine;

/** How a job's score was made up, so the screen can explain it to the user. */
public class ScoreBreakdown {
    public final double role, skills, program, total;
    public final String[] matched, missing;     // required skills the user has / still lacks

    public ScoreBreakdown(double role, double skills, double program, String[] matched, String[] missing) {
        this.role = role; this.skills = skills; this.program = program;
        this.total = role + skills + program;
        this.matched = matched; this.missing = missing;
    }
}
