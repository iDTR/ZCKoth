package me.DTR.zCKoth.managers;

import me.DTR.zCKoth.ZCKoth;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private final ZCKoth plugin;
    private final File configFile;
    private FileConfiguration config;

    public ConfigManager(ZCKoth plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");

        // Save default config if it doesn't exist
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }

        this.config = YamlConfiguration.loadConfiguration(configFile);

        // Load default values for missing options
        loadDefaults();
    }

    private void loadDefaults() {
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream == null) {
            // Create basic config if default doesn't exist
            if (!config.contains("settings.capture-time")) {
                config.set("settings.capture-time", 300);
            }

            if (!config.contains("settings.broadcast-messages")) {
                config.set("settings.broadcast-messages", true);
            }

            if (!config.contains("blocked-commands")) {
                config.set("blocked-commands", new String[] { "spawn", "home", "tpa" });
            }

            saveConfig();
            return;
        }

        // Load default config
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));

        // Set defaults for missing options
        for (String key : defaultConfig.getKeys(true)) {
            if (!config.contains(key)) {
                config.set(key, defaultConfig.get(key));
            }
        }

        saveConfig();
    }

    public void reloadConfig() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadDefaults();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config.yml file: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }
}