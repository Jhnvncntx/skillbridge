package com.skillbridge.service;

import com.skillbridge.data.DataLoader;
import com.skillbridge.data.DataLoader.Keywords;
import com.skillbridge.data.DataLoader.LoadReport;
import com.skillbridge.datastructures.*;
import com.skillbridge.dto.Requests.*;
import com.skillbridge.dto.Responses.*;
import com.skillbridge.engine.*;
import com.skillbridge.model.*;
import com.skillbridge.persistence.Persistence;
import com.skillbridge.util.Text;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * All business logic. Working copies of the data live in memory in our own data structures.
 * Whenever users, saved jobs or applications change, the change is saved through `Persistence` FIRST
 * (PostgreSQL when DATABASE_URL is set), and only then applied in memory, so a failed save never leaves
 * the two out of step. Job listings always come from jobs.csv.
 * Methods are synchronized because the structures are not thread-safe and the server uses several threads.
 */
@Service
public class SkillBridgeService {
    private static final SecureRandom RANDOM = new SecureRandom();

    // ---- reference data (loaded from CSV)
    private final MyLinkedList<Program> programs;
    private final MyHashTable<String, Program> programByCode = new MyHashTable<>();
    private final MyHashTable<String, String> baseProgramOf = new MyHashTable<>();   // major code -> base program code
    private final MyHashTable<String, Keywords> keywords;
    private final MyTree<ProgramNode> programTree = new MyTree<>(new ProgramNode("BPSU Main Campus", null));
    private final MatchingEngine engine = new MatchingEngine();
    private final LoadReport report = new LoadReport();

    // ---- storage and accounts
    private final Persistence db;
    private final MyHashTable<String, User> usersByEmail = new MyHashTable<>();
    private final MyHashTable<String, User> usersById = new MyHashTable<>();
    private final MyLinkedList<User> userList = new MyLinkedList<>();
    private final MyLinkedList<Application> applications = new MyLinkedList<>();
    private final MyHashTable<String, MyStack<Application>> undoStacks = new MyHashTable<>();   // user id -> stack

    // ---- sessions (memory only): token -> who and when they were last active
    private static class Session {
        final int userId; long lastActive;
        Session(int userId, long lastActive) { this.userId = userId; this.lastActive = lastActive; }
    }
    private final MyHashTable<String, Session> sessions = new MyHashTable<>();
    private final long sessionMillis;
    private LongSupplier clock = System::currentTimeMillis;

    /** Tree node payload: a label, plus the program it stands for (null for colleges/groups). */
    private static class ProgramNode {
        final String name; final Program program;
        ProgramNode(String name, Program program) { this.name = name; this.program = program; }
    }

    /** Used by Spring: storage from DATABASE_URL, inactivity limit from SESSION_MINUTES (default 10). */
    public SkillBridgeService() {
        this(Persistence.fromEnvironment(), envInt("SESSION_MINUTES", 10), !"false".equalsIgnoreCase(System.getenv("SEED_DEMO")));
    }

    public SkillBridgeService(Persistence db, int sessionMinutes, boolean seedDemo) {
        this.db = db;
        this.sessionMillis = sessionMinutes * 60_000L;
        try {
            programs = DataLoader.loadPrograms(DataLoader.readResource("data/programs.csv"));
            keywords = DataLoader.loadKeywords(DataLoader.readResource("data/program_keywords.csv"));
            for (Program p : programs) programByCode.put(p.code, p);
            buildProgramTree();
            for (String code : programByCode.keys()) {
                Keywords k = keywords.get(code);
                if (k != null) engine.addKnownSkills(k.skills);
            }
            for (Job job : DataLoader.loadJobs(DataLoader.readJobsText(), programByCode, report)) engine.addJob(job);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load data files: " + e.getMessage(), e);
        }
        System.out.println("SkillBridge data: " + programs.size() + " programs, " + report.jobsLoaded + " jobs loaded, "
                + report.problems.size() + " job rows skipped.");
        for (String problem : report.problems) System.out.println("  skipped: " + problem);

        db.init();
        loadSavedData();
        System.out.println("SkillBridge storage: " + (db.isEnabled() ? "PostgreSQL (data is saved)" : "memory only (data is lost on restart)")
                + ", " + userList.size() + " accounts, " + applications.size() + " applications.");
        if (seedDemo) DemoData.load(this);
    }

