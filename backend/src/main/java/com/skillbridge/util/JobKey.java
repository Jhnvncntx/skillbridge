package com.skillbridge.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A stable id for a job posting, built from its link. It does not change when jobs.csv is edited or reordered,
 * so saved jobs and applications keep pointing at the right job. Tracking parameters in the link are ignored.
 *   JobStreet: jobstreet-95263012    Indeed: indeed-8dd834be09a07811    Anything else: link-<hash of the cleaned link>
 */
public final class JobKey {
    private static final Pattern JOBSTREET_PATH = Pattern.compile("jobstreet\\.[^/]+/(?:[^?#]*/)?job/(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOBSTREET_PARAM = Pattern.compile("jobstreet\\.[^/]+/.*[?&]jobId=(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INDEED = Pattern.compile("indeed\\.[^/]+/.*[?&]jk=([0-9a-fA-F]+)", Pattern.CASE_INSENSITIVE);

    private JobKey() {}

    public static String of(String link) {
        String l = link == null ? "" : link.trim();
        Matcher m = JOBSTREET_PATH.matcher(l);
        if (m.find()) return "jobstreet-" + m.group(1);
        m = JOBSTREET_PARAM.matcher(l);
        if (m.find()) return "jobstreet-" + m.group(1);
        m = INDEED.matcher(l);
        if (m.find()) return "indeed-" + m.group(1).toLowerCase();
        String cleaned = l.toLowerCase().replaceAll("[?#].*$", "").replaceAll("/+$", "");
        return "link-" + sha256(cleaned).substring(0, 12);
    }

    private static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
