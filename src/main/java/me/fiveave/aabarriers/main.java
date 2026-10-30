package me.fiveave.aabarriers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public final class main extends JavaPlugin {
    static final String AAB_HEAD = "[" + ChatColor.RED + "AA" + ChatColor.WHITE + "Barriers" + ChatColor.WHITE + "] ";
    public static main plugin;
    static absyaml gateStorage;
    private barrier barrierManager;

    static void errorLog(Exception e) {
        Bukkit.getLogger().log(Level.SEVERE, ChatColor.stripColor(AAB_HEAD) + "An error occurred!", e);
    }

    @Override
    public void onEnable() {
        plugin = this;

        // Initialize config file loader mechanics using absyaml structure
        gateStorage = new absyaml(this, "gates.yml");

        this.barrierManager = new barrier();

        Objects.requireNonNull(this.getCommand("aab")).setExecutor(new cmds(barrierManager));
        Objects.requireNonNull(this.getCommand("aab")).setTabCompleter(new cmds(barrierManager));
    }

    @Override
    public void onDisable() {
        if (barrierManager != null) {
            // 1. Loop through all tracked data logs saved in gates.yml and physically restore them to the world
            if (gateStorage != null && gateStorage.dataconfig != null && gateStorage.dataconfig.contains("gates")) {
                var gateKeys = Objects.requireNonNull(gateStorage.dataconfig.getConfigurationSection("gates")).getKeys(false);
                for (String gateKey : gateKeys) {
                    try {
                        // Parse string keys (startX_startY_startZ_endX_endY_endZ) back into structural locations
                        String[] split = gateKey.split("_");
                        if (split.length >= 6 && Bukkit.getWorlds().getFirst() != null) {
                            org.bukkit.World defaultWorld = Bukkit.getWorlds().getFirst();

                            Location startLoc = new Location(defaultWorld, Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
                            Location endLoc = new Location(defaultWorld, Integer.parseInt(split[3]), Integer.parseInt(split[4]), Integer.parseInt(split[5]));

                            // Revert the block grid changes before display entities are purged from memory
                            barrierManager.forceResetGate(startLoc, endLoc);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            // 2. Clear out any remaining hanging client display entities and cancel active timer schedulers safely
            barrierManager.clearAllActiveDisplays();
        }
    }
}
