package me.DTR.zCKoth.managers;

import me.DTR.zCKoth.ZCKoth;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final ZCKoth plugin;
    private final File messagesFile;
    private FileConfiguration messagesConfig;
    private final Map<String, String> messages;

    public MessageManager(ZCKoth plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = new HashMap<>();

        // Load or create messages file
        if (!messagesFile.exists()) {
            saveDefaultMessages();
        }

        loadMessages();
    }

    private void saveDefaultMessages() {
        try {
            plugin.getDataFolder().mkdirs();
            messagesFile.createNewFile();

            // Copy default messages from resource
            InputStream in = plugin.getResource("messages.yml");
            if (in != null) {
                FileConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                defaultMessages.save(messagesFile);
            } else {
                // Create basic messages if default resource doesn't exist
                FileConfiguration config = new YamlConfiguration();

                // Add basic messages
                config.set("prefix", "&7[&6ZCKoth&7] ");
                config.set("commands.no-permission", "&cNo tienes permiso para usar este comando.");
                config.set("commands.player-only", "&cEste comando solo puede ser ejecutado por un jugador.");
                config.set("commands.reload", "&aConfiguracion recargada correctamente.");

                config.save(messagesFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create messages.yml file: " + e.getMessage());
        }
    }

    public void loadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        messages.clear();

        // Load all messages from config
        for (String key : messagesConfig.getKeys(true)) {
            if (messagesConfig.isString(key)) {
                messages.put(key, messagesConfig.getString(key));
            }
        }
    }

    public void reloadMessages() {
        loadMessages();
    }

    public String getMessage(String key) {
        String message = messages.getOrDefault(key, "&cMissing message: " + key);
        String prefix = messages.getOrDefault("prefix", "&7[&6ZCKoth&7] ");

        return colorize(prefix + message);
    }

    public String getMessageNoPrefix(String key) {
        String message = messages.getOrDefault(key, "&cMissing message: " + key);
        return colorize(message);
    }

    public String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

}