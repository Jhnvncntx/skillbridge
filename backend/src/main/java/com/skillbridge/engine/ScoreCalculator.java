package com.skillbridge.engine;

import com.skillbridge.datastructures.MyLinkedList;
import com.skillbridge.model.Job;
import com.skillbridge.model.Program;

/**
 * Relevance score out of 100: up to 60 for skill coverage + 40 for exact program match.
 *
 * GENDER-BLIND BY DESIGN: this method only receives a Program and a skill list.
 * It has no access to the User object, so gender cannot influence the score.
 */
public class ScoreCalculator {
    public static final double SKILL_WEIGHT = 60.0, PROGRAM_WEIGHT = 40.0;

    public static int countMatchedSkills(MyLinkedList<String> userSkills, Job job) {
        int matched = 0;
        for (String req : job.getRequiredSkills()) if (userSkills.contains(req)) matched++;
        return matched;
    }

    public static double score(Program userProgram, MyLinkedList<String> userSkills, Job job) {
        int req = job.getRequiredSkills().length;
        double skillPart = req == 0 ? 0 : SKILL_WEIGHT * countMatchedSkills(userSkills, job) / req;
        double programPart = job.targets(userProgram) ? PROGRAM_WEIGHT : 0;
        return skillPart + programPart;
    }
}
