package me.DTR.zCKoth.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerMessageResetTask extends BukkitRunnable {

    private final MessageHandler messageHandler;

    public PlayerMessageResetTask(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        messageHandler.resetAllNotifiedPlayers();
    }

    public static void start(JavaPlugin plugin, MessageHandler messageHandler, long interval) {
        new PlayerMessageResetTask(messageHandler).runTaskTimer(plugin, 0L, interval);
    }
}
