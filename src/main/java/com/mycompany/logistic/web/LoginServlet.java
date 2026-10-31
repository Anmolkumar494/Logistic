package com.mycompany.logistic.web;

import com.mycompany.logistic.model.AppUser;
import com.mycompany.logistic.security.PasswordUtil;
import jakarta.persistence.EntityManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

@WebServlet("/login")
public class LoginServlet extends JsonServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username;
        String password;
        if (request.getContentType() != null && request.getContentType().contains("application/json")) {
            Map<String, Object> input = body(request);
            username = stringValue(input.get("username"), "");
            password = stringValue(input.get("password"), "");
        } else {
            username = stringValue(request.getParameter("username"), "");
            password = stringValue(request.getParameter("password"), "");
        }
        if (username.isBlank() || password.isBlank()) {
            error(response, 400, "Enter your username and password.");
            return;
        }
        try (EntityManager em = entityManager()) {
            AppUser user = em.createQuery("select u from AppUser u where u.username = :username and u.active = true",
                    AppUser.class).setParameter("username", username).getResultStream().findFirst().orElse(null);
            if (user == null || !PasswordUtil.matches(password, user.passwordHash)) {
                error(response, 401, "The username or password is incorrect.");
                return;
            }
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute("userId", user.id);
            session.setAttribute("username", user.username);
            session.setAttribute("fullName", user.fullName);
            Object configuredBootstrap = getServletContext().getAttribute("bootstrapAdminUsername");
            String bootstrapUsername = configuredBootstrap instanceof String ? (String) configuredBootstrap : "";
            String effectiveRole = !bootstrapUsername.isBlank() && bootstrapUsername.equalsIgnoreCase(user.username)
                    ? "SUPER_ADMIN" : user.role;
            session.setAttribute("role", effectiveRole);
            response.setHeader("Set-Cookie", "JSESSIONID=" + session.getId()
                    + "; Path=" + request.getContextPath() + "; HttpOnly; SameSite=Lax"
                    + (request.isSecure() ? "; Secure" : ""));
            json(response, 200, Map.of("user", Map.of("name", user.fullName, "role", effectiveRole)));
        } catch (RuntimeException ex) {
            getServletContext().log("Login failed due to a database error.", ex);
            error(response, 500, "Sign-in is temporarily unavailable.");
        }
    }
}
