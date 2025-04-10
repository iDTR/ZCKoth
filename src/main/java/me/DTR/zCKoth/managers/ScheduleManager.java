package me.DTR.zCKoth.managers;

import me.DTR.zCKoth.ZCKoth;
import me.DTR.zCKoth.models.Koth;
import me.DTR.zCKoth.models.KothSchedule;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestor para las programaciones automáticas de KOTHs
 */
public class ScheduleManager {

    private final ZCKoth plugin;
    private final Map<UUID, KothSchedule> schedules;
    private final File scheduleFile;
    private FileConfiguration scheduleConfig;
    private BukkitTask checkTask;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public ScheduleManager(ZCKoth plugin) {
        this.plugin = plugin;
        this.schedules = new HashMap<>();
        this.scheduleFile = new File(plugin.getDataFolder(), "schedules.yml");

        // Crear archivo si no existe
        if (!scheduleFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                scheduleFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Error al crear archivo schedules.yml: " + e.getMessage());
            }
        }

        this.scheduleConfig = YamlConfiguration.loadConfiguration(scheduleFile);

        // Cargar programaciones
        loadSchedules();

        // Iniciar tarea de verificación
        startCheckTask();
    }

    /**
     * Carga las programaciones desde el archivo
     */
    private void loadSchedules() {
        schedules.clear();

        ConfigurationSection schedulesSection = scheduleConfig.getConfigurationSection("schedules");
        if (schedulesSection == null) return;

        for (String key : schedulesSection.getKeys(false)) {
            ConfigurationSection scheduleSection = schedulesSection.getConfigurationSection(key);
            if (scheduleSection == null) continue;

            try {
                Map<String, Object> scheduleMap = new HashMap<>();
                for (String field : scheduleSection.getKeys(false)) {
                    scheduleMap.put(field, scheduleSection.get(field));
                }

                KothSchedule schedule = new KothSchedule(scheduleMap);
                schedules.put(schedule.getId(), schedule);
            } catch (Exception e) {
                plugin.getLogger().warning("Error al cargar programación " + key + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Cargadas " + schedules.size() + " programaciones de KOTHs");
    }

    /**
     * Guarda las programaciones en el archivo
     */
    public void saveSchedules() {
        scheduleConfig.set("schedules", null);

        int index = 0;
        for (KothSchedule schedule : schedules.values()) {
            scheduleConfig.set("schedules." + index, schedule.serialize());
            index++;
        }

        try {
            scheduleConfig.save(scheduleFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar programaciones: " + e.getMessage());
        }
    }

    /**
     * Inicia la tarea de verificación periódica
     */
    private void startCheckTask() {
        if (checkTask != null) {
            checkTask.cancel();
        }

        // Verificar cada minuto si hay programaciones que ejecutar
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkSchedules, 60L, 1200L);
    }

    /**
     * Verifica si hay programaciones que deben ejecutarse
     */
    private void checkSchedules() {
        LocalDateTime now = LocalDateTime.now();
        //plugin.getLogger().info("Verificando programaciones de KOTHs...");

        for (KothSchedule schedule : new ArrayList<>(schedules.values())) {
            if (schedule.shouldExecute(now)) {
                executeSchedule(schedule);
            }
        }
    }

    /**
     * Ejecuta una programación activando el KOTH correspondiente
     */
    private void executeSchedule(KothSchedule schedule) {
        String kothName = schedule.getKothName();
        Koth koth = plugin.getKothManager().getKoth(kothName);

        plugin.getLogger().info("Ejecutando programación para KOTH: " + kothName);

        if (koth == null) {
            plugin.getLogger().warning("El KOTH programado '" + kothName + "' no existe");
            return;
        }

        if (koth.isActive()) {
            plugin.getLogger().warning("El KOTH '" + kothName + "' ya está activo, omitiendo programación");
            return;
        }

        // Marcar como ejecutada
        schedule.markExecuted();
        saveSchedules();

        // Ejecutar en el hilo principal
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Activar el KOTH
            koth.startKoth();

            // Anunciar la activación programada
            Bukkit.broadcastMessage(plugin.getMessageManager().colorize(
                    plugin.getMessageManager().getMessage("schedule.koth-activated")
                            .replace("%koth%", kothName)));
        });
    }

    /**
     * Añade una nueva programación
     */
    public KothSchedule addSchedule(String kothName, String timeStr, List<String> daysStr)
            throws IllegalArgumentException {

        // Verificar que el KOTH existe
        Koth koth = plugin.getKothManager().getKoth(kothName);
        if (koth == null) {
            throw new IllegalArgumentException("El KOTH '" + kothName + "' no existe");
        }

        // Parsear hora
        LocalTime time;
        try {
            time = LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de hora inválido, use HH:MM");
        }

        // Parsear días
        List<DayOfWeek> days = new ArrayList<>();
        for (String dayStr : daysStr) {
            try {
                int dayNumber = Integer.parseInt(dayStr);
                if (dayNumber < 1 || dayNumber > 7) {
                    throw new IllegalArgumentException("Día inválido: " + dayNumber + ". Debe estar entre 1 (Lunes) y 7 (Domingo)");
                }
                days.add(DayOfWeek.of(dayNumber));
            } catch (NumberFormatException e) {
                // Intentar parsear como texto
                try {
                    DayOfWeek day = DayOfWeek.valueOf(dayStr.toUpperCase());
                    days.add(day);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Día inválido: " + dayStr);
                }
            }
        }

        if (days.isEmpty()) {
            throw new IllegalArgumentException("Debe especificar al menos un día");
        }

        // Crear y guardar la programación
        KothSchedule schedule = new KothSchedule(kothName, time, days);
        schedules.put(schedule.getId(), schedule);
        saveSchedules();

        return schedule;
    }

    /**
     * Elimina una programación
     */
    public boolean removeSchedule(UUID scheduleId) {
        KothSchedule removed = schedules.remove(scheduleId);
        if (removed != null) {
            saveSchedules();
            return true;
        }
        return false;
    }

    /**
     * Activa o desactiva una programación
     */
    public boolean toggleSchedule(UUID scheduleId) {
        KothSchedule schedule = schedules.get(scheduleId);
        if (schedule != null) {
            schedule.setEnabled(!schedule.isEnabled());
            saveSchedules();
            return true;
        }
        return false;
    }

    /**
     * Obtiene todas las programaciones
     */
    public List<KothSchedule> getAllSchedules() {
        return new ArrayList<>(schedules.values());
    }

    /**
     * Obtiene una programación por su ID
     */
    public KothSchedule getSchedule(UUID scheduleId) {
        return schedules.get(scheduleId);
    }

    /**
     * Detiene las tareas del gestor
     */
    public void shutdown() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }
}