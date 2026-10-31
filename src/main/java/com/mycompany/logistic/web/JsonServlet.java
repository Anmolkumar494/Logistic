package com.mycompany.logistic.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.logistic.config.PersistenceContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public abstract class JsonServlet extends HttpServlet {
    protected static final ObjectMapper JSON = new ObjectMapper();

    protected EntityManager entityManager() {
        EntityManagerFactory factory = (EntityManagerFactory) getServletContext()
                .getAttribute(PersistenceContext.ENTITY_MANAGER_FACTORY);
        if (factory == null || !factory.isOpen()) {
            throw new IllegalStateException("Database connection is unavailable.");
        }
        return factory.createEntityManager();
    }

    protected Map<String, Object> body(HttpServletRequest request) throws IOException {
        return JSON.readValue(request.getInputStream(), JSON.getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class));
    }

    protected void json(HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JSON.writeValue(response.getOutputStream(), value);
    }

    protected void error(HttpServletResponse response, int status, String message) throws IOException {
        json(response, status, Map.of("error", message));
    }

    protected String currentUserId(HttpServletRequest request) {
        Object value = request.getSession(false) == null ? null : request.getSession(false).getAttribute("userId");
        return value instanceof String ? (String) value : null;
    }

    protected String currentRole(HttpServletRequest request) {
        Object value = request.getSession(false) == null ? null : request.getSession(false).getAttribute("role");
        return value instanceof String ? (String) value : null;
    }

    protected boolean requireRole(HttpServletRequest request, HttpServletResponse response, String... roles)
            throws IOException {
        String role = currentRole(request);
        if ("SUPER_ADMIN".equals(role)) {
            return true;
        }
        for (String allowed : roles) {
            if (allowed.equals(role)) {
                return true;
            }
        }
        error(response, role == null ? 401 : 403, role == null ? "Sign in to continue." : "Your role cannot perform this action.");
        return false;
    }

    protected static String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value).trim();
    }
}
