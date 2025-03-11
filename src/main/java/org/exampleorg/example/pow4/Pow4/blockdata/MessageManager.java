package org.exampleorg.example.pow4.Pow4.blockdata;

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
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "E", e);
            }
        } else {
            System.out.println("Messages file not found!");
            messages = new JsonObject(); // Evitar erros se o arquivo não existir
        }
    }


    public String getMessage(String key, String language, String... placeholders) {
        if (messages == null || !messages.has(key)) {
            return "Message not found!";
        }

        String message = messages.getAsJsonObject(key).get(language).getAsString();

        // Substituir placeholders no texto
        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            message = message.replace("{" + placeholder + "}", value);
        }

        return message;
    }
}