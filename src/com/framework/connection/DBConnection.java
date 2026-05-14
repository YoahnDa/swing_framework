package com.framework.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static Connection connection;   
    private DBConnection() { }

    public static Connection getConnection(boolean autoCommit) {
        try {
            if (connection == null) {
                Properties config = ConfigReader.loadConfig();
                
                String driver = config.getProperty("DB_DRIVER");
                String url    = config.getProperty("DB_URL");
                String user   = config.getProperty("DB_USER");
                String pass   = config.getProperty("DB_PASSWORD");

                if (driver == null || url == null) {
                    throw new RuntimeException("Parametres DB_DRIVER ou DB_URL manquants.");
                }

                Class.forName(driver);
                connection = DriverManager.getConnection(url, user, pass);
                System.out.println("[Framework] Connexion a la base de donnees etablie avec succes.");
            }

            if (connection.getAutoCommit() != autoCommit) {
                connection.setAutoCommit(autoCommit);
            }

        } catch (Exception e) {
            System.err.println("[Framework ERREUR] Impossible de gerer la connexion a la base de donnees.");
            e.printStackTrace();
        }
        
        return connection;
    }

    public static Connection getConnection() {
        return getConnection(false);
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("[Framework] Connexion fermee.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
