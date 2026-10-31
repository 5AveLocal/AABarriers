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
    static absyaml warnPresets;

    private barrier barrierManager;
    private warningsignal warningManager;

    static void errorLog(Exception e) {
        Bukkit.getLogger().log(Level.SEVERE, ChatColor.stripColor(AAB_HEAD) + "An error occurred!", e);
    }

    @Override
    public void onEnable() {
        plugin = this;

        // Load configs
        gateStorage = new absyaml(this, "gates.yml");
        warnPresets = new absyaml(this, "warn_presets.yml");

        this.barrierManager = new barrier();
        this.warningManager = new warningsignal();

        cmds commandExecutor = new cmds(barrierManager, warningManager);
        Objects.requireNonNull(this.getCommand("aab")).setExecutor(commandExecutor);
        Objects.requireNonNull(this.getCommand("aab")).setTabCompleter(commandExecutor);
    }

    /**
     * Version 0.1.2: Hot-reloads all static YAML structures into active memory safely.
     */
    public void reloadPluginConfigs() {
        if (gateStorage != null) {
            gateStorage.reloadConfig();
        }
        if (warnPresets != null) {
            warnPresets.reloadConfig();
        }
    }

    @Override
    public void onDisable() {
        if (warningManager != null) {
            warningManager.stopAllSignals();
        }

        if (barrierManager != null) {
            if (gateStorage != null && gateStorage.dataconfig != null && gateStorage.dataconfig.contains("gates")) {
                var gateKeys = Objects.requireNonNull(gateStorage.dataconfig.getConfigurationSection("gates")).getKeys(false);
                for (String gateKey : gateKeys) {
                    try {
                        String[] split = gateKey.split("_");
                        if (split.length >= 6 && Bukkit.getWorlds().getFirst() != null) {
                            org.bukkit.World defaultWorld = Bukkit.getWorlds().getFirst();

                            Location startLoc = new Location(defaultWorld, Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
                            Location endLoc = new Location(defaultWorld, Integer.parseInt(split[3]), Integer.parseInt(split[4]), Integer.parseInt(split[5]));

                            barrierManager.forceResetGate(startLoc, endLoc);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            barrierManager.clearAllActiveDisplays();
        }
    }
}
