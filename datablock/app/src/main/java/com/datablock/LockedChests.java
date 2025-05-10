package com.datablock;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LockedChests {
    private final Map<String, String> lockedChests = new ConcurrentHashMap<>();
    private boolean isLoaded = false;
    private static final String DB_URL = "jdbc:sqlite:plugins/blockdata/blockdata.db";
    private Connection connection;

    public LockedChests() {
        setupDatabaseConnection();
        setupDatabase();
    }

   private void setupDatabaseConnection() {
    try {
        Class.forName("org.sqlite.JDBC"); // Registra o driver SQLite
        connection = DriverManager.getConnection(DB_URL);
        connection.createStatement().execute("PRAGMA busy_timeout = 3000;");
        log("Database connection reestablished.", Level.INFO);
    } catch (ClassNotFoundException e) {
        log("SQLite JDBC driver not found: " + e.getMessage(), Level.SEVERE);
    } catch (SQLException e) {
        log("Error connecting to the database: " + e.getMessage(), Level.SEVERE);
    }
    
    }

    private void setupDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS locked_chests (" +
                "location TEXT PRIMARY KEY, " +
                "password TEXT NOT NULL, " +
                "player TEXT NOT NULL)";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            log("Table 'locked_chests' successfully configured!", Level.INFO);
        } catch (Exception e) {
            log("Error configuring the database: " + e.getMessage(), Level.SEVERE);
        }
    }

    public void addLockedChest(String location, String password, String player) {
        lockedChests.put(location, password);
        saveLockedChest(location, password, player);
    }

    public void removeLockedChest(String location) {
        lockedChests.remove(location);
        deleteLockedChestFromDatabase(location);
    }

    public boolean isLocked(String location) {
        return lockedChests.containsKey(location);
    }

    public String getPassword(String location) {
        return lockedChests.get(location);
    }

    private void saveLockedChest(String location, String password, String player) {
        String sql = "INSERT OR REPLACE INTO locked_chests (location, password, player) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location);
            pstmt.setString(2, password);
            pstmt.setString(3, player);
            pstmt.executeUpdate();
            log("Chest saved to database: " + location, Level.INFO);
        } catch (Exception e) {
            log("Error saving locked chest: " + e.getMessage(), Level.SEVERE);
        }
    }

    private void deleteLockedChestFromDatabase(String location) {
        String sql = "DELETE FROM locked_chests WHERE location = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location);
            pstmt.executeUpdate();
            log("Chest removed from database: " + location, Level.INFO);
        } catch (Exception e) {
            log("Error removing locked chest: " + e.getMessage(), Level.SEVERE);
        }
    }

    public void loadLockedChests() {
        if (isLoaded) {
            log("Chests have already been loaded. Skipping load.", Level.WARNING);
            return;
        }

        String sql = "SELECT location, password FROM locked_chests";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String location = rs.getString("location");
                String password = rs.getString("password");
                lockedChests.putIfAbsent(location, password);
            }

            log("All chests successfully loaded into memory.", Level.INFO);
            isLoaded = true;
            removeDuplicateEntries();

        } catch (Exception e) {
            log("Error loading chests: " + e.getMessage(), Level.SEVERE);
        }
    }

    public void removeDuplicateEntries() {
        Map<String, String> tempMap = new ConcurrentHashMap<>();

        String sql = "SELECT location, password FROM locked_chests";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String location = rs.getString("location");
                String password = rs.getString("password");
                tempMap.putIfAbsent(location, password);
            }

            lockedChests.clear();
            lockedChests.putAll(tempMap);
            log("Duplicate entries removed.", Level.INFO);

        } catch (Exception e) {
            log("Error removing duplicates: " + e.getMessage(), Level.SEVERE);
        }
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                log("Database connection closed.", Level.INFO);
            } catch (Exception e) {
                log("Error closing database connection: " + e.getMessage(), Level.SEVERE);
            }
        }
    }

    private void log(String message, Level level) {
        Logger.getLogger(LockedChests.class.getName()).log(level, message);
    }
}