package com.studentblueprint.web;

import com.studentblueprint.model.Student;
import com.studentblueprint.security.SessionManager;
import com.studentblueprint.service.StudentService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// boundary class for the register, login and logout pages (FR-01, FR-02, FR-22, NFR-06)
public class AuthController {

    private final StudentService studentService;
    private final SessionManager sessions;
    private final Page page;

    public AuthController(StudentService studentService, SessionManager sessions, Page page) {
        this.studentService = studentService;
        this.sessions = sessions;
        this.page = page;
    }

    public void login(Request r) throws IOException {
        if (r.isPost()) {
            String email = r.param("email").trim();
            if (sessions.isLocked(email)) {
                showLogin(r, "<p class=\"alert error\">Too many wrong attempts. Please try again in 15 minutes.</p>");
                return;
            }
            Student s = studentService.login(email, r.param("password"));
            if (s != null) {
                sessions.loginWorked(email);
                // HttpOnly = javascript cant read it, SameSite = other websites cant use it
                r.setCookie("SESSION=" + sessions.create(s.getId()) + "; Path=/; HttpOnly; SameSite=Lax");
                r.redirect("/profile");
                return;
            }
            sessions.wrongPassword(email);
            showLogin(r, "<p class=\"alert error\">Wrong email or password, please try again.</p>");
            return;
        }
        String message = "";
        if (r.param("done").equals("logout")) message = "<p class=\"alert success\">You have been logged out.</p>";
        if (r.param("done").equals("registered")) message = "<p class=\"alert success\">Account created! You can log in now.</p>";
        showLogin(r, message);
    }

    private void showLogin(Request r, String message) throws IOException {
        r.html(200, page.render("login.html", Map.of("message", message)));
    }

    public void register(Request r) throws IOException {
        if (!r.isPost()) {
            showRegister(r, List.of(), "", "");
            return;
        }
        String name = r.param("fullName"), email = r.param("email");
        List<String> errors = studentService.checkRegister(name, email, r.param("password"), r.param("confirmPassword"));
        if (!errors.isEmpty()) {
            showRegister(r, errors, name, email); // show the form again with the errors
            return;
        }
        studentService.register(name, email, r.param("password"));
        r.redirect("/login?done=registered");
    }

    private void showRegister(Request r, List<String> errors, String name, String email) throws IOException {
        StringBuilder html = new StringBuilder();
        for (String e : errors) {
            html.append("<p class=\"field-error\">").append(Page.esc(e)).append("</p>");
        }
        Map<String, String> values = new HashMap<>();
        values.put("errors", html.toString());
        values.put("fullName", Page.esc(name));
        values.put("email", Page.esc(email));
        r.html(200, page.render("register.html", values));
    }

    public void logout(Request r) throws IOException {
        String token = r.cookie("SESSION");
        if (token != null) sessions.remove(token);
        r.setCookie("SESSION=; Path=/; Max-Age=0");
        r.redirect("/login?done=logout");
    }
}
