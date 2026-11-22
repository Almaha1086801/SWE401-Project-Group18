package com.studentblueprint.web;

import com.studentblueprint.data.Database;
import com.studentblueprint.model.*;
import com.studentblueprint.service.ProgressTracker;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// boundary class for the read only page the advisor sees (FR-24)
// no login needed, but the link only works while the student has sharing on
public class ShareController {

    private final Database db;
    private final ProgressTracker tracker;
    private final Page page;

    public ShareController(Database db, ProgressTracker tracker, Page page) {
        this.db = db;
        this.tracker = tracker;
        this.page = page;
    }

    public void show(Request r, String token) throws IOException {
        Student s = db.findStudentByShareToken(token);
        if (s == null) {
            r.html(404, page.message("Link not available",
                    "This link is wrong or the student has stopped sharing their profile.", "/login", "Go to login"));
            return;
        }
        Map<String, String> v = new HashMap<>();
        v.put("name", Page.esc(s.getFullName()));
        v.put("major", s.getMajor().isEmpty() ? "Not given" : Page.esc(s.getMajor()));
        v.put("career", s.hasCareer() ? Page.esc(db.findCareer(s.getCareerId()).getName()) : "Not chosen yet");

        StringBuilder skills = new StringBuilder();
        for (StudentSkill sk : db.getStudentSkills(s.getId())) {
            skills.append("<tr><td>").append(Page.esc(sk.getSkillName())).append("</td><td>")
                  .append(sk.getLevelName()).append("</td></tr>");
        }
        v.put("skillRows", skills.length() == 0 ? "<tr><td colspan=\"2\" class=\"empty\">No skills added.</td></tr>" : skills.toString());

        StringBuilder ach = new StringBuilder();
        for (Achievement a : db.getAchievements(s.getId())) {
            ach.append("<tr><td>").append(a.getTypeName()).append("</td><td>").append(Page.esc(a.getTitle()))
               .append("</td><td>").append(Page.esc(a.getDetails())).append("</td></tr>");
        }
        v.put("achievementRows", ach.length() == 0 ? "<tr><td colspan=\"3\" class=\"empty\">Nothing added.</td></tr>" : ach.toString());

        List<RoadmapStep> steps = db.getRoadmapSteps(s.getId());
        StringBuilder progress = new StringBuilder();
        if (steps.isEmpty()) {
            progress.append("<p class=\"empty\">The student has not made a roadmap yet.</p>");
        } else {
            int percent = tracker.calculatePercent(steps);
            progress.append("<div class=\"progress\"><div class=\"progress-fill\" style=\"width:").append(percent)
                    .append("%\"></div></div><p>").append(percent).append("% complete (").append(tracker.countDone(steps))
                    .append(" of ").append(steps.size()).append(" steps)</p><ol>");
            for (RoadmapStep st : steps) {
                progress.append("<li>").append(Page.esc(st.getSkillName())).append(" <span class=\"small\">")
                        .append(st.isDone() ? "(done)" : "(not done yet)").append("</span></li>");
            }
            progress.append("</ol>");
        }
        v.put("progress", progress.toString());
        r.html(200, page.render("shared.html", v));
    }
}
