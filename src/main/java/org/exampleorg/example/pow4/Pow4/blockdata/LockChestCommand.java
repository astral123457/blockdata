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

        String language = loadLanguage();//funcao do tradutor
        MessageManager messageManager = new MessageManager();//funcao do tradutor

        if (sender instanceof Player player) {
            Block block = player.getTargetBlockExact(5);
            if (block != null && block.getType() == Material.CHEST) {
                Chest chest = (Chest) block.getState();
                if (args.length > 0) {
                    String password = args[0];
                    if (label.equalsIgnoreCase("lock")) {
                        chestLockListener.lockChest(chest, password, player);

                        String message = messageManager.getMessage(// traducao complex
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
                        player.sendMessage("A senha deste baú é: " + chestPassword);
                    } else {
                        player.sendMessage("Este baú não está trancado.");
                    }
                } else {
                    player.sendMessage("Por favor, forneça uma senha.");
                }
                return true;
            } else {
                player.sendMessage("Olhe para um baú para trancá-lo, destrancá-lo ou ver a senha.");
            }
        }
        return false;
    }
}
