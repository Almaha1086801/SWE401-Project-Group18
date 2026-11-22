package com.studentblueprint.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// wraps java's HttpExchange so the controllers stay short:
// read the form fields and cookies, and send back a page or a redirect
public class Request {

    private final HttpExchange exchange;
    private final Map<String, String> params = new HashMap<>();

    public Request(HttpExchange exchange) throws IOException {
        this.exchange = exchange;
        // fields from the url (?a=1&b=2) and from a submitted form are read the same way
        parse(exchange.getRequestURI().getRawQuery());
        if ("POST".equals(method())) {
            parse(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private void parse(String text) {
        if (text == null || text.isEmpty()) return;
        for (String pair : text.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
    }

    public String method() { return exchange.getRequestMethod(); }
    public boolean isPost() { return "POST".equals(method()); }
    public String path() { return exchange.getRequestURI().getPath(); }

    // a form field, or "" if its missing (so we never get null)
    public String param(String name) {
        return params.getOrDefault(name, "");
    }

    public int intParam(String name) {
        try {
            return Integer.parseInt(param(name).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String cookie(String name) {
        String header = exchange.getRequestHeaders().getFirst("Cookie");
        if (header == null) return null;
        for (String part : header.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) return kv[1];
        }
        return null;
    }

    public void setCookie(String cookie) {
        exchange.getResponseHeaders().add("Set-Cookie", cookie);
    }

    public void send(int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    public void html(int status, String body) throws IOException {
        send(status, "text/html", body);
    }

    // sends the browser to another page (used after every form so refresh doesnt submit twice)
    public void redirect(String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
    }
}
