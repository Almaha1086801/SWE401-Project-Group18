package com.studentblueprint.data;

import com.studentblueprint.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// our data layer. everything is saved in csv files in the data folder, one file per table:
//   careers.csv, required_skills.csv, opportunities.csv  -> starting data we wrote ourselves
//   students.csv, student_skills.csv, achievements.csv, roadmap_steps.csv -> made by the app
// we load everything into lists when the app starts and save the file again after every change
// csv rules: the first line is the header, a value with a comma, quote or new line goes inside "quotes"
public class Database {

    private final Path folder;

    private final List<Career> careers = new ArrayList<>();
    private final List<RequiredSkill> requiredSkills = new ArrayList<>();
    private final List<Opportunity> opportunities = new ArrayList<>();
    private final List<Student> students = new ArrayList<>();
    private final List<StudentSkill> studentSkills = new ArrayList<>();
    private final List<Achievement> achievements = new ArrayList<>();
    private final List<RoadmapStep> roadmapSteps = new ArrayList<>();

    public Database(Path folder) {
        this.folder = folder;
        try {
            load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the data folder " + folder.toAbsolutePath(), e);
        }
    }

    private void load() throws IOException {
        for (String[] r : readCsv(folder.resolve("careers.csv"))) {
            careers.add(new Career(num(r[0]), r[1], r[2]));
        }
        for (String[] r : readCsv(folder.resolve("required_skills.csv"))) {
            requiredSkills.add(new RequiredSkill(num(r[0]), num(r[1]), r[2], num(r[3]), num(r[4]), r[5], r[6]));
        }
        for (String[] r : readCsv(folder.resolve("opportunities.csv"))) {
            opportunities.add(new Opportunity(num(r[0]), num(r[1]), r[2], r[3], r[4]));
        }
        for (String[] r : readCsv(folder.resolve("students.csv"))) {
            students.add(new Student(num(r[0]), r[1], r[2], r[3], r[4], num(r[5]), r[6]));
        }
        for (String[] r : readCsv(folder.resolve("student_skills.csv"))) {
            studentSkills.add(new StudentSkill(num(r[0]), num(r[1]), r[2], num(r[3])));
        }
        for (String[] r : readCsv(folder.resolve("achievements.csv"))) {
            achievements.add(Achievement.create(r[2], num(r[0]), num(r[1]), r[3], r[4]));
        }
        for (String[] r : readCsv(folder.resolve("roadmap_steps.csv"))) {
            roadmapSteps.add(new RoadmapStep(num(r[0]), num(r[1]), num(r[2]), r[3], num(r[4]), r[5], r[6],
                    Boolean.parseBoolean(r[7])));
        }
    }

    // saves the 4 tables the app changes
    public void save() {
        try {
            List<String[]> rows = new ArrayList<>();
            for (Student s : students) {
                rows.add(new String[]{"" + s.getId(), s.getFullName(), s.getEmail(), s.getPasswordHash(),
                        s.getMajor(), "" + s.getCareerId(), s.getShareToken()});
            }
            writeCsv(folder.resolve("students.csv"),
                    "id,full_name,email,password_hash,major,career_id,share_token", rows);

            rows = new ArrayList<>();
            for (StudentSkill s : studentSkills) {
                rows.add(new String[]{"" + s.getId(), "" + s.getStudentId(), s.getSkillName(), "" + s.getSkillLevel()});
            }
            writeCsv(folder.resolve("student_skills.csv"), "id,student_id,skill_name,skill_level", rows);

            rows = new ArrayList<>();
            for (Achievement a : achievements) {
                rows.add(new String[]{"" + a.getId(), "" + a.getStudentId(), a.getTypeCode(), a.getTitle(), a.getExtra()});
            }
            writeCsv(folder.resolve("achievements.csv"), "id,student_id,type,title,extra", rows);

            rows = new ArrayList<>();
            for (RoadmapStep s : roadmapSteps) {
                rows.add(new String[]{"" + s.getId(), "" + s.getStudentId(), "" + s.getStepOrder(), s.getSkillName(),
                        "" + s.getTargetLevel(), s.getGapType(), s.getTip(), "" + s.isDone()});
            }
            writeCsv(folder.resolve("roadmap_steps.csv"),
                    "id,student_id,step_order,skill_name,target_level,gap_type,tip,done", rows);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the data", e);
        }
    }

    // ---------- careers, required skills, opportunities ----------

    public List<Career> getCareers() {
        return careers;
    }

    public Career findCareer(int id) {
        for (Career c : careers) {
            if (c.getId() == id) return c;
        }
        return null;
    }

    // the skills a career needs, sorted in learning order
    public List<RequiredSkill> getRequiredSkills(int careerId) {
        List<RequiredSkill> list = new ArrayList<>();
        for (RequiredSkill r : requiredSkills) {
            if (r.getCareerId() == careerId) list.add(r);
        }
        list.sort(Comparator.comparingInt(RequiredSkill::getLearningOrder));
        return list;
    }

