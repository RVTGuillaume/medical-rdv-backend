package com.medical.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("config.properties")) {
            Properties props = new Properties();
            props.load(in);

            cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", props.getProperty("cloudinary.cloud_name"),
                "api_key",    props.getProperty("cloudinary.api_key"),
                "api_secret", props.getProperty("cloudinary.api_secret"),
                "secure",     true
            ));
        } catch (IOException e) {
            throw new RuntimeException("Erreur init CloudinaryService", e);
        }
    }

    /**
     * Upload d'un fichier image (byte[]) vers Cloudinary.
     * @param imageBytes  contenu du fichier
     * @param folder      dossier Cloudinary ("patients" ou "medecins")
     * @param publicId    identifiant unique de l'image (ex: "pat_001")
     * @return URL sécurisée de l'image uploadée
     */
    @SuppressWarnings("unchecked")
    public String uploadImage(byte[] imageBytes, String folder, String publicId)
            throws IOException {

        Map<String, Object> options = ObjectUtils.asMap(
            "folder",          folder,
            "public_id",       publicId,
            "overwrite",       true,
            "resource_type",   "image",
            "transformation",  ObjectUtils.asMap(
                "width",  400,
                "height", 400,
                "crop",   "fill",
                "gravity","face"
            )
        );

        Map<?, ?> result = cloudinary.uploader().upload(imageBytes, options);
        return (String) result.get("secure_url");
    }

    /**
     * Supprime une image Cloudinary par son public_id complet (folder/publicId).
     */
    public void deleteImage(String fullPublicId) {
        try {
            cloudinary.uploader().destroy(fullPublicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            System.err.println("❌ Erreur suppression Cloudinary : " + e.getMessage());
        }
    }
}