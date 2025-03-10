package org.exampleorg.example.pow4.Pow4.blockdata;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.Statement;


import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;





public final class Blockdata extends JavaPlugin {

    private static final String FOLDER_PATH = "plugins/blockdata";
    private static final String CONFIG_FILE = FOLDER_PATH + "/config.json";
    private static final String MESSAGES_FILE_PATH = FOLDER_PATH + "/messages.json";

    private final LockedChests lockedChestsManager = new LockedChests();




    @Override
    public void onEnable() {
        getLogger().info("LockChestPlugin habilitado!");

        createFolderAndConfig();
        boolean isEnabled = loadPluginStatus();

        if (!isEnabled) {
            getLogger().warning("Plugin desativado via configuração.");
            getServer().getPluginManager().disablePlugin(this);
            return; // Finaliza a inicialização se o plugin estiver desativado
        }
        // Registre o evento
        ChestLockListener chestLockListener = new ChestLockListener(this);
        getServer().getPluginManager().registerEvents(chestLockListener, this);


        this.getCommand("lock").setExecutor(new LockChestCommand(chestLockListener));
        this.getCommand("unlock").setExecutor(new LockChestCommand(chestLockListener));

        // Configuração do banco de dados
        setupDatabase();
        loadLockedChests(); // Recarregar baús trancados

        // Carregar idioma
        String language = loadLanguage();
        getLogger().info("Idioma configurado: " + language);
    }

    @Override
    public void onDisable() {
        getLogger().info("LockChestPlugin desabilitado!");
    }


    private void createFolderAndConfig() {
        // Criar a pasta blockdata, se não existir
        File folder = new File(FOLDER_PATH);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Criar o arquivo config.json com configuração padrão
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            JsonObject defaultConfig = new JsonObject();
            defaultConfig.addProperty("enabled", true);
            defaultConfig.addProperty("language", "br"); // Adiciona o idioma padrão

            try (FileWriter writer = new FileWriter(configFile)) {
                Gson gson = new Gson();
                gson.toJson(defaultConfig, writer);
                getLogger().info("Arquivo config.json criado com configuração padrão.");
            } catch (IOException e) {
                getLogger().severe("Erro ao criar o arquivo config.json: " + e.getMessage());
            }
        }

        // Criar o arquivo messages.json com mensagens padrão, apenas se não existir
        File messagesFile = new File(MESSAGES_FILE_PATH);
        if (!messagesFile.exists()) {
            JsonObject messages = new JsonObject();

            JsonObject lockChestMessages = new JsonObject();
            lockChestMessages.addProperty("en", "Chest locked with password: {password}.");
            lockChestMessages.addProperty("es", "Cofre bloqueado con contraseña: {password}.");
            lockChestMessages.addProperty("br", "Baú trancado com a senha: {password}.");

            JsonObject unlockChestMessages = new JsonObject();
            unlockChestMessages.addProperty("en", "Chest unlocked successfully!");
            unlockChestMessages.addProperty("es", "¡Cofre desbloqueado con éxito!");
            unlockChestMessages.addProperty("br", "Baú destrancado com sucesso!");

            messages.add("lock_chest", lockChestMessages);
            messages.add("unlock_chest", unlockChestMessages);

            try (FileWriter writer = new FileWriter(messagesFile)) {
                Gson gson = new Gson();
                gson.toJson(messages, writer);
                getLogger().info("Arquivo messages.json criado com mensagens padrão.");
            } catch (IOException e) {
                getLogger().severe("Erro ao criar o arquivo messages.json: " + e.getMessage());
            }
        }
    }


    private boolean loadPluginStatus() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
            JsonObject config = new Gson().fromJson(content, JsonObject.class);
            return config.get("enabled").getAsBoolean();
        } catch (IOException e) {
            getLogger().severe("Erro ao ler o arquivo config.json: " + e.getMessage());
        }
        return false; // Desabilita o plugin em caso de erro
    }

    public String loadLanguage() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
                JsonObject config = new Gson().fromJson(content, JsonObject.class);
                return config.get("language").getAsString(); // Retorna o idioma configurado
            } catch (IOException e) {
                getLogger().severe("Erro ao ler o arquivo config.json: " + e.getMessage());
            }
        }
        return "br"; // Padrão para português, caso ocorra um erro
    }



    private void setupDatabase() {
        String DB_URL = "jdbc:sqlite:plugins/blockdata/blockdata.db";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS locked_chests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " + // ID auto-incremento
                    "location TEXT NOT NULL UNIQUE, " + // UNIQUE para garantir local único
                    "password TEXT NOT NULL, " +
                    "player TEXT NOT NULL)";
            stmt.executeUpdate(sql);
            getLogger().info("Tabela 'locked_chests' configurada com sucesso!");

        } catch (Exception e) {
            getLogger().severe("Erro ao configurar a tabela 'locked_chests': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadLockedChests() {
        String DB_URL = "jdbc:sqlite:plugins/blockdata/blockdata.db";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT location, password, player FROM locked_chests")) {

            while (rs.next()) {
                String location = rs.getString("location"); // Obtém a localização do baú
                String password = rs.getString("password"); // Obtém a senha do baú
                String player = rs.getString("player");     // Obtém o jogador que trancou o baú

                // Log para depurar o carregamento
                System.out.println("Carregando baú: Localização = " + location + ", Senha = " + password + ", Jogador = " + player);

                // Adiciona os dados ao mapa de baús trancados
                lockedChestsManager.addLockedChest(location, password, player);
            }

            getLogger().info("Baús trancados carregados com sucesso!");

        } catch (Exception e) {
            getLogger().severe("Erro ao carregar os baús trancados: " + e.getMessage());
            e.printStackTrace();
        }
    }






}


