package me.DTR.zCKoth.managers;

import me.DTR.zCKoth.ZCKoth;
import me.DTR.zCKoth.models.Koth;
import me.DTR.zCKoth.models.KothConquest;
import me.DTR.zCKoth.models.KothSolo;
import me.DTR.zCKoth.models.KothType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KothManager {

    private final ZCKoth plugin;
    private final Map<String, Koth> koths;
    private final File kothFile;
    private FileConfiguration kothConfig;
    private BukkitTask tickTask;
    private BukkitTask autoSaveTask;

    public KothManager(ZCKoth plugin) {
        this.plugin = plugin;
        this.koths = new HashMap<>();
        this.kothFile = new File(plugin.getDataFolder(), "koths.yml");

        // Create file if it doesn't exist
        if (!kothFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                kothFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create koths.yml file: " + e.getMessage());
            }
        }

        this.kothConfig = YamlConfiguration.loadConfiguration(kothFile);

        // Start tick task
        startTickTask();

        // Start auto-save task if enabled
        startAutoSaveTask();
    }

    private void startTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
        }

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickKoths, 20L, 20L);
    }

    private void startAutoSaveTask() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        boolean autoSave = plugin.getConfigManager().getConfig().getBoolean("settings.auto-save", true);
        if (autoSave) {
            int interval = plugin.getConfigManager().getConfig().getInt("settings.auto-save-interval", 10) * 60 * 20; // Convertir minutos a ticks
            autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveKoths, interval, interval);
            plugin.getLogger().info("Auto-save enabled. KOTHs will be saved every " +
                    (interval / (60)) + " minutes.");
        }
    }

    private void tickKoths() {
        for (Koth koth : koths.values()) {
            if (koth.isActive()) {
                koth.tick();

                // Si es un Conquest, verificar la consistencia de las etapas
                if (koth instanceof KothConquest) {
                    ((KothConquest) koth).enforceActiveStage();
                }
            }
        }
    }

    public void loadKoths() {
        koths.clear();

        // Recargar el archivo en caso de que haya sido modificado externamente
        this.kothConfig = YamlConfiguration.loadConfiguration(kothFile);

        ConfigurationSection kothSection = kothConfig.getConfigurationSection("koths");
        if (kothSection == null) return;

        for (String kothName : kothSection.getKeys(false)) {
            ConfigurationSection section = kothSection.getConfigurationSection(kothName);
            if (section == null) continue;

            String typeStr = section.getString("type", "SOLO");
            KothType type;

            try {
                type = KothType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid KOTH type for " + kothName + ": " + typeStr + ". Defaulting to SOLO.");
                type = KothType.SOLO;
            }

            Koth koth;
            try {
                if (type == KothType.SOLO) {
                    koth = new KothSolo(getKothMapFromSection(section));
                } else {
                    koth = new KothConquest(getKothMapFromSection(section));
                }

                koths.put(kothName, koth);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load KOTH " + kothName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + koths.size() + " KOTHs from koths.yml");
    }

    private Map<String, Object> getKothMapFromSection(ConfigurationSection section) {
        Map<String, Object> map = new HashMap<>();

        for (String key : section.getKeys(false)) {
            map.put(key, section.get(key));
        }

        return map;
    }

    public void saveKoths() {
        // Ejecutar en el hilo principal si se llama desde otro hilo
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::saveKothsSync);
            return;
        }

        saveKothsSync();
    }

    private void saveKothsSync() {
        try {
            // Crear una copia de seguridad del archivo actual antes de guardar
            if (kothFile.exists()) {
                File backupFile = new File(plugin.getDataFolder(), "koths_backup.yml");
                if (backupFile.exists()) {
                    backupFile.delete();
                }

                // Copiar el archivo actual a la copia de seguridad
                YamlConfiguration currentConfig = YamlConfiguration.loadConfiguration(kothFile);
                currentConfig.save(backupFile);
            }

            // Limpiar la sección de KOTHs en la configuración
            kothConfig.set("koths", null);

            // Guardar todos los KOTHs
            for (Map.Entry<String, Koth> entry : koths.entrySet()) {
                String kothName = entry.getKey();
                Koth koth = entry.getValue();

                kothConfig.set("koths." + kothName, koth.serialize());
            }

            // Guardar la configuración en el archivo
            kothConfig.save(kothFile);

            plugin.getLogger().info("Saved " + koths.size() + " KOTHs to koths.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save koths.yml file: " + e.getMessage());
            e.printStackTrace();

            // Si falló el guardado, intentar restaurar desde la copia de seguridad
            File backupFile = new File(plugin.getDataFolder(), "koths_backup.yml");
            if (backupFile.exists()) {
                try {
                    YamlConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);
                    backupConfig.save(kothFile);
                    plugin.getLogger().info("Restored koths.yml from backup after save failure.");
                } catch (IOException ex) {
                    plugin.getLogger().severe("Failed to restore from backup: " + ex.getMessage());
                }
            }
        }
    }

    public void addKoth(Koth koth) {
        koths.put(koth.getName(), koth);
        saveKoths();
    }

    public void removeKoth(String name) {
        Koth koth = koths.remove(name);
        if (koth != null && koth.isActive()) {
            koth.endKoth();
        }
        saveKoths();
    }

    public Koth getKoth(String name) {
        return koths.get(name);
    }

    public List<Koth> getAllKoths() {
        return new ArrayList<>(koths.values());
    }

    public boolean isPlayerInAnyKoth(Player player) {
        for (Koth koth : koths.values()) {
            if (koth.isActive() && koth.isPlayerInKoth(player)) {
                return true;
            }
        }

        return false;
    }

    public Koth getKothByPlayer(Player player) {
        for (Koth koth : koths.values()) {
            if (koth.isActive() && koth.isPlayerInKoth(player)) {
                return koth;
            }
        }

        return null;
    }

    // Método para cancelar las tareas programadas
    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }

        // Guardar todos los KOTHs antes de cerrar
        saveKoths();
    }
}