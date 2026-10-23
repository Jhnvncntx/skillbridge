package com.skillbridge.engine;

import com.skillbridge.model.Job;

public class MatchResult implements Comparable<MatchResult> {
    public final Job job;
    public final double score;
    public final int matchedSkills;

    public MatchResult(Job job, double score, int matchedSkills) {
        this.job = job; this.score = score; this.matchedSkills = matchedSkills;
    }

    @Override
    public int compareTo(MatchResult o) {
        int c = Double.compare(this.score, o.score);
        return c != 0 ? c : o.job.getId().compareTo(this.job.getId()); // deterministic tie-break
    }
}
