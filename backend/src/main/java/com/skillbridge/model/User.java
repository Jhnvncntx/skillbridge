package com.skillbridge.model;

import com.skillbridge.datastructures.MyLinkedList;

public class User {
    private final int id;
    private final String name;
    private final Program program;
    private final Gender gender;          // analytics only - never used for matching
    private MyLinkedList<String> skills = new MyLinkedList<>();

    public User(int id, String name, Program program, Gender gender) {
        this.id = id; this.name = name; this.program = program; this.gender = gender;
    }

    /** Replaces the whole skill list (input is expected to be normalized + de-duplicated). */
    public void replaceSkills(String[] newSkills) {
        MyLinkedList<String> list = new MyLinkedList<>();
        for (String s : newSkills) if (!list.contains(s)) list.add(s);
        skills = list;
    }

    public String[] skillsArray() {
        String[] arr = new String[skills.size()];
        int i = 0;
        for (String s : skills) arr[i++] = s;
        return arr;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Program getProgram() { return program; }
    public Gender getGender() { return gender; }
    public MyLinkedList<String> getSkills() { return skills; }
}
