package com.studentblueprint;

import com.studentblueprint.model.*;
import com.studentblueprint.security.PasswordHasher;
import com.studentblueprint.service.*;
import com.studentblueprint.service.AiService.AiAnswer;
import com.studentblueprint.service.GapResult.SkillGap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// unit tests for the backend (no server or files needed)
class BackendTest {

    private final SkillGapAnalyzer analyzer = new SkillGapAnalyzer(null);
    private final RoadmapGenerator generator = new RoadmapGenerator(null, analyzer, null);
    private final ProgressTracker tracker = new ProgressTracker(null);

    // web developer skills in the learning order from phase 1: HTML/CSS -> JavaScript -> Git/GitHub -> React
    private List<RequiredSkill> webSkills() {
        List<RequiredSkill> list = new ArrayList<>();
        list.add(new RequiredSkill(1, 2, "HTML/CSS", 2, 1, "TECHNICAL", "tip"));
        list.add(new RequiredSkill(2, 2, "JavaScript", 2, 2, "TECHNICAL", "tip"));
        list.add(new RequiredSkill(3, 2, "Git/GitHub", 2, 3, "TECHNICAL", "tip"));
        list.add(new RequiredSkill(4, 2, "React", 2, 4, "TECHNICAL", "tip"));
        return list;
    }

    // TC-01 (FR-11, FR-12): a student with no skills is missing everything
    @Test
    void noSkillsMeansEverythingIsMissing() {
        GapResult r = analyzer.compare(new ArrayList<>(), new ArrayList<>(), webSkills());
        assertEquals(0, r.getHave().size());
        assertEquals(4, r.getMissing().size());
    }

    // TC-02 (FR-12): a lower level goes to "needs improvement", the same level counts as "have"
    @Test
    void skillsAreSortedIntoTheRightGroups() {
        List<StudentSkill> skills = List.of(
                new StudentSkill(1, 1, "HTML/CSS", StudentSkill.INTERMEDIATE),
                new StudentSkill(2, 1, "javascript", StudentSkill.BEGINNER)); // lower case on purpose
        GapResult r = analyzer.compare(skills, new ArrayList<>(), webSkills());
        assertEquals(1, r.getHave().size());
        assertEquals(1, r.getNeedsImprovement().size());
        assertEquals(2, r.getMissing().size());
    }

    // TC-03 (FR-15): the roadmap follows the learning order and leaves out skills the student has
    @Test
    void roadmapFollowsTheLearningOrder() {
        List<StudentSkill> skills = List.of(new StudentSkill(1, 1, "HTML/CSS", StudentSkill.ADVANCED));
        GapResult r = analyzer.compare(skills, new ArrayList<>(), webSkills());
        List<RoadmapStep> steps = generator.buildSteps(1, r, webSkills());
        assertEquals(3, steps.size());
        assertEquals("JavaScript", steps.get(0).getSkillName());
        assertEquals("React", steps.get(2).getSkillName()); // react never comes before javascript
    }

    // TC-04 (FR-19): progress percent is right
    @Test
    void progressPercentIsCorrect() {
        List<RoadmapStep> steps = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            steps.add(new RoadmapStep(i, 1, i, "Skill " + i, 2, "MISSING", "tip", i <= 2)); // 2 of 4 done
        }
        assertEquals(50, tracker.calculatePercent(steps));
        assertEquals(0, tracker.calculatePercent(new ArrayList<>())); // empty roadmap doesnt crash
    }

    // TC-05 (NFR-15): without the AI the app still gives an explanation
    @Test
    void aiUsesFallbackWithoutKey() {
        GapResult r = new GapResult();
        r.getMissing().add(new SkillGap("React", 0, 2, "TECHNICAL"));
        AiAnswer answer = new AiService("").explainGaps("Web Developer", r);
        assertFalse(answer.isFromAi());
        assertTrue(answer.getText().contains("Web Developer"));
    }

    // TC-06 (NFR-05): passwords are hashed, never saved as plain text
    @Test
    void passwordIsHashed() {
        String saved = PasswordHasher.hash("Password123");
        assertFalse(saved.contains("Password123"));
        assertTrue(PasswordHasher.matches("Password123", saved));
        assertFalse(PasswordHasher.matches("wrong", saved));
    }
}
