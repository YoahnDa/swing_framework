package com.framework.connection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class ConfigReader {
    public static Properties loadConfig() throws Exception {
        Properties props = new Properties();
        File envFile = new File(".env");

        if (envFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        props.setProperty(parts[0].trim(), parts[1].trim());
                    }
                }
                System.out.println("[Framework] Configuration chargee depuis .env");
            }
        } else {
            throw new RuntimeException("Aucun fichier de configuration trouve (ni database.xml, ni .env)");
        }

        return props;
    }
}
