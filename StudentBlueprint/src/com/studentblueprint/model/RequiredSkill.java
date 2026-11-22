package com.studentblueprint.model;

// one skill a career needs, the level needed and the order to learn it in
public class RequiredSkill {

    private int id;
    private int careerId;
    private String skillName;
    private int requiredLevel;
    private int learningOrder; // 1 = learn first
    private String skillType;  // TECHNICAL or SOFT (FR-27)
    private String defaultTip; // used when the AI is not working (NFR-15)

    public RequiredSkill(int id, int careerId, String skillName, int requiredLevel,
                         int learningOrder, String skillType, String defaultTip) {
        this.id = id;
        this.careerId = careerId;
        this.skillName = skillName;
        this.requiredLevel = requiredLevel;
        this.learningOrder = learningOrder;
        this.skillType = skillType;
        this.defaultTip = defaultTip;
    }

    public int getId() { return id; }
    public int getCareerId() { return careerId; }
    public String getSkillName() { return skillName; }
    public int getRequiredLevel() { return requiredLevel; }
    public int getLearningOrder() { return learningOrder; }
    public String getSkillType() { return skillType; }
    public String getDefaultTip() { return defaultTip; }
}
