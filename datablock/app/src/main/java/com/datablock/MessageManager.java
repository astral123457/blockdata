package com.datablock;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageManager {

    private static final String MESSAGES_FILE_PATH = "plugins/blockdata/messages.json";

    private JsonObject messages;

    private static final Logger LOGGER = Logger.getLogger(MessageManager.class.getName());

    public MessageManager() {
        loadMessages();
    }

    private void loadMessages() {
        File file = new File(MESSAGES_FILE_PATH);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                messages = new Gson().fromJson(reader, JsonObject.class);
                LOGGER.log(Level.INFO, "Messages file loaded successfully: " + MESSAGES_FILE_PATH);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error loading messages file: " + MESSAGES_FILE_PATH, e);
            }
        } else {
            LOGGER.log(Level.WARNING, "Messages file not found: " + MESSAGES_FILE_PATH);
            messages = new JsonObject(); // Evitar erros se o arquivo não existir
        }
    }

    public String getMessage(String key, String language, String... placeholders) {
        if (messages == null || !messages.has(key)) {
            LOGGER.log(Level.WARNING, "Message key not found: " + key);
            return "Message not found!";
        }

        JsonObject keyObject = messages.getAsJsonObject(key);
        String message;
        if (keyObject.has(language)) {
            message = keyObject.get(language).getAsString();
        } else if (keyObject.has("en")) { // Fallback para inglês
            LOGGER.log(Level.WARNING, "Language not found for key: " + key + ", using fallback language: en");
            message = keyObject.get("en").getAsString();
        } else {
            LOGGER.log(Level.WARNING, "No valid language found for key: " + key);
            return "Message not available!";
        }

        // Substituir placeholders no texto
        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            message = message.replace("{" + placeholder + "}", value);
        }

        return message;
    }
}