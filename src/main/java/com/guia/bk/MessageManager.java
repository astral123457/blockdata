package com.guia.bk;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MessageManager {

    private static final String MESSAGES_FILE_PATH = "plugins/bk/messages.json";private JsonObject messages;

    public MessageManager() {
        loadMessages();
    }



    private void loadMessages() {
        File file = new File("plugins/bk/messages.json");
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                messages = new Gson().fromJson(reader, JsonObject.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Arquivo de mensagens não encontrado!");
            messages = new JsonObject(); // Evitar erros se o arquivo não existir
        }
    }

    public void createDefaultMessages() {
        File file = new File(MESSAGES_FILE_PATH);
        if (!file.exists()) {
        }


    }

    public String getMessage(String key, String language, String... placeholders) {
        if (messages == null || !messages.has(key)) {
            return "Mensagem não encontrada!";
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
