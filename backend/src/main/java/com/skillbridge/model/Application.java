package com.skillbridge.model;

public class Application {
    public final int id;
    public final int userId;
    public final String jobId;
    public final String note;
    public final String appliedAt;   // ISO-8601 timestamp

    public Application(int id, int userId, String jobId, String note, String appliedAt) {
        this.id = id; this.userId = userId; this.jobId = jobId; this.note = note; this.appliedAt = appliedAt;
    }
}
