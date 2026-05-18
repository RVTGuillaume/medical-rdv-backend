package com.medical.models;

import org.bson.types.ObjectId;

public class Horaire {
    private ObjectId id;
    private String idmed;
    private String dateHeure;       // ISO-8601 : "2025-06-15T10:00:00"
    private boolean disponible = true;
    private String idrdv;           // null si disponible, sinon ID du RDV qui occupe ce créneau
    private long createdAt;

    public Horaire() {
        this.createdAt = System.currentTimeMillis();
    }

    // ===== Getters / Setters =====
    public ObjectId getId()             { return id; }
    public void setId(ObjectId id)      { this.id = id; }

    /**
     * Retourne l'ObjectId sous forme de chaîne hexadécimale (24 caractères).
     * Utilisé par Jackson pour sérialiser l'id en JSON lisible côté frontend.
     */
    public String getIdStr() {
        return id != null ? id.toHexString() : null;
    }

    public String getIdmed()            { return idmed; }
    public void setIdmed(String idmed)  { this.idmed = idmed; }

    public String getDateHeure()        { return dateHeure; }
    public void setDateHeure(String d)  { this.dateHeure = d; }

    public boolean isDisponible()       { return disponible; }
    public void setDisponible(boolean d){ this.disponible = d; }

    public String getIdrdv()            { return idrdv; }
    public void setIdrdv(String idrdv)  { this.idrdv = idrdv; }

    public long getCreatedAt()          { return createdAt; }
    public void setCreatedAt(long t)    { this.createdAt = t; }
}