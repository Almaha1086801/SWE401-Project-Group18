package com.studentblueprint;

import com.studentblueprint.web.WebServer;

import java.nio.file.Path;

//Student Blueprint for project SWE401 Group 18
//run this file then open http://localhost:8080 in the browser
public class Main {

    public static void main(String[] args) throws Exception {
        String aiKey = System.getenv("GEMINI_API_KEY");

        WebServer server = new WebServer(Path.of("data"), Path.of("web"), aiKey, 8080);
        server.start();
        System.out.println("Student Blueprint is running: open http://localhost:8080");
        System.out.println(aiKey == null || aiKey.isBlank()
                ? "No GEMINI_API_KEY set, using the saved tips instead of the AI"
                : "Gemini AI is on");
    }
}
