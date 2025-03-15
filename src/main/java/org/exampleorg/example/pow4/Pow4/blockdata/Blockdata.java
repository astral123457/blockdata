package org.exampleorg.example.pow4.Pow4.blockdata;

import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;


public final class Blockdata extends JavaPlugin {

    private static final String FOLDER_PATH = "plugins/blockdata";
    private static final String CONFIG_FILE = FOLDER_PATH + "/config.json";
    private static final String MESSAGES_FILE = FOLDER_PATH + "/messages.json";


    @Override
    public void onEnable() {
        getLogger().info("Iniciando o plugin LockChestPlugin...");

        createFolderAndFiles();
        if (!isPluginEnabled()) {
            getLogger().warning("Plugin desativado via configuração.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

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

                // Gson com Pretty Printing
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(defaultConfig, writer);

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
                lockMessages.addProperty("br", "Baú trancado com a senha: {password}.");
                lockMessages.addProperty("en", "Chest locked with password: {password}.");
                lockMessages.addProperty("es", "Cofre bloqueado con la contraseña: {password}.");
                lockMessages.addProperty("fr", "Coffre verrouillé avec le mot de passe : {password}.");
                lockMessages.addProperty("de", "Truhe mit Passwort gesperrt: {password}.");

                JsonObject unlockMessages = new JsonObject();
                unlockMessages.addProperty("br", "Baú destrancado com sucesso!");
                unlockMessages.addProperty("en", "Chest successfully unlocked!");
                unlockMessages.addProperty("es", "Cofre desbloqueado con éxito!");
                unlockMessages.addProperty("fr", "Coffre déverrouillé avec succès !");
                unlockMessages.addProperty("de", "Truhe erfolgreich entsperrt!");

                JsonObject incorrectPasswordMessage = new JsonObject();
                incorrectPasswordMessage.addProperty("br", "Senha incorreta. Use a etiqueta correta!");
                incorrectPasswordMessage.addProperty("en", "Incorrect password. Use the correct name tag!");
                incorrectPasswordMessage.addProperty("es", "Contraseña incorrecta. Use la etiqueta correcta!");
                incorrectPasswordMessage.addProperty("fr", "Mot de passe incorrect. Utilisez la bonne étiquette!");
                incorrectPasswordMessage.addProperty("de", "Falsches Passwort. Verwenden Sie das richtige Namensschild!");

                JsonObject lockedChestMessage = new JsonObject();
                lockedChestMessage.addProperty("br", "O baú está trancado. Segure a etiqueta correta ou use /unlock.");
                lockedChestMessage.addProperty("en", "The chest is locked. Hold the correct name tag or use /unlock.");
                lockedChestMessage.addProperty("es", "El cofre está bloqueado. Sostén la etiqueta correcta o usa /unlock.");
                lockedChestMessage.addProperty("fr", "Le coffre est verrouillé. Tenez la bonne étiquette ou utilisez /unlock.");
                lockedChestMessage.addProperty("de", "Die Truhe ist gesperrt. Halten Sie das richtige Namensschild oder verwenden Sie /unlock.");

                JsonObject relockChestMessage = new JsonObject();
                relockChestMessage.addProperty("br", "O baú foi trancado novamente com a senha original.");
                relockChestMessage.addProperty("en", "The chest has been relocked with the original password.");
                relockChestMessage.addProperty("es", "El cofre se ha bloqueado nuevamente con la contraseña original.");
                relockChestMessage.addProperty("fr", "Le coffre a été reverrouillé avec le mot de passe original.");
                relockChestMessage.addProperty("de", "Die Truhe wurde mit dem ursprünglichen Passwort erneut gesperrt.");

                JsonObject unlockedTempMessage = new JsonObject();
                unlockedTempMessage.addProperty("br", "Baú destrancado com sucesso! Será trancado novamente em 5 segundos.");
                unlockedTempMessage.addProperty("en", "Chest successfully unlocked! It will be relocked in 5 seconds.");
                unlockedTempMessage.addProperty("es", "Cofre desbloqueado con éxito! Se bloqueará de nuevo en 5 segundos.");
                unlockedTempMessage.addProperty("fr", "Coffre déverrouillé avec succès! Il sera reverrouillé dans 5 secondes.");
                unlockedTempMessage.addProperty("de", "Truhe erfolgreich entsperrt! Sie wird in 5 Sekunden wieder gesperrt.");

                JsonObject blockBreakMessage = new JsonObject();
                blockBreakMessage.addProperty("br", "Você não pode destruir um baú trancado.");
                blockBreakMessage.addProperty("en", "You cannot destroy a locked chest.");
                blockBreakMessage.addProperty("es", "No puedes destruir un cofre bloqueado.");
                blockBreakMessage.addProperty("fr", "Vous ne pouvez pas détruire un coffre verrouillé.");
                blockBreakMessage.addProperty("de", "Sie können keine gesperrte Truhe zerstören.");

                JsonObject lockSuccessMessage = new JsonObject();
                lockSuccessMessage.addProperty("br", "Baú trancado com sucesso!");
                lockSuccessMessage.addProperty("en", "Chest successfully locked!");
                lockSuccessMessage.addProperty("es", "Cofre bloqueado con éxito!");
                lockSuccessMessage.addProperty("fr", "Coffre verrouillé avec succès!");
                lockSuccessMessage.addProperty("de", "Truhe erfolgreich gesperrt!");

                JsonObject nameTagReceivedMessage = new JsonObject();
                nameTagReceivedMessage.addProperty("br", "Você recebeu uma etiqueta com a senha.");
                nameTagReceivedMessage.addProperty("en", "You received a name tag with the password.");
                nameTagReceivedMessage.addProperty("es", "Recibiste una etiqueta con la contraseña.");
                nameTagReceivedMessage.addProperty("fr", "Vous avez reçu une étiquette avec le mot de passe.");
                nameTagReceivedMessage.addProperty("de", "Sie haben ein Namensschild mit dem Passwort erhalten.");

                JsonObject providePasswordMessage = new JsonObject();
                providePasswordMessage.addProperty("br", "Por favor, forneça uma senha.");
                providePasswordMessage.addProperty("en", "Please provide a password.");
                providePasswordMessage.addProperty("es", "Por favor, proporcione una contraseña.");
                providePasswordMessage.addProperty("fr", "Veuillez fournir un mot de passe.");
                providePasswordMessage.addProperty("de", "Bitte geben Sie ein Passwort an.");

                JsonObject lookAtChestMessage = new JsonObject();
                lookAtChestMessage.addProperty("br", "Olhe para um baú para trancá-lo, destrancá-lo ou ver a senha.");
                lookAtChestMessage.addProperty("en", "Look at a chest to lock, unlock, or view its password.");
                lookAtChestMessage.addProperty("es", "Mira un cofre para bloquearlo, desbloquearlo o ver su contraseña.");
                lookAtChestMessage.addProperty("fr", "Regardez un coffre pour le verrouiller, le déverrouiller ou voir son mot de passe.");
                lookAtChestMessage.addProperty("de", "Schauen Sie sich eine Truhe an, um sie zu sperren, zu entsperren oder ihr Passwort anzuzeigen.");

                JsonObject chestPasswordMessage = new JsonObject();
                chestPasswordMessage.addProperty("br", "A senha deste baú é: {password}.");
                chestPasswordMessage.addProperty("en", "The password for this chest is: {password}.");
                chestPasswordMessage.addProperty("es", "La contraseña de este cofre es: {password}.");
                chestPasswordMessage.addProperty("fr", "Le mot de passe de ce coffre est : {password}.");
                chestPasswordMessage.addProperty("de", "Das Passwort für diese Truhe lautet: {password}.");

                JsonObject chestNotLockedMessage = new JsonObject();
                chestNotLockedMessage.addProperty("br", "Este baú não está trancado.");
                chestNotLockedMessage.addProperty("en", "This chest is not locked.");
                chestNotLockedMessage.addProperty("es", "Este cofre no está bloqueado.");
                chestNotLockedMessage.addProperty("fr", "Ce coffre n'est pas verrouillé.");
                chestNotLockedMessage.addProperty("de", "Diese Truhe ist nicht gesperrt.");


                messages.add("provide_password", providePasswordMessage);
                messages.add("look_at_chest", lookAtChestMessage);
                messages.add("chest_password", chestPasswordMessage);
                messages.add("chest_not_locked", chestNotLockedMessage);
                messages.add("block_break_denied", blockBreakMessage);
                messages.add("lock_success", lockSuccessMessage);
                messages.add("name_tag_received", nameTagReceivedMessage);
                messages.add("lock_chest", lockMessages);
                messages.add("unlock_chest", unlockMessages);
                messages.add("incorrect_password", incorrectPasswordMessage);
                messages.add("locked_chest", lockedChestMessage);
                messages.add("relock_chest", relockChestMessage);
                messages.add("unlocked_temp", unlockedTempMessage);

                // Gson com Pretty Printing
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(messages, writer);

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
