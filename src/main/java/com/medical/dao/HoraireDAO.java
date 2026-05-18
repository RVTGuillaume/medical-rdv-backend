package com.medical.dao;

import com.medical.config.MongoConfig;
import com.medical.models.Horaire;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

public class HoraireDAO {

    private final MongoCollection<Document> col =
            MongoConfig.getDatabase().getCollection("horaires");

    public HoraireDAO() {
        // Index composé unique : un médecin ne peut pas avoir deux créneaux identiques
        col.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("idmed"),
                Indexes.ascending("dateHeure")
            ),
            new IndexOptions().unique(true)
        );
        col.createIndex(Indexes.ascending("idmed"));
        col.createIndex(Indexes.ascending("disponible"));
    }

    // ===== CREATE =====
    public void insert(Horaire h) {
        col.insertOne(toDoc(h));
    }

    public void insertMany(List<Horaire> horaires) {
        List<Document> docs = new ArrayList<>();
        horaires.forEach(h -> docs.add(toDoc(h)));
        col.insertMany(docs);
    }

    // ===== READ =====
    public Horaire findById(String objectId) {
        Document doc = col.find(eq("_id", new ObjectId(objectId))).first();
        return doc != null ? fromDoc(doc) : null;
    }

    /** Tous les créneaux d'un médecin */
    public List<Horaire> findByMedecin(String idmed) {
        List<Horaire> list = new ArrayList<>();
        col.find(eq("idmed", idmed))
           .sort(Sorts.ascending("dateHeure"))
           .forEach(doc -> list.add(fromDoc(doc)));
        return list;
    }

    /** Créneaux disponibles seulement */
    public List<Horaire> findDisponiblesByMedecin(String idmed) {
        List<Horaire> list = new ArrayList<>();
        col.find(and(eq("idmed", idmed), eq("disponible", true)))
           .sort(Sorts.ascending("dateHeure"))
           .forEach(doc -> list.add(fromDoc(doc)));
        return list;
    }

    /** Vérifie si un créneau précis est disponible */
    public boolean isDisponible(String idmed, String dateHeure) {
        Document doc = col.find(and(
                eq("idmed", idmed),
                eq("dateHeure", dateHeure),
                eq("disponible", true)
        )).first();
        return doc != null;
    }

    // ===== UPDATE =====
    /** Marque le créneau comme réservé et y associe l'idrdv */
    public void marquerReserve(String idmed, String dateHeure, String idrdv) {
        col.updateOne(
            and(eq("idmed", idmed), eq("dateHeure", dateHeure)),
            new Document("$set", new Document("disponible", false)
                    .append("idrdv", idrdv))
        );
    }

    /** Libère un créneau (annulation RDV) */
    public void liberer(String idmed, String dateHeure) {
        col.updateOne(
            and(eq("idmed", idmed), eq("dateHeure", dateHeure)),
            new Document("$set", new Document("disponible", true)
                    .append("idrdv", null))
        );
    }

    // ===== DELETE =====
    public void delete(String objectId) {
        col.deleteOne(eq("_id", new ObjectId(objectId)));
    }

    public void deleteByMedecin(String idmed) {
        col.deleteMany(eq("idmed", idmed));
    }

    // ===== MAPPING =====
    private Document toDoc(Horaire h) {
        Document doc = new Document();
        if (h.getId() != null) doc.append("_id", h.getId());
        doc.append("idmed",      h.getIdmed());
        doc.append("dateHeure",  h.getDateHeure());
        doc.append("disponible", h.isDisponible());
        doc.append("idrdv",      h.getIdrdv());
        doc.append("createdAt",  h.getCreatedAt());
        return doc;
    }

    private Horaire fromDoc(Document doc) {
        Horaire h = new Horaire();
        h.setId(doc.getObjectId("_id"));
        h.setIdmed(doc.getString("idmed"));
        h.setDateHeure(doc.getString("dateHeure"));
        Boolean d = doc.getBoolean("disponible");
        h.setDisponible(d != null ? d : true);
        h.setIdrdv(doc.getString("idrdv"));
        Long ca = doc.getLong("createdAt"); if (ca != null) h.setCreatedAt(ca);
        return h;
    }
}