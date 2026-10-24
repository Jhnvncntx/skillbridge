package com.skillbridge.service;

/**
 * Demo accounts so the app can be shown right after a restart (accounts are kept in memory only).
 * Password for all: Demo1234.  Turn off with the environment variable SEED_DEMO=false.
 */
final class DemoData {
    private DemoData() {}

    static void load(SkillBridgeService s) {
        String pw = "Demo1234";
        s.addDemoUser("cpe@demo.skillbridge", pw, "Juan Dela Cruz", 21, "CPE", "Embedded Systems Engineer",
                new String[]{"c++", "python", "embedded c", "pcb design"}, new String[]{"teamwork", "problem solving"},
                new String[]{"english", "filipino"});
        s.addDemoUser("cs@demo.skillbridge", pw, "Maria Santos", 22, "CS-SD", "Backend Developer",
                new String[]{"java", "spring boot", "sql", "git"}, new String[]{"communication"},
                new String[]{"english", "filipino"});
        s.addDemoUser("nurse@demo.skillbridge", pw, "Ana Lopez", 23, "BSN", "Staff Nurse",
                new String[]{"bls", "patient care"}, new String[]{"empathy"}, new String[]{"english", "filipino"});
    }
}
