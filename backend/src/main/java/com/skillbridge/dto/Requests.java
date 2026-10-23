package com.skillbridge.dto;

/** Request bodies. */
public final class Requests {
    private Requests() {}
    public record RegisterRequest(String name, String program, String gender) {}
    public record SkillsRequest(String[] skills) {}
    public record JobRequest(String title, String company, String[] targetPrograms, String[] requiredSkills) {}
    public record ApplyRequest(Integer userId, String jobId, String note) {}
}
