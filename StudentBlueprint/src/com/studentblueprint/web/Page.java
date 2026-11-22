package com.studentblueprint.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

// loads an html file from the web folder and fills in the {{placeholders}}
// anything the user typed must go through esc() first, so nobody can put their own html/script in a page
public class Page {

    private final Path folder;

    public Page(Path folder) {
        this.folder = folder;
    }

    public String render(String file, Map<String, String> values) throws IOException {
        String html = Files.readString(folder.resolve(file), StandardCharsets.UTF_8);
        for (Map.Entry<String, String> e : values.entrySet()) {
            html = html.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        return html;
    }

    public String readFile(String file) throws IOException {
        return Files.readString(folder.resolve(file), StandardCharsets.UTF_8);
    }

    // turns < > & " ' into safe html codes
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    // a simple page with a title, a message and a button, used for "not found" and similar
    public String message(String title, String message, String link, String linkText) throws IOException {
        return render("message.html", Map.of("title", esc(title), "message", esc(message),
                "link", link, "linkText", esc(linkText)));
    }
}
