package com.skillbridge.model;

import com.skillbridge.datastructures.MyLinkedList;

/**
 * A registered job seeker. Personal details (age, address, contact, gender) are stored for the
 * profile only. They are never passed to the matching engine.
 */
public class User {
    public final int id;
    public final String email;
    public final String passwordHash;

    public String fullName, contact, address, programCode, gradYear, targetRole;
    public int age;
    public Gender gender;
    public String[] hardSkills = new String[0], softSkills = new String[0], languages = new String[0];
    public final MyLinkedList<String> savedJobIds = new MyLinkedList<>();

    public User(int id, String email, String passwordHash) {
        this.id = id; this.email = email; this.passwordHash = passwordHash;
    }

    /** Hard skills as a linked list (the form the scorer uses). */
    public MyLinkedList<String> hardSkillList() {
        MyLinkedList<String> l = new MyLinkedList<>();
        for (String s : hardSkills) l.add(s);
        return l;
    }
}
