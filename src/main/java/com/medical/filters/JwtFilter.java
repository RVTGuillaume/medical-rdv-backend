package com.medical.filters;

import com.medical.utils.JwtUtil;
import com.medical.utils.ResponseUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class JwtFilter implements Filter {

    /** Routes exclues du contrôle JWT */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        // Routes publiques → passer directement
        if (isPublic(path) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        // Lire le header Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ResponseUtil.unauthorized(response, "Token manquant ou invalide");
            return;
        }

        String token = authHeader.substring(7);
        if (!JwtUtil.isValid(token)) {
            ResponseUtil.unauthorized(response, "Token expiré ou invalide");
            return;
        }

        // Injecter les claims dans les attributs de la requête
        Claims claims = JwtUtil.validateToken(token);
        request.setAttribute("userId", claims.getSubject());
        request.setAttribute("email",  claims.get("email", String.class));
        request.setAttribute("role",   claims.get("role",  String.class));

        chain.doFilter(req, res);
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}