    public List<Opportunity> getOpportunities(int careerId) {
        List<Opportunity> list = new ArrayList<>();
        for (Opportunity o : opportunities) {
            if (o.getCareerId() == careerId) list.add(o);
        }
        return list;
    }

    // ---------- students ----------

    public Student findStudentById(int id) {
        for (Student s : students) {
            if (s.getId() == id) return s;
        }
        return null;
    }

    public Student findStudentByEmail(String email) {
        for (Student s : students) {
            if (s.getEmail().equalsIgnoreCase(email)) return s;
        }
        return null;
    }

    public Student findStudentByShareToken(String token) {
        if (token == null || token.isEmpty()) return null;
        for (Student s : students) {
            if (token.equals(s.getShareToken())) return s;
        }
        return null;
    }

    public void addStudent(Student student) {
        int next = 1;
        for (Student s : students) next = Math.max(next, s.getId() + 1);
        student.setId(next);
        students.add(student);
    }

    // ---------- student skills ----------

    public List<StudentSkill> getStudentSkills(int studentId) {
        List<StudentSkill> list = new ArrayList<>();
        for (StudentSkill s : studentSkills) {
            if (s.getStudentId() == studentId) list.add(s);
        }
        list.sort(Comparator.comparing(s -> s.getSkillName().toLowerCase()));
        return list;
    }

    public StudentSkill findStudentSkill(int studentId, String skillName) {
        for (StudentSkill s : studentSkills) {
            if (s.getStudentId() == studentId && s.getSkillName().equalsIgnoreCase(skillName.trim())) return s;
        }
        return null;
    }

    public StudentSkill findStudentSkillById(int id) {
        for (StudentSkill s : studentSkills) {
            if (s.getId() == id) return s;
        }
        return null;
    }

    public void addStudentSkill(StudentSkill skill) {
        int next = 1;
        for (StudentSkill s : studentSkills) next = Math.max(next, s.getId() + 1);
        skill.setId(next);
        studentSkills.add(skill);
    }

    public void deleteStudentSkill(StudentSkill skill) {
        studentSkills.remove(skill);
    }

    // ---------- achievements ----------

    public List<Achievement> getAchievements(int studentId) {
        List<Achievement> list = new ArrayList<>();
        for (Achievement a : achievements) {
            if (a.getStudentId() == studentId) list.add(a);
        }
        return list;
    }

    public Achievement findAchievementById(int id) {
        for (Achievement a : achievements) {
            if (a.getId() == id) return a;
        }
        return null;
    }

    public void addAchievement(Achievement achievement) {
        int next = 1;
        for (Achievement a : achievements) next = Math.max(next, a.getId() + 1);
        achievement.setId(next);
        achievements.add(achievement);
    }

    public void deleteAchievement(Achievement achievement) {
        achievements.remove(achievement);
    }

    // ---------- roadmap steps ----------

    public List<RoadmapStep> getRoadmapSteps(int studentId) {
        List<RoadmapStep> list = new ArrayList<>();
        for (RoadmapStep s : roadmapSteps) {
            if (s.getStudentId() == studentId) list.add(s);
        }
        list.sort(Comparator.comparingInt(RoadmapStep::getStepOrder));
        return list;
    }

    public RoadmapStep findRoadmapStep(int id) {
        for (RoadmapStep s : roadmapSteps) {
            if (s.getId() == id) return s;
        }
        return null;
    }

    // deletes the old roadmap of this student and adds the new steps
    public void replaceRoadmap(int studentId, List<RoadmapStep> newSteps) {
        roadmapSteps.removeIf(s -> s.getStudentId() == studentId);
        int next = 1;
        for (RoadmapStep s : roadmapSteps) next = Math.max(next, s.getId() + 1);
        for (RoadmapStep s : newSteps) {
            s.setId(next++);
            roadmapSteps.add(s);
        }
    }

    private static int num(String s) {
        return s == null || s.isEmpty() ? 0 : Integer.parseInt(s.trim());
    }

    // ---------- reading and writing the csv files ----------


    // returns all rows except the header. if the file doesnt exist yet we return an empty list
    private static List<String[]> readCsv(Path file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(file)) {
            return rows;
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        List<String> row = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    value.append('"'); // "" inside quotes means one "
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    value.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                row.add(value.toString());
                value.setLength(0);
            } else if (c == '\n') {
                row.add(value.toString());
                value.setLength(0);
                rows.add(row.toArray(new String[0]));
                row = new ArrayList<>();
            } else if (c != '\r') {
                value.append(c);
            }
        }
        // last line if the file doesnt end with a new line
        if (value.length() > 0 || !row.isEmpty()) {
            row.add(value.toString());
            rows.add(row.toArray(new String[0]));
        }
        if (!rows.isEmpty()) {
            rows.remove(0); // remove the header
        }
        return rows;
    }

    private static void writeCsv(Path file, String header, List<String[]> rows) throws IOException {
        StringBuilder sb = new StringBuilder(header).append('\n');
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(escape(row[i]));
            }
            sb.append('\n');
        }
        Files.createDirectories(file.getParent());
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
