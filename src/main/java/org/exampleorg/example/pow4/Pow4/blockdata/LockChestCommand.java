package org.exampleorg.example.pow4.Pow4.blockdata;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest; // Para trabalhar com o tipo Chest
import org.bukkit.entity.Player;
import org.bukkit.command.Command; // Para lidar com comandos em plugins
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.logging.Level;
import java.util.logging.Logger;



import java.io.File;//funcao do tradutor
import java.io.FileReader;//funcao do tradutor
import java.io.IOException;//funcao do tradutor


public class LockChestCommand implements CommandExecutor {
    private final ChestLockListener chestLockListener;

    public LockChestCommand(ChestLockListener chestLockListener) {
        this.chestLockListener = chestLockListener;
    }

    private static final Logger LOGGER = Logger.getLogger(LockChestCommand.class.getName());

    private String loadLanguage() {
        File configFile = new File("plugins/blockdata/config.json");
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                JsonObject config = new Gson().fromJson(reader, JsonObject.class);
                return config.get("language").getAsString(); // Retorna o idioma configurado
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "An error occurred", e);
            }
        }
        return "br"; // Retorna "br" como idioma padrão
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String language = loadLanguage();
        MessageManager messageManager = new MessageManager();

        if (sender instanceof Player player) {
            Block block = player.getTargetBlockExact(5);
            if (block != null && block.getType() == Material.CHEST) {
                Chest chest = (Chest) block.getState();
                if (args.length > 0) {
                    String password = args[0];
                    if (label.equalsIgnoreCase("lock")) {
                        chestLockListener.lockChest(chest, password, player);
                        String message = messageManager.getMessage(
                                "lock_chest", language,
                                "location", chest.getLocation().toString(),
                                "password", password
                        );
                        player.sendMessage(ChatColor.LIGHT_PURPLE + message);
                    } else if (label.equalsIgnoreCase("unlock")) {
                        chestLockListener.unlockChest(player, chest, password);
                    }
                } else if (label.equalsIgnoreCase("viewpassword") && player.hasPermission("admin.viewpassword")) {
                    String chestPassword = chestLockListener.getChestPassword(chest);
                    if (chestPassword != null) {
                        player.sendMessage(messageManager.getMessage(
                                "chest_password", language,
                                "password", chestPassword
                        ));
                    } else {
                        player.sendMessage(messageManager.getMessage("chest_not_locked", language));
                    }
                } else {
                    player.sendMessage(messageManager.getMessage("provide_password", language));
                }
                return true;
            } else {
                player.sendMessage(messageManager.getMessage("look_at_chest", language));
            }
        }
        return false;
    }
}