    private static int envInt(String name, int fallback) {
        try { return Integer.parseInt(System.getenv(name)); } catch (Exception e) { return fallback; }
    }

    /** For tests: replace the clock so the inactivity limit can be checked without waiting. */
    public synchronized void useClock(LongSupplier c) { this.clock = c; }

    /** Reads users, saved jobs and applications from storage into the in-memory structures. */
    private void loadSavedData() {
        Persistence.Loaded data = db.load();
        for (User u : data.users) {
            // keep only saved jobs that still exist in jobs.csv (the rows stay in the database)
            MyLinkedList<String> keep = new MyLinkedList<>();
            for (String key : u.savedJobIds) if (engine.findJob(key) != null) keep.add(key);
            u.savedJobIds.clear();
            for (String key : keep) u.savedJobIds.add(key);
            registerInMemory(u);
        }
        for (Application a : data.applications) {
            if (usersById.get(String.valueOf(a.userId)) == null) continue;
            applications.add(a);
            pushUndo(a);                          // oldest first, so the newest ends up on top of the stack
        }
    }

    private void registerInMemory(User u) {
        usersByEmail.put(u.email, u);
        usersById.put(String.valueOf(u.id), u);
        userList.add(u);
    }

    private void pushUndo(Application a) {
        String key = String.valueOf(a.userId);
        MyStack<Application> stack = undoStacks.get(key);
        if (stack == null) { stack = new MyStack<>(); undoStacks.put(key, stack); }
        stack.push(a);
    }

    /** College group -> program -> major. Also records which program each major belongs to. */
    private void buildProgramTree() {
        for (Program p : programs) {
            MyTree.Node<ProgramNode> group = findChild(programTree.getRoot(), p.group);
            if (group == null) group = programTree.getRoot().addChild(new ProgramNode(p.group, null));
            MyTree.Node<ProgramNode> prog = findChild(group, p.program);
            if (prog == null) prog = group.addChild(new ProgramNode(p.program, p.major.isEmpty() ? p : null));
            else if (p.major.isEmpty() && prog.data.program == null) {          // base program appears after its majors
                MyTree.Node<ProgramNode> replacement = group.addChild(new ProgramNode(p.program, p));
                for (MyTree.Node<ProgramNode> m : prog.children) replacement.children.add(m);
                group.children.remove(prog);
                prog = replacement;
            }
            if (!p.major.isEmpty()) prog.addChild(new ProgramNode(p.major, p));
        }
        // a major inherits the jobs of its base program (for example CS-SD also matches jobs tagged CS)
        for (MyTree.Node<ProgramNode> group : programTree.getRoot().children)
            for (MyTree.Node<ProgramNode> prog : group.children)
                if (prog.data.program != null)
                    for (MyTree.Node<ProgramNode> major : prog.children)
                        if (major.data.program != null) baseProgramOf.put(major.data.program.code, prog.data.program.code);
    }

    private MyTree.Node<ProgramNode> findChild(MyTree.Node<ProgramNode> parent, String name) {
        for (MyTree.Node<ProgramNode> c : parent.children) if (c.data.name.equals(name)) return c;
        return null;
    }

    /** The student's own program code, plus the base program when the student is in a major. */
    private String[] programCodesFor(String code) {
        String base = baseProgramOf.get(code);
        return base == null ? new String[]{code} : new String[]{code, base};
    }

    // ================================================================== reference data endpoints
    public synchronized HealthDto health() {
        return new HealthDto("ok", programs.size(), engine.jobCount(), userList.size(), db.isEnabled() ? "connected" : "memory-only");
    }

    /** Flat program list in tree order (a depth-first traversal of the program tree). */
    public synchronized ProgramDto[] programList() {
        MyLinkedList<ProgramDto> out = new MyLinkedList<>();
        for (ProgramNode n : programTree.preorder()) if (n.program != null) out.add(toDto(n.program));
        ProgramDto[] arr = new ProgramDto[out.size()];
        int i = 0;
        for (ProgramDto d : out) arr[i++] = d;
        return arr;
    }

    public synchronized TreeDto programTree() { return toDto(programTree.getRoot()); }

