package com.skillbridge.datastructures;

/**
 * Bipartite graph: one side = skills, other side = jobs.
 * An edge means "this job requires this skill". Stored as two adjacency lists.
 *
 * bfs() walks outward from a set of skills:
 *   skill (depth 0) -> jobs needing it (depth 1) -> other skills of those jobs (depth 2)
 *   -> jobs needing those related skills (depth 3).
 */
public class MyGraph {
    /** A job reached by the BFS and how many edges away it is (1 = direct skill match, 3 = related skills). */
    public static class Hit {
        public final String jobId;
        public final int depth;
        Hit(String jobId, int depth) { this.jobId = jobId; this.depth = depth; }
    }

    private static class Node {
        final String key; final boolean isSkill; final int depth;
        Node(String key, boolean isSkill, int depth) { this.key = key; this.isSkill = isSkill; this.depth = depth; }
    }

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

    /** Breadth-first search from the given skills, up to maxDepth edges. Each job is reported once, at its shallowest depth. */
    public MyLinkedList<Hit> bfs(Iterable<String> startSkills, int maxDepth) {
        MyLinkedList<Hit> hits = new MyLinkedList<>();
        MyQueue<Node> queue = new MyQueue<>();
        MyHashTable<String, Boolean> seenSkills = new MyHashTable<>();
        MyHashTable<String, Boolean> seenJobs = new MyHashTable<>();

        for (String s : startSkills) {
            if (seenSkills.get(s) == null) { seenSkills.put(s, true); queue.enqueue(new Node(s, true, 0)); }
        }
        while (!queue.isEmpty()) {
            Node n = queue.dequeue();
            if (n.depth >= maxDepth) continue;
            if (n.isSkill) {
                for (String jobId : jobsRequiring(n.key)) {
                    if (seenJobs.get(jobId) != null) continue;
                    seenJobs.put(jobId, true);
                    hits.add(new Hit(jobId, n.depth + 1));
                    queue.enqueue(new Node(jobId, false, n.depth + 1));
                }
            } else {
                for (String skill : skillsOf(n.key)) {
                    if (seenSkills.get(skill) != null) continue;
                    seenSkills.put(skill, true);
                    queue.enqueue(new Node(skill, true, n.depth + 1));
                }
            }
        }
        return hits;
    }
}
