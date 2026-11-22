package com.studentblueprint.model;

// the main user of the system (one row in students.csv)
public class Student {

    private int id;
    private String fullName;
    private String email;
    private String passwordHash; // never the real password, only the hash (NFR-05)
    private String major;        // FR-26
    private int careerId;        // 0 means no career chosen yet (FR-09)
    private String shareToken;   // empty means sharing is off (FR-24)

    public Student(int id, String fullName, String email, String passwordHash,
                   String major, int careerId, String shareToken) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.major = major;
        this.careerId = careerId;
        this.shareToken = shareToken;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public int getCareerId() { return careerId; }
    public void setCareerId(int careerId) { this.careerId = careerId; }
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }

    public boolean hasCareer() { return careerId != 0; }
    public boolean isSharing() { return shareToken != null && !shareToken.isEmpty(); }
}
