package org.exampleorg.example.pow4.Pow4.blockdata;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class LockChestCommand implements CommandExecutor {
    private final ChestLockListener chestLockListener;


    public LockChestCommand(ChestLockListener chestLockListener) {
        this.chestLockListener = chestLockListener;
    }






    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {


        MessageManager messageManager = new MessageManager(); // Função do tradutor

        if (sender instanceof Player player) {
            // Obtém o idioma do jogador
            String language = getPlayerLanguage(player);

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

                    if (label.equalsIgnoreCase("unlock")) {

                        // Verifica se o bloco não é nulo e se é um baú
                        if (block != null && block.getType() == Material.CHEST) {
                            Chest chest2 = (Chest) block.getState(); // Converte o bloco para um baú

                            // Destranca o baú com o listener
                            chestLockListener.unlockChest(player, chest2, password);

                            // Gera a mensagem personalizada
                            String message = messageManager.getMessage(
                                    "unlock_chest", language,
                                    "location", chest.getLocation().toString(),
                                    "password", password
                            );
                            player.sendMessage(ChatColor.LIGHT_PURPLE + message);
                        } else {
                            // Caso o jogador não esteja mirando em um baú válido
                            player.sendMessage(ChatColor.RED + "Você deve olhar para um baú para destrancá-lo!");
                        }
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

    private String getPlayerLanguage(Player player) {
        String language = player.getLocale().toLowerCase();
        return switch (language.substring(0, 2)) {
            case "pt" -> "br";
            case "en" -> "en";
            case "es" -> "es";
            case "fr" -> "fr";
            case "de" -> "de";
            default -> "default";
        };
    }


}
