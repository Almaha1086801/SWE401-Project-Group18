package com.studentblueprint.service;

import com.studentblueprint.data.Database;
import com.studentblueprint.model.*;
import com.studentblueprint.security.PasswordHasher;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

// everything about the account and the profile:
// register and login (FR-01, FR-02), profile (FR-03 to FR-09, FR-26, FR-27),
// checking input (FR-22) and sharing with an advisor (FR-24)
// the check methods return an error message, or null if the input is ok
public class StudentService {

    private final Database db;

    public StudentService(Database db) {
        this.db = db;
    }

    // ---------- register and login ----------

    public List<String> checkRegister(String name, String email, String password, String confirm) {
        List<String> errors = new ArrayList<>();
        if (name.isBlank()) errors.add("Please enter your full name");
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) errors.add("Please enter a valid email address");
        else if (db.findStudentByEmail(email.trim()) != null) errors.add("An account with this email already exists");
        // NFR-05: at least 12 characters with a letter, a number and a symbol
        if (password.length() < 12 || password.length() > 64
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9].*")) {
            errors.add("Password must be at least 12 characters with a letter, a number and a symbol");
        } else if (!password.equals(confirm)) {
            errors.add("Passwords do not match");
        }
        return errors;
    }

    public Student register(String name, String email, String password) {
        Student s = new Student(0, name.trim(), email.trim().toLowerCase(), PasswordHasher.hash(password), "", 0, "");
        db.addStudent(s);
        db.save();
        return s;
    }

    // returns the student if email and password are right, otherwise null
    public Student login(String email, String password) {
        Student s = db.findStudentByEmail(email.trim());
        if (s != null && PasswordHasher.matches(password, s.getPasswordHash())) {
            return s;
        }
        return null;
    }

    // ---------- studies and career goal ----------

    public void updateStudies(Student student, String major, int careerId) {
        student.setMajor(major.length() > 100 ? major.substring(0, 100) : major.trim());
        if (db.findCareer(careerId) != null) {
            // new career goal = the old roadmap doesnt fit anymore, so we remove it
            // and the student makes a new one on the plan page
            if (careerId != student.getCareerId()) {
                db.replaceRoadmap(student.getId(), new ArrayList<>());
            }
            student.setCareerId(careerId);
        }
        db.save();
    }

    // ---------- skills ----------

    public String addOrUpdateSkill(Student student, String name, int level) {
        if (name.isBlank()) return "Please enter a skill name";
        if (name.length() > 60) return "Skill name is too long";
        if (level < 1 || level > 3) return "Please choose a level";

        StudentSkill existing = db.findStudentSkill(student.getId(), name);
        if (existing != null) {
            existing.setSkillLevel(level); // already there, just change the level
        } else {
            db.addStudentSkill(new StudentSkill(0, student.getId(), name.trim(), level));
        }
        db.save();
        return null;
    }

    public void deleteSkill(Student student, int skillId) {
        StudentSkill s = db.findStudentSkillById(skillId);
        if (s != null && s.getStudentId() == student.getId()) { // only your own skills (FR-23)
            db.deleteStudentSkill(s);
            db.save();
        }
    }

    // the career's skill names, shown as suggestions so the names match
    public List<String> getCareerSkillNames(Student student) {
        List<String> names = new ArrayList<>();
        for (RequiredSkill r : db.getRequiredSkills(student.getCareerId())) {
            names.add(r.getSkillName());
        }
        return names;
    }

    // ---------- courses, projects, certifications, experience ----------

    public String addAchievement(Student student, String type, String title, String extra) {
        if (!type.matches("COURSE|PROJECT|CERTIFICATION|EXPERIENCE")) return "Please choose a type";
        if (title.isBlank()) return "Please enter a title";
        if (title.length() > 100 || extra.length() > 200) return "Text is too long";

        // if they add the same type and title again we update it instead of adding it twice
        for (Achievement a : db.getAchievements(student.getId())) {
            if (a.getTypeCode().equals(type) && a.getTitle().equalsIgnoreCase(title.trim())) {
                a.setExtra(extra.trim());
                db.save();
                return null;
            }
        }
        db.addAchievement(Achievement.create(type, 0, student.getId(), title.trim(), extra.trim()));
        db.save();
        return null;
    }

    public void deleteAchievement(Student student, int achievementId) {
        Achievement a = db.findAchievementById(achievementId);
        if (a != null && a.getStudentId() == student.getId()) {
            db.deleteAchievement(a);
            db.save();
        }
    }

    // ---------- sharing with advisor ----------

    // on: make a random link token. off: delete it, so the old link stops working
    public void toggleSharing(Student student) {
        if (student.isSharing()) {
            student.setShareToken("");
        } else {
            byte[] bytes = new byte[18];
            new SecureRandom().nextBytes(bytes);
            student.setShareToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        }
        db.save();
    }
}
