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
    // Variável global para armazenar os baus trancados
    private final Map<String, String> lockedChests;

    // URL do banco de dados
    private static final String DB_URL = "jdbc:sqlite:plugins/blockdata/blockdata.db";
    private static final Logger LOGGER = Logger.getLogger(LockedChests.class.getName());


    // Mensagens do setup do banco de dados
    //public static final String DB_SETUP_SUCCESS_BR = "Banco de dados e tabela configurados com sucesso!";
    public static final String DB_SETUP_SUCCESS_EN = "Database and table successfully set up!";
    //public static final String DB_SETUP_ERROR_BR = "Erro ao configurar o banco de dados.";
    public static final String DB_SETUP_ERROR_EN = "Error setting up the database.";

    // Mensagens de carregamento
    //public static final String CHEST_LOADED_BR = "Bau carregado: ";
    public static final String CHEST_LOADED_EN = "Chest loaded: ";
    //public static final String CHESTS_LOADED_SUCCESS_BR = "Todos os baús foram carregados com sucesso na memória!";
    public static final String CHESTS_LOADED_SUCCESS_EN = "All chests were successfully loaded into memory!";

    // Mensagens de salvamento
    //public static final String CHEST_SAVED_BR = "Bau salvo no banco de dados:";
    public static final String CHEST_SAVED_EN = "Chest saved to the database:";
    //public static final String CHEST_SAVE_ERROR_BR = "Erro ao salvar o baú trancado no banco de dados.";
    public static final String CHEST_SAVE_ERROR_EN = "Error saving the locked chest to the database.";

    // Mensagens de remoção
    //public static final String CHEST_REMOVED_BR = "Bau removido do banco de dados: Localização = {location}.";
    public static final String CHEST_REMOVED_EN = "Chest removed from the database: Location = {location}.";
    //public static final String CHEST_REMOVE_ERROR_BR = "Erro ao remover o baú trancado do banco de dados.";
    public static final String CHEST_REMOVE_ERROR_EN = "Error removing the locked chest from the database.";

    private boolean isLoaded = false; // Flag para rastrear se os baús já foram carregados


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
            System.out.println(DB_SETUP_SUCCESS_EN);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, DB_SETUP_ERROR_EN, e);
        }
    }


    // Método para adicionar um bau trancado
    public void addLockedChest(String location, String password, String player) {
        // Validação dos dados
        if (location == null || password == null || player == null) {
            System.out.println("Dados inválidos fornecidos. O registro foi ignorado.");
            return; // Sai do método
        }

        // Verifica duplicatas
        if (lockedChests.containsKey(location)) {
            System.out.printf("Registro duplicado ignorado: Localização = %s%n", location);
            return; // Sai do método para evitar sobrescrever
        }

        // Adiciona ao mapa e persiste no banco
        lockedChests.put(location, password);
        saveLockedChest(location, password, player);
        System.out.printf("Baú trancado: Localização = %s, Jogador = %s%n", location, player);
    }


    // Método para remover um bau trancado
    public void removeLockedChest(String location, String password) {
        // Remove do mapa na memória
        lockedChests.remove(location, password);
        System.out.println("Bau removido do local: ");
        // Remove do banco de dados
        deleteLockedChestFromDatabase(location);
    }

    // Método para verificar se um bau está trancado
    public boolean isLocked(String location) {
        return lockedChests.containsKey(location);
    }

    // Método para obter a senha de um bau
    public String getPassword(String location) {
        return lockedChests.get(location);
    }

    // Método para carregar baus do banco de dados na memória
    public void loadLockedChests() {
        if (isLoaded) {
            System.out.println("Os baús já foram carregados anteriormente. O carregamento será ignorado.");
            return; // Sai do método, pois já foi carregado
        }

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT location, password, player FROM locked_chests")) {

            while (rs.next()) {
                String location = rs.getString("location");
                String password = rs.getString("password");

                // Verifica se já existe no mapa
                if (!lockedChests.containsKey(location)) {
                    lockedChests.put(location, password);
                } else {
                    System.out.printf("A localização %s já existe no mapa.%n", location);
                }
            }

            System.out.println("Todos os itens foram processados com sucesso.");
            System.out.println(CHESTS_LOADED_SUCCESS_EN);
            isLoaded = true; // Marca como carregado após o sucesso

            // Chama o método de remoção de duplicatas no final para garantir consistência
            removeDuplicateEntries();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, CHEST_SAVE_ERROR_EN, e);
        }
    }



    public void removeDuplicateEntries() {
        Map<String, String> tempMap = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT location, password FROM locked_chests")) {

            while (rs.next()) {
                String location = rs.getString("location");
                String password = rs.getString("password");

                // Adiciona apenas o primeiro registro encontrado para cada localização
                if (!tempMap.containsKey(location)) {
                    tempMap.put(location, password);
                } else {
                    System.out.printf("Duplicata removida: Localização = %s%n", location);
                }
            }

            // Limpa e reescreve o mapa na memória
            lockedChests.clear();
            lockedChests.putAll(tempMap);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao remover duplicatas.", e);
        }
    }


    // Método para salvar um bau no banco de dados
    private void saveLockedChest(String location, String password, String player) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT OR REPLACE INTO locked_chests (location, password, player) VALUES (?, ?, ?)")) {

            pstmt.setString(1, location);
            pstmt.setString(2, password);
            pstmt.setString(3, player);
            pstmt.executeUpdate();

            System.out.printf(CHEST_SAVED_EN);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, CHEST_SAVE_ERROR_EN, e);
        }
    }

    // Método para excluir um bau do banco de dados
    private void deleteLockedChestFromDatabase(String location) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM locked_chests WHERE location = ?")) {

            pstmt.setString(1, location);
            pstmt.executeUpdate();
            System.out.printf(CHEST_REMOVED_EN + "Location = %s%n", location);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, CHEST_REMOVE_ERROR_EN, e);
        }
    }


}
