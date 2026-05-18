package com.medical.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.services.AuthService;
import com.medical.utils.ResponseUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Map;

public class AuthServlet extends HttpServlet {

    private final AuthService authService = new AuthService();
    private final ObjectMapper mapper     = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String path = req.getPathInfo(); // /login  ou  /register/patient  ou  /register/medecin

        if (path == null) { ResponseUtil.badRequest(res, "Route invalide"); return; }

        @SuppressWarnings("unchecked")
        Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);

        switch (path) {
            case "/login" -> {
                Map<String, Object> result = authService.login(body);
                if ((boolean) result.get("success")) ResponseUtil.ok(res, "Connexion réussie", result);
                else ResponseUtil.unauthorized(res, (String) result.get("message"));
            }
            case "/register/patient" -> {
                Map<String, Object> result = authService.registerPatient(body);
                if ((boolean) result.get("success")) ResponseUtil.created(res, "Compte patient créé", result);
                else ResponseUtil.conflict(res, (String) result.get("message"));
            }
            case "/register/medecin" -> {
                Map<String, Object> result = authService.registerMedecin(body);
                if ((boolean) result.get("success")) ResponseUtil.created(res, "Compte médecin créé", result);
                else ResponseUtil.conflict(res, (String) result.get("message"));
            }
            default -> ResponseUtil.notFound(res, "Route auth inconnue : " + path);
        }
    }
}