package com.skillbridge.engine;

/** How many job postings require a given skill. Sorted with merge sort (highest demand first). */
public class SkillDemand implements Comparable<SkillDemand> {
    public final String skill;
    public final int jobCount;

    public SkillDemand(String skill, int jobCount) { this.skill = skill; this.jobCount = jobCount; }

    @Override
    public int compareTo(SkillDemand o) {
        int c = Integer.compare(this.jobCount, o.jobCount);
        return c != 0 ? c : o.skill.compareTo(this.skill);   // ties: alphabetical when sorted descending
    }
}
