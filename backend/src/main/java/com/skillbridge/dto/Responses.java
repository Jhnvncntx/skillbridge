package com.skillbridge.dto;

import java.util.Map;

/** Response bodies. */
public final class Responses {
    private Responses() {}

    public record ProgramDto(String code, String name, String college) {}

    public record UserDto(int id, String name, String program, String programName, String college,
                          String gender, String[] skills) {}

    public record JobDto(String id, String title, String company, String source,
                         String[] targetPrograms, String[] requiredSkills) {}

    public record MatchDto(String jobId, String title, String company, String source, double score,
                           boolean programMatch, int matchedCount,
                           String[] matchedSkills, String[] missingSkills,
                           String[] requiredSkills, String[] targetPrograms) {}

    public record ApplicationDto(int id, int userId, String jobId, String jobTitle, String company,
                                 String note, String appliedAt) {}

    public record SkillCheckDto(String name, boolean known) {}

    public record HealthDto(String status, int users, int jobs) {}

    public record ProgramStat(String code, String name, String college,
                              int users, int applications, int jobsTargeting) {}

    public record SkillDemandDto(String skill, int jobCount) {}

    public record StatsDto(int totalUsers, int totalJobs, int totalApplications,
                           ProgramStat[] programs, SkillDemandDto[] topSkills,
                           Map<String, Integer> usersByGender) {}
}
