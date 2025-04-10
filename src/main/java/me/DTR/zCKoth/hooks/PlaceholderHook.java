package me.DTR.zCKoth.hooks;

import me.DTR.zCKoth.ZCKoth;
import me.DTR.zCKoth.models.Koth;
import me.DTR.zCKoth.models.KothConquest;
import me.DTR.zCKoth.models.KothSchedule;
import me.DTR.zCKoth.models.KothType;
import me.DTR.zCKoth.utils.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class PlaceholderHook extends PlaceholderExpansion {

    private final ZCKoth plugin;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public PlaceholderHook(ZCKoth plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "zckoth";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier == null) {
            return "";
        }

        // ===== KOTHs Básicos =====

        // %zckoth_active% - Devuelve el nombre del KOTH activo o "Ninguno"
        if (identifier.equals("active")) {
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.getType() != KothType.CONQUEST) {
                    return koth.getName();
                }
            }
            return "Ninguno";
        }

        // %zckoth_active_count% - Devuelve el número de KOTHs activos
        if (identifier.equals("active_count")) {
            int count = 0;
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive()) {
                    count++;
                }
            }
            return String.valueOf(count);
        }

        // %zckoth_capturing_player% - Devuelve el nombre del jugador capturando o "Ninguno"
        if (identifier.equals("capturing_player")) {
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.getCapturingPlayer() != null) {
                    Player capturingPlayer = Bukkit.getPlayer(koth.getCapturingPlayer());
                    if (capturingPlayer != null) {
                        return capturingPlayer.getName();
                    }
                }
            }
            return "Ninguno";
        }

        // %zckoth_capturing_clan% - Devuelve el nombre del clan capturando o "Ninguno"
        if (identifier.equals("capturing_clan")) {
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.getCapturingClan() != null) {
                    return koth.getCapturingClan();
                }
            }
            return "Ninguno";
        }

        // %zckoth_progress% - Devuelve el progreso de captura como porcentaje
        if (identifier.equals("progress")) {
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.getCapturingPlayer() != null) {
                    int percentage = (koth.getCaptureProgress() * 100) / koth.getCaptureTime();
                    return percentage + "%";
                }
            }
            return "0%";
        }

        // %zckoth_time_left% - Devuelve el tiempo restante para capturar el KOTH
        if (identifier.equals("time_left")) {
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.getCapturingPlayer() != null) {
                    int timeLeft = koth.getCaptureTime() - koth.getCaptureProgress();
                    return TimeUtil.formatTime(timeLeft);
                }
            }
            return "0s";
        }

        // %zckoth_next_koth% - Devuelve el nombre del próximo KOTH programado o "Ninguno"
        if (identifier.equals("next_koth")) {
            KothSchedule nextSchedule = getNextScheduledKoth();
            if (nextSchedule != null) {
                return nextSchedule.getKothName();
            }
            return "Ninguno";
        }

        // %zckoth_next_koth_time% - Devuelve la hora del próximo KOTH programado
        if (identifier.equals("next_koth_time")) {
            KothSchedule nextSchedule = getNextScheduledKoth();
            if (nextSchedule != null) {
                return nextSchedule.getTime().format(TIME_FORMATTER);
            }
            return "??:??";
        }

        // %zckoth_next_koth_day% - Devuelve el día del próximo KOTH programado
        if (identifier.equals("next_koth_day")) {
            KothSchedule nextSchedule = getNextScheduledKoth();
            if (nextSchedule != null) {
                return nextSchedule.getDaysString();
            }
            return "??";
        }

        // %zckoth_next_koth_full% - Devuelve la información completa del próximo KOTH
        if (identifier.equals("next_koth_full")) {
            KothSchedule nextSchedule = getNextScheduledKoth();
            if (nextSchedule != null) {
                return nextSchedule.getKothName() + " - " +
                        nextSchedule.getTime().format(TIME_FORMATTER) + " - " +
                        nextSchedule.getDaysString();
            }
            return "No hay KOTHs programados";
        }

        // ===== Conquest Específicos =====

        // %zckoth_conquest_active% - Devuelve el nombre del Conquest activo o "Ninguno"
        if (identifier.equals("conquest_active")) {
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.getType() == KothType.CONQUEST) {
                    return koth.getName();
                }
            }
            return "Ninguno";
        }

        // %zckoth_conquest_stage% - Devuelve la etapa actual del Conquest activo
        if (identifier.equals("conquest_stage")) {
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.getType() == KothType.CONQUEST) {
                    KothConquest conquest = (KothConquest) koth;
                    return String.valueOf(conquest.getStage() + 1); // +1 para que sea amigable (1-indexed)
                }
            }
            return "0";
        }

        // %zckoth_conquest_total_stages% - Devuelve el número total de etapas del Conquest activo
        if (identifier.equals("conquest_total_stages")) {
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.getType() == KothType.CONQUEST) {
                    KothConquest conquest = (KothConquest) koth;
                    return String.valueOf(conquest.getNextKoths().size());
                }
            }
            return "0";
        }

        // %zckoth_conquest_progress% - Devuelve el progreso general del Conquest como porcentaje
        if (identifier.equals("conquest_progress")) {
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.getType() == KothType.CONQUEST) {
                    KothConquest conquest = (KothConquest) koth;
                    int totalStages = conquest.getNextKoths().size();
                    if (totalStages > 0) {
                        int currentStage = conquest.getStage();
                        int percentage = (currentStage * 100) / totalStages;
                        return percentage + "%";
                    }
                }
            }
            return "0%";
        }

        // %zckoth_conquest_current_koth% - Devuelve el nombre del KOTH actual en el Conquest
        if (identifier.equals("conquest_current_koth")) {
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.getType() == KothType.CONQUEST) {
                    KothConquest conquest = (KothConquest) koth;
                    int stage = conquest.getStage();
                    List<String> stages = conquest.getNextKoths();
                    if (stage >= 0 && stage < stages.size()) {
                        return stages.get(stage);
                    }
                }
            }
            return "Ninguno";
        }

        // %zckoth_conquest_bossbar% - Devuelve una barra de progreso textual del Conquest
        if (identifier.equals("conquest_bossbar")) {
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.getType() == KothType.CONQUEST) {
                    KothConquest conquest = (KothConquest) koth;
                    int totalStages = conquest.getNextKoths().size();
                    int currentStage = conquest.getStage();

                    StringBuilder bar = new StringBuilder();
                    for (int i = 0; i < totalStages; i++) {
                        if (i < currentStage) {
                            bar.append(ChatColor.GREEN).append("■"); // Completada
                        } else if (i == currentStage) {
                            bar.append(ChatColor.GOLD).append("■"); // Actual
                        } else {
                            bar.append(ChatColor.RED).append("■"); // Pendiente
                        }
                    }
                    return bar.toString();
                }
            }
            return "";
        }

        // %zckoth_conquest_progress_bar_N% - Devuelve una barra de progreso personalizada de N caracteres
        if (identifier.startsWith("conquest_progress_bar_")) {
            try {
                int length = Integer.parseInt(identifier.substring("conquest_progress_bar_".length()));
                for (Koth koth : plugin.getKothManager().getAllKoths()) {
                    if (koth.isActive() && koth.getType() == KothType.CONQUEST) {
                        KothConquest conquest = (KothConquest) koth;
                        int totalStages = conquest.getNextKoths().size();
                        int currentStage = conquest.getStage();

                        if (totalStages > 0) {
                            // Calcular cuántos caracteres completados
                            int filledChars = (int) Math.ceil((double) currentStage * length / totalStages);

                            StringBuilder bar = new StringBuilder();
                            for (int i = 0; i < length; i++) {
                                if (i < filledChars) {
                                    bar.append(ChatColor.GREEN).append("█"); // Completado
                                } else {
                                    bar.append(ChatColor.RED).append("█"); // Pendiente
                                }
                            }
                            return bar.toString();
                        }
                    }
                }
                return ChatColor.GRAY + "■".repeat(length);
            } catch (NumberFormatException e) {
                return "Error en formato";
            }
        }

        // ===== KOTHs específicos =====

        // %zckoth_specific_active_<kothname>% - Devuelve si un KOTH específico está activo
        if (identifier.startsWith("specific_active_")) {
            String kothName = identifier.substring("specific_active_".length());
            Koth koth = plugin.getKothManager().getKoth(kothName);
            if (koth != null) {
                return koth.isActive() ? "Sí" : "No";
            }
            return "No encontrado";
        }

        // %zckoth_specific_progress_<kothname>% - Devuelve el progreso de un KOTH específico
        if (identifier.startsWith("specific_progress_")) {
            String kothName = identifier.substring("specific_progress_".length());
            Koth koth = plugin.getKothManager().getKoth(kothName);
            if (koth != null && koth.isActive() && koth.getCapturingPlayer() != null) {
                int percentage = (koth.getCaptureProgress() * 100) / koth.getCaptureTime();
                return percentage + "%";
            }
            return "0%";
        }

        // %zckoth_specific_time_left_<kothname>% - Devuelve el tiempo restante de un KOTH específico
        if (identifier.startsWith("specific_time_left_")) {
            String kothName = identifier.substring("specific_time_left_".length());
            Koth koth = plugin.getKothManager().getKoth(kothName);
            if (koth != null && koth.isActive() && koth.getCapturingPlayer() != null) {
                int timeLeft = koth.getCaptureTime() - koth.getCaptureProgress();
                return TimeUtil.formatTime(timeLeft);
            }
            return "0s";
        }

        // %zckoth_specific_capturing_<kothname>% - Devuelve quién está capturando un KOTH específico
        if (identifier.startsWith("specific_capturing_")) {
            String kothName = identifier.substring("specific_capturing_".length());
            Koth koth = plugin.getKothManager().getKoth(kothName);
            if (koth != null && koth.isActive() && koth.getCapturingPlayer() != null) {
                Player capturingPlayer = Bukkit.getPlayer(koth.getCapturingPlayer());
                if (capturingPlayer != null) {
                    return capturingPlayer.getName();
                }
            }
            return "Nadie";
        }

        // %zckoth_player_in_koth% - Devuelve si el jugador está en algún KOTH
        if (identifier.equals("player_in_koth")) {
            if (player == null) return "No";

            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.isPlayerInKoth(player)) {
                    return "Sí";
                }
            }
            return "No";
        }

        // %zckoth_player_koth% - Devuelve el nombre del KOTH en el que está el jugador
        if (identifier.equals("player_koth")) {
            if (player == null) return "Ninguno";

            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.isActive() && koth.isPlayerInKoth(player)) {
                    return koth.getName();
                }
            }
            return "Ninguno";
        }

        // %zckoth_total_koths% - Devuelve el número total de KOTHs configurados
        if (identifier.equals("total_koths")) {
            return String.valueOf(plugin.getKothManager().getAllKoths().size());
        }

        // %zckoth_total_solo_koths% - Devuelve el número total de KOTHs solitarios configurados
        if (identifier.equals("total_solo_koths")) {
            int count = 0;
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.getType() == KothType.SOLO) {
                    count++;
                }
            }
            return String.valueOf(count);
        }

        // %zckoth_total_conquest_koths% - Devuelve el número total de Conquests configurados
        if (identifier.equals("total_conquest_koths")) {
            int count = 0;
            for (Koth koth : plugin.getKothManager().getAllKoths()) {
                if (koth.getType() == KothType.CONQUEST) {
                    count++;
                }
            }
            return String.valueOf(count);
        }

        // %zckoth_list_active% - Devuelve una lista de KOTHs activos
        if (identifier.equals("list_active")) {
            List<String> activeKoths = plugin.getKothManager().getAllKoths().stream()
                    .filter(Koth::isActive)
                    .map(Koth::getName)
                    .collect(Collectors.toList());

            if (activeKoths.isEmpty()) {
                return "Ninguno";
            }

            return String.join(", ", activeKoths);
        }

        return null;
    }

    /**
     * Obtiene la siguiente programación de KOTH
     * @return La próxima programación o null si no hay ninguna
     */
    private KothSchedule getNextScheduledKoth() {
        LocalDateTime now = LocalDateTime.now();
        KothSchedule nextSchedule = null;
        LocalDateTime nextTime = null;

        for (KothSchedule schedule : plugin.getScheduleManager().getAllSchedules()) {
            if (!schedule.isEnabled()) continue;

            // Si aún no se ha encontrado uno, este es el siguiente
            if (nextSchedule == null) {
                nextSchedule = schedule;
                continue;
            }

            // Comparar horarios para encontrar el más cercano
            // (Esta lógica es simplificada, una implementación completa consideraría
            // fechas futuras basadas en los días de ejecución)
        }

        return nextSchedule;
    }
}