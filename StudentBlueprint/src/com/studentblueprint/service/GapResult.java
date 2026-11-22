package com.studentblueprint.service;

import com.studentblueprint.model.StudentSkill;

import java.util.ArrayList;
import java.util.List;

// the result of the gap analysis in 3 groups (FR-12)
public class GapResult {

    private final List<SkillGap> have = new ArrayList<>();
    private final List<SkillGap> needsImprovement = new ArrayList<>();
    private final List<SkillGap> missing = new ArrayList<>();

    public List<SkillGap> getHave() { return have; }
    public List<SkillGap> getNeedsImprovement() { return needsImprovement; }
    public List<SkillGap> getMissing() { return missing; }

    public int getTotal() {
        return have.size() + needsImprovement.size() + missing.size();
    }

    // percentages for the coloured bar, they always add up to 100
    public int getHavePercent() { return percent(have.size()); }
    public int getImprovePercent() { return percent(needsImprovement.size()); }
    public int getMissingPercent() {
        return getTotal() == 0 ? 0 : 100 - getHavePercent() - getImprovePercent();
    }

    private int percent(int count) {
        return getTotal() == 0 ? 0 : Math.round(count * 100f / getTotal());
    }

    public boolean hasNoGaps() {
        return needsImprovement.isEmpty() && missing.isEmpty();
    }

    // one line of the gap analysis result, e.g. Git/GitHub: student level 1, needs level 2
    public static class SkillGap {
    
        private String skillName;
        private int studentLevel;
        private int requiredLevel;
        private String skillType;
    
        public SkillGap(String skillName, int studentLevel, int requiredLevel, String skillType) {
            this.skillName = skillName;
            this.studentLevel = studentLevel;
            this.requiredLevel = requiredLevel;
            this.skillType = skillType;
        }
    
        public String getSkillName() { return skillName; }
        public int getStudentLevel() { return studentLevel; }
        public int getRequiredLevel() { return requiredLevel; }
        public String getSkillType() { return skillType; }
        public String getStudentLevelName() { return StudentSkill.levelName(studentLevel); }
        public String getRequiredLevelName() { return StudentSkill.levelName(requiredLevel); }
    }
}
