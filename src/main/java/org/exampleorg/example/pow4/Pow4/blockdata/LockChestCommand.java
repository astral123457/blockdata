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
    private final String configPath = "plugins/blockdata/config.json";//funcao do tradutor

    public LockChestCommand(ChestLockListener chestLockListener) {
        this.chestLockListener = chestLockListener;
    }


    // Método para obter o idioma com base no Player
    private String getPlayerLanguage(Player player) {
        return mapLocaleToLanguage(player.getLocale());
    }

    // Método auxiliar para mapear locale para idioma
    private String mapLocaleToLanguage(String locale) {
        return switch (locale.toLowerCase().substring(0, 2)) {
            case "pt" -> "br";
            case "en" -> "en";
            case "es" -> "es";
            case "fr" -> "fr";
            case "de" -> "de";
            case "ru" -> "ru";
            case "zh" -> "zh";
            case "zh-tw" -> "zh-tw";
            case "ja" -> "ja";
            case "ko" -> "ko";
            case "it" -> "it";
            case "nl" -> "nl";
            case "pl" -> "pl";
            case "sv" -> "sv";
            case "cs" -> "cs";
            case "hu" -> "hu";
            case "tr" -> "tr";
            case "ar" -> "ar";
            case "fi" -> "fi";
            case "da" -> "da";
            default -> "default";
        };
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {


        MessageManager messageManager = new MessageManager();//funcao do tradutor

        if (sender instanceof Player player) {
            // Obtém o idioma do jogador
            String language = getPlayerLanguage(player);
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
                        //player.sendMessage("A senha deste baú é: " + chestPassword);
                        // Obtém a mensagem com placeholder
                        String message = messageManager.getMessage("lock_chest", language);
                        // Substitui "{password}" pela senha real
                        message = message.replace("{password}", chestPassword);
                        player.sendMessage(ChatColor.DARK_GRAY + message);
                        //chest_password
                    } else {
                        player.sendMessage(ChatColor.YELLOW + messageManager.getMessage("chest_not_locked", language));
                        //player.sendMessage("Este baú não está trancado.");
                    }
                } else {
                    player.sendMessage(ChatColor.DARK_RED + messageManager.getMessage("incorrect_password", language));
                    //player.sendMessage("Por favor, forneça uma senha.");
                }
                return true;
            } else {
                player.sendMessage("👀" + ChatColor.GOLD + messageManager.getMessage("look_at_chest", language));
                //player.sendMessage("👀 Olhe para um baú para trancá-lo, destrancá-lo ou ver a senha.");
            }
        }
        return false;
    }
}