    private TreeDto toDto(MyTree.Node<ProgramNode> node) {
        TreeDto[] kids = new TreeDto[node.children.size()];
        int i = 0;
        for (MyTree.Node<ProgramNode> c : node.children) kids[i++] = toDto(c);
        return new TreeDto(node.data.name, node.data.program == null ? null : node.data.program.code, kids);
    }

    /** Suggested target roles for a program: the guide's keywords first, then roles seen in real postings. */
    public synchronized String[] rolesFor(String programCode) {
        Program p = requireProgram(programCode);
        String[] codes = programCodesFor(p.code);
        MyLinkedList<String> roles = new MyLinkedList<>();
        Keywords k = keywords.get(p.code);
        if (k != null) for (String r : k.roles) roles.add(r);
        for (Job j : engine.allJobs())
            if (j.targetsAny(codes) && !containsIgnoreCase(roles, j.role)) roles.add(j.role);
        String[] out = new String[roles.size()];
        int i = 0;
        for (String r : roles) out[i++] = r;
        return out;
    }

    /** Suggested hard skills for a program (from the keyword guide). */
    public synchronized String[] skillsFor(String programCode) {
        Keywords k = keywords.get(requireProgram(programCode).code);
        return k == null ? new String[0] : k.skills;
    }

    public synchronized String[] skillSuggestions(String prefix) { return engine.suggestSkills(prefix, 10); }

    private static boolean containsIgnoreCase(MyLinkedList<String> list, String s) {
        for (String x : list) if (x.equalsIgnoreCase(s)) return true;
        return false;
    }

    // ================================================================== accounts
    public synchronized AuthDto signup(SignupRequest r) {
        if (r == null) throw ApiException.badRequest("Request body is required.");
        String name = Text.clean(r.fullName());
        if (name.length() < 2 || name.length() > 80) throw ApiException.badRequest("Full name is required (2-80 characters).");
        String email = Text.norm(r.email());
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || email.length() > 120) throw ApiException.badRequest("Enter a valid email address.");
        if (usersByEmail.get(email) != null) throw ApiException.conflict("An account with this email already exists.");
        String password = r.password() == null ? "" : r.password();
        if (password.length() < 8 || password.length() > 72) throw ApiException.badRequest("Password must be 8-72 characters.");

