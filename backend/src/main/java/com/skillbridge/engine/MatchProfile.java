package com.skillbridge.engine;

import com.skillbridge.datastructures.MyLinkedList;

/**
 * The ONLY information about a user that the matching engine ever sees:
 * program code, target job role and hard skills.
 * Gender, age, address, contact number and everything else are left out on purpose,
 * so the engine cannot use them (gender-inclusive by design).
 */
public class MatchProfile {
    public final String programCode, targetRole;
    public final MyLinkedList<String> hardSkills;

    public MatchProfile(String programCode, String targetRole, MyLinkedList<String> hardSkills) {
        this.programCode = programCode; this.targetRole = targetRole; this.hardSkills = hardSkills;
    }
}
