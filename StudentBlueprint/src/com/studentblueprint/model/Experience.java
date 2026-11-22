package com.studentblueprint.model;

// an internship or job, extra = where (FR-26)
public class Experience extends Achievement {

    public Experience(int id, int studentId, String title, String organisation) {
        super(id, studentId, title, organisation);
    }

    public String getTypeCode() { return "EXPERIENCE"; }
    public String getTypeName() { return "Experience"; }
    public String getDetails() { return extra.isEmpty() ? "" : "At: " + extra; }
}
