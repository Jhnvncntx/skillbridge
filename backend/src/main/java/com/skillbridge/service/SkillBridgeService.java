package com.skillbridge.service;

import com.skillbridge.data.DataLoader;
import com.skillbridge.data.DataLoader.Keywords;
import com.skillbridge.data.DataLoader.LoadReport;
import com.skillbridge.datastructures.*;
import com.skillbridge.dto.Requests.*;
import com.skillbridge.dto.Responses.*;
import com.skillbridge.engine.*;
import com.skillbridge.model.*;
import com.skillbridge.util.Text;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All business logic. Data lives in memory in our own data structures and is reloaded from the
 * CSV files on every start. Methods are synchronized because the structures are not thread-safe
 * and the web server uses several threads.
 */
@Service
public class SkillBridgeService {
    private static final SecureRandom RANDOM = new SecureRandom();

    // ---- reference data (loaded from CSV)
    private final MyLinkedList<Program> programs;
    private final MyHashTable<String, Program> programByCode = new MyHashTable<>();
    private final MyHashTable<String, Keywords> keywords;
    private final MyTree<ProgramNode> programTree = new MyTree<>(new ProgramNode("BPSU Main Campus", null));
    private final MatchingEngine engine = new MatchingEngine();
    private final LoadReport report = new LoadReport();

    // ---- accounts and activity
    private final MyHashTable<String, User> usersByEmail = new MyHashTable<>();
    private final MyHashTable<String, User> usersById = new MyHashTable<>();
    private final MyLinkedList<User> userList = new MyLinkedList<>();
    private final MyHashTable<String, Integer> sessions = new MyHashTable<>();        // token -> user id
    private final MyLinkedList<Application> applications = new MyLinkedList<>();
    private final MyHashTable<String, MyStack<Application>> undoStacks = new MyHashTable<>();   // user id -> stack
    private int nextUserId = 1, nextAppId = 1;

    /** Tree node payload: a label, plus the program it stands for (null for colleges/groups). */
    private static class ProgramNode {
        final String name; final Program program;
        ProgramNode(String name, Program program) { this.name = name; this.program = program; }
    }

    public SkillBridgeService() {
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
        if (!"false".equalsIgnoreCase(System.getenv("SEED_DEMO"))) DemoData.load(this);
    }

