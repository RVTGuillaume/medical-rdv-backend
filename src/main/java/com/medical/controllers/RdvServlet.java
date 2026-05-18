package com.medical.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.services.RdvService;
import com.medical.utils.ResponseUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Map;

public class RdvServlet extends HttpServlet {

    private final RdvService   rdvService = new RdvService();
    private final ObjectMapper mapper     = new ObjectMapper();

    // ===== GET =====
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String path   = req.getPathInfo();
        String userId = (String) req.getAttribute("userId");
        String role   = (String) req.getAttribute("role");

        // GET /api/rdv/  → selon le rôle
        if (path == null || path.equals("/")) {
            if ("admin".equals(role)) {
                ResponseUtil.ok(res, "Tous les RDV", rdvService.getAll());
            } else if ("patient".equals(role)) {
                ResponseUtil.ok(res, "Mes RDV", rdvService.getByPatient(userId));
            } else if ("medecin".equals(role)) {
                ResponseUtil.ok(res, "Mes RDV", rdvService.getByMedecin(userId));
            }
            return;
        }

        // GET /api/rdv/{idrdv}
        String idrdv = path.substring(1);
        var rdv = rdvService.getByIdrdv(idrdv);
        if (rdv == null) { ResponseUtil.notFound(res, "RDV introuvable"); return; }
        ResponseUtil.ok(res, "RDV trouvé", rdv);
    }

    // ===== POST /api/rdv/  → créer =====
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String userId = (String) req.getAttribute("userId");
        String role   = (String) req.getAttribute("role");

        if (!"patient".equals(role)) {
            ResponseUtil.forbidden(res, "Seul un patient peut créer un RDV"); return;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> body = mapper.readValue(req.getInputStream(), Map.class);

        String idmed   = body.get("idmed");
        String dateRdv = body.get("dateRdv");
        String motif   = body.getOrDefault("motif", "");

        if (idmed == null || dateRdv == null) {
            ResponseUtil.badRequest(res, "idmed et dateRdv sont requis"); return;
        }

        Map<String, Object> result = rdvService.creerRdv(userId, idmed, dateRdv, motif);
        if ((boolean) result.get("success")) ResponseUtil.created(res, "Rendez-vous créé", result);
        else ResponseUtil.conflict(res, (String) result.get("message"));
    }

    // ===== DELETE /api/rdv/{idrdv}  → annuler =====
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String path   = req.getPathInfo();
        String userId = (String) req.getAttribute("userId");

        if (path == null || path.equals("/")) { ResponseUtil.badRequest(res, "ID RDV requis"); return; }

        String idrdv = path.substring(1);
        Map<String, Object> result = rdvService.annulerRdv(idrdv, userId);

        if ((boolean) result.get("success")) ResponseUtil.ok(res, "RDV annulé", null);
        else ResponseUtil.badRequest(res, (String) result.get("message"));
    }
}