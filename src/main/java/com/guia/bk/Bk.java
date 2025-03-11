package com.guia.bk;

import com.google.gson.GsonBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;


import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class Bk extends JavaPlugin implements Listener {

    private static final String DB_URL = "jdbc:sqlite:plugins/bk/bk.db";
    private static final String FOLDER_PATH = "plugins/bk";
    private static final String CONFIG_FILE = FOLDER_PATH + "/config.json";
    private static final String MESSAGES_FILE_PATH = FOLDER_PATH + "/messages.json";

    @Override
    public void onEnable() {
        // Mensagem no console para indicar ativação
        getLogger().info("Bk plugin habilitado!");

        createFolderAndConfig();
        boolean isEnabled = loadPluginStatus();

        if (!isEnabled) {
            getLogger().warning("Plugin desativado via configuração.");
            getServer().getPluginManager().disablePlugin(this);
            return; // Finaliza a inicialização se o plugin estiver desativado
        }

        // Registrar eventos
        getServer().getPluginManager().registerEvents(this, this);

        // Configurar banco de dados
        setupDatabase();

        // Carregar idioma
        String language = loadLanguage();
        getLogger().info("Idioma configurado: " + language);

        // Registrar comandos
        getCommand("oi").setExecutor(this);
        getCommand("vip").setExecutor(this);
        getCommand("addvip").setExecutor(this);
        getCommand("rmvip").setExecutor(this);

        getCommand("emanuel").setExecutor(this);
        getCommand("lilith").setExecutor(this);
        getCommand("eva").setExecutor(this);
        getCommand("adan").setExecutor(this);

        getCommand("limpar").setExecutor(this);


    }




    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Bk plugin desabilitado!");
    }



    int contador = 0;

    public void adicionarItem(Player player) {
        // Carregar o idioma configurado
        String language = loadLanguage();

        // Carregar mensagens
        MessageManager messageManager = new MessageManager();

        if (contador < 3) {
            player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 1));
            player.getInventory().addItem(new ItemStack(Material.BREAD, 1));
            player.getInventory().addItem(new ItemStack(Material.APPLE, 1));

            contador++;
            // Busca a mensagem traduzida
            String message = messageManager.getMessage("burguer_1", language, "contador", String.valueOf(contador));
            player.sendMessage(ChatColor.GREEN + message + " (" + contador + "/3)");
        } else {
            // Busca a mensagem traduzida
            String message = messageManager.getMessage("burguer_2", language);
            player.sendMessage(ChatColor.RED + message);
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String language = loadLanguage(); // Carrega o idioma configurado
        MessageManager messageManager = new MessageManager(); // Gerenciador de mensagens

        if (command.getName().equalsIgnoreCase("oi")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String playerName = player.getName();

                if (!hasReceivedKit(playerName)) {
                    sender.sendMessage(ChatColor.LIGHT_PURPLE +
                            messageManager.getMessage("welcome", language, "playerName", playerName));
                    sender.sendMessage(ChatColor.GREEN + messageManager.getMessage("kit_received", language));

                    // Dando o kit ao jogador
                    player.getInventory().addItem(new ItemStack(Material.TORCH, 13));
                    player.getInventory().addItem(new ItemStack(Material.BOOK, 1));
                    player.getInventory().addItem(new ItemStack(Material.BREAD, 3));
                    player.getInventory().addItem(new ItemStack(Material.APPLE, 2));
                    player.getInventory().addItem(new ItemStack(Material.CHERRY_WOOD, 36));

                    markKitReceived(playerName);
                } else {
                    sender.sendMessage(ChatColor.LIGHT_PURPLE +
                            messageManager.getMessage("welcome_back", language, "playerName", playerName));
                    sender.sendMessage(ChatColor.GREEN + messageManager.getMessage("kit_already_received", language));
                    adicionarItem(player);
                }
                return true;
            } else {
                sender.sendMessage(messageManager.getMessage("player_only_command", language));
                return true;
            }
        }

        else if (command.getName().equalsIgnoreCase("addvip")) {
            if (sender.hasPermission("vip.add")) {
                if (args.length == 1) {
                    String targetPlayerName = args[0];
                    Player targetPlayer = getServer().getPlayer(targetPlayerName);

                    getServer().dispatchCommand(getServer().getConsoleSender(),
                            "lp user " + targetPlayerName + " permission set vip.use true");

                    if (targetPlayer != null) {
                        addVipToDatabase(targetPlayerName);
                        sender.sendMessage(ChatColor.GREEN +
                                messageManager.getMessage("player_vip", language, "targetPlayerName", targetPlayerName));
                        targetPlayer.sendMessage(ChatColor.GOLD +
                                messageManager.getMessage("vip_congrats", language));
                    } else {
                        sender.sendMessage(ChatColor.RED +
                                messageManager.getMessage("player_not_found", language));
                    }
                } else {
                    sender.sendMessage(ChatColor.RED +
                            messageManager.getMessage("addvip_usage", language));
                }
            } else {
                sender.sendMessage(ChatColor.RED +
                        messageManager.getMessage("no_permission", language));
            }
            return true;
        }

        else if (command.getName().equalsIgnoreCase("rmvip")) {
            if (sender.hasPermission("vip.add")) {
                if (args.length == 1) {
                    String targetPlayerName = args[0];
                    if (removeVipFromDatabase(targetPlayerName)) {
                        getServer().dispatchCommand(getServer().getConsoleSender(),
                                "lp user " + targetPlayerName + " permission unset vip.use");

                        sender.sendMessage(ChatColor.GREEN +
                                messageManager.getMessage("remove_vip", language, "targetPlayerName", targetPlayerName));
                    } else {
                        sender.sendMessage(ChatColor.RED +
                                messageManager.getMessage("not_found_vip", language, "targetPlayerName", targetPlayerName));
                    }
                } else {
                    sender.sendMessage(ChatColor.RED +
                            messageManager.getMessage("removevip_usage", language));
                }
            } else {
                sender.sendMessage(ChatColor.RED +
                        messageManager.getMessage("no_permission", language));
            }
            return true;
        }

        else if (command.getName().equalsIgnoreCase("emanuel")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("vip.use")) {
                    sender.sendMessage(ChatColor.AQUA +messageManager.getMessage("emanuel_comando", language));
                    // Adicionando itens VIP ao jogador

                    player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 40));
                    player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 64));
                    player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 36));
                    player.getInventory().addItem(new ItemStack(Material.EMERALD, 12));
                    player.getInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING, 1));//Sepulcral
                    player.getInventory().addItem(new ItemStack(Material.COAL, 40));//Covodos
                    player.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
                    player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 5)); // + 5 GOLD LINGOT


                    // Criar o capacete de Netherite
                    ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET, 1);
                    helmet.addUnsafeEnchantment(Enchantment.getByName("PROTECTION_ENVIRONMENTAL"), 4);
                    helmet.addUnsafeEnchantment(Enchantment.UNBREAKING, 3); // Inquebrável III
                    helmet.addUnsafeEnchantment(Enchantment.MENDING, 1); // Remendo

                    // Criar outras peças de armadura de Netherite
                    ItemStack chestplate = new ItemStack(Material.NETHERITE_CHESTPLATE, 1);
                    chestplate.addUnsafeEnchantment(Enchantment.getByName("PROTECTION_ENVIRONMENTAL"), 4);
                    chestplate.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
                    chestplate.addUnsafeEnchantment(Enchantment.MENDING, 1);

                    ItemStack leggings = new ItemStack(Material.NETHERITE_LEGGINGS, 1);
                    leggings.addUnsafeEnchantment(Enchantment.getByName("PROTECTION_ENVIRONMENTAL"), 4);
                    leggings.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
                    leggings.addUnsafeEnchantment(Enchantment.MENDING, 1);

                    ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS, 1);
                    boots.addUnsafeEnchantment(Enchantment.getByName("PROTECTION_ENVIRONMENTAL"), 4);
                    boots.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
                    boots.addUnsafeEnchantment(Enchantment.MENDING, 1);
                    boots.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, 3); // Passos Profundos III

                    // Adicionar os itens ao inventário do jogador
                    player.getInventory().addItem(helmet, chestplate, leggings, boots);

                    // Criar a Shulker Box cheia de diamantes
                    ItemStack shulkerBox = new ItemStack(Material.SHULKER_BOX);
                    BlockStateMeta meta = (BlockStateMeta) shulkerBox.getItemMeta();
                    ShulkerBox shulker = (ShulkerBox) meta.getBlockState();

                    // Adicionar 900 diamantes na Shulker Box
                    for (int i = 0; i < 14; i++) { // 14 stacks de 64
                        shulker.getInventory().addItem(new ItemStack(Material.DIAMOND, 64));
                    }
                    shulker.getInventory().addItem(new ItemStack(Material.DIAMOND, 52)); // Resto (52)

                    // Salvar e aplicar as mudanças na Shulker Box
                    meta.setBlockState(shulker);
                    shulkerBox.setItemMeta(meta);

                    // Adicionar a Shulker Box ao inventário do jogador
                    player.getInventory().addItem(shulkerBox);


                } else {
                    sender.sendMessage(ChatColor.RED +
                            messageManager.getMessage("no_permission", language));
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED +messageManager.getMessage("jogador_comando", language));
                return true;
            }
        }

        else if (command.getName().equalsIgnoreCase("lilith")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("vip.use")) {
                    sender.sendMessage(ChatColor.DARK_GREEN + messageManager.getMessage("lilith_comando", language));
                    // Adicionando itens VIP ao jogador

                    player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 64));
                    player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 6));// Prata
                    player.getInventory().addItem(new ItemStack(Material.EMERALD, 10));
                    player.getInventory().addItem(new ItemStack(Material.WOLF_SPAWN_EGG, 1)); // Ovo de Lobo
                    player.getInventory().addItem(new ItemStack(Material.SHEEP_SPAWN_EGG, 1)); // Ovo de Ovelha
                    player.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
                    player.getInventory().addItem(new ItemStack(Material.FLOWER_POT, 10)); // 10 vasos de flores

                    // Criar uma espada de Netherite
                    ItemStack sword = new ItemStack(Material.NETHERITE_SWORD, 1);

                    sword.addUnsafeEnchantment(Enchantment.getByName("DAMAGE_ALL"), 5); // Afiado V
                    sword.addUnsafeEnchantment(Enchantment.getByName("UNBREAKING"), 3); // Inquebrável III
                    sword.addUnsafeEnchantment(Enchantment.getByName("MENDING"), 1); // Remendo
                    sword.addUnsafeEnchantment(Enchantment.getByName("FIRE_ASPECT"), 2); // Aspecto Flamejante II
                    sword.addUnsafeEnchantment(Enchantment.getByName("LOOT_BONUS_MOBS"), 3); // Saque III
                    player.getInventory().addItem(sword);

                    sender.sendMessage(ChatColor.GREEN +messageManager.getMessage("Maldita_comando", language));

                } else {
                    sender.sendMessage(ChatColor.RED +
                            messageManager.getMessage("no_permission", language));
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED +messageManager.getMessage("jogador_comando", language));
                return true;
            }
        }
        else if (command.getName().equalsIgnoreCase("eva")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("vip.use")) {
                    sender.sendMessage(ChatColor.LIGHT_PURPLE +messageManager.getMessage("eva_comando", language));
                    // Adicionando itens VIP ao jogador
                    player.getInventory().addItem(new ItemStack(Material.DIAMOND, 64));// Rainha do Mar do Lado Oeste 27 Talentos
                    player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 64));
                    player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 64));

                    player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 64));
                    player.getInventory().addItem(new ItemStack(Material.NETHERITE_BLOCK, 64));
                    player.getInventory().addItem(new ItemStack(Material.EMERALD, 64));
                    player.getInventory().addItem(new ItemStack(Material.COAL, 64));
                    player.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));


                    ItemStack hoe = new ItemStack(Material.NETHERITE_HOE, 1);
                    ItemStack axe = new ItemStack(Material.NETHERITE_AXE, 1);

                    // Criar um escudo
                    ItemStack shield = new ItemStack(Material.SHIELD, 1);

                    // Adicionar encantamentos ao escudo
                    shield.addUnsafeEnchantment(Enchantment.getByName("DURABILITY"), 3); // Inquebrável III
                    shield.addUnsafeEnchantment(Enchantment.MENDING, 1); // Remendo

                    // Adicionar o escudo ao inventário do jogador
                    player.getInventory().addItem(shield,hoe,axe);

                    sender.sendMessage(ChatColor.AQUA +messageManager.getMessage("Rainha_comando", language));

                } else {
                    sender.sendMessage(ChatColor.RED +
                            messageManager.getMessage("no_permission", language));
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED +messageManager.getMessage("jogador_comando", language));
                return true;
            }
        }
        else if (command.getName().equalsIgnoreCase("adan")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("vip.use")) {
                    sender.sendMessage(ChatColor.GOLD +messageManager.getMessage("adan_comando", language));
                    // Adicionando itens VIP ao jogador
                    player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64)); // 1 Esporao de 64 Rochas (Pedregulho)
                    player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 6));// Prata

                    player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 40));// Prata
                    player.getInventory().addItem(new ItemStack(Material.TNT, 13)); // 13 blocos de TNT
                    player.getInventory().addItem(new ItemStack(Material.SHEEP_SPAWN_EGG, 14)); // 14 Talentos do Norte do Friu bicho peludinho ovos de ovelhas

                    // Criar a Shulker Box cheia de diamantes
                    ItemStack shulkerBox = new ItemStack(Material.SHULKER_BOX);
                    BlockStateMeta meta = (BlockStateMeta) shulkerBox.getItemMeta();
                    ShulkerBox shulker = (ShulkerBox) meta.getBlockState();

                    player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET, 22)); // 22 Baldes de Água


                    // Salvar e aplicar as mudanças na Shulker Box
                    meta.setBlockState(shulker);
                    shulkerBox.setItemMeta(meta);

                    // Adicionar a Shulker Box ao inventário do jogador
                    player.getInventory().addItem(shulkerBox);


                } else {
                    sender.sendMessage(ChatColor.RED +
                            messageManager.getMessage("no_permission", language));
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED +messageManager.getMessage("jogador_comando", language));
                return true;
            }
        }

        else if (command.getName().equalsIgnoreCase("limpar")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("vip.use")) {

                        // Remove todos os itens do inventário do jogador
                        player.getInventory().clear();
                    sender.sendMessage(ChatColor.GREEN +messageManager.getMessage("limpo_comando", language));

                    } else {
                    sender.sendMessage(ChatColor.RED +messageManager.getMessage("jogador_comando", language));
                    }
                return true;
            } else {
                return true;
            }
        }


        else if (command.getName().equalsIgnoreCase("vip")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("vip.use")) {
                    sender.sendMessage(ChatColor.GOLD + "Bem-vindo, VIP!");
                    player.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
                } else {
                    sender.sendMessage(ChatColor.RED +
                            messageManager.getMessage("no_permission", language));
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED +messageManager.getMessage("jogador_comando", language));
                return true;
            }
        }
        return false;
    }



    private void setupDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS kit_log (" +
                    "player_name VARCHAR(255) NOT NULL," +
                    "PRIMARY KEY (player_name))";
            stmt.executeUpdate(sql);

            String sql2 = "CREATE TABLE IF NOT EXISTS death_locations (" +
                    "player_id VARCHAR(36) NOT NULL," +
                    "world VARCHAR(255) NOT NULL," +
                    "x DOUBLE NOT NULL," +
                    "y DOUBLE NOT NULL," +
                    "z DOUBLE NOT NULL," +
                    "yaw FLOAT NOT NULL," +
                    "pitch FLOAT NOT NULL," +
                    "PRIMARY KEY (player_id))";
            stmt.executeUpdate(sql2);


            String sql3 = "CREATE TABLE IF NOT EXISTS vip_players (" +
                    "player_name VARCHAR(255) NOT NULL," +
                    "PRIMARY KEY (player_name))";
            stmt.executeUpdate(sql3);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
// Métodos para armazenar e excluir
    private void addVipToDatabase(String playerName) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO vip_players (player_name) VALUES (?)")) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean removeVipFromDatabase(String playerName) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM vip_players WHERE player_name = ?")) {
            pstmt.setString(1, playerName);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean hasReceivedKit(String playerName) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT player_name FROM kit_log WHERE player_name = ?")) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void markKitReceived(String playerName) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO kit_log (player_name) VALUES (?)")) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void createFolderAndConfig() {
        // Criar a pasta blockdata, se não existir
        File folder = new File(FOLDER_PATH);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Criar o arquivo config.json com configuração padrão
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            JsonObject defaultConfig = new JsonObject();
            defaultConfig.addProperty("enabled", true);
            defaultConfig.addProperty("language", "br"); // Adiciona o idioma padrão

            try (FileWriter writer = new FileWriter(configFile)) {
                // Gson com Pretty Printing
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(defaultConfig, writer);
                getLogger().info("Arquivo config.json criado com configuração padrão.");
            } catch (IOException e) {
                getLogger().severe("Erro ao criar o arquivo config.json: " + e.getMessage());
            }
        }

        // Criar o arquivo messages.json com mensagens padrão, apenas se não existir
        File messagesFile = new File(MESSAGES_FILE_PATH);
        if (!messagesFile.exists()) {
            JsonObject messages = new JsonObject();

            JsonObject lockChestMessages = new JsonObject();
            lockChestMessages.addProperty("en", "You received a burger during the day today! (");
            lockChestMessages.addProperty("es", "¡Recibiste una hamburguesa hoy! (");
            lockChestMessages.addProperty("ru", "Сегодня вы получили бургер! (");
            lockChestMessages.addProperty("he", "קיבלת היום המבורגר! (");
            lockChestMessages.addProperty("ch", "今天你收到一個漢堡了！ (");
            lockChestMessages.addProperty("ar", "لقد تلقيت برجر اليوم! (");
            lockChestMessages.addProperty("gr", "Έλαβες ένα μπέργκερ σήμερα! (");
            lockChestMessages.addProperty("br", "Você recebeu um hambúrguer durante o dia de hoje! (");

            JsonObject unlockChestMessages = new JsonObject();
            unlockChestMessages.addProperty("en", "You have already received the maximum number of burgers allowed for today.");
            unlockChestMessages.addProperty("es", "Ya has recibido el número máximo de hamburguesas permitidas hoy.");
            unlockChestMessages.addProperty("ru", "Вы уже получили максимально разрешенное сегодня количество гамбургеров.");
            unlockChestMessages.addProperty("he", "כבר קיבלת את מספר ההמבורגרים המרבי המותר היום.");
            unlockChestMessages.addProperty("ch", "您今天已經收到了允許的最大數量的漢堡。");
            unlockChestMessages.addProperty("ar", "لقد تلقيت بالفعل الحد الأقصى لعدد البرغر المسموح به اليوم.");
            unlockChestMessages.addProperty("gr", "Έχετε ήδη λάβει τον μέγιστο επιτρεπόμενο αριθμό μπέργκερ σήμερα.");
            unlockChestMessages.addProperty("br", "Você já recebeu o máximo de hambúrgueres permitido durante o dia de hoje.");

            JsonObject vipMessages = new JsonObject();
            vipMessages.addProperty("en", "Welcome, VIP!");
            vipMessages.addProperty("es", "¡Bienvenido, VIP!");
            vipMessages.addProperty("ru", "Добро пожаловать, VIP!");
            vipMessages.addProperty("he", "ברוך הבא, VIP!");
            vipMessages.addProperty("ch", "欢迎，VIP！");
            vipMessages.addProperty("ar", "مرحبًا، VIP!");
            vipMessages.addProperty("gr", "Καλώς ήρθατε, VIP!");
            vipMessages.addProperty("es", "Bem vindo, VIP!");

            // Mensagem 1
            JsonObject maxBurgerMessages = new JsonObject();
            maxBurgerMessages.addProperty("br", "Você já recebeu o máximo de hambúrgueres permitido durante o dia de hoje.");
            maxBurgerMessages.addProperty("en", "You have already received the maximum number of burgers allowed for today.");
            messages.add("max_burger", maxBurgerMessages);

            messages.add("burguer_1", lockChestMessages);
            messages.add("burguer_2", unlockChestMessages);

// Mensagem 2
            JsonObject welcomeMessages = new JsonObject();
            welcomeMessages.addProperty("br", "Bem vindo, {playerName}!");
            welcomeMessages.addProperty("en", "Welcome, {playerName}!");
            messages.add("welcome", welcomeMessages);

// Mensagem 3
            JsonObject kitReceivedMessages = new JsonObject();
            kitReceivedMessages.addProperty("br", "Você acaba de receber um kit de sobrevivência básico!");
            kitReceivedMessages.addProperty("en", "You just received a basic survival kit!");
            messages.add("kit_received", kitReceivedMessages);

// Mensagem 4
            JsonObject welcomeBackMessages = new JsonObject();
            welcomeBackMessages.addProperty("br", "Bem vindo de volta, {playerName}!");
            welcomeBackMessages.addProperty("en", "Welcome back, {playerName}!");
            messages.add("welcome_back", welcomeBackMessages);

// Mensagem 5
            JsonObject kitAlreadyReceivedMessages = new JsonObject();
            kitAlreadyReceivedMessages.addProperty("br", "Você já recebeu seu kit!");
            kitAlreadyReceivedMessages.addProperty("en", "You have already received your kit!");
            messages.add("kit_already_received", kitAlreadyReceivedMessages);

// Mensagem 6
            JsonObject playerOnlyCommandMessages = new JsonObject();
            playerOnlyCommandMessages.addProperty("br", "Este comando só pode ser executado por um jogador.");
            playerOnlyCommandMessages.addProperty("en", "This command can only be executed by a player.");
            messages.add("player_only_command", playerOnlyCommandMessages);

// Mensagem 7
            JsonObject playerVIPMessages = new JsonObject();
            playerVIPMessages.addProperty("br", "O jogador {targetPlayerName} agora é um VIP!");
            playerVIPMessages.addProperty("en", "The player {targetPlayerName} is now a VIP!");
            messages.add("player_vip", playerVIPMessages);

// Mensagem 8
            JsonObject vipCongratsMessages = new JsonObject();
            vipCongratsMessages.addProperty("br", "Parabéns, você recebeu o status de VIP!");
            vipCongratsMessages.addProperty("en", "Congratulations, you have received VIP status!");
            messages.add("vip_congrats", vipCongratsMessages);

// Mensagem 9
            JsonObject playerNotFoundMessages = new JsonObject();
            playerNotFoundMessages.addProperty("br", "Jogador não encontrado.");
            playerNotFoundMessages.addProperty("en", "Player not found.");
            messages.add("player_not_found", playerNotFoundMessages);

// Mensagem 10
            JsonObject addVIPUsageMessages = new JsonObject();
            addVIPUsageMessages.addProperty("br", "Uso correto: /addvip <nome_do_jogador>");
            addVIPUsageMessages.addProperty("en", "Correct usage: /addvip <player_name>");
            messages.add("addvip_usage", addVIPUsageMessages);

// Mensagem 11
            JsonObject noPermissionMessages = new JsonObject();
            noPermissionMessages.addProperty("br", "Você não tem permissão para usar este comando.");
            noPermissionMessages.addProperty("en", "You do not have permission to use this command.");
            messages.add("no_permission", noPermissionMessages);

// Mensagem 12
            JsonObject removeVIPMessages = new JsonObject();
            removeVIPMessages.addProperty("br", "O jogador {targetPlayerName} não é mais VIP.");
            removeVIPMessages.addProperty("en", "The player {targetPlayerName} is no longer a VIP.");
            messages.add("remove_vip", removeVIPMessages);

// Mensagem 13
            JsonObject notFoundVIPMessages = new JsonObject();
            notFoundVIPMessages.addProperty("br", "O jogador {targetPlayerName} não foi encontrado como VIP.");
            notFoundVIPMessages.addProperty("en", "The player {targetPlayerName} was not found as a VIP.");
            messages.add("not_found_vip", notFoundVIPMessages);

// Mensagem 14
            JsonObject removeVIPUsageMessages = new JsonObject();
            removeVIPUsageMessages.addProperty("br", "Uso correto: /rmvip <nome_do_jogador>");
            removeVIPUsageMessages.addProperty("en", "Correct usage: /rmvip <player_name>");
            messages.add("removevip_usage", removeVIPUsageMessages);

// Mensagem 15
            JsonObject inventoryClearedMessages = new JsonObject();
            inventoryClearedMessages.addProperty("br", "Todo o seu inventário foi limpo!");
            inventoryClearedMessages.addProperty("en", "Your entire inventory has been cleared!");
            messages.add("inventory_cleared", inventoryClearedMessages);

// Mensagem 16
            JsonObject vipWelcomeMessages = new JsonObject();
            vipWelcomeMessages.addProperty("br", "Bem-vindo, VIP!");
            vipWelcomeMessages.addProperty("en", "Welcome, VIP!");
            messages.add("vip_welcome", vipWelcomeMessages);

            JsonObject jogadorcomando = new JsonObject();
            jogadorcomando.addProperty("br", "Este comando só pode ser executado por um jogador.");
            jogadorcomando.addProperty("en", "This command can only be executed by one player.");
            messages.add("jogador_comando", jogadorcomando);

            //sender.sendMessage(ChatColor.RED +messageManager.getMessage("jogador_comando", language));

            JsonObject emanuelcomando = new JsonObject();
            emanuelcomando.addProperty("br", "XAΓ, Cristo  estratégia de precificação, escala, série de ideias,  uma estrela, um macho dominante!");
            emanuelcomando.addProperty("en", "XAΓ, Christ pricing strategy, scale, series of ideas, a star, a dominant male!");
            messages.add("emanuel_comando", emanuelcomando);

            //sender.sendMessage(ChatColor.RED +messageManager.getMessage("emanuel_comando", language));

            JsonObject lilithcomando = new JsonObject();//Satanas persoadio a cobra ela era a mas linda do jardim tinha asas e fez ela trazer eva e a cobra(lilith) por sua vez com eva e adao
            lilithcomando.addProperty("br", "ΘΕ, Consciência emocional é pequena e leva a Morte Princesa Lilith");
            lilithcomando.addProperty("en", "ΘΕ, Emotional awareness is small and leads to Death Princess Lilith");
            messages.add("lilith_comando", lilithcomando);//a ma lingua da magia a ma fala a maldicao ela a cobrinha pesoadida de satanas mas nao e o proprio satanas

            //sender.sendMessage(ChatColor.RED +messageManager.getMessage("lilith_comando", language));


            JsonObject evacomando = new JsonObject();
            evacomando.addProperty("br", "ΣΚ, Lesbica Feminista é uma criatura aquática, semelhante a uma tartaruga, que habita rios e lagos ( EVA e Seus pecados Do mundo)");
            evacomando.addProperty("en", "ΣΚ, Lesbian Feminist is an aquatic creature, similar to a turtle, that inhabits rivers and lakes (EVE and Her Sins of the World)");
            messages.add("eva_comando", evacomando);

            //sender.sendMessage(ChatColor.RED +messageManager.getMessage("eva_comando", language));

            JsonObject adancomando = new JsonObject();
            adancomando.addProperty("br", "ΔΙ, LAtino Pequeno (Homen Barro Adao)");
            adancomando.addProperty("en", "ΔΙ, Small Latin (Clay Man Adam)");
            messages.add("adan_comando", adancomando);

            //sender.sendMessage(ChatColor.RED +messageManager.getMessage("adan_comando", language));

            JsonObject Rainhacomando = new JsonObject();
            Rainhacomando.addProperty("br", "Rainha do Mar do Lado Oeste 27 Talentos!");
            Rainhacomando.addProperty("en", "Queen of the West Sea 27 Talents!");
            messages.add("Rainha_comando", Rainhacomando);



            JsonObject Malditacomando = new JsonObject();
            Malditacomando.addProperty("br", "Sera Maldita ente todas sempentes voce mata os amimais!");
            Malditacomando.addProperty("en", "You will be cursed among all creatures, you kill animals!");
            messages.add("Maldita_comando", Malditacomando);


            JsonObject limpocomando = new JsonObject();
            limpocomando.addProperty("br", "Todo o seu inventário foi limpo!");
            limpocomando.addProperty("en", "Your entire inventory has been cleared!");
            messages.add("limpo_comando", limpocomando);







            try (FileWriter writer = new FileWriter(messagesFile)) {

                // Gson com Pretty Printing
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(messages, writer);
                getLogger().info("Arquivo messages.json criado com mensagens padrão.");
            } catch (IOException e) {
                getLogger().severe("Erro ao criar o arquivo messages.json: " + e.getMessage());
            }
        }
    }


    private boolean loadPluginStatus() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
            JsonObject config = new Gson().fromJson(content, JsonObject.class);
            return config.get("enabled").getAsBoolean();
        } catch (IOException e) {
            getLogger().severe("Erro ao ler o arquivo config.json: " + e.getMessage());
        }
        return false; // Desabilita o plugin em caso de erro
    }

    public String loadLanguage() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
                JsonObject config = new Gson().fromJson(content, JsonObject.class);
                return config.get("language").getAsString(); // Retorna o idioma configurado
            } catch (IOException e) {
                getLogger().severe("Erro ao ler o arquivo config.json: " + e.getMessage());
            }
        }
        return "br"; // Padrão para português, caso ocorra um erro
    }
}
