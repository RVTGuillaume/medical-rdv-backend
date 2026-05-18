package com.medical.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.dao.HoraireDAO;
import com.medical.models.Horaire;
import com.medical.utils.ResponseUtil;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class HoraireServlet extends HttpServlet {

    private final HoraireDAO  horaireDAO = new HoraireDAO();
    private final ObjectMapper mapper    = new ObjectMapper();

    // ===== GET =====
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String idmed      = req.getParameter("idmed");
        String disponible = req.getParameter("disponible");

        if (idmed == null || idmed.isBlank()) {
            ResponseUtil.badRequest(res, "Paramètre idmed requis"); return;
        }

        List<Horaire> horaires = "true".equals(disponible)
                ? horaireDAO.findDisponiblesByMedecin(idmed)
                : horaireDAO.findByMedecin(idmed);

        ResponseUtil.ok(res, "Horaires", horaires);
    }

    // ===== POST — ajouter un ou plusieurs créneaux =====
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String userId = (String) req.getAttribute("userId");
        String role   = (String) req.getAttribute("role");

        if (!"medecin".equals(role) && !"admin".equals(role)) {
            ResponseUtil.forbidden(res, "Réservé au médecin"); return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);
        Object dateHeure = body.get("dateHeure");

        if (dateHeure instanceof List<?> dates) {
            // ── Liste de créneaux ─────────────────────────────────────────────
            for (Object d : dates) {
                String erreur = validerDateFutur((String) d);
                if (erreur != null) {
                    ResponseUtil.badRequest(res, erreur); return;
                }
            }
            List<Horaire> horaires = dates.stream()
                    .map(d -> buildHoraire(userId, (String) d))
                    .toList();
            horaireDAO.insertMany(horaires);
            ResponseUtil.created(res, dates.size() + " créneaux ajoutés", null);

        } else if (dateHeure instanceof String d) {
            // ── Créneau unique ────────────────────────────────────────────────
            String erreur = validerDateFutur(d);
            if (erreur != null) {
                ResponseUtil.badRequest(res, erreur); return;
            }
            horaireDAO.insert(buildHoraire(userId, d));
            ResponseUtil.created(res, "Créneau ajouté", null);

        } else {
            ResponseUtil.badRequest(res, "dateHeure requis (string ou liste)");
        }
    }

    // ===== DELETE =====
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String path = req.getPathInfo();
        String role = (String) req.getAttribute("role");

        if (!"medecin".equals(role) && !"admin".equals(role)) {
            ResponseUtil.forbidden(res, "Réservé au médecin"); return;
        }

        if (path == null || path.equals("/")) {
            ResponseUtil.badRequest(res, "ID créneau requis"); return;
        }

        String objectId = path.substring(1);
        Horaire h = horaireDAO.findById(objectId);
        if (h == null) { ResponseUtil.notFound(res, "Créneau introuvable"); return; }

        if (!h.isDisponible()) {
            ResponseUtil.conflict(res, "Ce créneau est déjà réservé, impossible de le supprimer");
            return;
        }

        horaireDAO.delete(objectId);
        ResponseUtil.ok(res, "Créneau supprimé", null);
    }

    // ===== Helpers =====

    /**
     * Vérifie que la date est strictement dans le futur.
     * Retourne un message d'erreur si invalide, null si valide.
     */
    private String validerDateFutur(String dateHeure) {
        if (dateHeure == null || dateHeure.isBlank()) {
            return "La date et l'heure sont obligatoires";
        }
        try {
            // Accepte "2025-06-15T10:00" et "2025-06-15T10:00:00"
            String normalized = dateHeure.length() == 16
                    ? dateHeure + ":00"
                    : dateHeure;
            LocalDateTime dt  = LocalDateTime.parse(normalized);
            LocalDateTime now = LocalDateTime.now();
            if (!dt.isAfter(now)) {
                return "Impossible d'ajouter un créneau dans le passé ou à l'heure actuelle";
            }
            return null;
        } catch (DateTimeParseException e) {
            return "Format de date invalide. Attendu : YYYY-MM-DDTHH:MM:SS";
        }
    }

    private Horaire buildHoraire(String idmed, String dateHeure) {
        Horaire h = new Horaire();
        h.setIdmed(idmed);
        h.setDateHeure(dateHeure);
        h.setDisponible(true);
        return h;
    }
}