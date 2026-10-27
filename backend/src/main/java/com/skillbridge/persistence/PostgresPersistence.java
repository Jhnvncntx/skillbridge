package com.skillbridge.persistence;

import com.skillbridge.model.Application;
import com.skillbridge.model.Gender;
import com.skillbridge.model.User;
import com.skillbridge.service.ApiException;
import com.skillbridge.util.Text;

import java.net.URI;
import java.sql.*;
import java.time.Instant;
import java.util.Properties;

/**
 * PostgreSQL storage using plain JDBC. A short-lived connection is opened for each operation, which copes with
 * Neon suspending an idle database (the first query after a pause just takes a moment longer).
 * DATABASE_URL looks like:  postgresql://user:password@host/dbname?sslmode=require
 */
public class PostgresPersistence implements Persistence {
    private final String jdbcUrl;
    private final Properties props = new Properties();

    public PostgresPersistence(String databaseUrl) {
        String[] parts = parse(databaseUrl);
        jdbcUrl = parts[0];
        props.setProperty("user", parts[1]);
        props.setProperty("password", parts[2]);
        props.setProperty("connectTimeout", "15");     // seconds
        props.setProperty("socketTimeout", "30");
        props.setProperty("prepareThreshold", "0");    // safe with pooled (pgbouncer) hosts
    }

