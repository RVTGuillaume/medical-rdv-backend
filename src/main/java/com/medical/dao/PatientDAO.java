package com.medical.dao;

import com.medical.config.MongoConfig;
import com.medical.models.Patient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

public class PatientDAO {

    private final MongoCollection<Document> col =
            MongoConfig.getDatabase().getCollection("patients");

    public PatientDAO() {
        col.createIndex(Indexes.ascending("email"),
                new IndexOptions().unique(true));
        col.createIndex(Indexes.ascending("idpat"),
                new IndexOptions().unique(true));
        // sparse=true : les patients sans téléphone ne bloquent pas l'index unique
        col.createIndex(Indexes.ascending("telephone"),
                new IndexOptions().unique(true).sparse(true));
    }

    // ===== CREATE =====
    public void insert(Patient p) {
        col.insertOne(toDoc(p));
    }

    // ===== READ =====
    public Patient findByEmail(String email) {
        Document doc = col.find(eq("email", email)).first();
        return doc != null ? fromDoc(doc) : null;
    }

    public Patient findByIdpat(String idpat) {
        Document doc = col.find(eq("idpat", idpat)).first();
        return doc != null ? fromDoc(doc) : null;
    }

    public Patient findById(String objectId) {
        Document doc = col.find(eq("_id", new ObjectId(objectId))).first();
        return doc != null ? fromDoc(doc) : null;
    }

    public List<Patient> findAll() {
        List<Patient> list = new ArrayList<>();
        col.find().forEach(doc -> list.add(fromDoc(doc)));
        return list;
    }

    public boolean emailExists(String email) {
        return col.find(eq("email", email)).first() != null;
    }

    /**
     * Vérifie si ce téléphone est déjà utilisé par un patient.
     * Utilisé à la CRÉATION.
     */
    public boolean phoneExists(String telephone) {
        if (telephone == null || telephone.isBlank()) return false;
        return col.find(eq("telephone", telephone.trim())).first() != null;
    }

    /**
     * Vérifie si ce téléphone est utilisé par un AUTRE patient (hors idpat courant).
     * Utilisé à la MODIFICATION pour permettre de conserver son propre numéro.
     */
    public boolean phoneExistsExcluding(String telephone, String idpat) {
        if (telephone == null || telephone.isBlank()) return false;
        return col.find(and(
                eq("telephone", telephone.trim()),
                ne("idpat", idpat)
        )).first() != null;
    }

    // ===== UPDATE =====
    public void update(Patient p) {
        col.replaceOne(eq("_id", p.getId()), toDoc(p));
    }

    public void updatePhotoUrl(String idpat, String url) {
        col.updateOne(eq("idpat", idpat),
                new Document("$set", new Document("photoUrl", url)));
    }

    // ===== DELETE =====
    public void delete(String idpat) {
        col.deleteOne(eq("idpat", idpat));
    }

    // ===== MAPPING =====
    private Document toDoc(Patient p) {
        Document doc = new Document();
        if (p.getId() != null) doc.append("_id", p.getId());
        doc.append("idpat",     p.getIdpat());
        doc.append("nom_pat",   p.getNom_pat());
        doc.append("datenais",  p.getDatenais() != null ? p.getDatenais().toString() : null);
        doc.append("email",     p.getEmail());
        doc.append("password",  p.getPassword());
        // Stocker null si vide pour que l'index sparse ne bloque pas
        String tel = p.getTelephone();
        doc.append("telephone", (tel != null && !tel.isBlank()) ? tel.trim() : null);
        doc.append("photoUrl",  p.getPhotoUrl());
        doc.append("role",      p.getRole());
        doc.append("actif",     p.isActif());
        doc.append("createdAt", p.getCreatedAt());
        return doc;
    }

    private Patient fromDoc(Document doc) {
        Patient p = new Patient();
        p.setId(doc.getObjectId("_id"));
        p.setIdpat(doc.getString("idpat"));
        p.setNom_pat(doc.getString("nom_pat"));
        String dn = doc.getString("datenais");
        if (dn != null) p.setDatenais(LocalDate.parse(dn));
        p.setEmail(doc.getString("email"));
        p.setPassword(doc.getString("password"));
        p.setTelephone(doc.getString("telephone"));
        p.setPhotoUrl(doc.getString("photoUrl"));
        p.setRole(doc.getString("role"));
        Boolean actif = doc.getBoolean("actif");
        p.setActif(actif != null ? actif : true);
        Long created = doc.getLong("createdAt");
        if (created != null) p.setCreatedAt(created);
        return p;
    }
}