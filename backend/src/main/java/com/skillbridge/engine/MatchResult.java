package com.skillbridge.engine;

import com.skillbridge.model.Job;

/**
 * A job with its score. "distance" says how far the match is (0 = best):
 *   recommendations: 0
 *   graph search:    BFS depth (1 = direct skill match, 3 = related skills)
 *   keyword search:  number of query words the job did NOT match
 * Ordering: smaller distance first, then higher score, then job id (so results are stable).
 */
public class MatchResult implements Comparable<MatchResult> {
    public final Job job;
    public final ScoreBreakdown breakdown;
    public final int distance;

    public MatchResult(Job job, ScoreBreakdown breakdown, int distance) {
        this.job = job; this.breakdown = breakdown; this.distance = distance;
    }

    @Override
    public int compareTo(MatchResult o) {           // "greater" = better match
        int c = Integer.compare(o.distance, this.distance);
        if (c != 0) return c;
        c = Double.compare(this.breakdown.total, o.breakdown.total);
        return c != 0 ? c : o.job.id.compareTo(this.job.id);
    }
}
