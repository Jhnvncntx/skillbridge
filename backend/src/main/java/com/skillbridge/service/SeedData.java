package com.skillbridge.service;

import static com.skillbridge.model.Program.*;
import com.skillbridge.model.Gender;
import com.skillbridge.model.Program;

/**
 * Simulated sample dataset (fictional companies and students), loaded on every startup.
 * Matches the paper's limitation: tested with simulated data, not live HR systems.
 */
final class SeedData {
    private SeedData() {}

    private static void job(SkillBridgeService s, String title, String company, Program[] programs, String... skills) {
        s.createJob(title, company, "seed", programs, skills);
    }

    private static Program[] p(Program... programs) { return programs; }

    static void load(SkillBridgeService s) {
        // College of Engineering and Architecture
        job(s, "Embedded Systems Engineer", "Bataan TechWorks", p(BSCPE, BSEE), "c", "microcontrollers", "circuit design", "iot");
        job(s, "Junior Architect", "BuildRight Design", p(BSARCH), "autocad", "revit", "design", "sketchup");
        job(s, "Site Engineer", "Northgate Construction", p(BSCE), "autocad", "structural analysis", "project management", "surveying");
        job(s, "Maintenance Engineer", "Peninsula Manufacturing", p(BSME), "machine design", "maintenance", "autocad", "thermodynamics");
        job(s, "Electrical Design Engineer", "Voltline Power", p(BSEE), "circuit design", "power systems", "autocad", "electrical codes");

        // College of Information and Communications Technology
        job(s, "Backend Developer", "Luzon Cloud Labs", p(BSCS, BSIT, BSCPE), "java", "sql", "data structures", "rest api");
        job(s, "Network Administrator", "Bataan NetServe", p(BSIT, BSCPE), "networking", "linux", "security", "troubleshooting");
        job(s, "Algorithm Engineer", "Pixel Logic", p(BSCS), "algorithms", "data structures", "python", "java");
        job(s, "Web Developer", "Pinoy WebWorks", p(BSIT, BSCS, BSEMC), "html", "css", "javascript", "react");
        job(s, "Junior Game Developer", "Pixelforge Studios", p(BSEMC, BSCS), "unity", "c#", "game design", "3d modeling");
        job(s, "Multimedia Artist", "Bright Frame Media", p(BSEMC), "animation", "adobe photoshop", "video editing", "3d modeling");

        // College of Business and Accountancy
        job(s, "Staff Accountant", "Mabini and Co.", p(BSA), "bookkeeping", "taxation", "excel", "financial reporting");
        job(s, "HR Assistant", "PeopleFirst Staffing", p(BSBA_HRM), "recruitment", "payroll", "communication", "labor law");
        job(s, "Marketing Associate", "BrightPath Marketing", p(BSBA_MKT, BSENTREP), "social media", "content writing", "market research", "canva");
        job(s, "Operations Analyst", "Harbor Logistics", p(BSBA_OPS), "supply chain", "excel", "inventory management", "data analysis");
        job(s, "Small Business Consultant", "Startup Bataan Hub", p(BSENTREP, BSBA_MKT), "business planning", "financial literacy", "marketing", "communication");

        // College of Education
        job(s, "High School Teacher", "Bataan Learning Academy", p(BSED), "lesson planning", "classroom management", "communication", "assessment");
        job(s, "Elementary Teacher", "Little Scholars School", p(BEED), "lesson planning", "classroom management", "child development", "communication");

        // College of Arts and Sciences
        job(s, "Guidance Associate", "Mindful Steps Center", p(BSPSYCH), "counseling", "psychological testing", "communication", "report writing");
        job(s, "Staff Nurse", "Peninsula Medical Center", p(BSN), "patient care", "vital signs", "communication", "medication administration");
        job(s, "Midwife", "Mother and Child Clinic", p(BSMIDWIFERY), "prenatal care", "patient care", "communication", "newborn care");
        job(s, "Laboratory Analyst", "Genesis Labs", p(BSSCI), "laboratory techniques", "data analysis", "research", "excel");

        // Law and Medicine
        job(s, "Legal Researcher", "Reyes Law Office", p(JD), "legal research", "legal writing", "communication");
        job(s, "Resident Physician (Entry)", "Peninsula Medical Center", p(MD), "patient care", "diagnosis", "communication");

        // Sample students
        s.createUser("Juan Dela Cruz", BSCPE, Gender.MALE, new String[]{"c", "java", "microcontrollers", "iot", "data structures"});
        s.createUser("Maria Santos", BSCS, Gender.FEMALE, new String[]{"java", "python", "algorithms", "data structures", "sql"});
        s.createUser("Carlo Reyes", BSIT, Gender.MALE, new String[]{"html", "css", "javascript", "react", "networking"});
        s.createUser("Bea Garcia", BSARCH, Gender.FEMALE, new String[]{"autocad", "revit", "design"});
        s.createUser("Ana Lopez", BSN, Gender.FEMALE, new String[]{"patient care", "vital signs", "communication"});
        s.createUser("Rico Mendoza", BSA, Gender.UNDISCLOSED, new String[]{"bookkeeping", "excel", "taxation"});
    }
}
