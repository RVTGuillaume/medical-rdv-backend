package com.medical.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Properties;

public class JwtUtil {

    private static final Key SECRET_KEY;
    private static final long EXPIRATION;

    static {
        try (InputStream in = JwtUtil.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            Properties props = new Properties();
            props.load(in);
            String secret = props.getProperty("jwt.secret");
            EXPIRATION = Long.parseLong(props.getProperty("jwt.expiration", "86400000"));
            SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Erreur init JwtUtil", e);
        }
    }

    /** Génère un token JWT contenant l'id, email et rôle de l'utilisateur */
    public static String generateToken(String userId, String email, String role) {
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SECRET_KEY)
                .compact();
    }

    /** Valide et retourne les claims du token */
    public static Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Extrait uniquement le rôle */
    public static String getRoleFromToken(String token) {
        return (String) validateToken(token).get("role");
    }

    /** Extrait l'id utilisateur (subject) */
    public static String getUserIdFromToken(String token) {
        return validateToken(token).getSubject();
    }

    /** Vérifie si le token est valide sans lever d'exception */
    public static boolean isValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}