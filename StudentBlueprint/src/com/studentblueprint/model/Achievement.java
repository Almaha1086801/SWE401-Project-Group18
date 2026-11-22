package com.studentblueprint.model;

// parent class for Course, Project, Certification and Experience (generalization)
// they all have a title, and one extra field that means something different for each type
// FR-06, FR-07, FR-08, FR-26
public abstract class Achievement {

    private int id;
    private int studentId;
    private String title;
    protected String extra;

    public Achievement(int id, int studentId, String title, String extra) {
        this.id = id;
        this.studentId = studentId;
        this.title = title;
        this.extra = extra == null ? "" : extra;
    }

    // the word saved in the csv file: COURSE, PROJECT, CERTIFICATION or EXPERIENCE
    public abstract String getTypeCode();

    // the word shown on the page
    public abstract String getTypeName();

    // the extra info shown on the page, like "Grade: A"
    public abstract String getDetails();

    // makes the right child class from the type word
    public static Achievement create(String type, int id, int studentId, String title, String extra) {
        if ("PROJECT".equals(type)) return new Project(id, studentId, title, extra);
        if ("CERTIFICATION".equals(type)) return new Certification(id, studentId, title, extra);
        if ("EXPERIENCE".equals(type)) return new Experience(id, studentId, title, extra);
        return new Course(id, studentId, title, extra);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getStudentId() { return studentId; }
    public String getTitle() { return title; }
    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
}
