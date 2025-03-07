package org.exampleorg.example.pow4.Pow4.blockdata;

import org.bukkit.plugin.java.JavaPlugin;

public final class Blockdata extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("LockChestPlugin habilitado!");
        // Registre o evento
        ChestLockListener chestLockListener = new ChestLockListener(this);
        getServer().getPluginManager().registerEvents(chestLockListener, this);
        this.getCommand("lock").setExecutor(new LockChestCommand(chestLockListener));
        this.getCommand("unlock").setExecutor(new LockChestCommand(chestLockListener));
    }

    @Override
    public void onDisable() {
        getLogger().info("LockChestPlugin desabilitado!");
    }
}
