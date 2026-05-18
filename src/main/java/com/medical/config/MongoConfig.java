package com.medical.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Singleton — une seule connexion MongoDB pour tout le cycle de vie de l'app.
 */
public class MongoConfig {

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static String dbName;

    static {
        try (InputStream in = MongoConfig.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {

            Properties props = new Properties();
            props.load(in);

            // Dev local  → pas de variable d'env → utilise config.properties (inchangé)
            // Render prod → variable d'env prioritaire → config.properties ignoré
            String uri = System.getenv("MONGO_URI") != null
                    ? System.getenv("MONGO_URI")
                    : props.getProperty("mongo.uri");

            dbName = System.getenv("MONGO_DB") != null
                    ? System.getenv("MONGO_DB")
                    : props.getProperty("mongo.database");

            MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToConnectionPoolSettings(b -> b
                    .minSize(1)                              // 1 connexion toujours ouverte
                    .maxSize(10)                             // max 10 connexions parallèles
                    .maxConnectionIdleTime(60, TimeUnit.SECONDS)
                )
                .applyToServerSettings(b -> b
                    .heartbeatFrequency(30, TimeUnit.SECONDS)
                )
                .build();

            mongoClient = MongoClients.create(settings);
            database    = mongoClient.getDatabase(dbName);

            System.out.println("✅ MongoDB connecté : " + dbName);

        } catch (IOException e) {
            throw new RuntimeException("❌ Impossible de charger config.properties", e);
        }
    }

    public static MongoDatabase getDatabase() {
        return database;
    }

    /** Fermeture propre (appelée depuis un ContextListener si besoin) */
    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    private MongoConfig() {}
}