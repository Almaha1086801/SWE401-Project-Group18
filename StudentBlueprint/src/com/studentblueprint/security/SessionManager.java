package com.studentblueprint.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

// remembers who is logged in
// after login the browser gets a cookie with a random token, and we keep token -> student id here
// if the student does nothing for 30 minutes the token stops working (NFR-09)
public class SessionManager {

    private static final long TIMEOUT_MS = 30 * 60 * 1000;

    private final Map<String, Integer> studentIds = new HashMap<>();
    private final Map<String, Long> lastUsed = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String create(int studentId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        studentIds.put(token, studentId);
        lastUsed.put(token, System.currentTimeMillis());
        return token;
    }

    // returns the student id, or -1 if the token is wrong or timed out
    public int getStudentId(String token) {
        if (token == null || !studentIds.containsKey(token)) {
            return -1;
        }
        long now = System.currentTimeMillis();
        if (now - lastUsed.get(token) > TIMEOUT_MS) {
            remove(token);
            return -1;
        }
        lastUsed.put(token, now); // still active, so reset the 30 minutes
        return studentIds.get(token);
    }

    // ---------- NFR-06: lock the account for 15 minutes after 5 wrong passwords ----------

    private static final int MAX_WRONG = 5;
    private static final long LOCK_MS = 15 * 60 * 1000;
    private final Map<String, Integer> wrongTries = new HashMap<>();
    private final Map<String, Long> lockedUntil = new HashMap<>();

    public boolean isLocked(String email) {
        Long until = lockedUntil.get(email.toLowerCase());
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            lockedUntil.remove(email.toLowerCase()); // 15 minutes are over
            return false;
        }
        return true;
    }

    public void wrongPassword(String email) {
        String key = email.toLowerCase();
        int tries = wrongTries.getOrDefault(key, 0) + 1;
        if (tries >= MAX_WRONG) {
            lockedUntil.put(key, System.currentTimeMillis() + LOCK_MS);
            tries = 0;
        }
        wrongTries.put(key, tries);
    }

    public void loginWorked(String email) {
        wrongTries.remove(email.toLowerCase());
    }

    public void remove(String token) {
        studentIds.remove(token);
        lastUsed.remove(token);
    }
}
