package me.DTR.zCKoth.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class KothCuboid implements ConfigurationSerializable {

    private final World world;
    private final int x1, y1, z1;
    private final int x2, y2, z2;

    public KothCuboid(Location loc1, Location loc2) {
        if (!loc1.getWorld().equals(loc2.getWorld())) {
            throw new IllegalArgumentException("Locations must be in the same world");
        }

        this.world = loc1.getWorld();

        this.x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        this.y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        this.z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());

        this.x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        this.y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        this.z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
    }

    /**
     * Constructor para deserialización
     */
    public KothCuboid(Map<String, Object> map) {
        // Obtener el nombre del mundo y cargarlo
        String worldName = (String) map.get("world");
        this.world = Bukkit.getWorld(worldName);

        if (this.world == null) {
            throw new IllegalArgumentException("Could not load world: " + worldName);
        }

        // Obtener las coordenadas
        this.x1 = getIntFromMap(map, "x1");
        this.y1 = getIntFromMap(map, "y1");
        this.z1 = getIntFromMap(map, "z1");
        this.x2 = getIntFromMap(map, "x2");
        this.y2 = getIntFromMap(map, "y2");
        this.z2 = getIntFromMap(map, "z2");
    }

    /**
     * Método auxiliar para obtener un entero de un mapa, con validación
     */
    private int getIntFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid value for " + key + ": " + value);
            }
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        } else {
            throw new IllegalArgumentException("Missing or invalid value for " + key);
        }
    }

    /**
     * Serializar la región a un mapa
     */
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("world", world.getName());
        map.put("x1", x1);
        map.put("y1", y1);
        map.put("z1", z1);
        map.put("x2", x2);
        map.put("y2", y2);
        map.put("z2", z2);
        return map;
    }

    /**
     * Constructor estático para deserialización (requerido por ConfigurationSerializable)
     */
    public static KothCuboid deserialize(Map<String, Object> map) {
        return new KothCuboid(map);
    }

    /**
     * Constructor estático para deserialización con valores explícitos
     */
    public static KothCuboid valueOf(Map<String, Object> map) {
        return deserialize(map);
    }

    /**
     * Verificar si una ubicación está dentro de esta región
     */
    public boolean isIn(Location loc) {
        if (loc == null || loc.getWorld() == null || !loc.getWorld().equals(world)) {
            return false;
        }

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }

    /**
     * Obtener el centro de la región
     */
    public Location getCenter() {
        return new Location(
                world,
                x1 + (x2 - x1) / 2.0,
                y1 + (y2 - y1) / 2.0,
                z1 + (z2 - z1) / 2.0
        );
    }

    /**
     * Obtener una representación en cadena
     */
    @Override
    public String toString() {
        return "KothCuboid{" +
                "world=" + world.getName() +
                ", x1=" + x1 +
                ", y1=" + y1 +
                ", z1=" + z1 +
                ", x2=" + x2 +
                ", y2=" + y2 +
                ", z2=" + z2 +
                '}';
    }

    // Getters
    public World getWorld() {
        return world;
    }

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getZ1() {
        return z1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public int getZ2() {
        return z2;
    }
}