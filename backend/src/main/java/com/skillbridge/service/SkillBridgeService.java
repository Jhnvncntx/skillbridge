package com.skillbridge.service;

import com.skillbridge.datastructures.MyHashTable;
import com.skillbridge.datastructures.MyLinkedList;
import com.skillbridge.datastructures.MyStack;
import com.skillbridge.dto.Requests.*;
import com.skillbridge.dto.Responses.*;
import com.skillbridge.engine.MatchResult;
import com.skillbridge.engine.MatchingEngine;
import com.skillbridge.engine.SkillDemand;
import com.skillbridge.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All business logic. Storage is in-memory (custom data structures).
 * Methods are synchronized because the custom structures are not thread-safe
 * and the web server handles requests on multiple threads.
 */
@Service
public class SkillBridgeService {
    private static final int MAX_SKILLS_USER = 30, MAX_SKILLS_JOB = 15, MAX_SKILL_LEN = 40;

    private final MatchingEngine engine = new MatchingEngine();
    private final MyHashTable<String, User> usersById = new MyHashTable<>();
    private final MyLinkedList<User> userList = new MyLinkedList<>();
    private final MyLinkedList<Job> jobList = new MyLinkedList<>();
    private final MyLinkedList<Application> applications = new MyLinkedList<>();
    private final MyHashTable<String, MyStack<Application>> historyByUser = new MyHashTable<>();
    private int nextUserId = 1, nextJobNum = 1, nextAppId = 1;

    public SkillBridgeService() { SeedData.load(this); }

    // ------------------------------------------------------------------ programs / health
    public synchronized HealthDto health() { return new HealthDto("ok", userList.size(), jobList.size()); }

    public ProgramDto[] programs() {
        Program[] all = Program.values();
        ProgramDto[] out = new ProgramDto[all.length];
        for (int i = 0; i < all.length; i++)
            out[i] = new ProgramDto(all[i].name(), all[i].getDisplayName(), all[i].getCollege());
        return out;
    }

    // ------------------------------------------------------------------ users
    public synchronized UserDto registerUser(RegisterRequest req) {
        if (req == null) throw ApiException.badRequest("Request body is required.");
        String name = clean(req.name());
        if (name.isEmpty() || name.length() > 80) throw ApiException.badRequest("Name is required (max 80 characters).");
        Program program = parseProgram(req.program());
        Gender gender = parseGender(req.gender());
        return toDto(createUser(name, program, gender, new String[0]));
    }

    /** Used by registerUser and the seed loader. */
    synchronized User createUser(String name, Program program, Gender gender, String[] skills) {
        User u = new User(nextUserId++, name, program, gender);
        u.replaceSkills(skills);
        usersById.put(String.valueOf(u.getId()), u);
        userList.add(u);
        return u;
    }

    public synchronized UserDto getUser(int id) { return toDto(requireUser(id)); }

    public synchronized UserDto setSkills(int id, SkillsRequest req) {
        User u = requireUser(id);
        if (req == null || req.skills() == null) throw ApiException.badRequest("'skills' array is required.");
        u.replaceSkills(cleanSkills(req.skills(), MAX_SKILLS_USER, false));
        return toDto(u);
    }

    // ------------------------------------------------------------------ skills
    public synchronized SkillCheckDto checkSkill(String name) {
        String n = normSkill(name);
        if (n.isEmpty()) throw ApiException.badRequest("Skill name is required.");
        return new SkillCheckDto(n, engine.isKnownSkill(n));
    }

    public synchronized String[] skillIndex() { return engine.getSortedSkillIndex().clone(); }

    // ------------------------------------------------------------------ jobs
    public synchronized JobDto postJob(JobRequest req) {
        if (req == null) throw ApiException.badRequest("Request body is required.");
        String title = clean(req.title()), company = clean(req.company());
        if (title.isEmpty() || title.length() > 100) throw ApiException.badRequest("Job title is required (max 100 characters).");
        if (company.isEmpty() || company.length() > 100) throw ApiException.badRequest("Company is required (max 100 characters).");
        Program[] programs = parsePrograms(req.targetPrograms());
        String[] skills = cleanSkills(req.requiredSkills(), MAX_SKILLS_JOB, true);
        return toDto(createJob(title, company, "employer", programs, skills));
    }

