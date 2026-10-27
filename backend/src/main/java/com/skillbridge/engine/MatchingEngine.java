package com.skillbridge.engine;

import com.skillbridge.datastructures.*;
import com.skillbridge.model.Job;
import com.skillbridge.util.Text;

/**
 * The matching engine. Where each data structure is used:
 *   MyHashTable  - jobs by id / program / role / skill, and the keyword index
 *   MyGraph+BFS  - skills <-> jobs; BFS (with MyQueue) finds jobs reachable through related skills
 *   MyMaxHeap    - top-K "Recommended for you"
 *   SortUtil     - merge sort for full ranked lists and the sorted indexes
 *   SearchUtil   - binary search / lower bound for skill and keyword prefixes
 */
public class MatchingEngine {
    private final MyLinkedList<Job> allJobs = new MyLinkedList<>();
    private final MyHashTable<String, Job> jobById = new MyHashTable<>();
    private final MyHashTable<String, MyLinkedList<Job>> byProgram = new MyHashTable<>();
    private final MyHashTable<String, MyLinkedList<Job>> byRole = new MyHashTable<>();
    private final MyHashTable<String, MyLinkedList<Job>> bySkill = new MyHashTable<>();
    private final MyHashTable<String, MyLinkedList<String>> tokenIndex = new MyHashTable<>();   // word -> job ids
    private final MyHashTable<String, Boolean> knownSkills = new MyHashTable<>();
    private final MyGraph graph = new MyGraph();

    private String[] sortedSkills = new String[0], sortedTokens = new String[0];
    private boolean indexDirty = false;

    // ------------------------------------------------------------------ building the indexes
    public void addKnownSkills(String[] skills) {
        for (String s : skills) knownSkills.put(Text.norm(s), true);
        indexDirty = true;
    }

    public void addJob(Job job) {
        allJobs.add(job);
        jobById.put(job.id, job);
        for (String code : job.programCodes) append(byProgram, code, job);
        append(byRole, Text.norm(job.role), job);
        for (String skill : job.requiredSkills) {
            append(bySkill, skill, job);
            graph.addEdge(skill, job.id);
            knownSkills.put(skill, true);
        }
        indexText(job.id, job.title); indexText(job.id, job.role);
        indexText(job.id, job.company); indexText(job.id, job.location);
        for (String skill : job.requiredSkills) indexText(job.id, skill);
        indexDirty = true;
    }

    private void append(MyHashTable<String, MyLinkedList<Job>> table, String key, Job job) {
        MyLinkedList<Job> list = table.get(key);
        if (list == null) { list = new MyLinkedList<>(); table.put(key, list); }
        list.add(job);
    }

    private void indexText(String jobId, String text) {
        for (String token : Text.tokens(text)) {
            MyLinkedList<String> ids = tokenIndex.get(token);
            if (ids == null) { ids = new MyLinkedList<>(); tokenIndex.put(token, ids); }
            if (!ids.contains(jobId)) ids.add(jobId);
        }
    }

    private void rebuildIndexes() {
        sortedSkills = Text.toArray(knownSkills.keys());
        SortUtil.mergeSort(sortedSkills, false);
        sortedTokens = Text.toArray(tokenIndex.keys());
        SortUtil.mergeSort(sortedTokens, false);
        indexDirty = false;
    }

    public Job findJob(String id) { return jobById.get(id); }
    public MyLinkedList<Job> allJobs() { return allJobs; }
    public int jobCount() { return allJobs.size(); }

    private MatchResult result(MatchProfile p, Job job, int distance) {
        return new MatchResult(job, ScoreCalculator.breakdown(p.programCodes, p.targetRole, p.hardSkills, job), distance);
    }

    private static MatchResult[] toArray(MyLinkedList<MatchResult> list) {
        MatchResult[] arr = new MatchResult[list.size()];
        int i = 0;
        for (MatchResult r : list) arr[i++] = r;
        return arr;
    }

