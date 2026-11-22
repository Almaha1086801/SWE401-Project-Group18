package com.studentblueprint.service;

import com.studentblueprint.data.Database;
import com.studentblueprint.model.*;
import com.studentblueprint.service.GapResult.SkillGap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// MAIN FUNCTION 1: skill gap analyzer (FR-10, FR-11, FR-12)
// compares the student's skills, projects, courses and certifications with the skills the career needs.
// the sorting is normal code, not AI, so the result is always the same and we can test it.
// the AI only explains the result afterwards (see AiService)
public class SkillGapAnalyzer {

    private final Database db;

    public SkillGapAnalyzer(Database db) {
        this.db = db;
    }

    public GapResult analyzeStudent(Student student) {
        if (!student.hasCareer()) {
            return new GapResult();
        }
        return compare(db.getStudentSkills(student.getId()),
                db.getAchievements(student.getId()),
                db.getRequiredSkills(student.getCareerId()));
    }

    // the actual comparison, no database needed so its easy to test
    public GapResult compare(List<StudentSkill> skills, List<Achievement> achievements, List<RequiredSkill> required) {
        // 1. skill name (lower case) -> student level
        Map<String, Integer> levels = new HashMap<>();
        for (StudentSkill s : skills) {
            levels.put(clean(s.getSkillName()), s.getSkillLevel());
        }
        // 2. a skill used in a project counts as beginner if the student didnt add it themselves
        for (Achievement a : achievements) {
            if (a instanceof Project) {
                for (String skill : ((Project) a).getSkillList()) {
                    levels.putIfAbsent(clean(skill), StudentSkill.BEGINNER);
                }
            }
        }
        // 3. titles of courses and certifications, e.g. "SQL for Data Analysis" shows some SQL
        List<String> titles = new ArrayList<>();
        for (Achievement a : achievements) {
            if (a instanceof Course || a instanceof Certification) {
                titles.add(clean(a.getTitle()));
            }
        }

        // 4. put every required skill in the right group
        GapResult result = new GapResult();
        for (RequiredSkill req : required) {
            int level = levels.getOrDefault(clean(req.getSkillName()), StudentSkill.NONE);
            if (level == StudentSkill.NONE) {
                for (String title : titles) {
                    if (title.contains(clean(req.getSkillName()))) {
                        level = StudentSkill.BEGINNER; // a course or certificate about it counts as beginner
                        break;
                    }
                }
            }
            SkillGap gap = new SkillGap(req.getSkillName(), level, req.getRequiredLevel(), req.getSkillType());
            if (level == StudentSkill.NONE) {
                result.getMissing().add(gap);
            } else if (level < req.getRequiredLevel()) {
                result.getNeedsImprovement().add(gap);
            } else {
                result.getHave().add(gap);
            }
        }
        return result;
    }

    private String clean(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}
