package com.medical.services;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Properties;

public class EmailService {

    private final String from;
    private final String fromName;
    private final String apiKey;

    private static final String MAILERSEND_URL = "https://api.mailersend.com/v1/email";

    // =========================================================================
    //  FORMATAGE DE DATE — WCAG + Bastien & Scapin
    //  Entrée  : "2025-06-15T10:30:00"  (ISO-8601, format MongoDB)
    //  Sortie  : "Dimanche 15 juin 2025 — 10h 30min 00s"
    // =========================================================================

    /**
     * Convertit une date ISO-8601 MongoDB en format lisible, précis à la seconde.
     */
    static String formatDate(String iso) {
        if (iso == null || iso.isBlank()) return "Date non précisée";
        try {
            String normalized = iso.length() == 16 ? iso + ":00" : iso;
            LocalDateTime ldt = LocalDateTime.parse(
                normalized,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            );

            String partieDate = ldt.format(
                DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
            );
            partieDate = Character.toUpperCase(partieDate.charAt(0))
                       + partieDate.substring(1);

            String partieHeure = String.format(
                "%dh %02dmin %02ds",
                ldt.getHour(),
                ldt.getMinute(),
                ldt.getSecond()
            );

            return partieDate + " \u2014 " + partieHeure;

        } catch (DateTimeParseException e) {
            return iso;
        }
    }

    // =========================================================================
    //  INITIALISATION — MailerSend HTTP API
    // =========================================================================

