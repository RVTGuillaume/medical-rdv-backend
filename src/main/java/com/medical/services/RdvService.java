package com.medical.services;

import com.medical.dao.HoraireDAO;
import com.medical.dao.MedecinDAO;
import com.medical.dao.PatientDAO;
import com.medical.dao.RdvDAO;
import com.medical.models.Medecin;
import com.medical.models.Patient;
import com.medical.models.Rdv;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RdvService {

    private final RdvDAO       rdvDAO       = new RdvDAO();
    private final HoraireDAO   horaireDAO   = new HoraireDAO();
    private final PatientDAO   patientDAO   = new PatientDAO();
    private final MedecinDAO   medecinDAO   = new MedecinDAO();
    private final EmailService emailService = new EmailService();

    // =========================================================================
    //  CRÉER UN RENDEZ-VOUS
    // =========================================================================

    public Map<String, Object> creerRdv(String idpat, String idmed,
                                         String dateRdv, String motif) {

        // 1. Vérifier la disponibilité du créneau
        if (rdvDAO.creneauPris(idmed, dateRdv))
            return Map.of("success", false, "message",
                    "Ce créneau est déjà réservé, veuillez en choisir un autre.");

        if (!horaireDAO.isDisponible(idmed, dateRdv))
            return Map.of("success", false, "message",
                    "Ce créneau n'existe pas ou n'est plus disponible.");

        // 2. Récupérer les entités
        Patient patient = patientDAO.findByIdpat(idpat);
        Medecin medecin = medecinDAO.findByIdmed(idmed);
        if (patient == null) return Map.of("success", false, "message", "Patient introuvable.");
        if (medecin == null) return Map.of("success", false, "message", "Médecin introuvable.");

        // 3. Créer le RDV
        String idrdv = "RDV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Rdv rdv = new Rdv();
        rdv.setIdrdv(idrdv);
        rdv.setIdpat(idpat);
        rdv.setIdmed(idmed);
        rdv.setDateRdv(dateRdv);
        rdv.setMotif(motif);
        rdv.setStatus("CONFIRMED");
        rdv.setNomMedecin(medecin.getNommed());
        rdv.setSpecialiteMedecin(medecin.getSpecialite());
        rdv.setNomPatient(patient.getNom_pat());

        rdvDAO.insert(rdv);

        // 4. Marquer le créneau comme réservé
        horaireDAO.marquerReserve(idmed, dateRdv, idrdv);

        // 5a. [EXISTANT] Confirmation au PATIENT
        emailService.envoyerConfirmationPatient(
            patient.getEmail(),
            patient.getNom_pat(),
            medecin.getNommed(),
            medecin.getSpecialite(),
            dateRdv,
            idrdv
        );

        // 5b. [FIX #1] Notification de nouvelle réservation au MÉDECIN
        if (medecin.getEmail() != null && !medecin.getEmail().isBlank()) {
            emailService.envoyerNotificationNouveauRdvMedecin(
                medecin.getEmail(),
                medecin.getNommed(),
                patient.getNom_pat(),
                dateRdv,
                motif,
                idrdv
            );
        }

        return Map.of("success", true, "message", "Rendez-vous confirmé.", "idrdv", idrdv);
    }

    // =========================================================================
    //  ANNULER UN RENDEZ-VOUS
    // =========================================================================

    public Map<String, Object> annulerRdv(String idrdv, String demandeurId) {

        Rdv rdv = rdvDAO.findByIdrdv(idrdv);
        if (rdv == null)
            return Map.of("success", false, "message", "Rendez-vous introuvable.");

        if ("CANCELLED".equals(rdv.getStatus()))
            return Map.of("success", false, "message", "Ce rendez-vous est déjà annulé.");

        // Seul le patient concerné ou le médecin concerné peut annuler
        boolean estLePatient = rdv.getIdpat().equals(demandeurId);
        boolean estLeMedecin = rdv.getIdmed().equals(demandeurId);

        if (!estLePatient && !estLeMedecin)
            return Map.of("success", false, "message", "Action non autorisée.");

        // Mise à jour BDD + libération du créneau
        rdvDAO.updateStatus(idrdv, "CANCELLED");
        horaireDAO.liberer(rdv.getIdmed(), rdv.getDateRdv());

        // [FIX #2] Email d'annulation au PATIENT dans les deux cas,
        // avec message adapté selon l'auteur de l'annulation.
        Patient patient = patientDAO.findByIdpat(rdv.getIdpat());
        if (patient != null && patient.getEmail() != null && !patient.getEmail().isBlank()) {
            emailService.envoyerAnnulationPatient(
                patient.getEmail(),
                patient.getNom_pat(),
                rdv.getNomMedecin(),
                rdv.getDateRdv(),
                estLeMedecin          // true → "le médecin a annulé" | false → "vous avez annulé"
            );
        }

        return Map.of("success", true, "message", "Rendez-vous annulé avec succès.");
    }

    // =========================================================================
    //  LECTURE
    // =========================================================================

    public List<Rdv> getByPatient(String idpat) { return rdvDAO.findByPatient(idpat); }
    public List<Rdv> getByMedecin(String idmed) { return rdvDAO.findByMedecin(idmed); }
    public List<Rdv>  getAll()                  { return rdvDAO.findAll(); }
    public Rdv        getByIdrdv(String idrdv)  { return rdvDAO.findByIdrdv(idrdv); }
}