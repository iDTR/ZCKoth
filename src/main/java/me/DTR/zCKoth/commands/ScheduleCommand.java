package me.DTR.zCKoth.commands;

import me.DTR.zCKoth.ZCKoth;
import me.DTR.zCKoth.models.KothSchedule;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Comando para gestionar las programaciones de KOTHs
 */
public class ScheduleCommand implements CommandExecutor, TabCompleter {

    private final ZCKoth plugin;

    public ScheduleCommand(ZCKoth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("zckoth.schedule")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                return handleListCommand(sender);
            case "add":
                return handleAddCommand(sender, args);
            case "remove":
                return handleRemoveCommand(sender, args);
            case "toggle":
                return handleToggleCommand(sender, args);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    /**
     * Maneja el comando para listar programaciones
     */
    private boolean handleListCommand(CommandSender sender) {
        List<KothSchedule> schedules = plugin.getScheduleManager().getAllSchedules();

        if (schedules.isEmpty()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("schedule.no-schedules"));
            return true;
        }

        sender.sendMessage(plugin.getMessageManager().getMessage("schedule.list.header"));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        int index = 1;
        for (KothSchedule schedule : schedules) {
            String timeStr = schedule.getTime().format(timeFormatter);
            String statusStr = schedule.isEnabled() ? ChatColor.GREEN + "Activo" : ChatColor.RED + "Inactivo";

            sender.sendMessage(plugin.getMessageManager().getMessage("schedule.list.entry")
                    .replace("%index%", String.valueOf(index))
                    .replace("%id%", schedule.getId().toString().substring(0, 8))
                    .replace("%koth%", schedule.getKothName())
                    .replace("%time%", timeStr)
                    .replace("%days%", schedule.getDaysString())
                    .replace("%status%", statusStr));
            index++;
        }

        return true;
    }

