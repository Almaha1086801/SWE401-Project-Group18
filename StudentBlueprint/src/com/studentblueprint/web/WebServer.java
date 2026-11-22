package com.studentblueprint.web;

import com.studentblueprint.data.Database;
import com.studentblueprint.model.Student;
import com.studentblueprint.security.SessionManager;
import com.studentblueprint.service.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

// the web server (uses the HttpServer that comes with java, no framework)
// it creates all the objects and sends every request to the right controller
public class WebServer {

    private final HttpServer server;
    private final Database db;
    private final SessionManager sessions = new SessionManager();
    private final Page page;
    private final AuthController auth;
    private final ProfileController profile;
    private final PlanController plan;
    private final ShareController share;

    public WebServer(Path dataFolder, Path webFolder, String aiKey, int port) throws IOException {
        // data layer -> services -> controllers (the 3 layers from our architecture)
        db = new Database(dataFolder);
        page = new Page(webFolder);
        AiService ai = new AiService(aiKey);
        StudentService students = new StudentService(db);
        SkillGapAnalyzer analyzer = new SkillGapAnalyzer(db);
        RoadmapGenerator generator = new RoadmapGenerator(db, analyzer, ai);
        ProgressTracker tracker = new ProgressTracker(db);

        auth = new AuthController(students, sessions, page);
        profile = new ProfileController(db, students, page);
        plan = new PlanController(db, analyzer, generator, tracker, ai, page);
        share = new ShareController(db, tracker, page);

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
    }

    public void start() { server.start(); }
    public void stop() { server.stop(0); }
    public int getPort() { return server.getAddress().getPort(); }

    // the router: looks at the url and calls the right controller method
    private void handle(HttpExchange exchange) throws IOException {
        try {
            Request r = new Request(exchange);
            String path = r.path();

            // pages anyone can open
            if (path.equals("/style.css")) {
                r.send(200, "text/css", page.readFile("style.css"));
            } else if (path.equals("/")) {
                r.redirect("/profile");
            } else if (path.equals("/login")) {
                auth.login(r);
            } else if (path.equals("/register")) {
                auth.register(r);
            } else if (path.equals("/logout") && r.isPost()) {
                auth.logout(r);
            } else if (path.startsWith("/shared/")) {
                share.show(r, path.substring("/shared/".length()));
            } else {
                // everything else needs login (FR-02, FR-23)
                Student s = db.findStudentById(sessions.getStudentId(r.cookie("SESSION")));
                if (s == null) {
                    r.redirect("/login");
                    return;
                }
                route(r, path, s);
            }
        } catch (IOException e) {
            // usually the browser closed or reloaded the page before we finished
            System.out.println("Problem sending " + exchange.getRequestURI().getPath() + ": " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (IOException ignored) {
                // response already started
            }
        } finally {
            exchange.close();
        }
    }

    private void route(Request r, String path, Student s) throws IOException {
        // forms that change data must be POST
        boolean post = r.isPost();
        if (path.equals("/profile")) profile.show(r, s);
        else if (post && path.equals("/profile/studies")) profile.saveStudies(r, s);
        else if (post && path.equals("/profile/skills")) profile.addSkill(r, s);
        else if (post && path.equals("/profile/skills/delete")) profile.deleteSkill(r, s);
        else if (post && path.equals("/profile/achievements")) profile.addAchievement(r, s);
        else if (post && path.equals("/profile/achievements/delete")) profile.deleteAchievement(r, s);
        else if (post && path.equals("/profile/share")) profile.toggleShare(r, s);
        else if (path.equals("/plan")) plan.show(r, s);
        else if (post && path.equals("/plan/generate")) plan.generate(r, s);
        else if (post && path.equals("/plan/done")) plan.markDone(r, s);
        else r.html(404, page.message("Page not found", "This page does not exist.", "/profile", "Go to my profile"));
    }
}
