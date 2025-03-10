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

    // Gerenciador de baús trancados
    private final LockedChests lockedChestsManager;

    // Mapa para rastrear o último envio de mensagem a cada jogador
    private final Map<Player, Long> lastMessageSent;

    // Construtor da classe
    public ChestLockListener(JavaPlugin plugin) {
        this.lockedChestsManager = new LockedChests(); // Inicialize o gerenciador de baús trancados
        this.lastMessageSent = new HashMap<>(); // Inicialize o mapa de mensagens enviadas
        plugin.getServer().getPluginManager().registerEvents(this, plugin); // Registra o listener
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.CHEST) {
            Player player = event.getPlayer();
            String blockLocation = block.getLocation().toString();
            long currentTime = System.currentTimeMillis();
            long lastMessageTime = lastMessageSent.getOrDefault(player, 0L);

            // Verifica se o baú está trancado
            if (lockedChestsManager.isLocked(blockLocation)) { // Verifica se o baú está trancado
                if (currentTime - lastMessageTime >= 5000) { // Controle de intervalo de mensagens
                    ItemStack itemInHand = player.getInventory().getItemInMainHand();
                    if (itemInHand.getType() == Material.NAME_TAG && itemInHand.getItemMeta() != null) {
                        String nameTag = itemInHand.getItemMeta().getDisplayName();
                        String originalPassword = lockedChestsManager.getPassword(blockLocation); // Obtém a senha original

                        if (nameTag.equals(originalPassword)) { // Verifica se a senha está correta
                            player.sendMessage(ChatColor.GREEN + "Baú destrancado com sucesso! Será trancado novamente em 5 segundos.");

                            // Destranca o baú temporariamente
                            unlockChest(player, (Chest) block.getState(), nameTag);

                            // Armazena a senha original e agenda o trancamento automático
                            new java.util.Timer().schedule(new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    // Tranca o baú novamente com a senha original
                                    lockChest((Chest) block.getState(), originalPassword, player);
                                    player.sendMessage(ChatColor.GOLD + "O baú foi trancado novamente com a senha original.");
                                }
                            }, 5000); // Rebloqueio após 5 segundos
                        } else {
                            player.sendMessage(ChatColor.DARK_RED + "Senha incorreta. Use a etiqueta correta!");
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_AQUA + "O baú está trancado. Segure a etiqueta correta ou use /unlock.");
                    }
                    lastMessageSent.put(player, currentTime); // Atualiza o tempo da última mensagem
                }
                event.setCancelled(true); // Cancela a interação com o baú para impedir acesso sem autorização
            } else {
                if (currentTime - lastMessageTime >= 5000) { // Controle de intervalo de mensagens
                    player.sendMessage("Baú aberto! Use /lock <senha> para trancá-lo.");
                    lastMessageSent.put(player, currentTime); // Atualiza o tempo da última mensagem
                }
            }
        }
    }



    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST) {
            String blockLocation = block.getLocation().toString();

            // Verifica usando lockedChestsManager
            if (lockedChestsManager.isLocked(blockLocation)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("Você não pode destruir um baú trancado.");
            }
        }
    }

    public void lockChest(Chest chest, String password, Player player) {
        String blockLocation = chest.getBlock().getLocation().toString();

        // Adiciona ao mapa usando lockedChestsManager
        lockedChestsManager.addLockedChest(blockLocation, password, player.getName());

        // Verifica e tranca o baú duplo, se houver
        Block adjacentBlock = getAdjacentChestBlock(chest.getBlock());
        if (adjacentBlock != null) {
            String adjacentBlockLocation = adjacentBlock.getLocation().toString();

            // Também adiciona o baú adjacente
            lockedChestsManager.addLockedChest(blockLocation, password, player.getName());
        }

        // Opcional: Mensagem para o jogador
        player.sendMessage(ChatColor.GOLD + "Baú trancado com sucesso!");

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

        // Verifica e remove usando lockedChestsManager
        if (lockedChestsManager.isLocked(blockLocation) && lockedChestsManager.getPassword(blockLocation).equals(password)) {
            lockedChestsManager.removeLockedChest(blockLocation);

            // Destranca o baú duplo, se houver
            Block adjacentBlock = getAdjacentChestBlock(chest.getBlock());
            if (adjacentBlock != null) {
                String adjacentBlockLocation = adjacentBlock.getLocation().toString();
                lockedChestsManager.removeLockedChest(adjacentBlockLocation);
            }

            player.sendMessage("Baú destrancado com sucesso!");
        } else {
            player.sendMessage("Senha incorreta.");
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

