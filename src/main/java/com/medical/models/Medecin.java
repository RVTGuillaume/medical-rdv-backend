package com.medical.models;

import org.bson.types.ObjectId;

public class Medecin {
    private ObjectId id;
    private String idmed;           // identifiant métier lisible
    private String nommed;
    private String specialite;
    private int taux_horaire;
    private String lieu;
    private String email;
    private String password;
    private String telephone;
    private String photoUrl;
    private String role = "medecin";
    private boolean actif = true;
    private long createdAt;

    public Medecin() {
        this.createdAt = System.currentTimeMillis();
    }

    // ===== Getters / Setters =====
    public ObjectId getId()             { return id; }
    public void setId(ObjectId id)      { this.id = id; }

    public String getIdmed()            { return idmed; }
    public void setIdmed(String idmed)  { this.idmed = idmed; }

    public String getNommed()           { return nommed; }
    public void setNommed(String n)     { this.nommed = n; }

    public String getSpecialite()       { return specialite; }
    public void setSpecialite(String s) { this.specialite = s; }

    public int getTaux_horaire()        { return taux_horaire; }
    public void setTaux_horaire(int t)  { this.taux_horaire = t; }

    public String getLieu()             { return lieu; }
    public void setLieu(String l)       { this.lieu = l; }

    public String getEmail()            { return email; }
    public void setEmail(String e)      { this.email = e; }

    public String getPassword()         { return password; }
    public void setPassword(String p)   { this.password = p; }

    public String getTelephone()        { return telephone; }
    public void setTelephone(String t)  { this.telephone = t; }

    public String getPhotoUrl()         { return photoUrl; }
    public void setPhotoUrl(String u)   { this.photoUrl = u; }

    public String getRole()             { return role; }
    public void setRole(String role)    { this.role = role; }

    public boolean isActif()            { return actif; }
    public void setActif(boolean a)     { this.actif = a; }

    public long getCreatedAt()          { return createdAt; }
    public void setCreatedAt(long t)    { this.createdAt = t; }
}