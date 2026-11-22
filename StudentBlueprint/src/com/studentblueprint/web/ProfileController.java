package com.studentblueprint.web;

import com.studentblueprint.data.Database;
import com.studentblueprint.model.*;
import com.studentblueprint.service.StudentService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// boundary class for the profile page: studies, career goal, skills, achievements, sharing
// (FR-03 to FR-09, FR-22, FR-24, FR-26, FR-27)
public class ProfileController {

    private final Database db;
    private final StudentService studentService;
    private final Page page;

    public ProfileController(Database db, StudentService studentService, Page page) {
        this.db = db;
        this.studentService = studentService;
        this.page = page;
    }

    public void show(Request r, Student s) throws IOException {
        show(r, s, "", "");
    }

    public void saveStudies(Request r, Student s) throws IOException {
        studentService.updateStudies(s, r.param("major"), r.intParam("careerId"));
        r.redirect("/profile");
    }

    public void addSkill(Request r, Student s) throws IOException {
        String error = studentService.addOrUpdateSkill(s, r.param("skillName"), r.intParam("skillLevel"));
        if (error != null) {
            show(r, s, error, "");
            return;
        }
        r.redirect("/profile");
    }

    public void deleteSkill(Request r, Student s) throws IOException {
        studentService.deleteSkill(s, r.intParam("id"));
        r.redirect("/profile");
    }

    public void addAchievement(Request r, Student s) throws IOException {
        String error = studentService.addAchievement(s, r.param("type"), r.param("title"), r.param("extra"));
        if (error != null) {
            show(r, s, "", error);
            return;
        }
        r.redirect("/profile");
    }

    public void deleteAchievement(Request r, Student s) throws IOException {
        studentService.deleteAchievement(s, r.intParam("id"));
        r.redirect("/profile");
    }

    public void toggleShare(Request r, Student s) throws IOException {
        studentService.toggleSharing(s);
        r.redirect("/profile");
    }

    // builds the profile page. skillError / achievementError are shown under that form (FR-22)
    private void show(Request r, Student s, String skillError, String achievementError) throws IOException {
        Map<String, String> v = new HashMap<>();
        v.put("name", Page.esc(s.getFullName()));
        v.put("major", Page.esc(s.getMajor()));

        // career dropdown
        StringBuilder options = new StringBuilder();
        if (!s.hasCareer()) options.append("<option value=\"\" selected disabled>Choose a career...</option>");
        for (Career c : db.getCareers()) {
            options.append("<option value=\"").append(c.getId()).append("\"")
                   .append(c.getId() == s.getCareerId() ? " selected" : "").append(">")
                   .append(Page.esc(c.getName())).append("</option>");
        }
        v.put("careerOptions", options.toString());
        Career career = db.findCareer(s.getCareerId());
        v.put("careerDescription", career == null ? "" : Page.esc(career.getDescription()));

        // skills table
        StringBuilder skills = new StringBuilder();
        for (StudentSkill sk : db.getStudentSkills(s.getId())) {
            skills.append("<tr><td>").append(Page.esc(sk.getSkillName())).append("</td><td>")
                  .append(sk.getLevelName()).append("</td><td class=\"right\">")
                  .append(deleteButton("/profile/skills/delete", sk.getId())).append("</td></tr>");
        }
        v.put("skillRows", skills.length() == 0
                ? "<tr><td colspan=\"3\" class=\"empty\">You have not added any skills yet.</td></tr>" : skills.toString());
        v.put("skillError", error(skillError));

        // suggestions while typing a skill, so the names match the career
        StringBuilder suggestions = new StringBuilder();
        for (String name : studentService.getCareerSkillNames(s)) {
            suggestions.append("<option value=\"").append(Page.esc(name)).append("\">");
        }
        v.put("suggestions", suggestions.toString());

        // achievements table
        StringBuilder rows = new StringBuilder();
        for (Achievement a : db.getAchievements(s.getId())) {
            rows.append("<tr><td><span class=\"tag\">").append(a.getTypeName()).append("</span></td><td>")
                .append(Page.esc(a.getTitle())).append("</td><td>").append(Page.esc(a.getDetails()))
                .append("</td><td class=\"right\">").append(deleteButton("/profile/achievements/delete", a.getId()))
                .append("</td></tr>");
        }
        v.put("achievementRows", rows.length() == 0
                ? "<tr><td colspan=\"4\" class=\"empty\">Nothing added yet.</td></tr>" : rows.toString());
        v.put("achievementError", error(achievementError));

        // share box (FR-24)
        if (s.isSharing()) {
            v.put("shareBox", "<p>Sharing is <b>on</b>. Send this read-only link to your advisor:</p>"
                    + "<p class=\"share-link\">http://localhost:8080/shared/" + Page.esc(s.getShareToken()) + "</p>"
                    + "<form method=\"post\" action=\"/profile/share\"><button class=\"button secondary\">Stop sharing</button></form>");
        } else {
            v.put("shareBox", "<p>Sharing is <b>off</b>. Nobody else can see your profile.</p>"
                    + "<form method=\"post\" action=\"/profile/share\"><button class=\"button secondary\">Create a share link</button></form>");
        }
        r.html(200, page.render("profile.html", v));
    }

    private String deleteButton(String action, int id) {
        return "<form method=\"post\" action=\"" + action + "\" class=\"inline\">"
                + "<input type=\"hidden\" name=\"id\" value=\"" + id + "\">"
                + "<button class=\"link-button danger\">Remove</button></form>";
    }

    private String error(String message) {
        return message.isEmpty() ? "" : "<p class=\"field-error\">" + Page.esc(message) + "</p>";
    }
}
