package com.mycompany.logistic.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.ServletContext;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppConfig {
    private static Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private AppConfig() {
    }

    public static void initialize(ServletContext context) {
        String explicitFile = System.getenv("APP_ENV_FILE");
        if (explicitFile == null || explicitFile.isBlank()) {
            explicitFile = System.getProperty("app.env.file");
        }
        if (explicitFile != null && !explicitFile.isBlank()) {
            Path file = Path.of(explicitFile).toAbsolutePath().normalize();
            if (!Files.isRegularFile(file)) {
                throw new IllegalStateException("APP_ENV_FILE does not point to a readable .env file.");
            }
            dotenv = Dotenv.configure().directory(file.getParent().toString())
                    .filename(file.getFileName().toString()).ignoreIfMissing().load();
            return;
        }

        Path webRoot = null;
        String realPath = context.getRealPath("/");
        if (realPath != null) {
            webRoot = Path.of(realPath);
        }
        Path workingDirectory = Path.of(System.getProperty("user.dir", "."));
        for (Path start : new Path[]{webRoot, workingDirectory}) {
            Path candidate = start;
            for (int level = 0; candidate != null && level < 9; level++, candidate = candidate.getParent()) {
                Path envFile = candidate.resolve(".env");
                if (Files.isRegularFile(envFile)) {
                    dotenv = Dotenv.configure().directory(candidate.toString()).ignoreIfMissing().load();
                    return;
                }
            }
        }
    }

    public static String get(String key, String fallback) {
        String systemValue = System.getenv(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        String fileValue = dotenv.get(key);
        return fileValue == null || fileValue.isBlank() ? fallback : fileValue;
    }
}
