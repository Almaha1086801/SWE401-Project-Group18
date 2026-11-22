package com.studentblueprint.service;

import com.studentblueprint.model.RoadmapStep;
import com.studentblueprint.service.GapResult.SkillGap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// talks to the Google Gemini API using java's built in HttpClient
// used for: 1. explaining the gap result (FR-13)  2. a learning tip per roadmap step (FR-16)
// we only send skill names, levels and the career name, never the student's name or email (NFR-08)
// if there is no key, no internet or no answer in 10 seconds we use the fallback (NFR-15)
public class AiService {

    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    // if google says a model is busy (503) we try the next one in this list
    private static final String[] MODELS = {"gemini-flash-latest", "gemini-3.6-flash", "gemini-3.5-flash-lite"};

    private final String apiKey;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    // the key comes from the GEMINI_API_KEY environment variable so its never in the code
    public AiService(String apiKey) {
        this.apiKey = apiKey;
    }

    public AiAnswer explainGaps(String careerName, GapResult result) {
        String prompt = "You are a friendly career guide for university students. "
                + "A student wants to become a " + careerName + ". "
                + "Skills they have: " + names(result.getHave()) + ". "
                + "Skills to improve: " + names(result.getNeedsImprovement()) + ". "
                + "Skills missing: " + names(result.getMissing()) + ". "
                + "In 3 to 4 short sentences explain what this means and what to focus on first. "
                + "Simple words, no markdown, no bullet points.";
        String answer = ask(prompt);
        if (answer != null) {
            return new AiAnswer(answer.trim(), true);
        }
        return fallbackExplanation(careerName, result);
    }

    // the standard summary we show when the AI is not used, no internet needed so its instant
    public AiAnswer fallbackExplanation(String careerName, GapResult result) {
        // nothing left to learn, so dont tell them to generate a roadmap
        if (result.hasNoGaps()) {
            return new AiAnswer("You have all " + result.getTotal() + " skills needed for " + careerName
                    + ". Well done! To keep growing, pick another career goal or add new skills on your profile.", false);
        }
        return new AiAnswer("You already have " + result.getHave().size() + " of the " + result.getTotal()
                + " skills needed for " + careerName + ". " + result.getNeedsImprovement().size()
                + " skills need more practice and " + result.getMissing().size() + " skills are missing. "
                + "Generate your roadmap to see which skill to start with.", false);
    }

    // returns skill name (lower case) -> tip. empty map if the AI didnt work
    public Map<String, String> getTips(String careerName, List<RoadmapStep> steps) {
        if (steps.isEmpty()) {
            return new HashMap<>();
        }
        StringBuilder prompt = new StringBuilder("A university student wants to become a " + careerName + ". "
                + "Give one short practical learning suggestion (one sentence, a type of course, certification "
                + "or project) for each skill: ");
        for (RoadmapStep s : steps) {
            prompt.append(s.getSkillName()).append(" (reach ").append(s.getTargetLevelName()).append(" level); ");
        }
        prompt.append("Answer with one line per skill in this format: Skill name: suggestion. No markdown.");
        String answer = ask(prompt.toString());
        return answer == null ? new HashMap<>() : parseTips(answer);
    }

    // "React: build a to-do app" lines -> map. public so we can test it
    public static Map<String, String> parseTips(String text) {
        Map<String, String> tips = new HashMap<>();
        for (String line : text.split("\n")) {
            String l = line.trim();
            while (l.startsWith("-") || l.startsWith("*")) {
                l = l.substring(1).trim(); // the AI sometimes adds bullets anyway
            }
            int colon = l.indexOf(':');
            if (colon <= 0) continue;
            String skill = l.substring(0, colon).replace("*", "").trim().toLowerCase();
            String tip = l.substring(colon + 1).trim();
            if (!tip.isEmpty()) {
                tips.put(skill, tip.length() > 900 ? tip.substring(0, 900) : tip);
            }
        }
        return tips;
    }

    // sends the prompt to gemini, returns the answer text or null if anything goes wrong
    // all the tries together must finish in 10 seconds (NFR-15)
    private String ask(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        long deadline = System.currentTimeMillis() + 10_000;
        String body = "{\"contents\":[{\"parts\":[{\"text\":" + quote(prompt) + "}]}]}";

        for (String model : MODELS) {
            long timeLeft = deadline - System.currentTimeMillis();
            if (timeLeft < 500) {
                break; // out of time, use the fallback
            }
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(URL + model + ":generateContent"))
                        .timeout(Duration.ofMillis(timeLeft))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status == 200) {
                    // the answer is the first "text" value in the json
                    String text = firstString(response.body(), "text");
                    if (text != null && !text.isBlank()) {
                        return text;
                    }
                } else if (status == 400 || status == 401 || status == 403) {
                    System.out.println("Gemini rejected the request (" + status + "), check the API key. Using fallback");
                    return null; // a key problem wont be fixed by another model
                } else {
                    System.out.println("Gemini model " + model + " returned " + status + ", trying the next one");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                System.out.println("Gemini model " + model + " failed (" + e.getMessage() + ")");
            }
        }
        System.out.println("AI not available, using fallback");
        return null;
    }

    private String names(List<SkillGap> gaps) {
        if (gaps.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        for (SkillGap g : gaps) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(g.getSkillName()).append(" (").append(g.getStudentLevelName())
              .append(", needs ").append(g.getRequiredLevelName()).append(")");
        }
        return sb.toString();
    }

    // ---------- small json helpers so we dont need a json library ----------


    // "hello "you"" -> "\"hello \\\"you\\\"\""
    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    // finds the first "key": "value" in the json and returns value, or null
    private static String firstString(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        i = json.indexOf(':', i);
        if (i < 0) return null;
        i = json.indexOf('"', i);
        if (i < 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int j = i + 1; j < json.length(); j++) {
            char c = json.charAt(j);
            if (c == '"') return sb.toString();
            if (c == '\\' && j + 1 < json.length()) {
                char n = json.charAt(++j);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': break;
                    case 'u':
                        sb.append((char) Integer.parseInt(json.substring(j + 1, j + 5), 16));
                        j += 4;
                        break;
                    default: sb.append(n); // \" \\ \/
                }
            } else {
                sb.append(c);
            }
        }
        return null;
    }

    // ---------- what the AI gives back ----------
    // the text, and if it really came from the AI or from our fallback
    public static class AiAnswer {
    
        private final String text;
        private final boolean fromAi;
    
        public AiAnswer(String text, boolean fromAi) {
            this.text = text;
            this.fromAi = fromAi;
        }
    
        public String getText() { return text; }
        public boolean isFromAi() { return fromAi; }
    }
}
