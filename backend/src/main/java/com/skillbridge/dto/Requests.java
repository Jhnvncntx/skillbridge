package com.skillbridge.dto;

/** Request bodies sent by the front end. */
public final class Requests {
    private Requests() {}

    public record SignupRequest(String fullName, Integer age, String email, String contact, String address,
                                String gender, String password, String programCode, String gradYear,
                                String targetRole, String[] hardSkills, String[] softSkills, String[] languages) {}

    public record LoginRequest(String email, String password) {}

    /** Everything a user can edit on the profile page (email and password are not editable here). */
    public record ProfileRequest(String fullName, Integer age, String contact, String address, String gender,
                                 String programCode, String gradYear, String targetRole,
                                 String[] hardSkills, String[] softSkills, String[] languages) {}

    public record ApplyRequest(String jobId, String note) {}
}
