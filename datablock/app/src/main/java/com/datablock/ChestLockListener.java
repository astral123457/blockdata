package com.datablock;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerJoinEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ChestLockListener implements Listener {

    private final LockedChests lockedChestsManager;
    private final MessageManager messageManager;
    private final JavaPlugin plugin;
    private boolean isLoaded = false;

    public ChestLockListener(JavaPlugin plugin) {
        this.lockedChestsManager = new LockedChests();
        this.messageManager = new MessageManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isLoaded) {
            lockedChestsManager.loadLockedChests();
            isLoaded = true;
        } else {
            plugin.getLogger().info("Os baús já estão carregados na memória.");
        }
    }

    public String getChestPassword(Chest chest) {
        return lockedChestsManager.getPassword(chest.getBlock().getLocation().toString());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.CHEST) {
            Player player = event.getPlayer();
            String blockLocation = block.getLocation().toString();
            String language = getPlayerLanguage(player);

            if (lockedChestsManager.isLocked(blockLocation)) {
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                
                if (itemInHand.getType() == Material.NAME_TAG && itemInHand.hasItemMeta()) {
                    ItemMeta meta = itemInHand.getItemMeta();
                    
                    if (meta != null && meta.hasDisplayName()) {
                        String nameTag = meta.displayName() != null ? meta.displayName().toString() : "";
                        String originalPassword = lockedChestsManager.getPassword(blockLocation);

                        if (nameTag.equals(originalPassword)) {
                            player.sendMessage(Component.text(messageManager.getMessage("unlocked_temp", language, "password", originalPassword))
                                    .color(NamedTextColor.AQUA));
                            unlockChest(player, (Chest) block.getState(), nameTag);

                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                lockChest((Chest) block.getState(), originalPassword, player);
                                player.sendMessage(Component.text(messageManager.getMessage("relock_chest", language))
                                        .color(NamedTextColor.AQUA));
                            }, 100L);
                        } else {
                            player.sendMessage(Component.text(messageManager.getMessage("incorrect_password", language))
                                    .color(NamedTextColor.RED));
                        }
                    }
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
                    player.setFireTicks(369);
                    player.getWorld().strikeLightning(player.getLocation());
                    player.sendMessage(Component.text(messageManager.getMessage("relock_chest", language))
                            .color(NamedTextColor.GOLD));
                }
                event.setCancelled(true);
            }
        }
    }

    public void lockChest(Chest chest, String password, Player player) {
        String language = getPlayerLanguage(player);
        String blockLocation = chest.getBlock().getLocation().toString();

        if (lockedChestsManager.isLocked(blockLocation)) {
            player.sendMessage(Component.text(messageManager.getMessage("lock_already_exists", language))
                    .color(NamedTextColor.RED));
            return;
        }

        lockedChestsManager.addLockedChest(blockLocation, password, player.getName());

        boolean playerHasNameTag = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.NAME_TAG && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                
                if (meta != null && meta.hasDisplayName() && 
                    meta.displayName() != null && Component.text(password).equals(meta.displayName())) {
                    playerHasNameTag = true;
                    break;
                }
            }
        }

        if (!playerHasNameTag) {
            ItemStack nameTag = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = nameTag.getItemMeta();
            
            if (meta != null) {
                meta.displayName(Component.text(password));
                nameTag.setItemMeta(meta);
            }
            
            player.getInventory().addItem(nameTag);
            chest.getInventory().addItem(nameTag);
            player.sendMessage(Component.text(messageManager.getMessage("name_tag_received", language))
                    .color(NamedTextColor.AQUA));
        }
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

    public void unlockChest(Player player, Chest chest, String password) {
        String blockLocation = chest.getBlock().getLocation().toString();
        lockedChestsManager.removeLockedChest(blockLocation);
        player.sendMessage(Component.text(messageManager.getMessage("chest_unlocked", getPlayerLanguage(player)))
                .color(NamedTextColor.GREEN));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST) {
            String blockLocation = block.getLocation().toString();
            lockedChestsManager.removeLockedChest(blockLocation);
        }
    }
}