    public EmailService() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("config.properties")) {

            Properties config = new Properties();
            config.load(in);

            this.from     = config.getProperty("mail.from");
            this.fromName = config.getProperty("mail.from.name", "MedicalRDV");

            // Priorité : variable d'env Render > config.properties
            this.apiKey   = System.getenv("MAILERSEND_API_KEY") != null
                            ? System.getenv("MAILERSEND_API_KEY")
                            : config.getProperty("mailersend.api.key", "");

            if (this.apiKey.isBlank()) {
                System.err.println("⚠️  MAILERSEND_API_KEY manquante — emails désactivés");
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur init EmailService", e);
        }
    }

    // =========================================================================
    //  MÉTHODES PUBLIQUES
    // =========================================================================

    /** Confirmation de réservation envoyée au PATIENT. */
    public void envoyerConfirmationPatient(String toEmail,
                                            String nomPatient,
                                            String nomMedecin,
                                            String specialite,
                                            String dateRdv,
                                            String idrdv) {

        String sujet = "Votre rendez-vous est confirmé \u2014 MedicalRDV";
        String corps = buildEmail(
            Type.CONFIRMATION,
            "Rendez-vous confirm\u00e9",
            "Bonjour " + escape(nomPatient) + "\u00a0,",
            "Votre rendez-vous m\u00e9dical a bien \u00e9t\u00e9 enregistr\u00e9. "
            + "Retrouvez ci-dessous le r\u00e9capitulatif de votre consultation&nbsp;:",
            infoTable(new String[][]{
                {"M\u00e9decin",         "Dr.\u00a0" + escape(nomMedecin)},
                {"Sp\u00e9cialit\u00e9", escape(specialite)},
                {"Date &amp; heure",     escape(formatDate(dateRdv))},
                {"R\u00e9f\u00e9rence",  escape(idrdv)}
            }),
            "Pr\u00e9sentez-vous <strong>10\u00a0minutes avant</strong> votre heure "
            + "de rendez-vous et pensez \u00e0 apporter vos documents m\u00e9dicaux "
            + "et ordonnances."
        );
        envoyer(toEmail, sujet, corps);
    }

    /** Notification de nouvelle réservation envoyée au MÉDECIN. */
    public void envoyerNotificationNouveauRdvMedecin(String toEmail,
                                                       String nomMedecin,
                                                       String nomPatient,
                                                       String dateRdv,
                                                       String motif,
                                                       String idrdv) {

        String sujet = "Nouvelle r\u00e9servation\u00a0: "
                     + escape(nomPatient) + " \u2014 MedicalRDV";
        String corps = buildEmail(
            Type.INFO,
            "Nouvelle r\u00e9servation",
            "Bonjour Dr.\u00a0" + escape(nomMedecin) + "\u00a0,",
            "Un patient vient de r\u00e9server un cr\u00e9neau dans votre agenda. "
            + "Voici les informations de ce rendez-vous&nbsp;:",
            infoTable(new String[][]{
                {"Patient",             escape(nomPatient)},
                {"Date &amp; heure",    escape(formatDate(dateRdv))},
                {"Motif",               (motif != null && !motif.isBlank())
                                            ? escape(motif) : "Non pr\u00e9cis\u00e9"},
                {"R\u00e9f\u00e9rence", escape(idrdv)}
            }),
            "Connectez-vous \u00e0 votre espace professionnel pour g\u00e9rer votre "
            + "agenda et consulter les d\u00e9tails complets du dossier patient."
        );
        envoyer(toEmail, sujet, corps);
    }

    /**
     * Annulation envoyée AU PATIENT.
     * @param annuleParleMedecin true si le médecin a annulé, false si le patient.
     */
    public void envoyerAnnulationPatient(String toEmail,
                                          String nomPatient,
                                          String nomMedecin,
                                          String dateRdv,
                                          boolean annuleParleMedecin) {

        String sujet = "Annulation de rendez-vous \u2014 MedicalRDV";
        String dateFormatee = escape(formatDate(dateRdv));

        String message = annuleParleMedecin
            ? "Le Dr.\u00a0<strong>" + escape(nomMedecin) + "</strong> a d\u00fb annuler "
              + "votre rendez-vous pr\u00e9vu le&nbsp;: <strong>" + dateFormatee + "</strong>. "
              + "Nous vous invitons \u00e0 prendre un nouveau rendez-vous d\u00e8s que possible."
            : "Votre annulation du rendez-vous avec le Dr.\u00a0<strong>"
              + escape(nomMedecin) + "</strong> pr\u00e9vu le&nbsp;: "
              + "<strong>" + dateFormatee + "</strong> a bien \u00e9t\u00e9 prise en compte.";

        String note = annuleParleMedecin
            ? "Nous sommes d\u00e9sol\u00e9s pour ce d\u00e9sagr\u00e9ment. Vous pouvez "
              + "r\u00e9server un nouveau cr\u00e9neau \u00e0 tout moment depuis votre espace patient."
            : "Vous pouvez r\u00e9server un nouveau rendez-vous \u00e0 tout moment "
              + "depuis votre espace patient.";

        String corps = buildEmail(
            Type.ANNULATION,
            "Rendez-vous annul\u00e9",
            "Bonjour " + escape(nomPatient) + "\u00a0,",
            message,
            infoTable(new String[][]{
                {"M\u00e9decin",       "Dr.\u00a0" + escape(nomMedecin)},
                {"Date annul\u00e9e",  dateFormatee}
            }),
            note
        );
        envoyer(toEmail, sujet, corps);
    }

    /** Rappel J-1 envoyé au patient. */
    public void envoyerRappel(String toEmail,
                               String nomPatient,
                               String nomMedecin,
                               String specialite,
                               String dateRdv,
                               String idrdv) {

        String sujet = "Rappel\u00a0: votre rendez-vous est demain \u2014 MedicalRDV";
        String corps = buildEmail(
            Type.RAPPEL,
            "Rappel de rendez-vous",
            "Bonjour " + escape(nomPatient) + "\u00a0,",
            "Votre rendez-vous m\u00e9dical est pr\u00e9vu <strong>demain</strong>. "
            + "Voici un r\u00e9capitulatif pour vous pr\u00e9parer&nbsp;:",
            infoTable(new String[][]{
                {"M\u00e9decin",         "Dr.\u00a0" + escape(nomMedecin)},
                {"Sp\u00e9cialit\u00e9", escape(specialite)},
                {"Date &amp; heure",     escape(formatDate(dateRdv))},
                {"R\u00e9f\u00e9rence",  escape(idrdv)}
            }),
            "N\u2019oubliez pas d\u2019apporter vos ordonnances, r\u00e9sultats "
            + "d\u2019analyses et tout document m\u00e9dical utile \u00e0 votre consultation."
        );
        envoyer(toEmail, sujet, corps);
    }

    // =========================================================================
    //  ENVOI ASYNCHRONE — MailerSend HTTP API
    // =========================================================================

    private void envoyer(String to, String sujet, String corps) {
        if (apiKey.isBlank()) {
            System.err.println("❌ Email non envoyé (MAILERSEND_API_KEY absente) à " + to);
            return;
        }
        new Thread(() -> {
            try {
                String json = "{"
                    + "\"from\":{\"email\":\"" + escJson(from)
                    + "\",\"name\":\"" + escJson(fromName) + "\"},"
                    + "\"to\":[{\"email\":\"" + escJson(to) + "\"}],"
                    + "\"subject\":\"" + escJson(sujet) + "\","
                    + "\"html\":\"" + escJson(corps) + "\""
                    + "}";

                URL url = new URL(MAILERSEND_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type",  "application/json");
                conn.setRequestProperty("Accept",        "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(10000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code == 202) {
                    System.out.println("✉️  Email envoyé à " + to);
                } else {
                    InputStream err = conn.getErrorStream();
                    if (err != null) {
                        try (BufferedReader br = new BufferedReader(
                                new InputStreamReader(err, StandardCharsets.UTF_8))) {
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) sb.append(line);
                            System.err.println("❌ MailerSend HTTP " + code + " : " + sb);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur envoi email à " + to + " : " + e.getMessage());
            }
        }).start();
    }

    // =========================================================================
    //  TEMPLATE ENGINE — inchangé
    // =========================================================================

    private enum Type { CONFIRMATION, ANNULATION, RAPPEL, INFO }

    private String buildEmail(Type type,
                               String titre,
                               String salutation,
                               String messageHtml,
                               String tableau,
                               String noteHtml) {

        String headerBg, badgeBg, badgeColor, accentBorder,
               iconChar, badgeLabel, iconBg, iconBorder;

        switch (type) {
            case CONFIRMATION:
                headerBg     = "#0a4a2e";
                badgeBg      = "#d1fae5";
                badgeColor   = "#064e3b";
                accentBorder = "#10b981";
                iconChar     = "&#10003;";
                iconBg       = "#1a6b42";
                iconBorder   = "#2d9e62";
                badgeLabel   = "CONFIRM\u00c9";
                break;
            case ANNULATION:
                headerBg     = "#7f1d1d";
                badgeBg      = "#fee2e2";
                badgeColor   = "#7f1d1d";
                accentBorder = "#ef4444";
                iconChar     = "&#10007;";
                iconBg       = "#a83232";
                iconBorder   = "#c94444";
                badgeLabel   = "ANNUL\u00c9";
                break;
            case RAPPEL:
                headerBg     = "#6b3a0f";
                badgeBg      = "#fef3c7";
                badgeColor   = "#78350f";
                accentBorder = "#f59e0b";
                iconChar     = "&#9711;";
                iconBg       = "#92500f";
                iconBorder   = "#b56a1a";
                badgeLabel   = "RAPPEL J-1";
                break;
            default:
                headerBg     = "#1a3457";
                badgeBg      = "#dbeafe";
                badgeColor   = "#1e3a5f";
                accentBorder = "#3b82f6";
                iconChar     = "&#9993;";
                iconBg       = "#234a7a";
                iconBorder   = "#3060a0";
                badgeLabel   = "NOTIFICATION";
                break;
        }

        int    year    = Year.now().getValue();
        String FONT    = "Arial,Helvetica,sans-serif";
        String BG_PAGE = "#f0f4f8";
        String BG_CARD = "#ffffff";
        String BG_FOOT = "#f8fafc";
        String BORDER  = "#e5e7eb";
        String C_DARK  = "#111827";
        String C_MID   = "#374151";
        String C_GREY  = "#6b7280";
        String C_LITE  = "#9ca3af";
        String C_BLUE  = "#a3c4e8";

        return
          "<!DOCTYPE html>\n"
        + "<html lang=\"fr\""
        + " xmlns=\"http://www.w3.org/1999/xhtml\""
        + " xmlns:v=\"urn:schemas-microsoft-com:vml\""
        + " xmlns:o=\"urn:schemas-microsoft-com:office:office\">\n"
        + "<head>\n"
        + "  <meta charset=\"UTF-8\">\n"
        + "  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
        + "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n"
        + "  <title>" + titre + " \u2014 MedicalRDV</title>\n"
        + "  <!--[if mso]><noscript>"
        + "<xml><o:OfficeDocumentSettings>"
        + "<o:PixelsPerInch>96</o:PixelsPerInch>"
        + "</o:OfficeDocumentSettings></xml>"
        + "</noscript><![endif]-->\n"
        + "  <style type=\"text/css\">\n"
        + "    #outlook a{padding:0}\n"
        + "    body{margin:0;padding:0;"
        +       "-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%}\n"
        + "    table,td{border-collapse:collapse;"
        +       "mso-table-lspace:0pt;mso-table-rspace:0pt}\n"
        + "    img{border:0;height:auto;line-height:100%;"
        +       "outline:none;text-decoration:none}\n"
        + "    @media only screen and (max-width:620px){\n"
        + "      .email-card{width:100%!important}\n"
        + "      .email-pad{padding-left:20px!important;"
        +                  "padding-right:20px!important}\n"
        + "    }\n"
        + "  </style>\n"
        + "</head>\n"
        + "<body style=\"margin:0;padding:0;"
        +   "background-color:" + BG_PAGE + ";"
        +   "font-family:" + FONT + ";\">\n"
        + "<div style=\"display:none;max-height:0;overflow:hidden;"
        +   "mso-hide:all;font-size:1px;color:" + BG_PAGE + ";"
        +   "line-height:1px;\">"
        + escape(titre) + " \u2014 MedicalRDV"
        + "</div>\n"
        + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\""
        + " border=\"0\" width=\"100%\""
        + " style=\"background-color:" + BG_PAGE + ";\">\n"
        + "<tr><td align=\"center\" style=\"padding:48px 16px;\">\n"
        + "<!--[if mso]>"
        + "<table role=\"presentation\" align=\"center\" width=\"600\""
        + " cellpadding=\"0\" cellspacing=\"0\" border=\"0\">"
        + "<tr><td>"
        + "<![endif]-->\n"
        + "<table class=\"email-card\" role=\"presentation\""
        + " cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"600\""
        + " style=\"max-width:600px;width:100%;\">\n"
        + "<tr>\n"
        + "<td class=\"email-pad\" align=\"center\""
        + " style=\"background-color:" + headerBg + ";"
        +   "border-radius:16px 16px 0 0;"
        +   "padding:44px 40px 40px;\">\n"
        + "  <p style=\"margin:0 0 24px 0;"
        +     "font-family:" + FONT + ";"
        +     "font-size:11px;font-weight:700;"
        +     "letter-spacing:4px;text-transform:uppercase;"
        +     "color:" + C_BLUE + ";"
        +     "mso-line-height-rule:exactly;line-height:16px;\">"
        + "MedicalRDV</p>\n"
        + "  <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\""
        + "   border=\"0\" align=\"center\""
        + "   style=\"margin:0 auto 24px auto;\">\n"
        + "  <tr>\n"
        + "    <td align=\"center\" valign=\"middle\""
        + "     width=\"64\" height=\"64\""
        + "     style=\""
        +        "width:64px;height:64px;"
        +        "background-color:" + iconBg + ";"
        +        "border:2px solid " + iconBorder + ";"
        +        "border-radius:50%;"
        +        "font-family:" + FONT + ";"
        +        "font-size:26px;color:#ffffff;"
        +        "text-align:center;"
        +        "mso-line-height-rule:exactly;line-height:64px;\">"
        + "      " + iconChar + "\n"
        + "    </td>\n"
        + "  </tr>\n"
        + "  </table>\n"
        + "  <h1 style=\"margin:0;"
        +     "font-family:" + FONT + ";"
        +     "font-size:28px;font-weight:700;"
        +     "color:#ffffff;letter-spacing:0.3px;"
        +     "mso-line-height-rule:exactly;line-height:1.3;\">"
        + titre
        + "  </h1>\n"
        + "</td>\n"
        + "</tr>\n"
        + "<tr>\n"
        + "<td class=\"email-pad\" align=\"center\""
        + " style=\"background-color:" + BG_CARD + ";"
        +   "padding:22px 40px 0;\">\n"
        + "  <span style=\""
        +     "display:inline-block;"
        +     "background-color:" + badgeBg + ";"
        +     "color:" + badgeColor + ";"
        +     "padding:5px 22px;"
        +     "border-radius:999px;"
        +     "font-family:" + FONT + ";"
        +     "font-size:10px;font-weight:800;"
        +     "letter-spacing:2px;text-transform:uppercase;"
        +     "mso-line-height-rule:exactly;line-height:20px;\">"
        + badgeLabel
        + "  </span>\n"
        + "</td>\n"
        + "</tr>\n"
        + "<tr>\n"
        + "<td class=\"email-pad\""
        + " style=\"background-color:" + BG_CARD + ";"
        +   "padding:32px 40px 4px;\">\n"
        + "  <p style=\"margin:0 0 18px 0;"
        +     "font-family:" + FONT + ";"
        +     "font-size:16px;font-weight:600;"
        +     "color:" + C_DARK + ";"
        +     "mso-line-height-rule:exactly;line-height:1.5;\">"
        + salutation
        + "  </p>\n"
        + "  <p style=\"margin:0;"
        +     "font-family:" + FONT + ";"
        +     "font-size:15px;color:" + C_MID + ";"
        +     "mso-line-height-rule:exactly;line-height:1.8;\">"
        + messageHtml
        + "  </p>\n"
        + "</td>\n"
        + "</tr>\n"
        + "<tr>\n"
        + "<td class=\"email-pad\""
        + " style=\"background-color:" + BG_CARD + ";"
        +   "padding:8px 40px 4px;\">\n"
        + tableau + "\n"
        + "</td>\n"
        + "</tr>\n"
        + "<tr>\n"
        + "<td class=\"email-pad\""
        + " style=\"background-color:" + BG_CARD + ";"
        +   "padding:4px 40px 36px;\">\n"
        + "  <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\""
        + "   border=\"0\" width=\"100%\">\n"
        + "  <tr>\n"
        + "    <td width=\"4\""
        + "     style=\"width:4px;background-color:" + accentBorder + ";"
        +        "font-size:0;line-height:0;\">&nbsp;</td>\n"
        + "    <td style=\""
        +        "padding:14px 18px;"
        +        "background-color:" + BG_FOOT + ";"
        +        "border-top:1px solid " + BORDER + ";"
        +        "border-right:1px solid " + BORDER + ";"
        +        "border-bottom:1px solid " + BORDER + ";\">\n"
        + "      <p style=\"margin:0;"
        +           "font-family:" + FONT + ";"
        +           "font-size:13px;color:" + C_MID + ";"
        +           "mso-line-height-rule:exactly;line-height:1.7;\">"
        + "        <strong style=\"color:" + badgeColor + ";\">"
        +            "Information&nbsp;</strong>"
        + noteHtml
        + "      </p>\n"
        + "    </td>\n"
        + "  </tr>\n"
        + "  </table>\n"
        + "</td>\n"
        + "</tr>\n"
        + "<tr>\n"
        + "<td class=\"email-pad\""
        + " style=\"background-color:" + BG_CARD + ";padding:0 40px;\">\n"
        + "  <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\""
        + "   border=\"0\" width=\"100%\">\n"
        + "  <tr><td style=\"border-top:1px solid " + BORDER + ";"
        +     "font-size:0;line-height:0;mso-line-height-rule:exactly;\">&nbsp;</td></tr>\n"
        + "  </table>\n"
        + "</td>\n"
        + "</tr>\n"
        + "<tr>\n"
        + "<td class=\"email-pad\" align=\"center\""
        + " style=\"background-color:" + BG_FOOT + ";"
        +   "border-radius:0 0 16px 16px;"
        +   "padding:28px 40px 32px;\">\n"
        + "  <p style=\"margin:0 0 8px 0;"
        +     "font-family:" + FONT + ";"
        +     "font-size:12px;color:" + C_GREY + ";"
        +     "mso-line-height-rule:exactly;line-height:1.7;\">\n"
        + "    Cet e-mail a \u00e9t\u00e9 g\u00e9n\u00e9r\u00e9 automatiquement"
        + "    par <strong style=\"color:" + C_MID + ";\">MedicalRDV</strong>."
        + "    <br>Merci de ne pas r\u00e9pondre \u00e0 ce message.\n"
        + "  </p>\n"
        + "  <p style=\"margin:10px 0 0 0;"
        +     "font-family:" + FONT + ";"
        +     "font-size:11px;color:" + C_LITE + ";"
        +     "mso-line-height-rule:exactly;line-height:1.5;\">\n"
        + "    &copy; " + year
        + "    MedicalRDV \u2014 Plateforme de gestion de rendez-vous m\u00e9dicaux\n"
        + "  </p>\n"
        + "</td>\n"
        + "</tr>\n"
        + "</table>\n"
        + "<!--[if mso]></td></tr></table><![endif]-->\n"
        + "</td></tr>\n"
        + "</table>\n"
        + "</body>\n"
        + "</html>";
    }

    // =========================================================================
    //  TABLEAU D'INFORMATIONS — inchangé
    // =========================================================================

    private String infoTable(String[][] rows) {
        final String FONT = "Arial,Helvetica,sans-serif";
        StringBuilder sb = new StringBuilder();
        sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\""
                + " border=\"0\" width=\"100%\""
                + " style=\"border-collapse:collapse;margin:20px 0 4px 0;\">\n");

        for (int i = 0; i < rows.length; i++) {
            boolean only  = (rows.length == 1);
            boolean first = (i == 0);
            boolean last  = (i == rows.length - 1);

            String radLabel, radValue;
            if (only) {
                radLabel = "border-radius:8px 0 0 8px;";
                radValue = "border-radius:0 8px 8px 0;";
            } else if (first) {
                radLabel = "border-radius:8px 0 0 0;";
                radValue = "border-radius:0 8px 0 0;";
            } else if (last) {
                radLabel = "border-radius:0 0 0 8px;";
                radValue = "border-radius:0 0 8px 0;";
            } else {
                radLabel = "";
                radValue = "";
            }

            sb.append("<tr>\n")
              .append("  <td style=\""
                    + "padding:13px 16px;"
                    + "background-color:#dbeafe;"
                    + "border:1px solid #bfdbfe;"
                    + "font-family:" + FONT + ";"
                    + "font-size:12px;font-weight:700;"
                    + "color:#1e3a5f;"
                    + "width:36%;vertical-align:middle;"
                    + "mso-line-height-rule:exactly;line-height:1.5;"
                    + radLabel + "\">")
              .append(rows[i][0])
              .append("</td>\n")
              .append("  <td style=\""
                    + "padding:13px 16px;"
                    + "background-color:#ffffff;"
                    + "border:1px solid #bfdbfe;"
                    + "font-family:" + FONT + ";"
                    + "font-size:14px;"
                    + "color:#111827;"
                    + "vertical-align:middle;"
                    + "mso-line-height-rule:exactly;line-height:1.5;"
                    + radValue + "\">")
              .append(rows[i][1])
              .append("</td>\n")
              .append("</tr>\n");
        }

        sb.append("</table>");
        return sb.toString();
    }

    /** Échappe les caractères HTML pour prévenir les injections. */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#x27;");
    }

    /** Échappe les caractères spéciaux pour le JSON. */
    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }
}