package org.exampleorg.example.pow4.Pow4.blockdata;

import org.bukkit.ChatColor;
import org.bukkit.Material;
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

import java.util.HashMap;
import java.util.Map;

public class ChestLockListener implements Listener {
    private final Map<String, String> lockedChests;
    private final Map<Player, Long> lastMessageSent; // Adicione este mapa para rastrear o último envio de mensagem a cada jogador

    public ChestLockListener(JavaPlugin plugin) {
        this.lockedChests = new HashMap<>();
        this.lastMessageSent = new HashMap<>(); // Inicialize o mapa
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.CHEST) {
            Player player = event.getPlayer();
            String blockLocation = block.getLocation().toString();

            // Verifica se o baú está trancado
            if (lockedChests.containsKey(blockLocation)) {
                long currentTime = System.currentTimeMillis();
                long lastMessageTime = lastMessageSent.getOrDefault(player, 0L);

                // Evita o envio de mensagens em menos de 5 segundos
                if (currentTime - lastMessageTime >= 5000) { // 5000 ms = 5 segundos
                    ItemStack itemInHand = player.getInventory().getItemInMainHand();
                    if (itemInHand.getType() == Material.NAME_TAG && itemInHand.getItemMeta() != null) {
                        String nameTag = itemInHand.getItemMeta().getDisplayName();
                        if (nameTag.equals(lockedChests.get(blockLocation))) {
                            // Desbloqueia o baú
                            player.sendMessage(ChatColor.GREEN + "Baú destrancado com sucesso! Será trancado novamente em 5 segundos.");
                            lockedChests.remove(blockLocation);

                            new java.util.Timer().schedule(new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    lockChest((Chest) block.getState(), nameTag, player);
                                    player.sendMessage(ChatColor.GOLD + "O baú foi trancado novamente.");
                                }
                            }, 5000); // Rebloqueio após 5 segundos
                        } else {
                            player.sendMessage(ChatColor.DARK_RED + "Senha incorreta. Use a etiqueta correta!");
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_AQUA + "O baú está trancado. Segure a etiqueta correta ou use /unlock.");
                    }

                    // Atualiza o tempo da última mensagem
                    lastMessageSent.put(player, currentTime);
                }

                event.setCancelled(true); // Cancela a interação com o baú
            } else {
                player.sendMessage("Baú aberto! Use /lock <senha> para trancá-lo.");
            }
        }
    }





    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST) {
            String blockLocation = block.getLocation().toString();
            if (lockedChests.containsKey(blockLocation)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("Você não pode destruir um baú trancado.");
            }
        }
    }

    public void lockChest(Chest chest, String password, Player player) { // Torne o método público
        String blockLocation = chest.getBlock().getLocation().toString();
        lockedChests.put(blockLocation, password);

        // Verifica e tranca o baú duplo, se houver
        Block adjacentBlock = getAdjacentChestBlock(chest.getBlock());
        if (adjacentBlock != null) {
            String adjacentBlockLocation = adjacentBlock.getLocation().toString();
            lockedChests.put(adjacentBlockLocation, password);
        }

        // Verifica se o jogador já possui uma etiqueta com a senha
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
            player.sendMessage(ChatColor.GOLD + "Você recebeu uma etiqueta com a senha.");
        }
    }

    public void unlockChest(Player player, Chest chest, String password) {
        String blockLocation = chest.getBlock().getLocation().toString();
        if (lockedChests.containsKey(blockLocation) && lockedChests.get(blockLocation).equals(password)) {
            lockedChests.remove(blockLocation);

            // Destranca o baú duplo, se houver
            Block adjacentBlock = getAdjacentChestBlock(chest.getBlock());
            if (adjacentBlock != null) {
                String adjacentBlockLocation = adjacentBlock.getLocation().toString();
                lockedChests.remove(adjacentBlockLocation);
            }

            player.sendMessage("Baú destrancado com sucesso!");
        } else {
            player.sendMessage("Senha incorreta.");
        }
    }

    public String getChestPassword(Chest chest) { // Novo método para obter a senha do baú
        String blockLocation = chest.getBlock().getLocation().toString();
        return lockedChests.get(blockLocation);
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
