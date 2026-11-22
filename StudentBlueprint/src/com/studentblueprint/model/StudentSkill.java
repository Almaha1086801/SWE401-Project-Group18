package com.studentblueprint.model;

// a skill the student has, with their level (FR-05)
public class StudentSkill {

    // we save levels as numbers because its easier to compare them
    public static final int NONE = 0;
    public static final int BEGINNER = 1;
    public static final int INTERMEDIATE = 2;
    public static final int ADVANCED = 3;

    // turns the number into a word for the pages
    public static String levelName(int level) {
        if (level == BEGINNER) return "Beginner";
        if (level == INTERMEDIATE) return "Intermediate";
        if (level == ADVANCED) return "Advanced";
        return "None";
    }

    private int id;
    private int studentId;
    private String skillName;
    private int skillLevel;

    public StudentSkill(int id, int studentId, String skillName, int skillLevel) {
        this.id = id;
        this.studentId = studentId;
        this.skillName = skillName;
        this.skillLevel = skillLevel;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getStudentId() { return studentId; }
    public String getSkillName() { return skillName; }
    public int getSkillLevel() { return skillLevel; }
    public void setSkillLevel(int skillLevel) { this.skillLevel = skillLevel; }
    public String getLevelName() { return levelName(skillLevel); }
}
