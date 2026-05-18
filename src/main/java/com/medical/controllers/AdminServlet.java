package com.medical.controllers;

import com.medical.dao.MedecinDAO;
import com.medical.dao.PatientDAO;
import com.medical.dao.RdvDAO;
import com.medical.utils.ResponseUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/admin/*")
public class AdminServlet extends HttpServlet {

    private final PatientDAO patientDAO = new PatientDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();
    private final RdvDAO     rdvDAO     = new RdvDAO();

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String role = (String) req.getAttribute("role");
        if (!"admin".equals(role)) {
            ResponseUtil.forbidden(res, "Accès réservé à l'administrateur");
            return;
        }

        String path = req.getPathInfo();
        // DELETE /admin/rdv/{idrdv}
        if (path != null && path.startsWith("/rdv/")) {
            String idrdv = path.substring(5);
            if (idrdv.isEmpty()) { ResponseUtil.badRequest(res, "ID RDV requis"); return; }
            rdvDAO.delete(idrdv);
            ResponseUtil.ok(res, "RDV supprimé", null);
        } else {
            ResponseUtil.notFound(res, "Route admin DELETE inconnue : " + path);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String role = (String) req.getAttribute("role");
        if (!"admin".equals(role)) {
            ResponseUtil.forbidden(res, "Accès réservé à l'administrateur");
            return;
        }

        String path = req.getPathInfo();
        if (path == null) path = "/";

        switch (path) {
            case "/stats" -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalPatients",  patientDAO.findAll().size());
                stats.put("totalMedecins",  medecinDAO.findAll().size());
                stats.put("totalRdv",       rdvDAO.findAll().size());
                stats.put("rdvConfirmes",   rdvDAO.findAll().stream()
                        .filter(r -> "CONFIRMED".equals(r.getStatus())).count());
                stats.put("rdvAnnules",     rdvDAO.findAll().stream()
                        .filter(r -> "CANCELLED".equals(r.getStatus())).count());
                stats.put("top5Medecins",   medecinDAO.top5Consultes());
                ResponseUtil.ok(res, "Statistiques", stats);
            }
            case "/patients" -> ResponseUtil.ok(res, "Tous les patients",
                    patientDAO.findAll().stream().peek(p -> p.setPassword(null)).toList());
            case "/medecins" -> ResponseUtil.ok(res, "Tous les médecins",
                    medecinDAO.findAll().stream().peek(m -> m.setPassword(null)).toList());
            case "/rdv"     -> ResponseUtil.ok(res, "Tous les RDV", rdvDAO.findAll());
            default         -> ResponseUtil.notFound(res, "Route admin inconnue : " + path);
        }
    }
}