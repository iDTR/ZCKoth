package me.DTR.zCKoth.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.DTR.zCKoth.ZCKoth;
import org.bukkit.Location;
import org.bukkit.World;

public class WorldGuardHook {

    private final ZCKoth plugin;

    public WorldGuardHook(ZCKoth plugin) {
        this.plugin = plugin;
    }

    public boolean isInRegion(Location location, String regionId) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);

        for (ProtectedRegion region : query.getApplicableRegions(loc).getRegions()) {
            if (region.getId().equalsIgnoreCase(regionId)) {
                return true;
            }
        }

        return false;
    }


    public void setRegionFlag(String regionId, String flagName, String value) {
        try {
            // Get all worlds since we don't know which world the region is in
            for (World world : plugin.getServer().getWorlds()) {
                RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

                ProtectedRegion region = container.get(BukkitAdapter.adapt(world)).getRegion(regionId);
                if (region == null) continue;

                // Try to find the flag
                if (flagName.equalsIgnoreCase("entry")) {
                    if (value.equalsIgnoreCase("allow")) {
                        region.setFlag(Flags.ENTRY, StateFlag.State.ALLOW);
                    } else if (value.equalsIgnoreCase("deny")) {
                        region.setFlag(Flags.ENTRY, StateFlag.State.DENY);
                    }
                    return;
                }

                // For other flags
                for (Flag<?> flag : WorldGuard.getInstance().getFlagRegistry().getAll()) {
                    if (flag.getName().equalsIgnoreCase(flagName)) {
                        if (flag instanceof StateFlag) {
                            if (value.equalsIgnoreCase("allow")) {
                                region.setFlag((StateFlag) flag, StateFlag.State.ALLOW);
                            } else if (value.equalsIgnoreCase("deny")) {
                                region.setFlag((StateFlag) flag, StateFlag.State.DENY);
                            }
                        }
                        return;
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error setting WorldGuard flag: " + e.getMessage());
        }
    }
}