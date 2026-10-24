package com.skillbridge.dto;

import java.util.Map;

/** Response bodies sent to the front end. */
public final class Responses {
    private Responses() {}

    public record HealthDto(String status, int programs, int jobs, int users) {}

    public record ProgramDto(String code, String group, String program, String major, String label,
                             String level, String status) {}

    /** One node of the College -> Program -> Major tree. code is null for nodes that are not selectable. */
    public record TreeDto(String name, String code, TreeDto[] children) {}

    public record UserDto(int id, String fullName, int age, String email, String contact, String address,
                          String gender, String programCode, String programLabel, String group, String gradYear,
                          String targetRole, String[] hardSkills, String[] softSkills, String[] languages) {}

    public record AuthDto(String token, UserDto user) {}

    public record JobDto(String id, String title, String role, String company, String location,
                         String description, String licensure, String link, String source, String dateCollected,
                         String[] programCodes, String[] requiredSkills, boolean saved, boolean applied) {}

    public record MatchDto(JobDto job, double score, double roleScore, double skillScore, double programScore,
                           String[] matchedSkills, String[] missingSkills, int distance) {}

    public record HomeDto(MatchDto[] recommended, MatchDto[] alsoQualify) {}

    public record ApplicationDto(int id, String jobId, String jobTitle, String company, String link,
                                 String note, String appliedAt) {}

    public record ProgramStat(String code, String label, String group, int users, int applications,
                              int jobs, boolean gap) {}

    public record SkillDemandDto(String skill, int jobCount) {}

    public record DataReportDto(int rowsRead, int jobsLoaded, String[] problems) {}

    public record StatsDto(int totalUsers, int totalJobs, int totalApplications, int programsWithJobs,
                           int programsTotal, ProgramStat[] programs, SkillDemandDto[] topSkills,
                           Map<String, Integer> usersByGender, DataReportDto dataReport) {}
}
