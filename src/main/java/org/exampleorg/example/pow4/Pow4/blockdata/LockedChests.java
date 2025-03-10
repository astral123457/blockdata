package org.exampleorg.example.pow4.Pow4.blockdata;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class LockedChests {
    // Variável global para armazenar os baús trancados
    private final Map<String, String> lockedChests;

    // URL do banco de dados
    private static final String DB_URL = "jdbc:sqlite:plugins/blockdata/blockdata.db";

    // Construtor para inicializar o mapa e configurar o banco de dados
    public LockedChests() {
        this.lockedChests = new HashMap<>();
        setupDatabase();
    }

    // Método para configurar o banco de dados
    private void setupDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS locked_chests (" +
                    "location TEXT PRIMARY KEY, " +
                    "password TEXT NOT NULL, " +
                    "player TEXT NOT NULL)";
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.err.println("Erro ao configurar o banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para adicionar um baú trancado
    public void addLockedChest(String location, String password, String player) {
        lockedChests.put(location, password); // Adiciona ao mapa em memória
        saveLockedChest(location, password, player); // Salva no banco de dados
    }

    // Método para remover um baú trancado
    public void removeLockedChest(String location) {
        lockedChests.remove(location); // Remove do mapa em memória
        deleteLockedChestFromDatabase(location); // Remove do banco de dados
    }

    // Método para verificar se um baú está trancado
    public boolean isLocked(String location) {
        return lockedChests.containsKey(location);
    }

    // Método para obter a senha de um baú
    public String getPassword(String location) {
        return lockedChests.get(location);
    }




    // Método para salvar um baú trancado no banco de dados
    public void saveLockedChest(String location, String password, String player) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT OR REPLACE INTO locked_chests (location, password, player) VALUES (?, ?, ?)")) {
            pstmt.setString(1, location);
            pstmt.setString(2, password);
            pstmt.setString(3, player);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Erro ao salvar o baú trancado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para excluir um baú trancado do banco de dados
    private void deleteLockedChestFromDatabase(String location) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "DELETE FROM locked_chests WHERE location = ?")) {
            pstmt.setString(1, location);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Erro ao remover o baú trancado: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
