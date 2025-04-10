package me.DTR.zCKoth.utils;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProgressBarUtil {

    private static final Map<UUID, BossBar> playerBars = new HashMap<>();

    public static void showProgressBar(Player player, String title, double progress, BarColor color) {
        BossBar bar = playerBars.get(player.getUniqueId());

        if (bar == null) {
            bar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
            playerBars.put(player.getUniqueId(), bar);
            bar.addPlayer(player);
        } else {
            bar.setTitle(title);
            bar.setColor(color);
        }

        // Asegurar que el progreso esté entre 0 y 1
        progress = Math.max(0, Math.min(1, progress));
        bar.setProgress(progress);

        // Mostrar la barra
        bar.setVisible(true);
    }

    public static void removeProgressBar(Player player) {
        BossBar bar = playerBars.get(player.getUniqueId());

        if (bar != null) {
            bar.removePlayer(player);
            playerBars.remove(player.getUniqueId());
        }
    }

    public static void removeAllBars() {
        for (BossBar bar : playerBars.values()) {
            bar.removeAll();
        }
        playerBars.clear();
    }
}