    /** Used by postJob and the seed loader. */
    synchronized Job createJob(String title, String company, String source, Program[] programs, String[] skills) {
        Job job = new Job("J" + (nextJobNum++), title, company, source, programs, skills);
        engine.addJob(job);
        jobList.add(job);
        return job;
    }

    public synchronized JobDto[] listJobs() {
        JobDto[] out = new JobDto[jobList.size()];
        int i = 0;
        for (Job j : jobList) out[i++] = toDto(j);
        return out;
    }

    // ------------------------------------------------------------------ matching
    public synchronized MatchDto[] recommendations(int userId, int top) {
        if (top < 1 || top > 50) throw ApiException.badRequest("'top' must be between 1 and 50.");
        User u = requireUser(userId);
        MyLinkedList<MatchResult> results = engine.recommendTop(u, top);   // heap
        MatchDto[] out = new MatchDto[results.size()];
        int i = 0;
        for (MatchResult r : results) out[i++] = toDto(u, r);
        return out;
    }

    public synchronized MatchDto[] ranked(int userId) {
        User u = requireUser(userId);
        MatchResult[] results = engine.rankAll(u);                          // merge sort
        MatchDto[] out = new MatchDto[results.length];
        for (int i = 0; i < results.length; i++) out[i] = toDto(u, results[i]);
        return out;
    }

    // ------------------------------------------------------------------ applications
    public synchronized ApplicationDto apply(ApplyRequest req) {
        if (req == null || req.userId() == null) throw ApiException.badRequest("'userId' is required.");
        if (req.jobId() == null || req.jobId().isBlank()) throw ApiException.badRequest("'jobId' is required.");
        User u = requireUser(req.userId());
        Job job = engine.findJob(req.jobId().trim());
        if (job == null) throw ApiException.notFound("Job not found: " + req.jobId());
        String note = clean(req.note());
        if (note.length() > 500) throw ApiException.badRequest("Note is too long (max 500 characters).");
        for (Application a : applications)
            if (a.userId == u.getId() && a.jobId.equals(job.getId()))
                throw ApiException.conflict("You already applied to this job.");

        Application app = new Application(nextAppId++, u.getId(), job.getId(), note, Instant.now().toString());
        applications.add(app);
        String key = String.valueOf(u.getId());
        MyStack<Application> stack = historyByUser.get(key);
        if (stack == null) { stack = new MyStack<>(); historyByUser.put(key, stack); }
        stack.push(app);                                                    // stack = undo history
        return toDto(app);
    }

    public synchronized ApplicationDto[] userApplications(int userId) {
        requireUser(userId);
        int count = 0;
        for (Application a : applications) if (a.userId == userId) count++;
        ApplicationDto[] out = new ApplicationDto[count];
        int i = 0;
        for (Application a : applications) if (a.userId == userId) out[i++] = toDto(a);
        return out;
    }

    public synchronized ApplicationDto undoLastApplication(int userId) {
        requireUser(userId);
        MyStack<Application> stack = historyByUser.get(String.valueOf(userId));
        if (stack == null || stack.isEmpty()) throw ApiException.notFound("No application to undo.");
        Application app = stack.pop();
        applications.remove(app);
        return toDto(app);
    }

    // ------------------------------------------------------------------ admin
    public synchronized StatsDto stats() {
        Program[] programs = Program.values();
        int[] users = new int[programs.length], apps = new int[programs.length], jobs = new int[programs.length];
        Gender[] genders = Gender.values();
        int[] genderCounts = new int[genders.length];

        for (User u : userList) { users[u.getProgram().ordinal()]++; genderCounts[u.getGender().ordinal()]++; }
        for (Application a : applications) apps[requireUser(a.userId).getProgram().ordinal()]++;
        for (Job j : jobList) for (Program p : j.getTargetPrograms()) jobs[p.ordinal()]++;

        ProgramStat[] rows = new ProgramStat[programs.length];
        for (int i = 0; i < programs.length; i++)
            rows[i] = new ProgramStat(programs[i].name(), programs[i].getDisplayName(), programs[i].getCollege(),
                    users[i], apps[i], jobs[i]);

        SkillDemand[] demand = engine.skillDemand();
        int n = Math.min(10, demand.length);
        SkillDemandDto[] top = new SkillDemandDto[n];
        for (int i = 0; i < n; i++) top[i] = new SkillDemandDto(demand[i].skill, demand[i].jobCount);

        Map<String, Integer> byGender = new LinkedHashMap<>();
        for (int i = 0; i < genders.length; i++) byGender.put(genders[i].name(), genderCounts[i]);

        return new StatsDto(userList.size(), jobList.size(), applications.size(), rows, top, byGender);
    }

