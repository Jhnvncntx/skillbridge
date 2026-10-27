package com.skillbridge.persistence;

import com.skillbridge.datastructures.MyLinkedList;
import com.skillbridge.model.Application;
import com.skillbridge.model.User;

/**
 * Where users, saved jobs and applications are stored. The service keeps working copies in memory
 * (our own data structures) and calls this interface FIRST whenever something changes.
 *   MemoryPersistence   - stores nothing (data is lost on restart); used when DATABASE_URL is not set
 *   PostgresPersistence - stores everything in PostgreSQL (Neon)
 */
public interface Persistence {
    /** Everything read from storage at startup. Users arrive with their saved job keys filled in. */
    class Loaded {
        public final MyLinkedList<User> users = new MyLinkedList<>();
        public final MyLinkedList<Application> applications = new MyLinkedList<>();   // oldest first
    }

    boolean isEnabled();
    void init();
    Loaded load();

    /** Stores a new user and returns the id it was given. */
    int insertUser(User u);
    void updateUser(User u);
    /** Deletes the user together with their saved jobs and applications. */
    void deleteUser(int userId);

    void saveJob(int userId, String jobKey);
    void unsaveJob(int userId, String jobKey);

    /** Stores a new application and returns the id it was given. */
    int insertApplication(Application a);
    void deleteApplication(int applicationId);

    /** Chooses the storage from the DATABASE_URL environment variable. */
    static Persistence fromEnvironment() {
        String url = System.getenv("DATABASE_URL");
        return (url == null || url.isBlank()) ? new MemoryPersistence() : new PostgresPersistence(url);
    }
}
