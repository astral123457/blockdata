package org.exampleorg.example.pow4.Pow4.blockdata;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Note.Tone;

import org.bukkit.event.player.PlayerJoinEvent;

import org.bukkit.ChatColor;


public class ChestLockListener implements Listener {

    // Gerenciador de baús trancados
    private final LockedChests lockedChestsManager;

    // Mapa para rastrear o último envio de mensagem a cada jogador

    private final JavaPlugin plugin;

    // Construtor da classe
    public ChestLockListener(JavaPlugin plugin) {
        this.lockedChestsManager = new LockedChests(); // Inicialize o gerenciador de baús trancados
        plugin.getServer().getPluginManager().registerEvents(this, plugin); // Registra o listener
        this.plugin = plugin;
    }

    // Exemplo: Apenas recarrega ao primeiro jogador entrar, se necessário
    // Recarregar os baús
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        MessageManager messageManager = new MessageManager();

        // Idioma do jogador (defina conforme sua lógica)
        String language = "br"; // Ou "en"

        event.getPlayer().sendMessage(messageManager.getMessage("player_join_welcome", language));
        lockedChestsManager.loadLockedChests();
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.CHEST) {
            Player player = event.getPlayer();
            String blockLocation = block.getLocation().toString();
            MessageManager messageManager = new MessageManager();

            // Idioma do jogador (defina conforme sua lógica)
            String language = "br"; // Ou "en"

            if (lockedChestsManager.isLocked(blockLocation)) {
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getType() == Material.NAME_TAG && itemInHand.getItemMeta() != null) {
                    String nameTag = itemInHand.getItemMeta().getDisplayName();
                    String originalPassword = lockedChestsManager.getPassword(blockLocation);

                    if (nameTag.equals(originalPassword)) {
                        player.sendMessage(messageManager.getMessage("unlocked_temp", language, "password", originalPassword));

                        // Destranca o baú temporariamente
                        unlockChest(player, (Chest) block.getState(), nameTag);

                        // Relock automático
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            lockChest((Chest) block.getState(), originalPassword, player);
                            player.sendMessage(ChatColor.AQUA + messageManager.getMessage("relock_chest", language));
                        }, 100L); // 100 ticks = 5 segundos
                    } else {
                        player.sendMessage(ChatColor.RED + messageManager.getMessage("incorrect_password", language));
                    }
                } else {
                    player.sendMessage(ChatColor.GOLD + messageManager.getMessage("locked_chest", language));
                }
                event.setCancelled(true);
            } else {
                player.sendMessage(ChatColor.GREEN + messageManager.getMessage("unlock_chest", language, "password", "your_password"));
            }
        }
    }



    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST) {
            String blockLocation = block.getLocation().toString();
            MessageManager messageManager = new MessageManager();

            // Idioma do jogador (defina conforme sua lógica)
            String language = "br"; // Ou "en"

            if (lockedChestsManager.isLocked(blockLocation)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.GREEN + messageManager.getMessage("block_break_denied", language));
            }
        }
    }


    public void lockChest(Chest chest, String password, Player player) {
        MessageManager messageManager = new MessageManager();
        String language = "br"; // Ou "en"

        String blockLocation = chest.getBlock().getLocation().toString();
        lockedChestsManager.addLockedChest(blockLocation, password, player.getName());

        Block adjacentBlock = getAdjacentChestBlock(chest.getBlock());
        if (adjacentBlock != null) {
            lockedChestsManager.addLockedChest(blockLocation, password, player.getName());
        }

        player.sendMessage(ChatColor.GREEN + messageManager.getMessage("lock_success", language));

        boolean playerHasNameTag = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.NAME_TAG &&
                    item.getItemMeta() != null &&
                    item.getItemMeta().getDisplayName().equals(password)) {
                playerHasNameTag = true;
                break;
            }
        }

        if (!playerHasNameTag) {
            ItemStack nameTag = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = nameTag.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(password);
                nameTag.setItemMeta(meta);
            }
            player.getInventory().addItem(nameTag);
            chest.getInventory().addItem(nameTag);
            player.sendMessage(messageManager.getMessage("name_tag_received", language));
        }
    }

    public void unlockChest(Player player, Chest chest, String password) {
        MessageManager messageManager = new MessageManager();
        String language = "br"; // Ou "en"

        String blockLocation = chest.getBlock().getLocation().toString();
        if (lockedChestsManager.isLocked(blockLocation) && lockedChestsManager.getPassword(blockLocation).equals(password)) {
            lockedChestsManager.removeLockedChest(blockLocation);

            Block adjacentBlock = getAdjacentChestBlock(chest.getBlock());
            if (adjacentBlock != null) {
                String adjacentBlockLocation = adjacentBlock.getLocation().toString();
                lockedChestsManager.removeLockedChest(adjacentBlockLocation);
            }

            player.sendMessage(messageManager.getMessage("unlock_chest", language));
            player.playNote(player.getLocation(), Instrument.GUITAR, Note.flat(0, Tone.A));
        } else {
            player.sendMessage(messageManager.getMessage(ChatColor.RED + "incorrect_password", language));
        }
    }



    public String getChestPassword(Chest chest) { // Método para obter a senha do baú
        String blockLocation = chest.getBlock().getLocation().toString();
        return lockedChestsManager.getPassword(blockLocation); // Usa lockedChestsManager
    }

    private Block getAdjacentChestBlock(Block block) {
        Material chestMaterial = Material.CHEST;
        Block[] adjacentBlocks = {
                block.getRelative(1, 0, 0),
                block.getRelative(-1, 0, 0),
                block.getRelative(0, 0, 1),
                block.getRelative(0, 0, -1)
        };

        for (Block adjacentBlock : adjacentBlocks) {
            if (adjacentBlock.getType() == chestMaterial) {
                return adjacentBlock;
            }
        }
        return null;
    }
}

