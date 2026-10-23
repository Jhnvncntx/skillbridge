package com.skillbridge.datastructures;

/**
 * Bipartite graph: one side = skills, other side = jobs.
 * An edge means "this job requires this skill". Stored as two adjacency lists.
 */
public class MyGraph {
    private final MyHashTable<String, MyLinkedList<String>> skillToJobs = new MyHashTable<>();
    private final MyHashTable<String, MyLinkedList<String>> jobToSkills = new MyHashTable<>();

    public void addEdge(String skill, String jobId) {
        link(skillToJobs, skill, jobId);
        link(jobToSkills, jobId, skill);
    }

    private void link(MyHashTable<String, MyLinkedList<String>> table, String from, String to) {
        MyLinkedList<String> list = table.get(from);
        if (list == null) { list = new MyLinkedList<>(); table.put(from, list); }
        if (!list.contains(to)) list.add(to);
    }

    public MyLinkedList<String> jobsRequiring(String skill) {
        MyLinkedList<String> l = skillToJobs.get(skill);
        return l == null ? new MyLinkedList<>() : l;
    }

    public MyLinkedList<String> skillsOf(String jobId) {
        MyLinkedList<String> l = jobToSkills.get(jobId);
        return l == null ? new MyLinkedList<>() : l;
    }
}
