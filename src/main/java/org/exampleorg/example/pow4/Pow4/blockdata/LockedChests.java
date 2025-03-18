package org.exampleorg.example.pow4.Pow4.blockdata;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;


public class LockedChests {
    // Variável global para armazenar os baús trancados
    private final Map<String, String> lockedChests;
    private boolean isLoaded = false; // Flag para rastrear se os baús já foram carregados
    public static final String CHESTS_LOADED_SUCCESS_EN = "All chests were successfully loaded into memory!";
    public static final String CHEST_SAVE_ERROR_EN = "Error saving the locked chest to the database.";

    // URL do banco de dados
    private static final String DB_URL = "jdbc:sqlite:plugins/blockdata/blockdata.db";

    // Conexão compartilhada
    private Connection connection;

    // Construtor para inicializar o mapa e configurar o banco de dados
    public LockedChests() {
        this.lockedChests = new HashMap<>();
        setupDatabaseConnection();
        setupDatabase();
    }

    // Método para configurar a conexão compartilhada
    private void setupDatabaseConnection() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA busy_timeout = 3000;"); // Evita bloqueios
            System.out.println("Conexão com o banco de dados estabelecida.");
        } catch (Exception e) {
            System.err.println("Erro ao conectar ao banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para configurar o banco de dados
    private void setupDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS locked_chests (" +
                "location TEXT PRIMARY KEY, " +
                "password TEXT NOT NULL, " +
                "player TEXT NOT NULL)";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Tabela 'locked_chests' configurada com sucesso!");
        } catch (Exception e) {
            System.err.println("Erro ao configurar o banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para adicionar um baú trancado
    public synchronized void addLockedChest(String location, String password, String player) {
        lockedChests.put(location, password); // Adiciona ao mapa em memória
        saveLockedChest(location, password, player); // Salva no banco de dados
    }

    // Método para remover um baú trancado
    public synchronized void removeLockedChest(String location) {
        lockedChests.remove(location); // Remove do mapa em memória
        deleteLockedChestFromDatabase(location); // Remove do banco de dados
    }

    // Método para verificar se um baú está trancado
    public synchronized boolean isLocked(String location) {
        return lockedChests.containsKey(location);
    }

    // Método para obter a senha de um baú
    public synchronized String getPassword(String location) {
        return lockedChests.get(location);
    }

    // Método para salvar um baú trancado no banco de dados
    public synchronized void saveLockedChest(String location, String password, String player) {
        String sql = "INSERT OR REPLACE INTO locked_chests (location, password, player) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location);
            pstmt.setString(2, password);
            pstmt.setString(3, player);
            pstmt.executeUpdate();
            System.out.println("Baú salvo no banco de dados: " + location);
        } catch (Exception e) {
            System.err.println("Erro ao salvar o baú trancado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para excluir um baú trancado do banco de dados
    private synchronized void deleteLockedChestFromDatabase(String location) {
        String sql = "DELETE FROM locked_chests WHERE location = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location);
            pstmt.executeUpdate();
            System.out.println("Baú removido do banco de dados: " + location);
        } catch (Exception e) {
            System.err.println("Erro ao remover o baú trancado: " + e.getMessage());
            e.printStackTrace();
        }
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
            System.out.printf( CHEST_SAVE_ERROR_EN);
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
            System.out.printf( "Erro ao remover duplicatas.");
        }
    }

    // Método para fechar a conexão com o banco de dados
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Conexão com o banco de dados fechada.");
            } catch (Exception e) {
                System.err.println("Erro ao fechar a conexão: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
