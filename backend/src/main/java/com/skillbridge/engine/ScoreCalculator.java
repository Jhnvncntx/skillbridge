package com.skillbridge.engine;

import com.skillbridge.datastructures.MyLinkedList;
import com.skillbridge.model.Job;
import com.skillbridge.util.Text;

/**
 * Relevance score out of 100:
 *   50  target job role  - the user's target role equals the job's standard role
 *   30  hard skills      - 30 x (job skills the user has / job skills required)
 *   20  degree program   - the job targets the user's program or major
 *
 * To change how matching works, edit the three constants below. Soft skills, languages and
 * graduation year could be added here later as extra components.
 *
 * GENDER-INCLUSIVE BY DESIGN: this method receives only a program code, a role and a skill list.
 * It has no access to gender, age, address or any other personal detail.
 */
public final class ScoreCalculator {
    public static final double ROLE_POINTS = 50, SKILL_POINTS = 30, PROGRAM_POINTS = 20;

    private ScoreCalculator() {}

    public static ScoreBreakdown breakdown(String[] programCodes, String targetRole, MyLinkedList<String> hardSkills, Job job) {
        MyLinkedList<String> matched = new MyLinkedList<>(), missing = new MyLinkedList<>();
        for (String required : job.requiredSkills) {
            if (hardSkills.contains(required)) matched.add(required); else missing.add(required);
        }
        int requiredCount = job.requiredSkills.length;
        double skillPart = requiredCount == 0 ? 0 : SKILL_POINTS * matched.size() / requiredCount;

        String wanted = Text.norm(targetRole);
        double rolePart = !wanted.isEmpty() && wanted.equals(Text.norm(job.role)) ? ROLE_POINTS : 0;
        double programPart = job.targetsAny(programCodes) ? PROGRAM_POINTS : 0;

        return new ScoreBreakdown(rolePart, skillPart, programPart, Text.toArray(matched), Text.toArray(missing));
    }
}
