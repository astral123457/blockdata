package org.exampleorg.example.pow4.Pow4.blockdata;

import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;

public final class Blockdata extends JavaPlugin {

    private static final String FOLDER_PATH = "plugins/blockdata";
    private static final String CONFIG_FILE = FOLDER_PATH + "/config.json";
    private static final String MESSAGES_FILE = FOLDER_PATH + "/messages.json";

    private LockedChests lockedChestsManager;

    @Override
    public void onEnable() {
        getLogger().info("Iniciando o plugin LockChestPlugin...");

        createFolderAndFiles();
        if (!isPluginEnabled()) {
            getLogger().warning("Plugin desativado via configuração.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        lockedChestsManager = new LockedChests(); // Instancia o gerenciador
        registerListeners(); // Registra eventos
        registerCommands(); // Registra comandos

        setupDatabase();

        String language = loadLanguage();
        getLogger().info("Idioma carregado: " + language);


    }


    @Override
    public void onDisable() {
        getLogger().info("Desativando o plugin LockChestPlugin...");
    }

    private void createFolderAndFiles() {
        File folder = new File(FOLDER_PATH);
        if (!folder.exists() && folder.mkdirs()) {
            getLogger().info("Pasta de configuração criada em: " + FOLDER_PATH);
        }

        // Criação de config.json
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            try (FileWriter writer = new FileWriter(configFile)) {
                JsonObject defaultConfig = new JsonObject();
                defaultConfig.addProperty("enabled", true);
                defaultConfig.addProperty("language", "br");
                new Gson().toJson(defaultConfig, writer);
                getLogger().info("Arquivo config.json criado com configurações padrão.");
            } catch (IOException e) {
                getLogger().severe("Erro ao criar config.json: " + e.getMessage());
            }
        }

        // Criação de messages.json
        File messagesFile = new File(MESSAGES_FILE);
        if (!messagesFile.exists()) {
            try (FileWriter writer = new FileWriter(messagesFile)) {
                JsonObject messages = new JsonObject();
                JsonObject lockMessages = new JsonObject();
                JsonObject unlockMessages = new JsonObject();

                lockMessages.addProperty("br", "Baú trancado com a senha: {password}.");
                unlockMessages.addProperty("br", "Baú destrancado com sucesso!");

                messages.add("lock_chest", lockMessages);
                messages.add("unlock_chest", unlockMessages);
                new Gson().toJson(messages, writer);

                getLogger().info("Arquivo messages.json criado com mensagens padrão.");
            } catch (IOException e) {
                getLogger().severe("Erro ao criar messages.json: " + e.getMessage());
            }
        }
    }

    private boolean isPluginEnabled() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
            JsonObject config = new Gson().fromJson(content, JsonObject.class);
            return config.get("enabled").getAsBoolean();
        } catch (IOException e) {
            getLogger().severe("Erro ao ler config.json: " + e.getMessage());
        }
        return false;
    }

    private String loadLanguage() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
            JsonObject config = new Gson().fromJson(content, JsonObject.class);
            return config.get("language").getAsString();
        } catch (IOException e) {
            getLogger().severe("Erro ao carregar o idioma do config.json: " + e.getMessage());
        }
        return "br";
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChestLockListener(this), this);
        getLogger().info("Listeners registrados.");
    }

    private void registerCommands() {
        this.getCommand("lock").setExecutor(new LockChestCommand(new ChestLockListener(this)));
        this.getCommand("unlock").setExecutor(new LockChestCommand(new ChestLockListener(this)));

        this.getCommand("reloadblockdata").setExecutor(this);

        getLogger().info("Comandos registrados.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("reloadblockdata")) {
            lockedChestsManager.loadLockedChests(); // Recarrega os dados
            sender.sendMessage(ChatColor.GREEN + "Baús recarregados com sucesso!");
            return true;
        }
        return false;
    }

    private void setupDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + FOLDER_PATH + "/blockdata.db");
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS locked_chests (" +
                    "location TEXT PRIMARY KEY, " +
                    "password TEXT NOT NULL, " +
                    "player TEXT NOT NULL)";
            stmt.executeUpdate(sql);
            getLogger().info("Banco de dados configurado com sucesso.");
        } catch (Exception e) {
            getLogger().severe("Erro ao configurar o banco de dados: " + e.getMessage());
        }
    }

}
