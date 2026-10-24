package com.skillbridge.model;

public class Application {
    public final int id, userId;
    public final String jobId, note, appliedAt;

    public Application(int id, int userId, String jobId, String note, String appliedAt) {
        this.id = id; this.userId = userId; this.jobId = jobId; this.note = note; this.appliedAt = appliedAt;
    }
}
