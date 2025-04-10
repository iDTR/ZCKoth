package me.DTR.zCKoth.api;

import me.DTR.zCKoth.ZCKoth;
import me.DTR.zCKoth.models.Koth;
import me.DTR.zCKoth.models.KothConquest;
import me.DTR.zCKoth.models.KothSolo;
import me.DTR.zCKoth.models.KothType;
import me.DTR.zCKoth.utils.KothCuboid;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * API class for interacting with the ZCKoth plugin
 */
public class KothAPI {

    private static ZCKoth plugin;

    /**
     * Initialize the API with the plugin instance
     *
     * @param plugin The ZCKoth plugin instance
     */
    public static void init(ZCKoth plugin) {
        KothAPI.plugin = plugin;
    }

    /**
     * Get a KOTH by name
     *
     * @param name The name of the KOTH
     * @return The KOTH, or null if not found
     */
    public static Koth getKoth(String name) {
        return plugin.getKothManager().getKoth(name);
    }

    /**
     * Get all KOTHs
     *
     * @return A list of all KOTHs
     */
    public static List<Koth> getAllKoths() {
        return plugin.getKothManager().getAllKoths();
    }

    /**
     * Create a new solo KOTH
     *
     * @param name The name of the KOTH
     * @param clanPoints The clan points to award
     * @param region The region of the KOTH
     * @return The created KOTH
     */
    public static KothSolo createSoloKoth(String name, int clanPoints, KothCuboid region) {
        KothSolo koth = new KothSolo(name, clanPoints, region);
        plugin.getKothManager().addKoth(koth);
        return koth;
    }

    /**
     * Create a new conquest KOTH
     *
     * @param name The name of the KOTH
     * @param clanPoints The clan points to award
     * @param region The region of the KOTH
     * @param nextKoths The names of the next KOTHs in the conquest
     * @param worldGuardRegion The WorldGuard region to disable entry for
     * @return The created KOTH
     */
    public static KothConquest createConquestKoth(String name, int clanPoints, KothCuboid region,
                                                  List<String> nextKoths, String worldGuardRegion) {
        KothConquest koth = new KothConquest(name, clanPoints, region, nextKoths, worldGuardRegion);
        plugin.getKothManager().addKoth(koth);
        return koth;
    }

    /**
     * Remove a KOTH
     *
     * @param name The name of the KOTH
     * @return true if the KOTH was removed, false if it wasn't found
     */
    public static boolean removeKoth(String name) {
        if (plugin.getKothManager().getKoth(name) == null) {
            return false;
        }

        plugin.getKothManager().removeKoth(name);
        return true;
    }

    /**
     * Start a KOTH
     *
     * @param name The name of the KOTH
     * @return true if the KOTH was started, false if it wasn't found or was already active
     */
    public static boolean startKoth(String name) {
        Koth koth = plugin.getKothManager().getKoth(name);
        if (koth == null || koth.isActive()) {
            return false;
        }

        koth.startKoth();
        return true;
    }

    /**
     * End a KOTH
     *
     * @param name The name of the KOTH
     * @return true if the KOTH was ended, false if it wasn't found or wasn't active
     */
    public static boolean endKoth(String name) {
        Koth koth = plugin.getKothManager().getKoth(name);
        if (koth == null || !koth.isActive()) {
            return false;
        }

        koth.endKoth();
        return true;
    }

    /**
     * Check if a player is in a KOTH
     *
     * @param player The player
     * @return true if the player is in an active KOTH, false otherwise
     */
    public static boolean isPlayerInKoth(Player player) {
        return plugin.getKothManager().isPlayerInAnyKoth(player);
    }

    /**
     * Get the KOTH a player is in
     *
     * @param player The player
     * @return The KOTH the player is in, or null if not in any KOTH
     */
    public static Koth getKothByPlayer(Player player) {
        return plugin.getKothManager().getKothByPlayer(player);
    }
}