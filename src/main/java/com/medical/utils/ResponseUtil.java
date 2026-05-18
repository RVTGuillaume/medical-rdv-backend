package com.medical.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper pour envoyer des réponses JSON standardisées.
 * Format : { "success": true/false, "message": "...", "data": {...} }
 */
public class ResponseUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    private ResponseUtil() {}

    public static void send(HttpServletResponse res, int status, boolean success,
                            String message, Object data) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("success", success);
        body.put("message", message);
        if (data != null) body.put("data", data);

        mapper.writeValue(res.getWriter(), body);
    }

    public static void ok(HttpServletResponse res, String message, Object data) throws IOException {
        send(res, 200, true, message, data);
    }

    public static void created(HttpServletResponse res, String message, Object data) throws IOException {
        send(res, 201, true, message, data);
    }

    public static void badRequest(HttpServletResponse res, String message) throws IOException {
        send(res, 400, false, message, null);
    }

    public static void unauthorized(HttpServletResponse res, String message) throws IOException {
        send(res, 401, false, message, null);
    }

    public static void forbidden(HttpServletResponse res, String message) throws IOException {
        send(res, 403, false, message, null);
    }

    public static void notFound(HttpServletResponse res, String message) throws IOException {
        send(res, 404, false, message, null);
    }

    public static void conflict(HttpServletResponse res, String message) throws IOException {
        send(res, 409, false, message, null);
    }

    public static void serverError(HttpServletResponse res, String message) throws IOException {
        send(res, 500, false, message, null);
    }
}