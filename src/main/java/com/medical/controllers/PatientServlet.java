package com.medical.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.dao.PatientDAO;
import com.medical.models.Patient;
import com.medical.services.CloudinaryService;
import com.medical.utils.PasswordUtil;
import com.medical.utils.ResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class PatientServlet extends HttpServlet {

    private final PatientDAO        patientDAO        = new PatientDAO();
    private final CloudinaryService cloudinaryService = new CloudinaryService();
    private final ObjectMapper      mapper            = new ObjectMapper();

    // ===== GET =====
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String role   = (String) req.getAttribute("role");
        String userId = (String) req.getAttribute("userId");
        String path   = req.getPathInfo();

        if (path == null || path.equals("/")) {
            if (!"admin".equals(role)) { ResponseUtil.forbidden(res, "Réservé à l'admin"); return; }
            ResponseUtil.ok(res, "Liste patients",
                    patientDAO.findAll().stream().peek(p -> p.setPassword(null)).toList());
            return;
        }

        String idpat = path.substring(1);
        if (!"admin".equals(role) && !idpat.equals(userId)) {
            ResponseUtil.forbidden(res, "Accès interdit"); return;
        }

        Patient p = patientDAO.findByIdpat(idpat);
        if (p == null) { ResponseUtil.notFound(res, "Patient introuvable"); return; }
        p.setPassword(null);
        ResponseUtil.ok(res, "Patient trouvé", p);
    }

    // ===== POST : création OU upload photo =====
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String path = req.getPathInfo();
        String role = (String) req.getAttribute("role");

        // POST /api/patients/{idpat}/photo → upload photo
        if (path != null && path.endsWith("/photo")) {
            String idpat  = path.replace("/photo", "").substring(1);
            String userId = (String) req.getAttribute("userId");
            if (!idpat.equals(userId) && !"admin".equals(role)) {
                ResponseUtil.forbidden(res, "Action non autorisée"); return;
            }
            Part filePart = req.getPart("photo");
            if (filePart == null) { ResponseUtil.badRequest(res, "Fichier photo manquant"); return; }
            byte[] bytes = filePart.getInputStream().readAllBytes();
            String url   = cloudinaryService.uploadImage(bytes, "patients", idpat);
            patientDAO.updatePhotoUrl(idpat, url);
            ResponseUtil.ok(res, "Photo mise à jour", Map.of("photoUrl", url));
            return;
        }

        // POST /api/patients/ → créer un patient (admin seulement)
        if (!"admin".equals(role)) {
            ResponseUtil.forbidden(res, "Réservé à l'admin"); return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);

        String email     = body.get("email")     != null ? body.get("email").toString().trim()     : null;
        String password  = body.get("password")  != null ? body.get("password").toString().trim()  : null;
        String nom       = body.get("nom_pat")   != null ? body.get("nom_pat").toString().trim()   : null;
        String telephone = body.get("telephone") != null ? body.get("telephone").toString().trim() : "";

        // ── Validations ──────────────────────────────────────────────────────
        if (email == null || password == null || nom == null) {
            ResponseUtil.badRequest(res, "Champs obligatoires manquants (nom_pat, email, password)");
            return;
        }
        if (password.length() < 6) {
            ResponseUtil.badRequest(res, "Le mot de passe doit contenir au moins 6 caractères");
            return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            ResponseUtil.badRequest(res, "Format email invalide");
            return;
        }
        if (patientDAO.emailExists(email)) {
            ResponseUtil.conflict(res, "Cet email est déjà utilisé");
            return;
        }
        // ── Vérification unicité téléphone à la CRÉATION ─────────────────────
        if (!telephone.isEmpty() && patientDAO.phoneExists(telephone)) {
            ResponseUtil.conflict(res, "Ce numéro de téléphone est déjà utilisé par un autre patient");
            return;
        }

        // ── Création ─────────────────────────────────────────────────────────
        Patient p = new Patient();
        p.setIdpat("PAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        p.setNom_pat(nom);
        p.setEmail(email);
        p.setPassword(PasswordUtil.hash(password));
        p.setTelephone(telephone.isEmpty() ? null : telephone);

        String datenais = body.get("datenais") != null ? body.get("datenais").toString() : null;
        if (datenais != null && !datenais.isEmpty()) {
            try { p.setDatenais(LocalDate.parse(datenais)); } catch (Exception ignored) {}
        }

        patientDAO.insert(p);
        p.setPassword(null);
        ResponseUtil.created(res, "Patient créé avec succès", p);
    }

    // ===== PUT : modifier =====
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String path   = req.getPathInfo();
        String userId = (String) req.getAttribute("userId");
        String role   = (String) req.getAttribute("role");

        if (path == null || path.equals("/")) { ResponseUtil.badRequest(res, "ID patient requis"); return; }
        String idpat = path.substring(1);

        if (!"admin".equals(role) && !idpat.equals(userId)) {
            ResponseUtil.forbidden(res, "Action non autorisée"); return;
        }

        Patient p = patientDAO.findByIdpat(idpat);
        if (p == null) { ResponseUtil.notFound(res, "Patient introuvable"); return; }

        @SuppressWarnings("unchecked")
        Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);

        // ── Vérification unicité téléphone à la MODIFICATION ─────────────────
        if (body.containsKey("telephone")) {
            String tel = body.get("telephone") != null ? body.get("telephone").toString().trim() : "";
            if (!tel.isEmpty() && patientDAO.phoneExistsExcluding(tel, idpat)) {
                ResponseUtil.conflict(res, "Ce numéro de téléphone est déjà utilisé par un autre patient");
                return;
            }
            p.setTelephone(tel.isEmpty() ? null : tel);
        }

        if (body.containsKey("nom_pat")) p.setNom_pat((String) body.get("nom_pat"));
        if (body.containsKey("datenais")) {
            String dn = (String) body.get("datenais");
            if (dn != null && !dn.isEmpty()) {
                try { p.setDatenais(LocalDate.parse(dn)); } catch (Exception ignored) {}
            }
        }

        patientDAO.update(p);
        p.setPassword(null);
        ResponseUtil.ok(res, "Profil mis à jour", p);
    }

    // ===== DELETE =====
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String role = (String) req.getAttribute("role");
        if (!"admin".equals(role)) { ResponseUtil.forbidden(res, "Réservé à l'admin"); return; }

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { ResponseUtil.badRequest(res, "ID requis"); return; }

        patientDAO.delete(path.substring(1));
        ResponseUtil.ok(res, "Patient supprimé", null);
    }
}