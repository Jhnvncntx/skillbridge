package com.skillbridge.model;

public class Job {
    private final String id, title, company, source;   // source: "seed", "employer", later "jsearch"
    private final Program[] targetPrograms;
    private final String[] requiredSkills;

    public Job(String id, String title, String company, String source,
               Program[] targetPrograms, String... requiredSkills) {
        this.id = id; this.title = title; this.company = company; this.source = source;
        this.targetPrograms = targetPrograms;
        this.requiredSkills = requiredSkills;   // already normalized by the service layer
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getSource() { return source; }
    public Program[] getTargetPrograms() { return targetPrograms; }
    public String[] getRequiredSkills() { return requiredSkills; }

    public boolean targets(Program p) {
        for (Program t : targetPrograms) if (t == p) return true;
        return false;
    }

    @Override public String toString() { return title + " @ " + company; }
}
