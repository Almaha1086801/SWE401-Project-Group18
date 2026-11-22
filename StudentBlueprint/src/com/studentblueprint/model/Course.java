package com.studentblueprint.model;

// a course the student finished, extra = the grade (FR-06, FR-26)
public class Course extends Achievement {

    public Course(int id, int studentId, String title, String grade) {
        super(id, studentId, title, grade);
    }

    public String getTypeCode() { return "COURSE"; }
    public String getTypeName() { return "Course"; }
    public String getDetails() { return extra.isEmpty() ? "" : "Grade: " + extra; }
}
