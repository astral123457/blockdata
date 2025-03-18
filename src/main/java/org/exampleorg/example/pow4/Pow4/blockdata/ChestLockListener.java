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
import org.bukkit.Material;

import org.bukkit.event.player.PlayerJoinEvent;

import org.bukkit.ChatColor;


public class ChestLockListener implements Listener {

    // Gerenciador de baús trancados
    private final LockedChests lockedChestsManager;

    private final JavaPlugin plugin;

    private boolean isLoaded = false;



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
        if (!isLoaded) { // Verifica se já foi carregado
            lockedChestsManager.loadLockedChests();
            isLoaded = true; // Marca como carregado
        } else {
            System.out.println("Os baus ja estao carregados na memoria.");
        }
    }


    public String getChestPassword(Chest chest) {
        String blockLocation = chest.getBlock().getLocation().toString();
        return lockedChestsManager.getPassword(blockLocation);
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.CHEST) {
            Player player = event.getPlayer();
            String blockLocation = block.getLocation().toString();
            MessageManager messageManager = new MessageManager();

            // Obtém o idioma do jogador
            String language = getPlayerLanguage(player);

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

                        }

                    }

                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f); // Som para assustar "ladrões"
                    player.setFireTicks(369);
                    player.getWorld().strikeLightning(player.getLocation());
                    player.sendMessage(ChatColor.GOLD + messageManager.getMessage("locked_chest", language));
                }
                event.setCancelled(true);
            } //else {
            // player.sendMessage(ChatColor.GREEN + "🔋 Baú aberto! Use /lock <senha> para trancá-lo." + ChatColor.WHITE +"📶");
            // player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1.0f, 1.2f);
            // Mensagem e som de baú destrancado
            //player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
            //player.sendMessage(ChatColor.GREEN + "🔋" +ChatColor.WHITE +"📶");
            //player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f); // Som para aviso de destravamento
            //}
        }
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST) {
            String blockLocation = block.getLocation().toString();
            MessageManager messageManager = new MessageManager();

            // Obtém o jogador que quebrou o bloco
            Player player = event.getPlayer();

            // Obtém o idioma do jogador
            String language = getPlayerLanguage(player);

            if (lockedChestsManager.isLocked(blockLocation)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.GREEN + messageManager.getMessage("block_break_denied", language));
            }
        }
    }


    public void lockChest(Chest chest, String password, Player player) {

        MessageManager messageManager = new MessageManager();
        // Obtém o idioma do jogador
        String language = getPlayerLanguage(player);

        // Obtém a localização do bloco como string
        String blockLocation = chest.getBlock().getLocation().toString();

        // Verifica se o baú já está trancado
        if (lockedChestsManager.isLocked(blockLocation)) {
            player.sendMessage(ChatColor.RED + messageManager.getMessage("lock_already_exists", language));
            return; // Sai do método para evitar sobrescrever
        }

        // Adiciona o baú ao gerenciador de baús trancados
        lockedChestsManager.addLockedChest(blockLocation, password, player.getName());

        // Verifica se há um baú adjacente e adiciona ao gerenciador, se necessário
        Block adjacentBlock = getAdjacentChestBlock(chest.getBlock());
        if (adjacentBlock != null) {
            String adjacentLocation = adjacentBlock.getLocation().toString();

            if (!lockedChestsManager.isLocked(adjacentLocation)) {
                lockedChestsManager.addLockedChest(adjacentLocation, password, player.getName());
            }
        }

        // Envia mensagem ao jogador confirmando o sucesso
        //player.sendMessage(ChatColor.GREEN + messageManager.getMessage("lock_success", language));
        // Adiciona um efeito visual ao desbloquear (por exemplo, partículas de magia)
        // Efeito de partículas atualizado
        chest.getWorld().spawnParticle(Particle.CRIT, chest.getLocation().add(0.5, 1, 0.5), 10, 0.3, 0.3, 0.3);
        chest.getWorld().spawnParticle(Particle.NOTE, chest.getLocation().add(0.5, 1, 0.5), 10, 0.3, 0.3, 0.3);

        // Verifica se o jogador já possui uma etiqueta com o nome/senha
        boolean playerHasNameTag = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.NAME_TAG &&
                    item.getItemMeta() != null &&
                    item.getItemMeta().getDisplayName().equals(password)) {
                playerHasNameTag = true;
                break;
            }
        }

        // Se o jogador não possui a etiqueta, cria e adiciona ao inventário
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
            default -> "default";
        };
    }



    // Método para desbloquear um baú unlockChest(player, (Chest) block.getState(), nameTag);
    public void unlockChest(Player player, Chest chest, String password) {
        String blockLocation = chest.getBlock().getLocation().toString();

        MessageManager messageManager = new MessageManager();
        // Obtém o idioma do jogador
        String language = getPlayerLanguage(player);

        // Verifica e remove usando lockedChestsManager
        if (lockedChestsManager.isLocked(blockLocation) && lockedChestsManager.getPassword(blockLocation).equals(password)) {
            lockedChestsManager.removeLockedChest(blockLocation);

            // Destranca o baú duplo, se houver
            Block adjacentBlock = getAdjacentChestBlock(chest.getBlock());
            if (adjacentBlock != null) {
                String adjacentBlockLocation = adjacentBlock.getLocation().toString();
                lockedChestsManager.removeLockedChest(adjacentBlockLocation);
            }

            //player.sendMessage("Baú destrancado com sucesso!");
            player.sendMessage(ChatColor.DARK_GREEN + messageManager.getMessage("unlock_chest", language));
        } else {
            //player.sendMessage("Senha incorreta.");
            player.sendMessage(ChatColor.DARK_RED + messageManager.getMessage("incorrect_password", language));
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

