package com.skillbridge.model;

/** Every BPSU Main Campus program is its own category (not grouped by college). */
public enum Program {
    JD("Juris Doctor", "College of Law"),
    MD("Doctor of Medicine", "College of Medicine"),
    BSCE("BS Civil Engineering", "College of Engineering and Architecture"),
    BSME("BS Mechanical Engineering", "College of Engineering and Architecture"),
    BSCPE("BS Computer Engineering", "College of Engineering and Architecture"),
    BSEE("BS Electrical Engineering", "College of Engineering and Architecture"),
    BSARCH("BS Architecture", "College of Engineering and Architecture"),
    BSCS("BS Computer Science", "College of Information and Communications Technology"),
    BSIT("BS Information Technology", "College of Information and Communications Technology"),
    BSEMC("BS Entertainment and Multimedia Computing", "College of Information and Communications Technology"),
    BSA("BS Accountancy", "College of Business and Accountancy"),
    BSBA_HRM("BSBA - Human Resource Management", "College of Business and Accountancy"),
    BSBA_MKT("BSBA - Marketing Management", "College of Business and Accountancy"),
    BSBA_OPS("BSBA - Operations Management", "College of Business and Accountancy"),
    BSENTREP("BS Entrepreneurship", "College of Business and Accountancy"),
    BSED("Bachelor of Secondary Education", "College of Education"),
    BEED("Bachelor of Elementary Education", "College of Education"),
    BSPSYCH("BS Psychology", "College of Arts and Sciences"),
    BSMIDWIFERY("BS Midwifery", "College of Arts and Sciences"),
    BSN("BS Nursing", "College of Arts and Sciences"),
    BSSCI("BS Science programs", "College of Arts and Sciences");

    private final String displayName, college;
    Program(String displayName, String college) { this.displayName = displayName; this.college = college; }
    public String getDisplayName() { return displayName; }
    public String getCollege() { return college; }
}
