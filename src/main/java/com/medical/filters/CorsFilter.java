package com.medical.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CorsFilter implements Filter {

    private String allowedOrigin;

    @Override
    public void init(FilterConfig config) throws ServletException {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("config.properties")) {
            Properties props = new Properties();
            props.load(in);
            allowedOrigin = props.getProperty("app.cors.origin", "http://localhost:5173");
        } catch (IOException e) {
            allowedOrigin = "http://localhost:5173";
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        response.setHeader("Access-Control-Allow-Origin",  allowedOrigin);
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");

        // Pré-vol OPTIONS → répondre 200 immédiatement
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }
}