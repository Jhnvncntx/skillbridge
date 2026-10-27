package com.skillbridge.model;

/**
 * One application. The job title, company and link are copied at the time of applying (a "snapshot"),
 * so the history still shows correctly even if the posting is later removed from jobs.csv.
 */
public class Application {
    public int id;
    public final int userId;
    public final String jobId, jobTitle, company, link, note, appliedAt;

    public Application(int id, int userId, String jobId, String jobTitle, String company, String link,
                       String note, String appliedAt) {
        this.id = id; this.userId = userId; this.jobId = jobId; this.jobTitle = jobTitle; this.company = company;
        this.link = link; this.note = note; this.appliedAt = appliedAt;
    }
}
