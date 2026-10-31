package com.mycompany.logistic.config;

import com.mycompany.logistic.security.PasswordUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.servlet.ServletContext;
import java.util.UUID;
import com.mycompany.logistic.model.AppUser;

final class BootstrapAdmin {
    private BootstrapAdmin() {
    }

    static String createIfConfigured(EntityManagerFactory factory, ServletContext context) {
        String username = AppConfig.get("APP_BOOTSTRAP_ADMIN_USERNAME", "");
        String password = AppConfig.get("APP_BOOTSTRAP_ADMIN_PASSWORD", "");
        if (username.isBlank() && password.isBlank()) {
            return null;
        }
        if (username.isBlank() || password.length() < 12) {
            context.log("Bootstrap administrator is disabled: configure both values and use a password of at least 12 characters.");
            return null;
        }
        EntityManager em = factory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            AppUser existing = em.createQuery("select u from AppUser u where u.username = :username", AppUser.class)
                    .setParameter("username", username).getResultStream().findFirst().orElse(null);
            if (existing == null) {
                AppUser admin = new AppUser();
                admin.id = UUID.randomUUID().toString();
                admin.username = username;
                admin.fullName = "System Administrator";
                admin.role = "ADMIN";
                admin.active = true;
                admin.passwordHash = PasswordUtil.hash(password);
                em.persist(admin);
            }
            tx.commit();
            return username;
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }
}
