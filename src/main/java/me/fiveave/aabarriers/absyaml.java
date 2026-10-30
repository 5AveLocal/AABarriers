package me.fiveave.aabarriers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import static me.fiveave.aabarriers.main.errorLog;

class absyaml {
    protected final main plugin;
    final FileConfiguration oldconfig;
    private final String fileName;
    FileConfiguration dataconfig;
    private File file;

    absyaml(main plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        file = new File(plugin.getDataFolder(), fileName);
        saveDefaultConfig();
        // Load the current config first
        dataconfig = YamlConfiguration.loadConfiguration(file);
        // Then load a copy for comparison
        oldconfig = YamlConfiguration.loadConfiguration(file);
        reloadConfig();
    }

    void reloadConfig() {
        InputStream stream = plugin.getResource(fileName);
        if (stream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
            // Check if config requires updates
            boolean updatereq = false;
            for (String key : defaultConfig.getKeys(true)) {
                if (!dataconfig.contains(key)) {
                    updatereq = true;
                    break;
                }
            }

            if (updatereq) {
                // Backup current values
                Set<String> currentKeys = oldconfig.getKeys(true);
                // Reload default config (including all new keys)
                plugin.saveResource(fileName, true);
                dataconfig = YamlConfiguration.loadConfiguration(file);
                // Restore old values
                for (String key : currentKeys) {
                    Object oldobj = oldconfig.get(key);
                    if (oldobj != null) {
                        dataconfig.set(key, oldobj);
                    }
                }
                // Add new default values
                for (String key : defaultConfig.getKeys(true)) {
                    Object newobj = defaultConfig.get(key);
                    if (!currentKeys.contains(key) && newobj != null) {
                        dataconfig.set(key, newobj);
                    }
                }
                try {
                    dataconfig.save(file);
                    Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + fileName + " has been updated with new config options");
                } catch (IOException e) {
                    errorLog(e);
                }
            }
        }
    }

    void save() {
        if (dataconfig == null || file == null) {
            return;
        }
        try {
            dataconfig.save(file);
        } catch (IOException e) {
            errorLog(e);
        }
    }

    void saveDefaultConfig() {
        if (file == null) {
            file = new File(plugin.getDataFolder(), fileName);
        }
        if (!file.exists()) {
            // Create data folder first if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Check if the file actually exists inside the JAR before trying to extract it
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                try {
                    // Create an empty file on disk safely if no default asset exists in resources
                    file.createNewFile();
                } catch (IOException e) {
                    errorLog(e);
                }
            }
        }
    }
}
