package com.skillbridge.model;

import com.skillbridge.datastructures.MyLinkedList;

/**
 * A registered job seeker. Age and gender are stored for the profile and anonymous statistics only.
 * They are never passed to the matching engine. (Address and contact number are deliberately not collected.)
 */
public class User {
    public int id;
    public final String email;
    public final String passwordHash;

    public String fullName, programCode, gradYear, targetRole;
    public int age;
    public Gender gender;
    public String[] hardSkills = new String[0], softSkills = new String[0], languages = new String[0];
    public final MyLinkedList<String> savedJobIds = new MyLinkedList<>();

    public User(int id, String email, String passwordHash) {
        this.id = id; this.email = email; this.passwordHash = passwordHash;
    }

    /** A copy of the editable profile, so changes can be saved to the database before they touch memory. */
    public User copy() {
        User c = new User(id, email, passwordHash);
        c.takeProfileFrom(this);
        return c;
    }

    public void takeProfileFrom(User o) {
        fullName = o.fullName; age = o.age; gender = o.gender; programCode = o.programCode;
        gradYear = o.gradYear; targetRole = o.targetRole;
        hardSkills = o.hardSkills; softSkills = o.softSkills; languages = o.languages;
    }

    /** Hard skills as a linked list (the form the scorer uses). */
    public MyLinkedList<String> hardSkillList() {
        MyLinkedList<String> l = new MyLinkedList<>();
        for (String s : hardSkills) l.add(s);
        return l;
    }
}
