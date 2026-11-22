package com.studentblueprint.web;

import com.studentblueprint.data.Database;
import com.studentblueprint.model.Opportunity;
import com.studentblueprint.model.RoadmapStep;
import com.studentblueprint.model.Student;
import com.studentblueprint.service.*;
import com.studentblueprint.service.AiService.AiAnswer;

import com.studentblueprint.service.GapResult.SkillGap;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// boundary class for the "My plan" page
// the skill gap and the roadmap are on the same page so the student sees
// what they are missing and the steps to fix it in one place
// use case 1: Analyze Skill Gap (FR-10 to FR-13)
// use case 2: Generate Career Roadmap (FR-14 to FR-17)
// use case 3: Update Progress (FR-18 to FR-21)
// also shows the opportunities (FR-30) and the AI notice (FR-25)
public class PlanController {

    private final Database db;
    private final SkillGapAnalyzer analyzer;
    private final RoadmapGenerator generator;
    private final ProgressTracker tracker;
    private final AiService ai;
    private final Page page;

    // asking the AI can take a few seconds, so we remember its explanation and only ask again
    // when the student generates or reassesses the roadmap. this keeps "mark done" fast
    // key = "studentId:careerId", so changing the career asks the AI again
    private final Map<String, String> savedExplanations = new HashMap<>();
    // when the AI didnt answer, we wait 1 minute before trying again so the page stays fast
    private final Map<String, Long> lastFailedTry = new HashMap<>();
    private static final long RETRY_AFTER_MS = 60 * 1000;

    public PlanController(Database db, SkillGapAnalyzer analyzer, RoadmapGenerator generator,
                          ProgressTracker tracker, AiService ai, Page page) {
        this.db = db;
        this.analyzer = analyzer;
        this.generator = generator;
        this.tracker = tracker;
        this.ai = ai;
        this.page = page;
    }

    // the "generate my roadmap" button
    public void generate(Request r, Student s) throws IOException {
        generator.generate(s);
        // reassessment: forget the old explanation so the AI explains the new situation
        String key = s.getId() + ":" + s.getCareerId();
        savedExplanations.remove(key);
        lastFailedTry.remove(key);
        r.redirect("/plan");
    }

    // the "mark done" button on a step
    public void markDone(Request r, Student s) throws IOException {
        int id = r.intParam("id");
        tracker.markDone(s, id);
        // the #step-.. at the end makes the browser jump back to the same step, not the top of the page
        r.redirect("/plan#step-" + id);
    }

    public void show(Request r, Student s) throws IOException {
        // cant make a plan without a career goal
        if (!s.hasCareer()) {
            r.html(200, page.message("My plan", "Please choose a career goal on your profile first.",
                    "/profile", "Go to my profile"));
            return;
        }
        String career = db.findCareer(s.getCareerId()).getName();

        // ---------- part 1: the skill gap ----------
        GapResult result = analyzer.analyzeStudent(s);        // compare (normal code)
        AiAnswer explanation = explain(s, career, result);     // the AI explains it

        Map<String, String> v = new HashMap<>();
        v.put("careerName", Page.esc(career));
        v.put("haveCount", "" + result.getHave().size());
        v.put("improveCount", "" + result.getNeedsImprovement().size());
        v.put("missingCount", "" + result.getMissing().size());
        v.put("havePercent", "" + result.getHavePercent());
        v.put("improvePercent", "" + result.getImprovePercent());
        v.put("missingPercent", "" + result.getMissingPercent());
        v.put("readyText", "You are " + result.getHavePercent() + "% ready for this career ("
                + result.getHave().size() + " of " + result.getTotal() + " skills).");
        v.put("explanation", Page.esc(explanation.getText()));
        v.put("aiNote", explanation.isFromAi() ? "Explanation written by AI. It updates when you generate or reassess your roadmap."
                : ""); // no note when its our standard summary
        v.put("haveList", list(result.getHave(), "have"));
        v.put("improveList", list(result.getNeedsImprovement(), "improve"));
        v.put("missingList", list(result.getMissing(), "missing"));

        // ---------- part 2: the roadmap ----------
        v.put("roadmap", roadmap(s, result));

        // ---------- part 3: opportunities ----------
        StringBuilder opps = new StringBuilder();
        for (Opportunity o : db.getOpportunities(s.getCareerId())) {
            opps.append("<li><span class=\"tag\">").append(o.getCategoryName()).append("</span> <b>")
                .append(Page.esc(o.getTitle())).append("</b><p class=\"small\">")
                .append(Page.esc(o.getDescription())).append("</p></li>");
        }
        v.put("opportunities", opps.toString());

        r.html(200, page.render("plan.html", v));
    }

