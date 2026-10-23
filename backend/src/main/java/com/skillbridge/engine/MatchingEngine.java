package com.skillbridge.engine;

import com.skillbridge.datastructures.*;
import com.skillbridge.model.Job;
import com.skillbridge.model.Program;
import com.skillbridge.model.User;

/** Ties together: Hash Table -> Graph -> Scoring -> Heap / Merge Sort -> Binary Search. */
public class MatchingEngine {
    private final MyHashTable<String, Job> jobById = new MyHashTable<>();
    private final MyHashTable<String, MyLinkedList<Job>> byProgram = new MyHashTable<>();
    private final MyHashTable<String, MyLinkedList<Job>> bySkill = new MyHashTable<>();
    private final MyGraph graph = new MyGraph();
    private String[] sortedSkillIndex = new String[0];
    private boolean indexDirty = false;

    /** Index a job in the hash tables and add its skill edges to the graph. */
    public void addJob(Job job) {
        jobById.put(job.getId(), job);
        for (Program p : job.getTargetPrograms()) append(byProgram, p.name(), job);
        for (String skill : job.getRequiredSkills()) {
            append(bySkill, skill, job);
            graph.addEdge(skill, job.getId());
        }
        indexDirty = true;
    }

    public Job findJob(String id) { return jobById.get(id); }

    private void append(MyHashTable<String, MyLinkedList<Job>> t, String key, Job job) {
        MyLinkedList<Job> list = t.get(key);
        if (list == null) { list = new MyLinkedList<>(); t.put(key, list); }
        list.add(job);
    }

    /** Collect candidate jobs (graph traversal + program index) and score each. */
    private MyLinkedList<MatchResult> scoreCandidates(User user) {
        MyHashTable<String, Job> candidates = new MyHashTable<>();

        for (String skill : user.getSkills())                        // graph: skill -> jobs
            for (String jobId : graph.jobsRequiring(skill))
                candidates.put(jobId, jobById.get(jobId));

        MyLinkedList<Job> sameProgram = byProgram.get(user.getProgram().name());   // hash index
        if (sameProgram != null) for (Job j : sameProgram) candidates.put(j.getId(), j);

        MyLinkedList<MatchResult> results = new MyLinkedList<>();
        for (String id : candidates.keys()) {
            Job job = candidates.get(id);
            // NOTE: only program + skills are passed in. Gender never reaches the scorer.
            double score = ScoreCalculator.score(user.getProgram(), user.getSkills(), job);
            if (score > 0)
                results.add(new MatchResult(job, score, ScoreCalculator.countMatchedSkills(user.getSkills(), job)));
        }
        return results;
    }

    /** Top-K using a max-heap (priority queue). */
    public MyLinkedList<MatchResult> recommendTop(User user, int k) {
        MyMaxHeap<MatchResult> heap = new MyMaxHeap<>();
        for (MatchResult r : scoreCandidates(user)) heap.add(r);
        MyLinkedList<MatchResult> top = new MyLinkedList<>();
        while (!heap.isEmpty() && top.size() < k) top.add(heap.poll());
        return top;
    }

    /** Full ranked list using merge sort (highest score first). */
    public MatchResult[] rankAll(User user) {
        MyLinkedList<MatchResult> list = scoreCandidates(user);
        MatchResult[] arr = new MatchResult[list.size()];
        int i = 0;
        for (MatchResult r : list) arr[i++] = r;
        SortUtil.mergeSort(arr, true);
        return arr;
    }

    /** Is this skill required by any job? Binary search on the sorted skill index. */
    public boolean isKnownSkill(String skill) {
        if (indexDirty) rebuildSkillIndex();
        return SearchUtil.binarySearch(sortedSkillIndex, skill.trim().toLowerCase()) >= 0;
    }

    public String[] getSortedSkillIndex() {
        if (indexDirty) rebuildSkillIndex();
        return sortedSkillIndex;
    }

    /** Skills ranked by how many jobs require them (merge sort, descending). */
    public SkillDemand[] skillDemand() {
        MyLinkedList<String> keys = bySkill.keys();
        SkillDemand[] arr = new SkillDemand[keys.size()];
        int i = 0;
        for (String k : keys) arr[i++] = new SkillDemand(k, bySkill.get(k).size());
        SortUtil.mergeSort(arr, true);
        return arr;
    }

    private void rebuildSkillIndex() {
        MyLinkedList<String> keys = bySkill.keys();
        String[] arr = new String[keys.size()];
        int i = 0;
        for (String k : keys) arr[i++] = k;
        SortUtil.mergeSort(arr, false);
        sortedSkillIndex = arr;
        indexDirty = false;
    }
}