    // ------------------------------------------------------------------ "Recommended for you"
    /** Jobs for the user's own program or target role, best score first (max-heap, top K). */
    public MatchResult[] recommend(MatchProfile p, int k) {
        MyHashTable<String, Job> candidates = new MyHashTable<>();
        for (String code : p.programCodes) {
            MyLinkedList<Job> sameProgram = byProgram.get(code);
            if (sameProgram != null) for (Job j : sameProgram) candidates.put(j.id, j);
        }
        MyLinkedList<Job> sameRole = byRole.get(Text.norm(p.targetRole));
        if (sameRole != null) for (Job j : sameRole) candidates.put(j.id, j);

        MyMaxHeap<MatchResult> heap = new MyMaxHeap<>();
        for (String id : candidates.keys()) heap.add(result(p, candidates.get(id), 0));

        MyLinkedList<MatchResult> top = new MyLinkedList<>();
        while (!heap.isEmpty() && top.size() < k) top.add(heap.poll());
        return toArray(top);
    }

    // ------------------------------------------------------------------ "You might also qualify to"
    /** Jobs reached by BFS from the user's skills (direct matches first, then related skills), skipping `exclude`. */
    public MatchResult[] alsoQualify(MatchProfile p, int k, MyHashTable<String, Boolean> exclude) {
        MyLinkedList<MatchResult> found = new MyLinkedList<>();
        for (MyGraph.Hit hit : graph.bfs(p.hardSkills, 3)) {
            if (exclude.get(hit.jobId) != null) continue;
            found.add(result(p, jobById.get(hit.jobId), hit.depth));
        }
        MatchResult[] arr = toArray(found);
        SortUtil.mergeSort(arr, true);
        if (arr.length <= k) return arr;
        MatchResult[] top = new MatchResult[k];
        for (int i = 0; i < k; i++) top[i] = arr[i];
        return top;
    }

    // ------------------------------------------------------------------ full list for the Jobs page
    /** Every job with its score for this user, highest first (merge sort). */
    public MatchResult[] rankAll(MatchProfile p) {
        MatchResult[] arr = new MatchResult[allJobs.size()];
        int i = 0;
        for (Job j : allJobs) arr[i++] = result(p, j, 0);
        SortUtil.mergeSort(arr, true);
        return arr;
    }

    // ------------------------------------------------------------------ keyword search
    /** Words are matched by prefix using binary search on the sorted word list. Jobs matching more words rank first. */
    public MatchResult[] search(MatchProfile p, String query) {
        String[] words = Text.tokens(query);
        if (words.length == 0) return new MatchResult[0];
        if (indexDirty) rebuildIndexes();

        MyHashTable<String, Integer> wordsMatched = new MyHashTable<>();     // job id -> how many query words it matched
        for (String word : words) {
            MyHashTable<String, Boolean> idsForWord = new MyHashTable<>();
            for (int i = SearchUtil.lowerBound(sortedTokens, word); i < sortedTokens.length && sortedTokens[i].startsWith(word); i++)
                for (String id : tokenIndex.get(sortedTokens[i])) idsForWord.put(id, true);
            for (String id : idsForWord.keys()) {
                Integer n = wordsMatched.get(id);
                wordsMatched.put(id, n == null ? 1 : n + 1);
            }
        }
        MyLinkedList<MatchResult> found = new MyLinkedList<>();
        for (String id : wordsMatched.keys()) found.add(result(p, jobById.get(id), words.length - wordsMatched.get(id)));
        MatchResult[] arr = toArray(found);
        SortUtil.mergeSort(arr, true);
        return arr;
    }

    // ------------------------------------------------------------------ skills
    public boolean isKnownSkill(String skill) {
        if (indexDirty) rebuildIndexes();
        return SearchUtil.binarySearch(sortedSkills, Text.norm(skill)) >= 0;
    }

    /** Skill autocomplete: binary search to the first match, then walk forward while the prefix still fits. */
    public String[] suggestSkills(String prefix, int limit) {
        if (indexDirty) rebuildIndexes();
        String p = Text.norm(prefix);
        MyLinkedList<String> out = new MyLinkedList<>();
        for (int i = SearchUtil.lowerBound(sortedSkills, p); i < sortedSkills.length && out.size() < limit
                && sortedSkills[i].startsWith(p); i++) out.add(sortedSkills[i]);
        return Text.toArray(out);
    }

    /** Skills ranked by how many jobs require them (merge sort, highest first). */
    public SkillDemand[] skillDemand() {
        MyLinkedList<String> keys = bySkill.keys();
        SkillDemand[] arr = new SkillDemand[keys.size()];
        int i = 0;
        for (String k : keys) arr[i++] = new SkillDemand(k, bySkill.get(k).size());
        SortUtil.mergeSort(arr, true);
        return arr;
    }
}