    /** College group -> program -> major. Programs with majors become parents of their major nodes. */
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
    }

    private MyTree.Node<ProgramNode> findChild(MyTree.Node<ProgramNode> parent, String name) {
        for (MyTree.Node<ProgramNode> c : parent.children) if (c.data.name.equals(name)) return c;
        return null;
    }

    // ================================================================== reference data endpoints
    public synchronized HealthDto health() { return new HealthDto("ok", programs.size(), engine.jobCount(), userList.size()); }

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
        MyLinkedList<String> roles = new MyLinkedList<>();
        Keywords k = keywords.get(p.code);
        if (k != null) for (String r : k.roles) roles.add(r);
        for (Job j : engine.allJobs())
            if (j.targets(p.code) && !containsIgnoreCase(roles, j.role)) roles.add(j.role);
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

        User u = new User(nextUserId++, email, PasswordHasher.hash(password));
        applyProfile(u, name, r.age(), r.contact(), r.address(), r.gender(), r.programCode(), r.gradYear(),
                r.targetRole(), r.hardSkills(), r.softSkills(), r.languages());
        usersByEmail.put(email, u);
        usersById.put(String.valueOf(u.id), u);
        userList.add(u);
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
        if (token != null) sessions.put(token, -1);
    }

    private String newSession(User u) {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, u.id);
        return token;
    }

    private static String bearer(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        return header.substring(7).trim();
    }

    /** Turns the Authorization header into a user id, or throws 401. */
    public synchronized int userIdFromToken(String authHeader) {
        String token = bearer(authHeader);
        Integer id = token == null ? null : sessions.get(token);
        if (id == null || id < 0) throw ApiException.unauthorized("Please log in.");
        return id;
    }

    public synchronized UserDto me(int userId) { return toDto(requireUser(userId)); }

    public synchronized UserDto updateProfile(int userId, ProfileRequest r) {
        if (r == null) throw ApiException.badRequest("Request body is required.");
        User u = requireUser(userId);
        String name = Text.clean(r.fullName());
        if (name.length() < 2 || name.length() > 80) throw ApiException.badRequest("Full name is required (2-80 characters).");
        applyProfile(u, name, r.age(), r.contact(), r.address(), r.gender(), r.programCode(), r.gradYear(),
                r.targetRole(), r.hardSkills(), r.softSkills(), r.languages());
        return toDto(u);
    }

    private void applyProfile(User u, String name, Integer age, String contact, String address, String gender,
                              String programCode, String gradYear, String targetRole,
                              String[] hard, String[] soft, String[] languages) {
        if (age == null || age < 15 || age > 100) throw ApiException.badRequest("Age must be between 15 and 100.");
        String c = Text.clean(contact);
        if (!c.isEmpty() && !c.matches("^[0-9+\\-() ]{7,20}$")) throw ApiException.badRequest("Contact number looks invalid.");
        String a = Text.clean(address);
        if (a.length() > 200) throw ApiException.badRequest("Address is too long (max 200 characters).");
        Program program = requireProgram(programCode);
        String year = Text.clean(gradYear);
        if (!year.isEmpty() && !year.matches("^(20\\d\\d|2100)$")) throw ApiException.badRequest("Graduation year must look like 2026.");
        String role = Text.clean(targetRole);
        if (role.length() > 100) throw ApiException.badRequest("Target role is too long.");

        u.fullName = name; u.age = age; u.contact = c; u.address = a;
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
        ScoreBreakdown b = ScoreCalculator.breakdown(p.programCode, p.targetRole, p.hardSkills, j);
        return toDto(u, new MatchResult(j, b, 0));
    }

    public synchronized JobDto[] savedJobs(int userId) {
        User u = requireUser(userId);
        JobDto[] out = new JobDto[u.savedJobIds.size()];
        int i = 0;
        for (String id : u.savedJobIds) out[i++] = toDto(u, engine.findJob(id));
        return out;
    }

    public synchronized JobDto saveJob(int userId, String jobId) {
        User u = requireUser(userId);
        Job j = requireJob(jobId);
        if (!u.savedJobIds.contains(j.id)) u.savedJobIds.add(j.id);
        return toDto(u, j);
    }

    public synchronized void unsaveJob(int userId, String jobId) {
        User u = requireUser(userId);
        u.savedJobIds.remove(jobId);
    }

    // ================================================================== applications
    /** Records the application (paperless). The front end then opens the job's original link. */
    public synchronized ApplicationDto apply(int userId, ApplyRequest r) {
        if (r == null || r.jobId() == null || r.jobId().isBlank()) throw ApiException.badRequest("'jobId' is required.");
        User u = requireUser(userId);
        Job j = requireJob(r.jobId().trim());
        String note = Text.clean(r.note());
        if (note.length() > 500) throw ApiException.badRequest("Note is too long (max 500 characters).");
        if (hasApplied(userId, j.id)) throw ApiException.conflict("You already applied to this job.");

        Application app = new Application(nextAppId++, userId, j.id, note, Instant.now().toString());
        applications.add(app);
        String key = String.valueOf(userId);
        MyStack<Application> stack = undoStacks.get(key);
        if (stack == null) { stack = new MyStack<>(); undoStacks.put(key, stack); }
        stack.push(app);                                       // stack = undo history
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
        Application app = stack.pop();
        applications.remove(app);
        return toDto(app);
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

        for (User u : userList) { users[programByCode.get(u.programCode).index]++; genderCounts[u.gender.ordinal()]++; }
        for (Application a : applications) apps[programByCode.get(requireUser(a.userId).programCode).index]++;
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
    /** Package-private so DemoData can create accounts without going through HTTP validation. */
    synchronized User addDemoUser(String email, String password, String name, int age, String programCode,
                                  String targetRole, String[] hardSkills, String[] soft, String[] languages) {
        SignupRequest r = new SignupRequest(name, age, email, "", "", "UNDISCLOSED", password, programCode, "2026",
                targetRole, hardSkills, soft, languages);
        signup(r);
        return usersByEmail.get(email);
    }

    private MatchProfile profile(User u) { return new MatchProfile(u.programCode, u.targetRole, u.hardSkillList()); }

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
            String n = Text.norm(s);
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
        return new UserDto(u.id, u.fullName, u.age, u.email, u.contact, u.address, u.gender.name(), p.code, p.label(),
                p.group, u.gradYear, u.targetRole, u.hardSkills, u.softSkills, u.languages);
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
        Job j = engine.findJob(a.jobId);
        return new ApplicationDto(a.id, a.jobId, j.title, j.company, j.link, a.note, a.appliedAt);
    }
}
