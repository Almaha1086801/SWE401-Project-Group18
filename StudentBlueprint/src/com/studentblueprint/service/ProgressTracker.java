package com.studentblueprint.service;

import com.studentblueprint.data.Database;
import com.studentblueprint.model.RoadmapStep;
import com.studentblueprint.model.Student;
import com.studentblueprint.model.StudentSkill;

import java.util.List;

// MAIN FUNCTION 3: progress tracking (FR-18 to FR-21)
// "mark done" saves the step as done and raises the skill in the profile,
// so the next gap analysis uses the new level (reassessment)
public class ProgressTracker {

    private final Database db;

    public ProgressTracker(Database db) {
        this.db = db;
    }

    // returns false if the step doesnt exist or belongs to another student (FR-23)
    public boolean markDone(Student student, int stepId) {
        RoadmapStep step = db.findRoadmapStep(stepId);
        if (step == null || step.getStudentId() != student.getId()) {
            return false;
        }
        step.setDone(true);
        StudentSkill skill = db.findStudentSkill(student.getId(), step.getSkillName());
        if (skill == null) {
            db.addStudentSkill(new StudentSkill(0, student.getId(), step.getSkillName(), step.getTargetLevel()));
        } else if (skill.getSkillLevel() < step.getTargetLevel()) {
            skill.setSkillLevel(step.getTargetLevel());
        }
        db.save();
        return true;
    }

    public int countDone(List<RoadmapStep> steps) {
        int done = 0;
        for (RoadmapStep s : steps) {
            if (s.isDone()) done++;
        }
        return done;
    }

    // for the progress bar (FR-19)
    public int calculatePercent(List<RoadmapStep> steps) {
        return steps.isEmpty() ? 0 : Math.round(countDone(steps) * 100f / steps.size());
    }
}
