package com.medical.services;

import com.medical.dao.MedecinDAO;
import com.medical.dao.PatientDAO;
import com.medical.models.Medecin;
import com.medical.models.Patient;
import com.medical.utils.JwtUtil;
import com.medical.utils.PasswordUtil;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class AuthService {

    private final PatientDAO patientDAO = new PatientDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();

    private static final String ADMIN_EMAIL;
    private static final String ADMIN_PASSWORD;

    static {
        try (InputStream in = AuthService.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            Properties props = new Properties();
            props.load(in);
            ADMIN_EMAIL    = props.getProperty("admin.email",    "admin@medical.com");
            ADMIN_PASSWORD = props.getProperty("admin.password", "admin123");
        } catch (Exception e) {
            throw new RuntimeException("Erreur chargement config admin", e);
        }
    }

    private String str(Map<String, Object> data, String key) {
        Object v = data.get(key);
        return (v == null) ? null : v.toString().trim();
    }

    public Map<String, Object> registerPatient(Map<String, Object> data) {
        String email    = str(data, "email");
        String password = str(data, "password");
        String nom      = str(data, "nom_pat");
        String datenais = str(data, "datenais");

        if (email == null || password == null || nom == null)
            return error("Champs obligatoires manquants (email, password, nom_pat)");
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
            return error("Format email invalide");
        if (password.length() < 6)
            return error("Le mot de passe doit contenir au moins 6 caractères");
        if (patientDAO.emailExists(email))
            return error("Cet email est déjà utilisé");

        Patient p = new Patient();
        p.setIdpat("PAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        p.setNom_pat(nom);
        p.setEmail(email);
        p.setPassword(PasswordUtil.hash(password));
        p.setTelephone(str(data, "telephone") != null ? str(data, "telephone") : "");
        if (datenais != null && !datenais.isEmpty()) {
            try { p.setDatenais(LocalDate.parse(datenais)); } catch (Exception ignored) {}
        }
        patientDAO.insert(p);
        String token = JwtUtil.generateToken(p.getIdpat(), p.getEmail(), "patient");
        return success(token, p.getIdpat(), "patient", p.getNom_pat());
    }

    public Map<String, Object> registerMedecin(Map<String, Object> data) {
        String email      = str(data, "email");
        String password   = str(data, "password");
        String nom        = str(data, "nommed");
        String specialite = str(data, "specialite");
        String lieu       = str(data, "lieu");
        String telephone  = str(data, "telephone");

        if (email == null || password == null || nom == null || specialite == null)
            return error("Champs obligatoires manquants");
        if (medecinDAO.emailExists(email) || patientDAO.emailExists(email))
            return error("Cet email est déjà utilisé");

        Medecin m = new Medecin();
        m.setIdmed("MED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        m.setNommed(nom);
        m.setSpecialite(specialite);
        m.setLieu(lieu != null ? lieu : "");
        m.setEmail(email);
        m.setPassword(PasswordUtil.hash(password));
        m.setTelephone(telephone != null ? telephone : "");
        try {
            Object th = data.get("taux_horaire");
            if (th != null) m.setTaux_horaire(Integer.parseInt(th.toString()));
        } catch (NumberFormatException ignored) {}

        medecinDAO.insert(m);
        String token = JwtUtil.generateToken(m.getIdmed(), m.getEmail(), "medecin");
        return success(token, m.getIdmed(), "medecin", m.getNommed());
    }

    public Map<String, Object> login(Map<String, Object> data) {
        String email    = str(data, "email");
        String password = str(data, "password");

        if (email == null || password == null)
            return error("Email et mot de passe requis");

        // ── Admin ─────────────────────────────────────────────────────────────
        if (ADMIN_EMAIL.equals(email) && ADMIN_PASSWORD.equals(password)) {
            String token = JwtUtil.generateToken("ADMIN-001", email, "admin");
            return success(token, "ADMIN-001", "admin", "Administrateur");
        }

        // ── Patient ───────────────────────────────────────────────────────────
        Patient patient = patientDAO.findByEmail(email);
        if (patient != null) {
            if (!PasswordUtil.verify(password, patient.getPassword()))
                return error("Mot de passe incorrect");
            if (!patient.isActif()) return error("Compte désactivé");
            String token = JwtUtil.generateToken(patient.getIdpat(), patient.getEmail(), "patient");
            return success(token, patient.getIdpat(), "patient", patient.getNom_pat());
        }

        // ── Médecin ───────────────────────────────────────────────────────────
        Medecin medecin = medecinDAO.findByEmail(email);
        if (medecin != null) {
            if (!PasswordUtil.verify(password, medecin.getPassword()))
                return error("Mot de passe incorrect");
            if (!medecin.isActif()) return error("Compte désactivé");
            String token = JwtUtil.generateToken(medecin.getIdmed(), medecin.getEmail(), "medecin");
            return success(token, medecin.getIdmed(), "medecin", medecin.getNommed());
        }

        return error("Aucun compte trouvé avec cet email");
    }

    private Map<String, Object> success(String token, String userId, String role, String nom) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("token",   token);
        res.put("userId",  userId);
        res.put("role",    role);
        res.put("nom",     nom);
        return res;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", false);
        res.put("message", msg);
        return res;
    }
}