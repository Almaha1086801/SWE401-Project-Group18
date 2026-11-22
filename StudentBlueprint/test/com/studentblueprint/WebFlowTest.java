package com.studentblueprint;

import com.studentblueprint.web.WebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// integration tests: start the real server with a copy of the data and use it like a browser
class WebFlowTest {

    private WebServer server;
    private String url;
    private Path dataCopy;

    @BeforeEach
    void start() throws Exception {
        // copy only the starting data, so the tests never touch our real student files
        dataCopy = Files.createTempDirectory("blueprint");
        for (String f : new String[]{"careers.csv", "required_skills.csv", "opportunities.csv"}) {
            Files.copy(Path.of("data", f), dataCopy.resolve(f));
        }
        server = new WebServer(dataCopy, Path.of("web"), "", 0); // port 0 = any free port, no AI key
        server.start();
        url = "http://localhost:" + server.getPort();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    // a "browser" that keeps its cookie and doesnt follow redirects, so we can check them
    private HttpClient browser() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private HttpResponse<String> get(HttpClient b, String path) throws Exception {
        return b.send(HttpRequest.newBuilder(URI.create(url + path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    // form = "name=value" pairs
    private HttpResponse<String> post(HttpClient b, String path, String... form) throws Exception {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < form.length; i += 2) {
            if (body.length() > 0) body.append('&');
            body.append(form[i]).append('=').append(URLEncoder.encode(form[i + 1], StandardCharsets.UTF_8));
        }
        return b.send(HttpRequest.newBuilder(URI.create(url + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpClient registerAndLogin(String email) throws Exception {
        HttpClient b = browser();
        post(b, "/register", "fullName", "Test Student", "email", email, "password", "Password123!", "confirmPassword", "Password123!");
        HttpResponse<String> login = post(b, "/login", "email", email, "password", "Password123!");
        assertEquals("/profile", login.headers().firstValue("Location").orElse(""));
        return b;
    }

    // TC-07 (FR-02, FR-23): you cant see pages without logging in
    @Test
    void pagesNeedLogin() throws Exception {
        HttpResponse<String> r = get(browser(), "/profile");
        assertEquals(303, r.statusCode());
        assertEquals("/login", r.headers().firstValue("Location").orElse(""));
    }

    // TC-08: the 3 use cases together: register -> see the gap -> generate roadmap -> mark done
    @Test
    void fullStudentFlow() throws Exception {
        HttpClient b = registerAndLogin("flow@test.com");
        post(b, "/profile/studies", "major", "Software Engineering", "careerId", "1");
        post(b, "/profile/skills", "skillName", "Programming", "skillLevel", "1");

        // use case 1: the skill gap is on the plan page
        HttpResponse<String> plan = get(b, "/plan");
        assertTrue(plan.body().contains("You are 0% ready for this career (0 of 10 skills)."));

        // use case 2: generate the roadmap, it shows on the same page
        post(b, "/plan/generate");
        plan = get(b, "/plan");
        assertTrue(plan.body().contains("0 of 10 steps done"));

        // use case 3: mark the first step done
        String id = plan.body().split("name=\"id\" value=\"")[1].split("\"")[0];
        post(b, "/plan/done", "id", id);
        assertTrue(get(b, "/plan").body().contains("10% complete"));
        assertTrue(get(b, "/profile").body().contains("<td>Programming</td><td>Intermediate"));
    }

}