    // ------------------------------------------------------------------ helpers
    private User requireUser(int id) {
        User u = usersById.get(String.valueOf(id));
        if (u == null) throw ApiException.notFound("User not found: " + id);
        return u;
    }

    private static String clean(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", " "); }
    private static String normSkill(String s) { return clean(s).toLowerCase(); }

    private static Program parseProgram(String code) {
        if (code == null || code.isBlank()) throw ApiException.badRequest("Program is required.");
        try { return Program.valueOf(code.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw ApiException.badRequest("Unknown program: " + code); }
    }

    private static Program[] parsePrograms(String[] codes) {
        if (codes == null || codes.length == 0) throw ApiException.badRequest("At least one target program is required.");
        MyLinkedList<Program> list = new MyLinkedList<>();
        for (String c : codes) { Program p = parseProgram(c); if (!list.contains(p)) list.add(p); }
        Program[] out = new Program[list.size()];
        int i = 0;
        for (Program p : list) out[i++] = p;
        return out;
    }

    private static String[] cleanSkills(String[] raw, int max, boolean atLeastOne) {
        if (raw == null) raw = new String[0];
        MyLinkedList<String> list = new MyLinkedList<>();
        for (String s : raw) {
            String n = normSkill(s);
            if (n.isEmpty()) continue;
            if (n.length() > MAX_SKILL_LEN) throw ApiException.badRequest("Skill too long (max " + MAX_SKILL_LEN + " characters): " + n);
            if (!list.contains(n)) list.add(n);
        }
        if (atLeastOne && list.isEmpty()) throw ApiException.badRequest("At least one required skill is needed.");
        if (list.size() > max) throw ApiException.badRequest("Too many skills (max " + max + ").");
        String[] out = new String[list.size()];
        int i = 0;
        for (String s : list) out[i++] = s;
        return out;
    }

    private static Gender parseGender(String g) {
        if (g == null || g.isBlank()) return Gender.UNDISCLOSED;
        try { return Gender.valueOf(g.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw ApiException.badRequest("Unknown gender value: " + g); }
    }

    private static String[] codes(Program[] programs) {
        String[] out = new String[programs.length];
        for (int i = 0; i < programs.length; i++) out[i] = programs[i].name();
        return out;
    }

    private static UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getName(), u.getProgram().name(), u.getProgram().getDisplayName(),
                u.getProgram().getCollege(), u.getGender().name(), u.skillsArray());
    }

    private static JobDto toDto(Job j) {
        return new JobDto(j.getId(), j.getTitle(), j.getCompany(), j.getSource(),
                codes(j.getTargetPrograms()), j.getRequiredSkills());
    }

    private MatchDto toDto(User u, MatchResult r) {
        Job j = r.job;
        MyLinkedList<String> matched = new MyLinkedList<>(), missing = new MyLinkedList<>();
        for (String s : j.getRequiredSkills()) { if (u.getSkills().contains(s)) matched.add(s); else missing.add(s); }
        return new MatchDto(j.getId(), j.getTitle(), j.getCompany(), j.getSource(), r.score,
                j.targets(u.getProgram()), r.matchedSkills, toArray(matched), toArray(missing),
                j.getRequiredSkills(), codes(j.getTargetPrograms()));
    }

    private ApplicationDto toDto(Application a) {
        Job j = engine.findJob(a.jobId);
        return new ApplicationDto(a.id, a.userId, a.jobId, j == null ? "" : j.getTitle(),
                j == null ? "" : j.getCompany(), a.note, a.appliedAt);
    }

    private static String[] toArray(MyLinkedList<String> list) {
        String[] out = new String[list.size()];
        int i = 0;
        for (String s : list) out[i++] = s;
        return out;
    }
}
