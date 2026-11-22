package com.studentblueprint.model;

// one step of the student's roadmap (FR-14 to FR-18)
public class RoadmapStep {

    private int id;
    private int studentId;
    private int stepOrder;
    private String skillName;
    private int targetLevel;
    private String gapType; // MISSING or IMPROVE
    private String tip;
    private boolean done;

    public RoadmapStep(int id, int studentId, int stepOrder, String skillName, int targetLevel,
                       String gapType, String tip, boolean done) {
        this.id = id;
        this.studentId = studentId;
        this.stepOrder = stepOrder;
        this.skillName = skillName;
        this.targetLevel = targetLevel;
        this.gapType = gapType;
        this.tip = tip;
        this.done = done;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getStudentId() { return studentId; }
    public int getStepOrder() { return stepOrder; }
    public String getSkillName() { return skillName; }
    public int getTargetLevel() { return targetLevel; }
    public String getTargetLevelName() { return StudentSkill.levelName(targetLevel); }
    public String getGapType() { return gapType; }
    public String getTip() { return tip; }
    public void setTip(String tip) { this.tip = tip; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
}
