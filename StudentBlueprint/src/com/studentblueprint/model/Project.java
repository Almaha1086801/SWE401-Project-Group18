package com.studentblueprint.model;

import java.util.ArrayList;
import java.util.List;

// a project the student did, extra = skills used, like "Java, Git/GitHub" (FR-07)
// the gap analyzer counts these skills as at least beginner
public class Project extends Achievement {

    public Project(int id, int studentId, String title, String skillsUsed) {
        super(id, studentId, title, skillsUsed);
    }

    public String getTypeCode() { return "PROJECT"; }
    public String getTypeName() { return "Project"; }
    public String getDetails() { return extra.isEmpty() ? "" : "Skills used: " + extra; }

    // "Java, Git/GitHub" -> ["Java", "Git/GitHub"]
    public List<String> getSkillList() {
        List<String> list = new ArrayList<>();
        for (String part : extra.split(",")) {
            if (!part.trim().isEmpty()) {
                list.add(part.trim());
            }
        }
        return list;
    }
}
