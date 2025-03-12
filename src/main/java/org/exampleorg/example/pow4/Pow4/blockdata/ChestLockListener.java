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
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.Inventory;




import org.bukkit.event.player.PlayerJoinEvent;

import org.bukkit.ChatColor;


public class ChestLockListener implements Listener {

    // Gerenciador de baús trancados
    private final LockedChests lockedChestsManager;

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
            String language = "br"; // Pode mudar para "en" se necessário

            if (lockedChestsManager.isLocked(blockLocation)) {
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getType() == Material.NAME_TAG && itemInHand.getItemMeta() != null) {
                    String nameTag = itemInHand.getItemMeta().getDisplayName();
                    String originalPassword = lockedChestsManager.getPassword(blockLocation);

                    if (nameTag.equals(originalPassword)) {
                        // Mensagem e som de destravamento bem-sucedido
                        player.sendMessage(messageManager.getMessage("unlocked_temp", language, "password", originalPassword));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);

                        // Destranca o baú temporariamente
                        unlockChest(player, (Chest) block.getState(), nameTag);

                        // Relock automático
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            lockChest((Chest) block.getState(), originalPassword, player);
                            player.sendMessage(ChatColor.AQUA + messageManager.getMessage("relock_chest", language));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f); // Som para relock
                        }, 100L); // 100 ticks = 5 segundos
                    } else {
                        // Mensagem e som de senha incorreta
                        player.sendMessage(ChatColor.RED + messageManager.getMessage("incorrect_password", language));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                } else {
                    // Mensagem e som de baú trancado
                    // Verifica se o bloco colocado é um baú

                    if (block.getType() == Material.CHEST) {
                        // Obtém a posição 1 bloco acima do baú
                        Block aboveBlock = block.getLocation().add(0, 1, 0).getBlock();

                        if (aboveBlock.getType() == Material.AIR) {
                            // Define o bloco como uma cabeça de dragão
                            aboveBlock.setType(Material.DRAGON_HEAD);

                            // Configura a rotação da cabeça
                            BlockState state = aboveBlock.getState();
                            if (state instanceof Skull skull) {
                                // Define a rotação baseada na direção do jogador
                                skull.setRotation(player.getFacing().getOppositeFace()); // Gira para a direção oposta
                                skull.update();
                            }

                        }

                    }

                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f); // Som para assustar "ladrões"
                    player.setFireTicks(369);
                    //player.sendMessage(ChatColor.GOLD + messageManager.getMessage("locked_chest", language));
                }
                event.setCancelled(true);
            } else {
                // Mensagem e som de baú destrancado
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f); // Som para aviso de destravamento
                //player.sendMessage(ChatColor.GREEN + messageManager.getMessage("unlock_chest", language, "password", "your_password"));
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

            player.playNote(player.getLocation(), Instrument.GUITAR, Note.flat(0, Tone.A));
        } else {
            player.sendMessage(messageManager.getMessage(ChatColor.RED + "incorrect_password", language));
        }
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

