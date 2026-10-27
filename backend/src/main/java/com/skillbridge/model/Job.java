package com.skillbridge.model;

/** A real job posting loaded from jobs.csv. */
public class Job {
    public final String id, title, role, company, location, description, licensure, link, source, dateCollected;
    public final String[] programCodes;     // every program this posting fits (primary + others)
    public final String[] requiredSkills;   // normalized (lower-case)

    public Job(String id, String title, String role, String company, String location, String description,
               String licensure, String link, String source, String dateCollected,
               String[] programCodes, String[] requiredSkills) {
        this.id = id; this.title = title; this.role = role; this.company = company; this.location = location;
        this.description = description; this.licensure = licensure; this.link = link; this.source = source;
        this.dateCollected = dateCollected; this.programCodes = programCodes; this.requiredSkills = requiredSkills;
    }

    /** True if the job targets any of the given program codes (a student's program plus its base program). */
    public boolean targetsAny(String[] codes) {
        for (String c : codes) if (targets(c)) return true;
        return false;
    }

    public boolean targets(String programCode) {
        for (String c : programCodes) if (c.equals(programCode)) return true;
        return false;
    }
}
