package com.medical.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import java.time.LocalDate;

public class Patient {
    @JsonIgnore
    private ObjectId id;
    private String idpat;
    private String nom_pat;
    private LocalDate datenais;
    private String email;
    private String password;
    private String telephone;
    private String photoUrl;
    private String role = "patient";
    private boolean actif = true;
    private long createdAt;

    public Patient() {
        this.createdAt = System.currentTimeMillis();
    }

    // ===== Getters / Setters =====
    @JsonIgnore
    public ObjectId getId()             { return id; }
    public void setId(ObjectId id)      { this.id = id; }

    public String getIdpat()            { return idpat; }
    public void setIdpat(String idpat)  { this.idpat = idpat; }

    public String getNom_pat()          { return nom_pat; }
    public void setNom_pat(String n)    { this.nom_pat = n; }

    // Retourne String pour que Jackson puisse sérialiser en JSON
    public String getDatenais()             { return datenais != null ? datenais.toString() : null; }
    public void setDatenais(LocalDate d)    { this.datenais = d; }

    public String getEmail()            { return email; }
    public void setEmail(String email)  { this.email = email; }

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