    // gets the AI explanation, but doesnt call the AI on every page load (see savedExplanations above)
    private AiAnswer explain(Student s, String career, GapResult result) {
        // all skills done, so we dont need the AI (and the old saved explanation is out of date)
        if (result.hasNoGaps()) {
            return ai.fallbackExplanation(career, result);
        }
        String key = s.getId() + ":" + s.getCareerId();
        if (savedExplanations.containsKey(key)) {
            return new AiAnswer(savedExplanations.get(key), true);
        }
        Long failedAt = lastFailedTry.get(key);
        boolean canTry = failedAt == null || System.currentTimeMillis() - failedAt > RETRY_AFTER_MS;
        if (canTry) {
            AiAnswer answer = ai.explainGaps(career, result);
            if (answer.isFromAi()) {
                savedExplanations.put(key, answer.getText());
                lastFailedTry.remove(key);
            } else {
                lastFailedTry.put(key, System.currentTimeMillis());
            }
            return answer;
        }
        // the AI failed less than a minute ago, dont make the student wait again
        return ai.fallbackExplanation(career, result);
    }

    // builds the roadmap part of the page
    private String roadmap(Student s, GapResult result) {
        List<RoadmapStep> steps = db.getRoadmapSteps(s.getId());
        StringBuilder c = new StringBuilder();

        if (steps.isEmpty() && result.hasNoGaps()) {
            // nothing left to learn for this career
            c.append("<section class=\"card\"><h2>You have all the skills for this career!</h2>")
             .append("<p>Great work. Add new skills or pick another career goal on your profile to get a new roadmap.</p></section>");
            return c.toString();
        }
        if (steps.isEmpty()) {
            // they have gaps but didnt make a roadmap yet
            c.append("<section class=\"card\"><h2>My roadmap</h2><p>Turn your skill gaps into an ordered plan.</p>")
             .append("<form method=\"post\" action=\"/plan/generate\"><button class=\"button big\">Generate my roadmap</button></form></section>");
            return c.toString();
        }

        int done = tracker.countDone(steps);
        int percent = tracker.calculatePercent(steps);

        // progress report (FR-19)
        c.append("<section class=\"card\"><h2>My roadmap</h2><div class=\"progress\"><div class=\"progress-fill\" style=\"width:")
         .append(percent).append("%\"></div></div><p><b>").append(percent).append("% complete</b> &middot; ")
         .append(done).append(" of ").append(steps.size()).append(" steps done &middot; ")
         .append(steps.size() - done).append(" steps left</p>");

        // the steps in the learning order
        c.append("<ol class=\"steps\">");
        for (RoadmapStep st : steps) {
            c.append("<li id=\"step-").append(st.getId()).append("\"").append(st.isDone() ? " class=\"done\"" : "").append("><div class=\"step-head\">")
             .append("<span class=\"step-num\">").append(st.getStepOrder()).append("</span><b>")
             .append(Page.esc(st.getSkillName())).append("</b>")
             // the missing/improve tag only shows on steps that are not done yet
             .append(st.isDone() ? "" : st.getGapType().equals("MISSING") ? "<span class=\"tag missing\">missing</span>"
                     : "<span class=\"tag improve\">improve</span>")
             .append("<span class=\"small\">reach ").append(st.getTargetLevelName()).append("</span></div>")
             .append("<p class=\"tip\">").append(Page.esc(st.getTip())).append("</p>");
            if (st.isDone()) {
                c.append("<p class=\"done-label\">&#10003; Done</p>");
            } else {
                c.append("<form method=\"post\" action=\"/plan/done\"><input type=\"hidden\" name=\"id\" value=\"")
                 .append(st.getId()).append("\"><button class=\"button small-button\">Mark done</button></form>");
            }
            c.append("</li>");
        }
        c.append("</ol>");

        // roadmap finished and nothing left to learn, so reassessing wouldnt change anything
        if (result.hasNoGaps()) {
            c.append("<p><b>You finished your roadmap!</b> To keep going, pick another career goal or add new skills on ")
             .append("<a href=\"/profile\">your profile</a>, then come back here to make a new roadmap.</p></section>");
            return c.toString();
        }

        // reassess after changing the profile (FR-20, FR-21)
        c.append("<p class=\"small\">Changed your skills or career? Make a new roadmap from your current profile. ")
         .append("Finished skills will not appear again.</p>")
         .append("<form method=\"post\" action=\"/plan/generate\"><button class=\"button secondary\">Reassess and make a new roadmap</button></form></section>");
        return c.toString();
    }

    // one of the 3 skill lists (have / improve / missing)
    private String list(List<SkillGap> gaps, String group) {
        if (gaps.isEmpty()) return "<li class=\"empty\">None</li>";
        StringBuilder sb = new StringBuilder();
        for (SkillGap g : gaps) {
            String level = group.equals("have") ? g.getStudentLevelName()
                    : group.equals("improve") ? g.getStudentLevelName() + " &rarr; needs " + g.getRequiredLevelName()
                    : "needs " + g.getRequiredLevelName();
            sb.append("<li><b>").append(Page.esc(g.getSkillName())).append("</b> <span class=\"small\">")
              .append(level).append("</span>")
              .append("SOFT".equals(g.getSkillType()) ? " <span class=\"tag soft\">soft skill</span>" : "")
              .append("</li>");
        }
        return sb.toString();
    }
}
