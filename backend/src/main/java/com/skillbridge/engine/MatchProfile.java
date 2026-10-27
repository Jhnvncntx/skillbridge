package com.skillbridge.engine;

import com.skillbridge.datastructures.MyLinkedList;

/**
 * The ONLY information about a user that the matching engine ever sees:
 * program code, target job role and hard skills.
 * Gender, age, address, contact number and everything else are left out on purpose,
 * so the engine cannot use them (gender-inclusive by design).
 */
public class MatchProfile {
    public final String[] programCodes;      // the student's program, plus its base program when it is a major
    public final String targetRole;
    public final MyLinkedList<String> hardSkills;

    public MatchProfile(String[] programCodes, String targetRole, MyLinkedList<String> hardSkills) {
        this.programCodes = programCodes; this.targetRole = targetRole; this.hardSkills = hardSkills;
    }
}