    /**
     * Turns postgresql://user:password@host:port/db?sslmode=require into {jdbcUrl, user, password}.
     * Only the sslmode option is kept (default "require"): other options Neon adds, such as channel_binding,
     * are not understood by the JDBC URL and are dropped.
     */
    public static String[] parse(String raw) {
        try {
            URI uri = new URI(raw.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme();
            if (!scheme.equals("postgresql") && !scheme.equals("postgres"))
                throw new IllegalArgumentException("DATABASE_URL must start with postgresql://");
            String info = uri.getUserInfo();
            if (info == null || !info.contains(":")) throw new IllegalArgumentException("DATABASE_URL must contain user:password@");
            String user = info.substring(0, info.indexOf(':')), password = info.substring(info.indexOf(':') + 1);
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String db = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
            if (uri.getHost() == null || db.isEmpty()) throw new IllegalArgumentException("DATABASE_URL must contain host and database name");
            String sslmode = "require";
            if (uri.getRawQuery() != null)
                for (String kv : uri.getRawQuery().split("&")) if (kv.startsWith("sslmode=")) sslmode = kv.substring(8);
            return new String[]{"jdbc:postgresql://" + uri.getHost() + ":" + port + "/" + db + "?sslmode=" + sslmode, user, password};
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("DATABASE_URL is not a valid address");
        }
    }

    // ------------------------------------------------------------------ connections
    private interface Work<T> { T run(Connection c) throws SQLException; }

    /** Runs one piece of database work. Connection problems are retried once; every failure becomes a clean 503. */
    private <T> T withConnection(Work<T> work) {
        SQLException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try (Connection c = DriverManager.getConnection(jdbcUrl, props)) {
                return work.run(c);
            } catch (SQLException e) {
                last = e;
                boolean connectionProblem = e.getSQLState() != null && e.getSQLState().startsWith("08");
                if (!connectionProblem) break;
                sleep(700);
            }
        }
        if (last != null && "23505".equals(last.getSQLState())) throw ApiException.conflict("That record already exists.");
        System.err.println("Database error: " + (last == null ? "unknown" : last.getMessage()));
        throw new ApiException(503, "The database is temporarily unavailable. Please try again in a moment.");
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    public boolean isEnabled() { return true; }

    // ------------------------------------------------------------------ startup
    /** Waits for the database (Neon may need a few seconds to wake) and creates the tables if they do not exist. */
    public void init() {
        SQLException last = null;
        for (int attempt = 1; attempt <= 5; attempt++) {
            try (Connection c = DriverManager.getConnection(jdbcUrl, props); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS users ("
                        + "id SERIAL PRIMARY KEY, email TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL,"
                        + "full_name TEXT NOT NULL, age INT NOT NULL, gender TEXT NOT NULL, program_code TEXT NOT NULL,"
                        + "grad_year TEXT, target_role TEXT, hard_skills TEXT, soft_skills TEXT, languages TEXT,"
                        + "created_at TIMESTAMPTZ NOT NULL DEFAULT now())");
                st.execute("CREATE TABLE IF NOT EXISTS saved_jobs ("
                        + "user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE, job_key TEXT NOT NULL,"
                        + "saved_at TIMESTAMPTZ NOT NULL DEFAULT now(), PRIMARY KEY (user_id, job_key))");
                st.execute("CREATE TABLE IF NOT EXISTS applications ("
                        + "id SERIAL PRIMARY KEY, user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                        + "job_key TEXT NOT NULL, job_title TEXT NOT NULL, company TEXT NOT NULL, link TEXT NOT NULL,"
                        + "note TEXT, applied_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE (user_id, job_key))");
                return;
            } catch (SQLException e) {
                last = e;
                System.err.println("Waiting for the database (attempt " + attempt + "): " + e.getMessage());
                sleep(3000);
            }
        }
        throw new IllegalStateException("Cannot connect to the database. Check DATABASE_URL. " + (last == null ? "" : last.getMessage()));
    }

    // ------------------------------------------------------------------ reading
    public Loaded load() {
        return withConnection(c -> {
            Loaded data = new Loaded();
            com.skillbridge.datastructures.MyHashTable<String, User> byId = new com.skillbridge.datastructures.MyHashTable<>();
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT id,email,password_hash,full_name,age,gender,program_code,grad_year,target_role,"
                            + "hard_skills,soft_skills,languages FROM users ORDER BY id")) {
                while (rs.next()) {
                    User u = new User(rs.getInt("id"), rs.getString("email"), rs.getString("password_hash"));
                    u.fullName = rs.getString("full_name"); u.age = rs.getInt("age");
                    u.gender = parseGender(rs.getString("gender")); u.programCode = rs.getString("program_code");
                    u.gradYear = nullToEmpty(rs.getString("grad_year")); u.targetRole = nullToEmpty(rs.getString("target_role"));
                    u.hardSkills = Text.splitList(rs.getString("hard_skills"), ";");
                    u.softSkills = Text.splitList(rs.getString("soft_skills"), ";");
                    u.languages = Text.splitList(rs.getString("languages"), ";");
                    data.users.add(u);
                    byId.put(String.valueOf(u.id), u);
                }
            }
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT user_id, job_key FROM saved_jobs ORDER BY saved_at, job_key")) {
                while (rs.next()) {
                    User u = byId.get(String.valueOf(rs.getInt("user_id")));
                    if (u != null) u.savedJobIds.add(rs.getString("job_key"));
                }
            }
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT id,user_id,job_key,job_title,company,link,note,applied_at FROM applications ORDER BY id")) {
                while (rs.next())
                    data.applications.add(new Application(rs.getInt("id"), rs.getInt("user_id"), rs.getString("job_key"),
                            rs.getString("job_title"), rs.getString("company"), rs.getString("link"),
                            nullToEmpty(rs.getString("note")), rs.getTimestamp("applied_at").toInstant().toString()));
            }
            return data;
        });
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static Gender parseGender(String g) {
        try { return Gender.valueOf(g); } catch (Exception e) { return Gender.UNDISCLOSED; }
    }

    // ------------------------------------------------------------------ writing
    public int insertUser(User u) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users(email,password_hash,full_name,age,gender,program_code,grad_year,target_role,"
                            + "hard_skills,soft_skills,languages) VALUES (?,?,?,?,?,?,?,?,?,?,?) RETURNING id")) {
                ps.setString(1, u.email); ps.setString(2, u.passwordHash); ps.setString(3, u.fullName);
                ps.setInt(4, u.age); ps.setString(5, u.gender.name()); ps.setString(6, u.programCode);
                ps.setString(7, u.gradYear); ps.setString(8, u.targetRole);
                ps.setString(9, String.join(";", u.hardSkills)); ps.setString(10, String.join(";", u.softSkills));
                ps.setString(11, String.join(";", u.languages));
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
            }
        });
    }

    public void updateUser(User u) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE users SET full_name=?, age=?, gender=?, program_code=?, grad_year=?, target_role=?,"
                            + "hard_skills=?, soft_skills=?, languages=? WHERE id=?")) {
                ps.setString(1, u.fullName); ps.setInt(2, u.age); ps.setString(3, u.gender.name());
                ps.setString(4, u.programCode); ps.setString(5, u.gradYear); ps.setString(6, u.targetRole);
                ps.setString(7, String.join(";", u.hardSkills)); ps.setString(8, String.join(";", u.softSkills));
                ps.setString(9, String.join(";", u.languages)); ps.setInt(10, u.id);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void deleteUser(int userId) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
                ps.setInt(1, userId); ps.executeUpdate(); return null;
            }
        });
    }

    public void saveJob(int userId, String jobKey) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO saved_jobs(user_id, job_key) VALUES (?,?) ON CONFLICT DO NOTHING")) {
                ps.setInt(1, userId); ps.setString(2, jobKey); ps.executeUpdate(); return null;
            }
        });
    }

    public void unsaveJob(int userId, String jobKey) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM saved_jobs WHERE user_id=? AND job_key=?")) {
                ps.setInt(1, userId); ps.setString(2, jobKey); ps.executeUpdate(); return null;
            }
        });
    }

    public int insertApplication(Application a) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO applications(user_id,job_key,job_title,company,link,note,applied_at) "
                            + "VALUES (?,?,?,?,?,?,?) RETURNING id")) {
                ps.setInt(1, a.userId); ps.setString(2, a.jobId); ps.setString(3, a.jobTitle);
                ps.setString(4, a.company); ps.setString(5, a.link); ps.setString(6, a.note);
                ps.setTimestamp(7, Timestamp.from(Instant.parse(a.appliedAt)));
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
            }
        });
    }

    public void deleteApplication(int applicationId) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM applications WHERE id=?")) {
                ps.setInt(1, applicationId); ps.executeUpdate(); return null;
            }
        });
    }
}
