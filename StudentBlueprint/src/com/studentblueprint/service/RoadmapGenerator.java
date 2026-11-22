package com.studentblueprint.service;

import com.studentblueprint.data.Database;
import com.studentblueprint.model.RequiredSkill;
import com.studentblueprint.model.RoadmapStep;
import com.studentblueprint.model.Student;

import com.studentblueprint.service.GapResult.SkillGap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// MAIN FUNCTION 2: personalized career roadmap (FR-14 to FR-17)
// puts the missing and weak skills in the career's learning order
// so the student doesnt learn React before JavaScript
public class RoadmapGenerator {

    private final Database db;
    private final SkillGapAnalyzer analyzer;
    private final AiService ai;

    public RoadmapGenerator(Database db, SkillGapAnalyzer analyzer, AiService ai) {
        this.db = db;
        this.analyzer = analyzer;
        this.ai = ai;
    }

    public List<RoadmapStep> generate(Student student) {
        if (!student.hasCareer()) {
            return new ArrayList<>();
        }
        GapResult result = analyzer.analyzeStudent(student);
        List<RoadmapStep> steps = buildSteps(student.getId(), result, db.getRequiredSkills(student.getCareerId()));
        if (steps.isEmpty()) {
            return steps; // no gaps left, so we keep the old roadmap (and its progress) as it is
        }

        // ask the AI for better tips, if it works they replace the saved ones
        String careerName = db.findCareer(student.getCareerId()).getName();
        Map<String, String> aiTips = ai.getTips(careerName, steps);
        for (RoadmapStep step : steps) {
            String tip = aiTips.get(step.getSkillName().toLowerCase());
            if (tip != null) step.setTip(tip);
        }

        db.replaceRoadmap(student.getId(), steps); // old roadmap is deleted
        db.save();
        return steps;
    }

    // builds the steps without saving, so we can test the order
    public List<RoadmapStep> buildSteps(int studentId, GapResult result, List<RequiredSkill> requiredInOrder) {
        Set<String> missing = new HashSet<>();
        for (SkillGap g : result.getMissing()) missing.add(g.getSkillName().toLowerCase());
        Set<String> improve = new HashSet<>();
        for (SkillGap g : result.getNeedsImprovement()) improve.add(g.getSkillName().toLowerCase());

        List<RoadmapStep> steps = new ArrayList<>();
        int order = 1;
        for (RequiredSkill req : requiredInOrder) {
            String name = req.getSkillName().toLowerCase();
            String gapType = missing.contains(name) ? "MISSING" : improve.contains(name) ? "IMPROVE" : null;
            if (gapType != null) { // skills the student already has are left out
                steps.add(new RoadmapStep(0, studentId, order++, req.getSkillName(), req.getRequiredLevel(),
                        gapType, req.getDefaultTip(), false));
            }
        }
        return steps;
    }
}
