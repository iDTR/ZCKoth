package me.DTR.zCKoth.utils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KothAxe {

    private static final Map<UUID, Location> firstSelections = new HashMap<>();
    private static final Map<UUID, Location> secondSelections = new HashMap<>();

    public static ItemStack createSelectionAxe() {
        ItemStack axe = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = axe.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "KOTH Selection Axe");
        meta.setLore(Arrays.asList(
                ChatColor.YELLOW + "Left Click: " + ChatColor.WHITE + "Set first position",
                ChatColor.YELLOW + "Right Click: " + ChatColor.WHITE + "Set second position"
        ));

        axe.setItemMeta(meta);
        return axe;
    }

    public static boolean isSelectionAxe(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_AXE) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() &&
                meta.getDisplayName().equals(ChatColor.GOLD + "KOTH Selection Axe");
    }

    public static void setFirstPoint(UUID playerUUID, Location location) {
        firstSelections.put(playerUUID, location);
    }

    public static void setSecondPoint(UUID playerUUID, Location location) {
        secondSelections.put(playerUUID, location);
    }

    public static Location getFirstPoint(UUID playerUUID) {
        return firstSelections.get(playerUUID);
    }

    public static Location getSecondPoint(UUID playerUUID) {
        return secondSelections.get(playerUUID);
    }

    public static KothCuboid getSelectionPoints(UUID playerUUID) {
        Location loc1 = firstSelections.get(playerUUID);
        Location loc2 = secondSelections.get(playerUUID);

        if (loc1 == null || loc2 == null) return null;
        if (!loc1.getWorld().equals(loc2.getWorld())) return null;

        return new KothCuboid(loc1, loc2);
    }

    public static void clearSelection(UUID playerUUID) {
        firstSelections.remove(playerUUID);
        secondSelections.remove(playerUUID);
    }
}