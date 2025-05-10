package com.datablock;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Set;

public class LockChestCommand implements CommandExecutor {
    private final ChestLockListener chestLockListener;

    public LockChestCommand(ChestLockListener chestLockListener) {
        this.chestLockListener = chestLockListener;
    }

    private String getPlayerLanguage(Player player) {
        return mapLocaleToLanguage(player.locale().toString());
    }

    private String mapLocaleToLanguage(String locale) {
        return switch (locale.toLowerCase().substring(0, 2)) {
            case "pt" -> "br";
            case "en" -> "en";
            case "es" -> "es";
            case "fr" -> "fr";
            case "de" -> "de";
            default -> "default";
        };
    }

@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    System.out.println("[LockChestCommand] Command received: " + label + " with args: " + String.join(", ", args));

    if (!(sender instanceof Player player)) {
        System.out.println("[LockChestCommand] Command sender is not a player.");
        sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
        return true;
    }

    String language = getPlayerLanguage(player);
    System.out.println("[LockChestCommand] Player language: " + language);

    MessageManager messageManager = new MessageManager();

    try {
        // Substitua getTargetBlockExact por getTargetBlock
        Block block = player.getTargetBlock(null, 5); // null permite todos os blocos transparentes
        if (block == null) {
            System.out.println("[LockChestCommand] Player is not looking at any block.");
            player.sendMessage(Component.text(messageManager.getMessage("look_at_chest", language))
                    .color(NamedTextColor.GOLD));
            return true;
        }

        if (block.getType() != Material.CHEST) {
            System.out.println("[LockChestCommand] Player is looking at a block, but it is not a chest. Block type: " + block.getType());
            player.sendMessage(Component.text(messageManager.getMessage("look_at_chest", language))
                    .color(NamedTextColor.GOLD));
            return true;
        }

        Chest chest = (Chest) block.getState();
        System.out.println("[LockChestCommand] Player is looking at a chest at location: " + chest.getLocation());

        if (args.length == 0) {
            System.out.println("[LockChestCommand] No password provided.");
            player.sendMessage(Component.text(messageManager.getMessage("provide_password", language))
                    .color(NamedTextColor.RED));
            return true;
        }

        String password = args[0];
        System.out.println("[LockChestCommand] Password provided: " + password);

        if (label.equalsIgnoreCase("lock")) {
            System.out.println("[LockChestCommand] Locking chest...");
            try {
                chestLockListener.lockChest(chest, password, player);
                System.out.println("[LockChestCommand] Chest locked successfully.");
            } catch (Exception e) {
                System.out.println("[LockChestCommand] Error while locking chest.");
                e.printStackTrace();
                player.sendMessage(Component.text("An error occurred while locking the chest.")
                        .color(NamedTextColor.RED));
                return true;
            }
        } else if (label.equalsIgnoreCase("unlock")) {
            System.out.println("[LockChestCommand] Unlocking chest...");
            chestLockListener.unlockChest(player, chest, password);
            System.out.println("[LockChestCommand] Chest unlocked successfully.");
        } else if (label.equalsIgnoreCase("viewpassword") && player.hasPermission("admin.viewpassword")) {
            System.out.println("[LockChestCommand] Viewing chest password...");
            String chestPassword = chestLockListener.getChestPassword(chest);
            if (chestPassword != null) {
                String message = messageManager.getMessage("chest_password", language, "password", chestPassword);
                player.sendMessage(Component.text(message).color(NamedTextColor.DARK_GRAY));
                System.out.println("[LockChestCommand] Chest password: " + chestPassword);
            } else {
                player.sendMessage(Component.text(messageManager.getMessage("chest_not_locked", language))
                        .color(NamedTextColor.YELLOW));
                System.out.println("[LockChestCommand] Chest is not locked.");
            }
        } else {
            System.out.println("[LockChestCommand] Invalid command or insufficient permissions.");
            player.sendMessage(Component.text(messageManager.getMessage("incorrect_password", language))
                    .color(NamedTextColor.DARK_RED));
        }
    } catch (Exception e) {
        System.out.println("[LockChestCommand] An error occurred while executing the command.");
        player.sendMessage(Component.text("An error occurred while executing the command.")
                .color(NamedTextColor.RED));
        e.printStackTrace();
    }

    return true;
}
}