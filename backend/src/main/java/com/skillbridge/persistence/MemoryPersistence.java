package com.skillbridge.persistence;

import com.skillbridge.model.Application;
import com.skillbridge.model.User;

/** Stores nothing. Hands out ids so the app works exactly as before when no database is configured. */
public class MemoryPersistence implements Persistence {
    private int nextUserId = 1, nextApplicationId = 1;

    public boolean isEnabled() { return false; }
    public void init() {}
    public Loaded load() { return new Loaded(); }
    public int insertUser(User u) { return nextUserId++; }
    public void updateUser(User u) {}
    public void deleteUser(int userId) {}
    public void saveJob(int userId, String jobKey) {}
    public void unsaveJob(int userId, String jobKey) {}
    public int insertApplication(Application a) { return nextApplicationId++; }
    public void deleteApplication(int applicationId) {}
}