        User u = new User(0, email, PasswordHasher.hash(password));
        applyProfile(u, name, r.age(), r.gender(), r.programCode(), r.gradYear(),
                r.targetRole(), r.hardSkills(), r.softSkills(), r.languages());
        u.id = db.insertUser(u);                     // save first; a failure here leaves memory untouched
        registerInMemory(u);
        return new AuthDto(newSession(u), toDto(u));
    }

    public synchronized AuthDto login(LoginRequest r) {
        if (r == null) throw ApiException.badRequest("Request body is required.");
        User u = usersByEmail.get(Text.norm(r.email()));
        // same message for "no such user" and "wrong password" so emails cannot be probed
        if (u == null || r.password() == null || !PasswordHasher.verify(r.password(), u.passwordHash))
            throw ApiException.unauthorized("Invalid email or password.");
        return new AuthDto(newSession(u), toDto(u));
    }

    public synchronized void logout(String authHeader) {
        String token = bearer(authHeader);
        if (token != null) sessions.remove(token);
    }

    private String newSession(User u) {
        purgeExpiredSessions();
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(u.id, clock.getAsLong()));
        return token;
    }

    private void purgeExpiredSessions() {
        long now = clock.getAsLong();
        for (String token : sessions.keys()) {
            Session s = sessions.get(token);
            if (s != null && now - s.lastActive > sessionMillis) sessions.remove(token);
        }
    }

    private static String bearer(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        return header.substring(7).trim();
    }

    /**
     * Turns the Authorization header into a user id, or throws 401.
     * Every request counts as activity: the session ends after SESSION_MINUTES (default 10) without one.
     */
    public synchronized int userIdFromToken(String authHeader) {
        String token = bearer(authHeader);
        Session s = token == null ? null : sessions.get(token);
        if (s == null) throw ApiException.unauthorized("Please log in.");
        long now = clock.getAsLong();
        if (now - s.lastActive > sessionMillis) {
            sessions.remove(token);
            throw ApiException.unauthorized("You were logged out due to inactivity. Please log in again.");
        }
        s.lastActive = now;
        return s.userId;
    }

    public synchronized UserDto me(int userId) { return toDto(requireUser(userId)); }

    public synchronized UserDto updateProfile(int userId, ProfileRequest r) {
        if (r == null) throw ApiException.badRequest("Request body is required.");
        User u = requireUser(userId);
        String name = Text.clean(r.fullName());
        if (name.length() < 2 || name.length() > 80) throw ApiException.badRequest("Full name is required (2-80 characters).");
        User draft = u.copy();                       // validate and save the draft, then copy it into memory
        applyProfile(draft, name, r.age(), r.gender(), r.programCode(), r.gradYear(),
                r.targetRole(), r.hardSkills(), r.softSkills(), r.languages());
        db.updateUser(draft);
        u.takeProfileFrom(draft);
        return toDto(u);
    }

    /** Deletes the account and everything stored for it (profile, saved jobs, applications, sessions). */
    public synchronized void deleteAccount(int userId, DeleteAccountRequest r) {
        User u = requireUser(userId);
        if (r == null || r.password() == null || !PasswordHasher.verify(r.password(), u.passwordHash))
            // 403, not 401: the front end treats 401 as "session ended" and would log the user out
            throw ApiException.forbidden("Incorrect password. Your account was not deleted.");
        db.deleteUser(userId);                       // the database removes saved jobs and applications too
        usersById.remove(String.valueOf(userId));
        usersByEmail.remove(u.email);
        userList.remove(u);
        MyLinkedList<Application> mine = new MyLinkedList<>();
        for (Application a : applications) if (a.userId == userId) mine.add(a);
        for (Application a : mine) applications.remove(a);
        undoStacks.remove(String.valueOf(userId));
        for (String token : sessions.keys()) {
            Session s = sessions.get(token);
            if (s != null && s.userId == userId) sessions.remove(token);
        }
    }

    private void applyProfile(User u, String name, Integer age, String gender, String programCode, String gradYear,
                              String targetRole, String[] hard, String[] soft, String[] languages) {
        if (age == null || age < 15 || age > 100) throw ApiException.badRequest("Age must be between 15 and 100.");
        Program program = requireProgram(programCode);
        String year = Text.clean(gradYear);
        if (!year.isEmpty() && !year.matches("^(20\\d\\d|2100)$")) throw ApiException.badRequest("Graduation year must look like 2026.");
        String role = Text.clean(targetRole);
        if (role.length() > 100) throw ApiException.badRequest("Target role is too long.");

        u.fullName = name; u.age = age;
        u.gender = parseGender(gender); u.programCode = program.code; u.gradYear = year; u.targetRole = role;
        u.hardSkills = cleanList(hard, 30, "Hard skills");
        u.softSkills = cleanList(soft, 20, "Soft skills");
        u.languages = cleanList(languages, 10, "Languages");
    }

    // ================================================================== jobs and matching
    public synchronized HomeDto home(int userId, int top) {
        if (top < 1 || top > 20) throw ApiException.badRequest("'top' must be between 1 and 20.");
        User u = requireUser(userId);
        MatchProfile p = profile(u);
        MatchResult[] recommended = engine.recommend(p, top);
        MyHashTable<String, Boolean> shown = new MyHashTable<>();
        for (MatchResult m : recommended) shown.put(m.job.id, true);
        MatchResult[] also = engine.alsoQualify(p, top, shown);
        return new HomeDto(toDtos(u, recommended), toDtos(u, also));
    }

    /** All jobs ranked for this user; with a query, only jobs matching the keywords. */
    public synchronized MatchDto[] jobs(int userId, String query) {
        User u = requireUser(userId);
        MatchProfile p = profile(u);
        MatchResult[] results = (query == null || query.isBlank()) ? engine.rankAll(p) : engine.search(p, query);
        return toDtos(u, results);
    }

    public synchronized MatchDto jobDetail(int userId, String jobId) {
        User u = requireUser(userId);
        Job j = requireJob(jobId);
        MatchProfile p = profile(u);
        ScoreBreakdown b = ScoreCalculator.breakdown(p.programCodes, p.targetRole, p.hardSkills, j);
        return toDto(u, new MatchResult(j, b, 0));
    }

    public synchronized JobDto[] savedJobs(int userId) {
        User u = requireUser(userId);
        int count = 0;
        for (String id : u.savedJobIds) if (engine.findJob(id) != null) count++;
        JobDto[] out = new JobDto[count];
        int i = 0;
        for (String id : u.savedJobIds) { Job j = engine.findJob(id); if (j != null) out[i++] = toDto(u, j); }
        return out;
    }

    public synchronized JobDto saveJob(int userId, String jobId) {
        User u = requireUser(userId);
        Job j = requireJob(jobId);
        if (!u.savedJobIds.contains(j.id)) { db.saveJob(userId, j.id); u.savedJobIds.add(j.id); }
        return toDto(u, j);
    }

    public synchronized void unsaveJob(int userId, String jobId) {
        User u = requireUser(userId);
        if (u.savedJobIds.contains(jobId)) { db.unsaveJob(userId, jobId); u.savedJobIds.remove(jobId); }
    }

    // ================================================================== applications
    /** Records the application (paperless). The front end then opens the job's original link. */
    public synchronized ApplicationDto apply(int userId, ApplyRequest r) {
        if (r == null || r.jobId() == null || r.jobId().isBlank()) throw ApiException.badRequest("'jobId' is required.");
        requireUser(userId);
        Job j = requireJob(r.jobId().trim());
        String note = Text.clean(r.note());
        if (note.length() > 500) throw ApiException.badRequest("Note is too long (max 500 characters).");
        if (hasApplied(userId, j.id)) throw ApiException.conflict("You already applied to this job.");

        Application app = new Application(0, userId, j.id, j.title, j.company, j.link, note, Instant.now().toString());
        app.id = db.insertApplication(app);          // save first
        applications.add(app);
        pushUndo(app);                               // stack = undo history
        return toDto(app);
    }

    public synchronized ApplicationDto[] applications(int userId) {
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
        MyStack<Application> stack = undoStacks.get(String.valueOf(userId));
        if (stack == null || stack.isEmpty()) throw ApiException.notFound("No application to undo.");
        Application app = stack.peek();
        db.deleteApplication(app.id);                // remove from storage before changing memory
        stack.pop();
        applications.remove(app);
        return toDto(app);
    }

    /**
     * Removes ONE application from the user's own list (any of them, not only the newest).
     * This only changes the list in SkillBridge; it cannot withdraw anything sent on the job site.
     * The undo stack is rebuilt from the remaining applications, so "Undo last" always removes the newest one left.
     */
    public synchronized ApplicationDto removeApplication(int userId, int applicationId) {
        requireUser(userId);
        Application target = null;
        for (Application a : applications) if (a.id == applicationId && a.userId == userId) target = a;   // only the owner's own
        if (target == null) throw ApiException.notFound("Application not found.");
        db.deleteApplication(applicationId);          // storage first
        applications.remove(target);
        rebuildUndoStack(userId);
        return toDto(target);
    }

    private void rebuildUndoStack(int userId) {
        MyStack<Application> fresh = new MyStack<>();
        for (Application a : applications) if (a.userId == userId) fresh.push(a);   // oldest first, so the newest ends on top
        undoStacks.put(String.valueOf(userId), fresh);
    }

    private boolean hasApplied(int userId, String jobId) {
        for (Application a : applications) if (a.userId == userId && a.jobId.equals(jobId)) return true;
        return false;
    }

    // ================================================================== admin
    public synchronized StatsDto stats() {
        int n = programs.size();
        int[] users = new int[n], apps = new int[n], jobs = new int[n];
        Gender[] genders = Gender.values();
        int[] genderCounts = new int[genders.length];

        for (User u : userList) {
            Program p = programByCode.get(u.programCode);
            if (p != null) users[p.index]++;
            genderCounts[u.gender.ordinal()]++;
        }
        for (Application a : applications) {
            User owner = usersById.get(String.valueOf(a.userId));
            Program p = owner == null ? null : programByCode.get(owner.programCode);
            if (p != null) apps[p.index]++;
        }
        for (Job j : engine.allJobs()) for (String code : j.programCodes) jobs[programByCode.get(code).index]++;

        ProgramStat[] rows = new ProgramStat[n];
        int withJobs = 0;
        for (Program p : programs) {
            rows[p.index] = new ProgramStat(p.code, p.label(), p.group, users[p.index], apps[p.index], jobs[p.index], jobs[p.index] == 0);
            if (jobs[p.index] > 0) withJobs++;
        }
        SkillDemand[] demand = engine.skillDemand();
        SkillDemandDto[] top = new SkillDemandDto[Math.min(10, demand.length)];
        for (int i = 0; i < top.length; i++) top[i] = new SkillDemandDto(demand[i].skill, demand[i].jobCount);

        Map<String, Integer> byGender = new LinkedHashMap<>();
        for (int i = 0; i < genders.length; i++) byGender.put(genders[i].name(), genderCounts[i]);

        return new StatsDto(userList.size(), engine.jobCount(), applications.size(), withJobs, n, rows, top, byGender,
                new DataReportDto(report.rowsRead, report.jobsLoaded, Text.toArray(report.problems)));
    }

    // ================================================================== helpers
    /** Package-private so DemoData can create accounts without going through HTTP validation. Skips existing ones. */
    synchronized void addDemoUser(String email, String password, String name, int age, String programCode,
                                  String targetRole, String[] hardSkills, String[] soft, String[] languages) {
        if (usersByEmail.get(email) != null) return;        // already saved from an earlier start
        signup(new SignupRequest(name, age, email, "UNDISCLOSED", password, programCode, "2026",
                targetRole, hardSkills, soft, languages));
    }

    private MatchProfile profile(User u) { return new MatchProfile(programCodesFor(u.programCode), u.targetRole, u.hardSkillList()); }

    private User requireUser(int id) {
        User u = usersById.get(String.valueOf(id));
        if (u == null) throw ApiException.notFound("User not found.");
        return u;
    }

    private Job requireJob(String id) {
        Job j = id == null ? null : engine.findJob(id);
        if (j == null) throw ApiException.notFound("Job not found: " + id);
        return j;
    }

    private Program requireProgram(String code) {
        String c = Text.clean(code).toUpperCase();
        if (c.isEmpty()) throw ApiException.badRequest("Program is required.");
        Program p = programByCode.get(c);
        if (p == null) throw ApiException.badRequest("Unknown program: " + code);
        return p;
    }

    private static Gender parseGender(String g) {
        if (g == null || g.isBlank()) return Gender.UNDISCLOSED;
        try { return Gender.valueOf(g.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw ApiException.badRequest("Unknown gender value: " + g); }
    }

    private static String[] cleanList(String[] raw, int max, String label) {
        MyLinkedList<String> list = new MyLinkedList<>();
        if (raw != null) for (String s : raw) {
            String n = Text.norm(s).replace(';', ',');         // ';' separates items in the database
            if (n.isEmpty()) continue;
            if (n.length() > 40) throw ApiException.badRequest(label + ": '" + n.substring(0, 20) + "...' is too long (max 40 characters).");
            if (!list.contains(n)) list.add(n);
        }
        if (list.size() > max) throw ApiException.badRequest(label + ": too many entries (max " + max + ").");
        return Text.toArray(list);
    }

    // ---- model -> DTO
    private ProgramDto toDto(Program p) {
        return new ProgramDto(p.code, p.group, p.program, p.major, p.label(), p.level, p.status);
    }

    private UserDto toDto(User u) {
        Program p = programByCode.get(u.programCode);
        return new UserDto(u.id, u.fullName, u.age, u.email, u.gender.name(), u.programCode,
                p == null ? u.programCode : p.label(), p == null ? "" : p.group, u.gradYear, u.targetRole,
                u.hardSkills, u.softSkills, u.languages);
    }

    private JobDto toDto(User u, Job j) {
        return new JobDto(j.id, j.title, j.role, j.company, j.location, j.description, j.licensure, j.link, j.source,
                j.dateCollected, j.programCodes, j.requiredSkills, u.savedJobIds.contains(j.id), hasApplied(u.id, j.id));
    }

    private MatchDto toDto(User u, MatchResult m) {
        ScoreBreakdown b = m.breakdown;
        return new MatchDto(toDto(u, m.job), b.total, b.role, b.skills, b.program, b.matched, b.missing, m.distance);
    }

    private MatchDto[] toDtos(User u, MatchResult[] results) {
        MatchDto[] out = new MatchDto[results.length];
        for (int i = 0; i < results.length; i++) out[i] = toDto(u, results[i]);
        return out;
    }

    private ApplicationDto toDto(Application a) {
        return new ApplicationDto(a.id, a.jobId, a.jobTitle, a.company, a.link, a.note, a.appliedAt);
    }
}
