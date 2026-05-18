package com.medical.dao;

import com.medical.config.MongoConfig;
import com.medical.models.Medecin;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;

public class MedecinDAO {

    private final MongoCollection<Document> col =
            MongoConfig.getDatabase().getCollection("medecins");

    public MedecinDAO() {
        col.createIndex(Indexes.ascending("email"),     new IndexOptions().unique(true));
        col.createIndex(Indexes.ascending("idmed"),     new IndexOptions().unique(true));
        col.createIndex(Indexes.ascending("specialite"));
        col.createIndex(Indexes.text("nommed"));
        // sparse=true : les médecins sans téléphone ne bloquent pas l'index unique
        col.createIndex(Indexes.ascending("telephone"), new IndexOptions().unique(true).sparse(true));
    }

    // ===== CREATE =====
    public void insert(Medecin m) {
        col.insertOne(toDoc(m));
    }

    // ===== READ =====
    public Medecin findByEmail(String email) {
        Document doc = col.find(eq("email", email)).first();
        return doc != null ? fromDoc(doc) : null;
    }

    public Medecin findByIdmed(String idmed) {
        Document doc = col.find(eq("idmed", idmed)).first();
        return doc != null ? fromDoc(doc) : null;
    }

    public Medecin findById(String objectId) {
        Document doc = col.find(eq("_id", new ObjectId(objectId))).first();
        return doc != null ? fromDoc(doc) : null;
    }

    public List<Medecin> findAll() {
        List<Medecin> list = new ArrayList<>();
        col.find().forEach(doc -> list.add(fromDoc(doc)));
        return list;
    }

    public List<Medecin> searchByNom(String keyword) {
        List<Medecin> list = new ArrayList<>();
        Bson filter = regex("nommed", keyword, "i");
        col.find(filter).forEach(doc -> list.add(fromDoc(doc)));
        return list;
    }

    public List<Medecin> findBySpecialite(String specialite) {
        List<Medecin> list = new ArrayList<>();
        col.find(eq("specialite", specialite)).forEach(doc -> list.add(fromDoc(doc)));
        return list;
    }

    public List<Document> top5Consultes() {
        MongoCollection<Document> rdvCol = MongoConfig.getDatabase().getCollection("rdv");
        List<Document> result = new ArrayList<>();
        rdvCol.aggregate(Arrays.asList(
                match(ne("status", "CANCELLED")),
                group("$idmed", Accumulators.sum("total", 1)),
                sort(Sorts.descending("total")),
                limit(5),
                lookup("medecins", "_id", "idmed", "medecin"),
                unwind("$medecin"),
                project(new Document("idmed", "$_id")
                        .append("total", 1)
                        .append("nommed", "$medecin.nommed")
                        .append("specialite", "$medecin.specialite"))
        )).forEach(result::add);
        return result;
    }

    public boolean emailExists(String email) {
        return col.find(eq("email", email)).first() != null;
    }

    /**
     * Vérifie si ce téléphone est déjà utilisé par un médecin.
     * Utilisé à la CRÉATION.
     */
    public boolean phoneExists(String telephone) {
        if (telephone == null || telephone.isBlank()) return false;
        return col.find(eq("telephone", telephone.trim())).first() != null;
    }

    /**
     * Vérifie si ce téléphone est utilisé par un AUTRE médecin (hors idmed courant).
     * Utilisé à la MODIFICATION pour permettre de conserver son propre numéro.
     */
    public boolean phoneExistsExcluding(String telephone, String idmed) {
        if (telephone == null || telephone.isBlank()) return false;
        return col.find(and(
                eq("telephone", telephone.trim()),
                ne("idmed", idmed)
        )).first() != null;
    }

    // ===== UPDATE =====
    public void update(Medecin m) {
        col.replaceOne(eq("_id", m.getId()), toDoc(m));
    }

    public void updatePhotoUrl(String idmed, String url) {
        col.updateOne(eq("idmed", idmed),
                new Document("$set", new Document("photoUrl", url)));
    }

    // ===== DELETE =====
    public void delete(String idmed) {
        col.deleteOne(eq("idmed", idmed));
    }

    // ===== MAPPING =====
    private Document toDoc(Medecin m) {
        Document doc = new Document();
        if (m.getId() != null) doc.append("_id", m.getId());
        doc.append("idmed",        m.getIdmed());
        doc.append("nommed",       m.getNommed());
        doc.append("specialite",   m.getSpecialite());
        doc.append("taux_horaire", m.getTaux_horaire());
        doc.append("lieu",         m.getLieu());
        doc.append("email",        m.getEmail());
        doc.append("password",     m.getPassword());
        // Stocker null si vide pour que l'index sparse ne bloque pas
        String tel = m.getTelephone();
        doc.append("telephone", (tel != null && !tel.isBlank()) ? tel.trim() : null);
        doc.append("photoUrl",     m.getPhotoUrl());
        doc.append("role",         m.getRole());
        doc.append("actif",        m.isActif());
        doc.append("createdAt",    m.getCreatedAt());
        return doc;
    }

    private Medecin fromDoc(Document doc) {
        Medecin m = new Medecin();
        m.setId(doc.getObjectId("_id"));
        m.setIdmed(doc.getString("idmed"));
        m.setNommed(doc.getString("nommed"));
        m.setSpecialite(doc.getString("specialite"));
        Integer th = doc.getInteger("taux_horaire");
        if (th != null) m.setTaux_horaire(th);
        m.setLieu(doc.getString("lieu"));
        m.setEmail(doc.getString("email"));
        m.setPassword(doc.getString("password"));
        m.setTelephone(doc.getString("telephone"));
        m.setPhotoUrl(doc.getString("photoUrl"));
        m.setRole(doc.getString("role"));
        Boolean actif = doc.getBoolean("actif");
        m.setActif(actif != null ? actif : true);
        Long created = doc.getLong("createdAt");
        if (created != null) m.setCreatedAt(created);
        return m;
    }
}