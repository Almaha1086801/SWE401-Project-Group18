package com.studentblueprint.model;

// a certification, extra = who gave it (FR-08)
public class Certification extends Achievement {

    public Certification(int id, int studentId, String title, String issuer) {
        super(id, studentId, title, issuer);
    }

    public String getTypeCode() { return "CERTIFICATION"; }
    public String getTypeName() { return "Certification"; }
    public String getDetails() { return extra.isEmpty() ? "" : "Issued by: " + extra; }
}
