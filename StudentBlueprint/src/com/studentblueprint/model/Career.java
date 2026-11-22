package com.studentblueprint.model;

// a career the student can pick, like Software Engineer or Architect
// careers come from data/careers.csv so we can add more without changing code (FR-28)
public class Career {

    private int id;
    private String name;
    private String description;

    public Career(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
