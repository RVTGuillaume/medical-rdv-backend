package com.medical.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.dao.MedecinDAO;
import com.medical.models.Medecin;
import com.medical.services.CloudinaryService;
import com.medical.utils.PasswordUtil;
import com.medical.utils.ResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MedecinServlet extends HttpServlet {

    private final MedecinDAO        medecinDAO        = new MedecinDAO();
    private final CloudinaryService cloudinaryService = new CloudinaryService();
    private final ObjectMapper      mapper            = new ObjectMapper();

    // ===== GET =====
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            String search     = req.getParameter("search");
            String specialite = req.getParameter("specialite");
            String top5       = req.getParameter("top5");

            if ("true".equals(top5)) {
                List<org.bson.Document> result = medecinDAO.top5Consultes();
                ResponseUtil.ok(res, "Top 5 médecins", result);
            } else if (search != null && !search.isBlank()) {
                ResponseUtil.ok(res, "Résultats recherche", medecinDAO.searchByNom(search));
            } else if (specialite != null && !specialite.isBlank()) {
                ResponseUtil.ok(res, "Médecins par spécialité", medecinDAO.findBySpecialite(specialite));
            } else {
                ResponseUtil.ok(res, "Liste médecins", medecinDAO.findAll());
            }
            return;
        }

        String idmed = path.substring(1);
        Medecin m = medecinDAO.findByIdmed(idmed);
        if (m == null) { ResponseUtil.notFound(res, "Médecin introuvable"); return; }
        m.setPassword(null);
        ResponseUtil.ok(res, "Médecin trouvé", m);
    }

    // ===== POST : création OU upload photo =====
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String path = req.getPathInfo();
        String role = (String) req.getAttribute("role");

        // POST /api/medecins/{idmed}/photo → upload photo
        if (path != null && path.endsWith("/photo")) {
            String idmed  = path.replace("/photo", "").substring(1);
            String userId = (String) req.getAttribute("userId");
            if (!idmed.equals(userId) && !"admin".equals(role)) {
                ResponseUtil.forbidden(res, "Action non autorisée"); return;
            }
            Part filePart = req.getPart("photo");
            if (filePart == null) { ResponseUtil.badRequest(res, "Fichier photo manquant"); return; }
            byte[] bytes = filePart.getInputStream().readAllBytes();
            String url   = cloudinaryService.uploadImage(bytes, "medecins", idmed);
            medecinDAO.updatePhotoUrl(idmed, url);
            ResponseUtil.ok(res, "Photo mise à jour", Map.of("photoUrl", url));
            return;
        }

        // POST /api/medecins/ → créer un médecin (admin seulement)
        if (!"admin".equals(role)) {
            ResponseUtil.forbidden(res, "Réservé à l'admin"); return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);

        String email      = body.get("email")      != null ? body.get("email").toString().trim()      : null;
        String password   = body.get("password")   != null ? body.get("password").toString().trim()   : null;
        String nom        = body.get("nommed")      != null ? body.get("nommed").toString().trim()     : null;
        String specialite = body.get("specialite") != null ? body.get("specialite").toString().trim() : null;
        String telephone  = body.get("telephone")  != null ? body.get("telephone").toString().trim()  : "";

        // ── Validations ──────────────────────────────────────────────────────
        if (email == null || password == null || nom == null || specialite == null) {
            ResponseUtil.badRequest(res, "Champs obligatoires manquants (nommed, specialite, email, password)");
            return;
        }
        if (password.length() < 6) {
            ResponseUtil.badRequest(res, "Le mot de passe doit contenir au moins 6 caractères");
            return;
        }
        if (medecinDAO.emailExists(email)) {
            ResponseUtil.conflict(res, "Cet email est déjà utilisé");
            return;
        }
        // ── Vérification unicité téléphone à la CRÉATION ─────────────────────
        if (!telephone.isEmpty() && medecinDAO.phoneExists(telephone)) {
            ResponseUtil.conflict(res, "Ce numéro de téléphone est déjà utilisé par un autre médecin");
            return;
        }

        // ── Création ─────────────────────────────────────────────────────────
        Medecin m = new Medecin();
        m.setIdmed("MED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        m.setNommed(nom);
        m.setSpecialite(specialite);
        m.setLieu(body.get("lieu") != null ? body.get("lieu").toString() : "");
        m.setTelephone(telephone.isEmpty() ? null : telephone);
        m.setEmail(email);
        m.setPassword(PasswordUtil.hash(password));
        try {
            Object th = body.get("taux_horaire");
            if (th != null) m.setTaux_horaire(Integer.parseInt(th.toString()));
        } catch (NumberFormatException ignored) {}

        medecinDAO.insert(m);
        m.setPassword(null);
        ResponseUtil.created(res, "Médecin créé avec succès", m);
    }

    // ===== PUT : modifier =====
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String path   = req.getPathInfo();
        String role   = (String) req.getAttribute("role");
        String userId = (String) req.getAttribute("userId");

        if (path == null || path.equals("/")) { ResponseUtil.badRequest(res, "ID médecin requis"); return; }
        String idmed = path.substring(1);

        if (!"admin".equals(role) && !idmed.equals(userId)) {
            ResponseUtil.forbidden(res, "Action non autorisée"); return;
        }

        Medecin m = medecinDAO.findByIdmed(idmed);
        if (m == null) { ResponseUtil.notFound(res, "Médecin introuvable"); return; }

        @SuppressWarnings("unchecked")
        Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);

        // ── Vérification unicité téléphone à la MODIFICATION ─────────────────
        if (body.containsKey("telephone")) {
            String tel = body.get("telephone") != null ? body.get("telephone").toString().trim() : "";
            if (!tel.isEmpty() && medecinDAO.phoneExistsExcluding(tel, idmed)) {
                ResponseUtil.conflict(res, "Ce numéro de téléphone est déjà utilisé par un autre médecin");
                return;
            }
            m.setTelephone(tel.isEmpty() ? null : tel);
        }

        if (body.containsKey("nommed"))       m.setNommed((String)  body.get("nommed"));
        if (body.containsKey("specialite"))   m.setSpecialite((String) body.get("specialite"));
        if (body.containsKey("lieu"))         m.setLieu((String)    body.get("lieu"));
        if (body.containsKey("taux_horaire")) m.setTaux_horaire(((Number) body.get("taux_horaire")).intValue());

        medecinDAO.update(m);
        m.setPassword(null);
        ResponseUtil.ok(res, "Médecin mis à jour", m);
    }

    // ===== DELETE =====
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String path = req.getPathInfo();
        String role = (String) req.getAttribute("role");

        if (!"admin".equals(role)) { ResponseUtil.forbidden(res, "Réservé à l'admin"); return; }
        if (path == null || path.equals("/")) { ResponseUtil.badRequest(res, "ID requis"); return; }

        medecinDAO.delete(path.substring(1));
        ResponseUtil.ok(res, "Médecin supprimé", null);
    }
}