package com.medical.dao;

import com.medical.config.MongoConfig;
import com.medical.models.Rdv;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

public class RdvDAO {

    private final MongoCollection<Document> col =
            MongoConfig.getDatabase().getCollection("rdv");

    public RdvDAO() {
        col.createIndex(Indexes.ascending("idrdv"),   new IndexOptions().unique(true));
        col.createIndex(Indexes.ascending("idmed"));
        col.createIndex(Indexes.ascending("idpat"));
        col.createIndex(Indexes.ascending("status"));
        // Index composé anti-doublon créneau
        col.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("idmed"),
                Indexes.ascending("dateRdv"),
                Indexes.ascending("status")
            )
        );
    }

    // ===== CREATE =====
    public void insert(Rdv r) {
        col.insertOne(toDoc(r));
    }

    // ===== READ =====
    public Rdv findByIdrdv(String idrdv) {
        Document doc = col.find(eq("idrdv", idrdv)).first();
        return doc != null ? fromDoc(doc) : null;
    }

    public Rdv findById(String objectId) {
        Document doc = col.find(eq("_id", new ObjectId(objectId))).first();
        return doc != null ? fromDoc(doc) : null;
    }

    public List<Rdv> findByPatient(String idpat) {
        List<Rdv> list = new ArrayList<>();
        col.find(eq("idpat", idpat))
           .sort(Sorts.descending("createdAt"))
           .forEach(doc -> list.add(fromDoc(doc)));
        return list;
    }

    public List<Rdv> findByMedecin(String idmed) {
        List<Rdv> list = new ArrayList<>();
        col.find(eq("idmed", idmed))
           .sort(Sorts.ascending("dateRdv"))
           .forEach(doc -> list.add(fromDoc(doc)));
        return list;
    }

    public List<Rdv> findAll() {
        List<Rdv> list = new ArrayList<>();
        col.find().sort(Sorts.descending("createdAt"))
           .forEach(doc -> list.add(fromDoc(doc)));
        return list;
    }

    /**
     * Vérifie qu'un créneau est libre pour ce médecin.
     * Un créneau est considéré pris si un RDV non-annulé existe
     * pour le même médecin à la même dateRdv.
     */
    public boolean creneauPris(String idmed, String dateRdv) {
        return col.find(and(
                eq("idmed", idmed),
                eq("dateRdv", dateRdv),
                ne("status", "CANCELLED")
        )).first() != null;
    }

    // ===== UPDATE =====
    public void updateStatus(String idrdv, String status) {
        col.updateOne(
            eq("idrdv", idrdv),
            new Document("$set", new Document("status", status)
                    .append("updatedAt", System.currentTimeMillis()))
        );
    }

    // ===== DELETE =====
    public void delete(String idrdv) {
        col.deleteOne(eq("idrdv", idrdv));
    }

    // ===== MAPPING =====
    private Document toDoc(Rdv r) {
        Document doc = new Document();
        if (r.getId() != null) doc.append("_id", r.getId());
        doc.append("idrdv",            r.getIdrdv());
        doc.append("idmed",            r.getIdmed());
        doc.append("idpat",            r.getIdpat());
        doc.append("dateRdv",          r.getDateRdv());
        doc.append("status",           r.getStatus());
        doc.append("motif",            r.getMotif());
        doc.append("nomMedecin",       r.getNomMedecin());
        doc.append("specialiteMedecin",r.getSpecialiteMedecin());
        doc.append("nomPatient",       r.getNomPatient());
        doc.append("createdAt",        r.getCreatedAt());
        doc.append("updatedAt",        r.getUpdatedAt());
        return doc;
    }

    private Rdv fromDoc(Document doc) {
        Rdv r = new Rdv();
        r.setId(doc.getObjectId("_id"));
        r.setIdrdv(doc.getString("idrdv"));
        r.setIdmed(doc.getString("idmed"));
        r.setIdpat(doc.getString("idpat"));
        r.setDateRdv(doc.getString("dateRdv"));
        r.setStatus(doc.getString("status"));
        r.setMotif(doc.getString("motif"));
        r.setNomMedecin(doc.getString("nomMedecin"));
        r.setSpecialiteMedecin(doc.getString("specialiteMedecin"));
        r.setNomPatient(doc.getString("nomPatient"));
        Long ca = doc.getLong("createdAt"); if (ca != null) r.setCreatedAt(ca);
        Long ua = doc.getLong("updatedAt"); if (ua != null) r.setUpdatedAt(ua);
        return r;
    }
}