package org.exampleorg.example.pow4.Pow4.blockdata;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LockedChests {
    // Variável global para armazenar os baús trancados
    private final Map<String, String> lockedChests;

    // URL do banco de dados
    private static final String DB_URL = "jdbc:sqlite:plugins/blockdata/blockdata.db";
    private static final Logger LOGGER = Logger.getLogger(LockedChests.class.getName());


    // Construtor para inicializar o mapa e configurar o banco de dados
    public LockedChests() {
        this.lockedChests = new HashMap<>();
        setupDatabase(); // Configurar banco na inicialização
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
            System.out.println("Banco de dados e tabela configurados com sucesso!");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao configurar o banco de dados: ", e);
        }
    }


    // Método para adicionar um baú trancado
    public void addLockedChest(String location, String password, String player) {
        // Adiciona ao mapa na memória
        lockedChests.put(location, password);
        // Salva no banco de dados
        saveLockedChest(location, password, player);
    }

    // Método para remover um baú trancado
    public void removeLockedChest(String location) {
        // Remove do mapa na memória
        lockedChests.remove(location);
        // Remove do banco de dados
        deleteLockedChestFromDatabase(location);
    }

    // Método para verificar se um baú está trancado
    public boolean isLocked(String location) {
        return lockedChests.containsKey(location);
    }

    // Método para obter a senha de um baú
    public String getPassword(String location) {
        return lockedChests.get(location);
    }

    // Método para carregar baús do banco de dados na memória
    public void loadLockedChests() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT location, password, player FROM locked_chests")) {

            while (rs.next()) {
                String location = rs.getString("location");
                String password = rs.getString("password");
                String player = rs.getString("player");

                // Adiciona ao mapa na memória
                lockedChests.put(location, password);

                System.out.printf("Baú carregado: Location = %s, Password = %s, Player = %s%n",
                        location, password, player);
            }

            System.out.println("Todos os baús foram carregados com sucesso na memória!");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar baús do banco de dados: ", e);
        }
    }

    // Método para salvar um baú no banco de dados
    private void saveLockedChest(String location, String password, String player) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT OR REPLACE INTO locked_chests (location, password, player) VALUES (?, ?, ?)")) {

            pstmt.setString(1, location);
            pstmt.setString(2, password);
            pstmt.setString(3, player);
            pstmt.executeUpdate();

            System.out.printf("Baú salvo no banco de dados: Location = %s, Player = %s%n", location, player);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao salvar o baú trancado no banco de dados: ", e);
        }
    }

    // Método para excluir um baú do banco de dados
    private void deleteLockedChestFromDatabase(String location) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM locked_chests WHERE location = ?")) {

            pstmt.setString(1, location);
            pstmt.executeUpdate();
            System.out.printf("Baú removido do banco de dados: Location = %s%n", location);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao remover o baú trancado do banco de dados: ", e);
        }
    }


}
