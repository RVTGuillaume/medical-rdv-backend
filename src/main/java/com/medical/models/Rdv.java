package com.medical.models;

import org.bson.types.ObjectId;

public class Rdv {
    private ObjectId id;
    private String idrdv;           // identifiant métier lisible
    private String idmed;
    private String idpat;
    private String dateRdv;         // ISO-8601 : "2025-06-15T10:00:00"
    private String status;          // PENDING | CONFIRMED | CANCELLED
    private String motif;
    private String nomMedecin;      // dénormalisation pour affichage rapide
    private String specialiteMedecin;
    private String nomPatient;
    private long createdAt;
    private long updatedAt;

    public Rdv() {
        this.status    = "PENDING";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // ===== Getters / Setters =====
    public ObjectId getId()              { return id; }
    public void setId(ObjectId id)       { this.id = id; }

    public String getIdrdv()             { return idrdv; }
    public void setIdrdv(String idrdv)   { this.idrdv = idrdv; }

    public String getIdmed()             { return idmed; }
    public void setIdmed(String idmed)   { this.idmed = idmed; }

    public String getIdpat()             { return idpat; }
    public void setIdpat(String idpat)   { this.idpat = idpat; }

    public String getDateRdv()           { return dateRdv; }
    public void setDateRdv(String d)     { this.dateRdv = d; }

    public String getStatus()            { return status; }
    public void setStatus(String s)      { this.status = s; this.updatedAt = System.currentTimeMillis(); }

    public String getMotif()             { return motif; }
    public void setMotif(String m)       { this.motif = m; }

    public String getNomMedecin()        { return nomMedecin; }
    public void setNomMedecin(String n)  { this.nomMedecin = n; }

    public String getSpecialiteMedecin()        { return specialiteMedecin; }
    public void setSpecialiteMedecin(String s)  { this.specialiteMedecin = s; }

    public String getNomPatient()        { return nomPatient; }
    public void setNomPatient(String n)  { this.nomPatient = n; }

    public long getCreatedAt()           { return createdAt; }
    public void setCreatedAt(long t)     { this.createdAt = t; }

    public long getUpdatedAt()           { return updatedAt; }
    public void setUpdatedAt(long t)     { this.updatedAt = t; }
}