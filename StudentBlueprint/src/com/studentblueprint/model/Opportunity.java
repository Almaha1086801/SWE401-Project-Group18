package com.studentblueprint.model;

// an example internship, certification or job for a career (FR-30)
public class Opportunity {

    private int id;
    private int careerId;
    private String category; // INTERNSHIP, CERTIFICATION or ENTRY_ROLE
    private String title;
    private String description;

    public Opportunity(int id, int careerId, String category, String title, String description) {
        this.id = id;
        this.careerId = careerId;
        this.category = category;
        this.title = title;
        this.description = description;
    }

    public int getId() { return id; }
    public int getCareerId() { return careerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }

    public String getCategoryName() {
        if ("INTERNSHIP".equals(category)) return "Internship";
        if ("CERTIFICATION".equals(category)) return "Certification";
        if ("ENTRY_ROLE".equals(category)) return "Entry-level role";
        return category;
    }
}
