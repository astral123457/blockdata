package org.exampleorg.example.pow4.Pow4.blockdata;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;//funcao do tradutor
import java.io.FileReader;//funcao do tradutor
import java.io.IOException;//funcao do tradutor

public class LockChestCommand implements CommandExecutor {
    private final ChestLockListener chestLockListener;
    private final String databasePath = "jdbc:sqlite:plugins/blockdata/blockdata.db";
    private final String configPath = "plugins/blockdata/config.json";//funcao do tradutor

    public LockChestCommand(ChestLockListener chestLockListener) {
        this.chestLockListener = chestLockListener;
    }


    private String loadLanguage() {
        File configFile = new File(configPath);
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                JsonObject config = new Gson().fromJson(reader, JsonObject.class);
                return config.get("language").getAsString(); // Retorna o idioma configurado
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "br"; // Retorna "br" como idioma padrão
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {


        MessageManager messageManager = new MessageManager(); // Função do tradutor

        if (sender instanceof Player player) {
            String language = player.getLocale().toLowerCase();
            if (language.startsWith("pt")) {
                language = "br";
            } else if (language.startsWith("en")) {
                language = "en";
            } else if (language.startsWith("es")) {
                language = "es";
            } else if (language.startsWith("fr")) {
                language = "fr";
            } else if (language.startsWith("de")) {
                language = "de";
            } else {
                language = "default"; // Idioma padrão caso não seja reconhecido
            }

            Block block = player.getTargetBlockExact(5);
            if (block != null && block.getType() == Material.CHEST) {
                Chest chest = (Chest) block.getState();


                if (args.length > 0) { // Verifica se há argumentos antes de acessar args[0]
                    String password = args[0]; // Obtém a senha do primeiro argumento

                    if (label.equalsIgnoreCase("lock")) {
                        chestLockListener.lockChest(chest, password, player);

                        String message = messageManager.getMessage(
                                "lock_chest", language,
                                "location", chest.getLocation().toString(),
                                "password", password
                        );
                        player.sendMessage(ChatColor.LIGHT_PURPLE + message);

                    }
                }  else {
                    player.sendMessage(ChatColor.RED + messageManager.getMessage("provide_password", language));
                }
                return true;
            } else {
                player.sendMessage(ChatColor.RED + messageManager.getMessage("look_at_chest", language));
            }
        }
        return false;
    }

}