    /**
     * Maneja el comando para añadir una programación
     */
    private boolean handleAddCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getMessageManager().getMessage("schedule.add.usage"));
            return true;
        }

        String kothName = args[1];
        String timeStr = args[2];
        List<String> days = new ArrayList<>(Arrays.asList(args).subList(3, args.length));

        try {
            // Verificar formato de hora
            try {
                LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException e) {
                sender.sendMessage(plugin.getMessageManager().getMessage("schedule.invalid-time-format"));
                return true;
            }

            // Crear programación
            KothSchedule schedule = plugin.getScheduleManager().addSchedule(kothName, timeStr, days);
            sender.sendMessage(plugin.getMessageManager().getMessage("schedule.added")
                    .replace("%koth%", kothName)
                    .replace("%time%", timeStr)
                    .replace("%days%", schedule.getDaysString()));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("schedule.add.error")
                    .replace("%error%", e.getMessage()));
        }

        return true;
    }

    /**
     * Maneja el comando para eliminar una programación
     */
    private boolean handleRemoveCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("schedule.remove.usage"));
            return true;
        }

        // Obtener ID o índice
        String idOrIndex = args[1];
        UUID scheduleId = null;

        // Intentar como UUID primero
        try {
            scheduleId = UUID.fromString(idOrIndex);
        } catch (IllegalArgumentException e) {
            // Intentar como índice
            try {
                int index = Integer.parseInt(idOrIndex) - 1;
                List<KothSchedule> schedules = plugin.getScheduleManager().getAllSchedules();

                if (index >= 0 && index < schedules.size()) {
                    scheduleId = schedules.get(index).getId();
                } else {
                    sender.sendMessage(plugin.getMessageManager().getMessage("schedule.invalid-index"));
                    return true;
                }
            } catch (NumberFormatException ex) {
                sender.sendMessage(plugin.getMessageManager().getMessage("schedule.invalid-id"));
                return true;
            }
        }

        // Eliminar la programación
        boolean removed = plugin.getScheduleManager().removeSchedule(scheduleId);
        if (removed) {
            sender.sendMessage(plugin.getMessageManager().getMessage("schedule.removed"));
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("schedule.not-found"));
        }

        return true;
    }

    /**
     * Maneja el comando para activar/desactivar una programación
     */
    private boolean handleToggleCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("schedule.toggle.usage"));
            return true;
        }

        // Obtener ID o índice
        String idOrIndex = args[1];
        UUID scheduleId = null;

        // Intentar como UUID primero
        try {
            scheduleId = UUID.fromString(idOrIndex);
        } catch (IllegalArgumentException e) {
            // Intentar como índice
            try {
                int index = Integer.parseInt(idOrIndex) - 1;
                List<KothSchedule> schedules = plugin.getScheduleManager().getAllSchedules();

                if (index >= 0 && index < schedules.size()) {
                    scheduleId = schedules.get(index).getId();
                } else {
                    sender.sendMessage(plugin.getMessageManager().getMessage("schedule.invalid-index"));
                    return true;
                }
            } catch (NumberFormatException ex) {
                sender.sendMessage(plugin.getMessageManager().getMessage("schedule.invalid-id"));
                return true;
            }
        }

        // Activar/desactivar la programación
        boolean toggled = plugin.getScheduleManager().toggleSchedule(scheduleId);
        if (toggled) {
            KothSchedule schedule = plugin.getScheduleManager().getSchedule(scheduleId);
            boolean nowEnabled = schedule.isEnabled();

            sender.sendMessage(plugin.getMessageManager().getMessage(nowEnabled ?
                            "schedule.enabled" : "schedule.disabled")
                    .replace("%koth%", schedule.getKothName()));
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("schedule.not-found"));
        }

        return true;
    }

    /**
     * Envía el mensaje de ayuda del comando
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "========== " + ChatColor.GOLD + "ZCKoth Schedule Help" + ChatColor.YELLOW + " ==========");
        sender.sendMessage(ChatColor.GOLD + "/zcschedule list " + ChatColor.WHITE + "- Muestra todas las programaciones");
        sender.sendMessage(ChatColor.GOLD + "/zcschedule add <koth> <hora> <días...> " + ChatColor.WHITE + "- Añade una programación");
        sender.sendMessage(ChatColor.GOLD + "/zcschedule remove <id|índice> " + ChatColor.WHITE + "- Elimina una programación");
        sender.sendMessage(ChatColor.GOLD + "/zcschedule toggle <id|índice> " + ChatColor.WHITE + "- Activa/desactiva una programación");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Formato de hora: HH:MM (24h)");
        sender.sendMessage(ChatColor.GRAY + "Días: 1-7 (Lunes-Domingo) o MONDAY, TUESDAY, etc.");
        sender.sendMessage(ChatColor.GRAY + "Ejemplo: /zcschedule add MiKoth 18:30 1 3 5 (Lunes, Miércoles, Viernes)");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("list", "add", "remove", "toggle");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
            Collections.sort(completions);
            return completions;
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                // Sugerir KOTHs disponibles
                List<String> kothNames = plugin.getKothManager().getAllKoths().stream()
                        .map(Koth -> Koth.getName())
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], kothNames, completions);
                Collections.sort(completions);
                return completions;
            } else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("toggle")) {
                // Sugerir índices de programaciones
                List<String> indices = new ArrayList<>();
                int count = plugin.getScheduleManager().getAllSchedules().size();
                for (int i = 1; i <= count; i++) {
                    indices.add(String.valueOf(i));
                }
                StringUtil.copyPartialMatches(args[1], indices, completions);
                return completions;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            // Sugerir formatos de hora comunes
            List<String> timeFormats = Arrays.asList("00:00", "06:00", "12:00", "18:00", "20:00", "22:00");
            StringUtil.copyPartialMatches(args[2], timeFormats, completions);
            return completions;
        } else if (args.length >= 4 && args[0].equalsIgnoreCase("add")) {
            // Sugerir días de la semana
            List<String> days = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "MONDAY", "TUESDAY", "WEDNESDAY",
                    "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
            StringUtil.copyPartialMatches(args[args.length - 1], days, completions);
            Collections.sort(completions);
            return completions;
        }

        return completions;
    }
}