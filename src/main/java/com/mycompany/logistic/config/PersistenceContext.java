package com.mycompany.logistic.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.HashMap;
import java.util.Map;

@WebListener
public class PersistenceContext implements ServletContextListener {
    public static final String ENTITY_MANAGER_FACTORY = "entityManagerFactory";
    private EntityManagerFactory entityManagerFactory;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        AppConfig.initialize(event.getServletContext());
        int sessionTimeout = Integer.parseInt(AppConfig.get("APP_SESSION_TIMEOUT_MINUTES", "30"));
        if (sessionTimeout < 5 || sessionTimeout > 1440) {
            throw new IllegalStateException("APP_SESSION_TIMEOUT_MINUTES must be between 5 and 1440.");
        }
        event.getServletContext().setSessionTimeout(sessionTimeout);
        String url = AppConfig.get("DB_URL", "");
        String user = AppConfig.get("DB_USER", "");
        String password = AppConfig.get("DB_PASSWORD", "");
        if (url.isBlank() || user.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Set DB_URL, DB_USER, and DB_PASSWORD in the process environment or .env file.");
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.driver", AppConfig.get("DB_DRIVER", "com.mysql.cj.jdbc.Driver"));
        properties.put("jakarta.persistence.jdbc.url", url);
        properties.put("jakarta.persistence.jdbc.user", user);
        properties.put("jakarta.persistence.jdbc.password", password);
        properties.put("hibernate.hbm2ddl.auto", AppConfig.get("HIBERNATE_DDL_AUTO", "validate"));
        properties.put("hibernate.show_sql", AppConfig.get("HIBERNATE_SHOW_SQL", "false"));
        properties.put("hibernate.jdbc.time_zone", "UTC");
        entityManagerFactory = Persistence.createEntityManagerFactory("logisticsPU", properties);
        event.getServletContext().setAttribute(ENTITY_MANAGER_FACTORY, entityManagerFactory);
        String bootstrapUsername = BootstrapAdmin.createIfConfigured(entityManagerFactory, event.getServletContext());
        if (bootstrapUsername != null) {
            event.getServletContext().setAttribute("bootstrapAdminUsername", bootstrapUsername);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }
}
