package org.exampleorg.example.pow4.Pow4.blockdata;

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
        if (sender instanceof Player player) {
            Block block = player.getTargetBlockExact(5);
            if (block != null && block.getType() == Material.CHEST) {
                Chest chest = (Chest) block.getState();
                if (args.length > 0) {
                    String password = args[0];
                    if (label.equalsIgnoreCase("lock")) {
                        chestLockListener.lockChest(chest, password, player);
                        player.sendMessage("Baú trancado com sucesso!");
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
