package me.DTR.zCKoth.commands;

import me.DTR.zCKoth.ZCKoth;
import me.DTR.zCKoth.models.Koth;
import me.DTR.zCKoth.models.KothConquest;
import me.DTR.zCKoth.models.KothSolo;
import me.DTR.zCKoth.models.KothType;
import me.DTR.zCKoth.utils.KothAxe;
import me.DTR.zCKoth.utils.KothCuboid;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class KothCommand implements CommandExecutor, TabCompleter, Listener {

    private final ZCKoth plugin;
    private final List<String> blockedCommands = new ArrayList<>();
    private final Map<UUID, ConquestCreationData> conquestCreationMap = new HashMap<>();

    private static class ConquestCreationData {
        String name;
        int captureTime;
        int clanPoints;
        KothCuboid mainRegion;
        List<KothCuboid> stages = new ArrayList<>();
        String worldGuardRegion;

        public ConquestCreationData(String name, int captureTime, int clanPoints, KothCuboid mainRegion) {
            this.name = name;
            this.captureTime = captureTime;
            this.clanPoints = clanPoints;
            this.mainRegion = mainRegion;
        }
    }

    public KothCommand(ZCKoth plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Load blocked commands
        blockedCommands.addAll(plugin.getConfigManager().getConfig().getStringList("blocked-commands"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("zckoth.reload")) {
                    sendNoPermissionMessage(sender);
                    return true;
                }

                plugin.getConfigManager().reloadConfig();
                plugin.getMessageManager().reloadMessages();
                plugin.getKothManager().loadKoths();

                sender.sendMessage(plugin.getMessageManager().getMessage("commands.reload"));
                return true;

            case "axe":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
                    return true;
                }

                if (!sender.hasPermission("zckoth.axe")) {
                    sendNoPermissionMessage(sender);
                    return true;
                }

                Player player = (Player) sender;
                ItemStack axe = KothAxe.createSelectionAxe();
                player.getInventory().addItem(axe);

                player.sendMessage(plugin.getMessageManager().getMessage("commands.axe"));
                return true;

            case "create":
                if (!sender.hasPermission("zckoth.create")) {
                    sendNoPermissionMessage(sender);
                    return true;
                }

                if (args.length < 5) { // Cambiado de 4 a 5 para incluir captureTime
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.create.usage"));
                    return true;
                }

                String kothName = args[1];
                int captureTime;
                String kothTypeStr = args[3]; // Cambiado de args[2] a args[3]
                int clanPoints;

                try {
                    captureTime = Integer.parseInt(args[2]); // Nuevo parámetro para tiempo de captura
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.invalid-number"));
                    return true;
                }

                try {
                    clanPoints = Integer.parseInt(args[4]); // Cambiado de args[3] a args[4]
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.invalid-number"));
                    return true;
                }

                KothType kothType;
                try {
                    kothType = KothType.valueOf(kothTypeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.invalid-type"));
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
                    return true;
                }

                Player playerCreator = (Player) sender;
                KothCuboid region = KothAxe.getSelectionPoints(playerCreator.getUniqueId());

                if (region == null) {
                    playerCreator.sendMessage(plugin.getMessageManager().getMessage("commands.no-selection"));
                    return true;
                }

                if (plugin.getKothManager().getKoth(kothName) != null) {
                    playerCreator.sendMessage(plugin.getMessageManager().getMessage("commands.koth-exists"));
                    return true;
                }

                Koth newKoth;

                if (kothType == KothType.SOLO) {
                    newKoth = new KothSolo(kothName, clanPoints, region);
                    newKoth.setCaptureTime(captureTime); // Establecer el tiempo de captura

                    plugin.getKothManager().addKoth(newKoth);
                    playerCreator.sendMessage(plugin.getMessageManager().getMessage("commands.koth-created")
                            .replace("%name%", kothName));

                    // Eliminar el hacha de selección después de crear un KOTH solitario
                    removeSelectionAxeFromPlayer(playerCreator);

                } else {
                    // Iniciar proceso de selección para conquest
                    startConquestSelectionProcess(playerCreator, kothName, captureTime, clanPoints, region);
                }
                return true;

            case "remove":
                if (!sender.hasPermission("zckoth.remove")) {
                    sendNoPermissionMessage(sender);
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.remove.usage"));
                    return true;
                }

                String kothToRemove = args[1];
                Koth koth = plugin.getKothManager().getKoth(kothToRemove);

                if (koth == null) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.koth-not-found"));
                    return true;
                }

                plugin.getKothManager().removeKoth(kothToRemove);
                /*plugin.getKothManager().removeKoth(kothToRemove + "_stage1");
                plugin.getKothManager().removeKoth(kothToRemove + "_stage2");
                plugin.getKothManager().removeKoth(kothToRemove + "_stage3");
                plugin.getKothManager().removeKoth(kothToRemove + "_stage4");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aSe han eliminado todos los stages junto pertenecientes a este Conquest &6(" + kothToRemove + ")"));*/
                sender.sendMessage(plugin.getMessageManager().getMessage("commands.koth-removed")
                        .replace("%name%", kothToRemove));

                return true;

            case "on":
                if (!sender.hasPermission("zckoth.on")) {
                    sendNoPermissionMessage(sender);
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.on.usage"));
                    return true;
                }

                String kothToStart = args[1];
                Koth kothStart = plugin.getKothManager().getKoth(kothToStart);

                if (kothStart == null) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.koth-not-found"));
                    return true;
                }

                if (kothStart.isActive()) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.koth-already-active"));
                    return true;
                }

                kothStart.startKoth();
                sender.sendMessage(plugin.getMessageManager().getMessage("commands.koth-started")
                        .replace("%name%", kothToStart));

                return true;

            case "off":
                if (!sender.hasPermission("zckoth.off")) {
                    sendNoPermissionMessage(sender);
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.off.usage"));
                    return true;
                }

                String kothToStop = args[1];
                Koth kothStop = plugin.getKothManager().getKoth(kothToStop);

                if (kothStop == null) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.koth-not-found"));
                    return true;
                }

                if (!kothStop.isActive()) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.koth-not-active"));
                    return true;
                }

                kothStop.endKoth();
                sender.sendMessage(plugin.getMessageManager().getMessage("commands.koth-stopped")
                        .replace("%name%", kothToStop));

                return true;

            case "move":
                if (!sender.hasPermission("zckoth.move")) {
                    sendNoPermissionMessage(sender);
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.move.usage"));
                    return true;
                }

                String kothToMove = args[1];
                Koth kothMove = plugin.getKothManager().getKoth(kothToMove);

                if (kothMove == null) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.koth-not-found"));
                    return true;
                }

                Player playerMover = (Player) sender;
                KothCuboid newRegion = KothAxe.getSelectionPoints(playerMover.getUniqueId());

                if (newRegion == null) {
                    playerMover.sendMessage(plugin.getMessageManager().getMessage("commands.no-selection"));
                    return true;
                }

                kothMove.setRegion(newRegion);
                plugin.getKothManager().saveKoths();

                playerMover.sendMessage(plugin.getMessageManager().getMessage("commands.koth-moved")
                        .replace("%name%", kothToMove));

                return true;

            case "list":
                if (!sender.hasPermission("zckoth.list")) {
                    sendNoPermissionMessage(sender);
                    return true;
                }

                List<Koth> koths = plugin.getKothManager().getAllKoths();

                if (koths.isEmpty()) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.no-koths"));
                    return true;
                }

                sender.sendMessage(plugin.getMessageManager().getMessage("commands.koth-list.header"));

                for (Koth k : koths) {
                    String status = k.isActive() ? ChatColor.GREEN + "Activo" : ChatColor.RED + "Inactivo";
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.koth-list.entry")
                            .replace("%name%", k.getName())
                            .replace("%type%", k.getType().toString())
                            .replace("%status%", status));
                }

                return true;

            case "loot":
                if (!sender.hasPermission("zckoth.loot")) {
                    sendNoPermissionMessage(sender);
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.loot.usage"));
                    return true;
                }

                String kothLoot = args[1];
                Koth kothForLoot = plugin.getKothManager().getKoth(kothLoot);

                if (kothForLoot == null) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.koth-not-found"));
                    return true;
                }

                int page = 1;
                if (args.length > 2) {
                    try {
                        page = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(plugin.getMessageManager().getMessage("commands.invalid-number"));
                        return true;
                    }
                }

                Player playerLoot = (Player) sender;
                plugin.getLootManager().openLootEditor(playerLoot, kothForLoot, page);

                return true;

            case "deletecommand":
                if (!sender.hasPermission("zckoth.deletecommand")) {
                    sendNoPermissionMessage(sender);
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.deletecommand.usage"));
                    return true;
                }

                String commandToBlock = args[1];

                if (blockedCommands.contains(commandToBlock)) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("commands.command-already-blocked"));
                    return true;
                }

                blockedCommands.add(commandToBlock);

                // Save to config
                plugin.getConfigManager().getConfig().set("blocked-commands", blockedCommands);
                plugin.getConfigManager().saveConfig();

                sender.sendMessage(plugin.getMessageManager().getMessage("commands.command-blocked")
                        .replace("%command%", commandToBlock));

                return true;

            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "========== " + ChatColor.GOLD + "ZCKoth Help" + ChatColor.YELLOW + " ==========");
        sender.sendMessage(ChatColor.GOLD + "/zckoth " + ChatColor.WHITE + "- Shows this help message");

        if (sender.hasPermission("zckoth.reload"))
            sender.sendMessage(ChatColor.GOLD + "/zckoth reload " + ChatColor.WHITE + "- Reloads the plugin configuration");

        if (sender.hasPermission("zckoth.axe"))
            sender.sendMessage(ChatColor.GOLD + "/zckoth axe " + ChatColor.WHITE + "- Gives you the selection axe");

        if (sender.hasPermission("zckoth.create"))
            sender.sendMessage(ChatColor.GOLD + "/zckoth create <name> <captureTime> <type> <clanPoints> " + ChatColor.WHITE + "- Creates a new KOTH");

        if (sender.hasPermission("zckoth.remove"))
            sender.sendMessage(ChatColor.GOLD + "/zckoth remove <name> " + ChatColor.WHITE + "- Removes a KOTH");

        if (sender.hasPermission("zckoth.on"))
            sender.sendMessage(ChatColor.GOLD + "/zckoth on <name> " + ChatColor.WHITE + "- Activates a KOTH");

        if (sender.hasPermission("zckoth.off"))
            sender.sendMessage(ChatColor.GOLD + "/zckoth off <name> " + ChatColor.WHITE + "- Deactivates a KOTH");

        if (sender.hasPermission("zckoth.move"))
            sender.sendMessage(ChatColor.GOLD + "/zckoth move <name> " + ChatColor.WHITE + "- Moves a KOTH to your selection");

        if (sender.hasPermission("zckoth.list"))
            sender.sendMessage(ChatColor.GOLD + "/zckoth list " + ChatColor.WHITE + "- Lists all KOTHs");

        if (sender.hasPermission("zckoth.loot"))
            sender.sendMessage(ChatColor.GOLD + "/zckoth loot <name> [page] " + ChatColor.WHITE + "- Opens the loot editor");

        if (sender.hasPermission("zckoth.deletecommand"))
            sender.sendMessage(ChatColor.GOLD + "/zckoth deletecommand <command> " + ChatColor.WHITE + "- Blocks a command in KOTHs");
    }

    private void sendNoPermissionMessage(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> commands = new ArrayList<>();

            if (sender.hasPermission("zckoth.reload")) commands.add("reload");
            if (sender.hasPermission("zckoth.axe")) commands.add("axe");
            if (sender.hasPermission("zckoth.create")) commands.add("create");
            if (sender.hasPermission("zckoth.remove")) commands.add("remove");
            if (sender.hasPermission("zckoth.on")) commands.add("on");
            if (sender.hasPermission("zckoth.off")) commands.add("off");
            if (sender.hasPermission("zckoth.move")) commands.add("move");
            if (sender.hasPermission("zckoth.list")) commands.add("list");
            if (sender.hasPermission("zckoth.loot")) commands.add("loot");
            if (sender.hasPermission("zckoth.deletecommand")) commands.add("deletecommand");

            return StringUtil.copyPartialMatches(args[0], commands, completions);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "remove":
                case "on":
                case "off":
                case "move":
                case "loot":
                    List<String> kothNames = plugin.getKothManager().getAllKoths().stream()
                            .map(Koth::getName)
                            .collect(Collectors.toList());

                    return StringUtil.copyPartialMatches(args[1], kothNames, completions);

                case "create":
                    // For creating, first argument is the name which can be anything
                    return Collections.emptyList();

                case "deletecommand":
                    // Suggest common commands to block
                    List<String> commonCommands = Arrays.asList("tp", "tpa", "spawn", "home", "f home", "warp");
                    return StringUtil.copyPartialMatches(args[1], commonCommands, completions);
            }
        } else if (args.length == 3) {
            if (args[0].toLowerCase().equals("create")) {
                // Sugerencias para el tiempo de captura
                return StringUtil.copyPartialMatches(args[2], Arrays.asList("60", "120", "180", "300", "600"), completions);
            }
        } else if (args.length == 4) {
            if (args[0].toLowerCase().equals("create")) {
                // Type suggestions
                List<String> types = Arrays.asList("SOLO", "CONQUEST");
                return StringUtil.copyPartialMatches(args[3], types, completions);
            }
        } else if (args.length == 5) {
            if (args[0].toLowerCase().equals("create")) {
                // Sugerencias para puntos de clan
                return StringUtil.copyPartialMatches(args[4], Arrays.asList("5", "10", "15", "20", "25", "50"), completions);
            }
        }

        return completions;
    }

    // Métodos para el manejo de la creación de Conquest
    private void startConquestSelectionProcess(Player player, String kothName, int captureTime, int clanPoints, KothCuboid mainRegion) {
        // Guardar datos iniciales del conquest
        ConquestCreationData data = new ConquestCreationData(kothName, captureTime, clanPoints, mainRegion);
        conquestCreationMap.put(player.getUniqueId(), data);

        player.sendMessage(ChatColor.GREEN + "Iniciando selección de Conquest " + ChatColor.YELLOW + kothName + ChatColor.GREEN + ". Selecciona las 4 etapas.");
        player.sendMessage(ChatColor.YELLOW + "Selecciona la zona para la etapa " + ChatColor.RED + "1" + ChatColor.YELLOW + " usando el hacha de selección.");

        // Dar hacha de selección nuevamente para la primera etapa
        player.getInventory().addItem(KothAxe.createSelectionAxe());
    }

    public void continueConquestSelection(Player player) {
        ConquestCreationData data = conquestCreationMap.get(player.getUniqueId());

        if (data == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.conquest.not-creating"));
            return;
        }

        KothCuboid selection = KothAxe.getSelectionPoints(player.getUniqueId());

        if (selection == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.no-selection"));
            return;
        }

        // Añadir esta selección como una etapa
        data.stages.add(selection);
        int currentStage = data.stages.size();

        player.sendMessage(ChatColor.GREEN + "Etapa " + ChatColor.YELLOW + currentStage + ChatColor.GREEN + " seleccionada correctamente.");

        // Limpiar la selección actual
        KothAxe.clearSelection(player.getUniqueId());

        // Si ya tenemos 4 etapas, solicitar la región de WorldGuard y finalizar
        if (currentStage >= 4) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.conquest.select.worldguard"));
            // Eliminar el hacha de selección
            removeSelectionAxeFromPlayer(player);
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Selecciona la zona para la etapa " + ChatColor.RED + (currentStage + 1) + ChatColor.YELLOW + " usando el hacha de selección.");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (conquestCreationMap.containsKey(player.getUniqueId()) &&
                conquestCreationMap.get(player.getUniqueId()).stages.size() >= 4) {
            event.setCancelled(true);

            // Procesar en el hilo principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                finalizeConquestCreation(player, event.getMessage());
            });
        }
    }

    private void finalizeConquestCreation(Player player, String worldGuardRegion) {
        ConquestCreationData data = conquestCreationMap.get(player.getUniqueId());

        if (data == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.conquest.not-creating"));
            return;
        }

        data.worldGuardRegion = worldGuardRegion;

        // Crear los KOTHs individuales para cada etapa
        List<String> nextKoths = new ArrayList<>();
        for (int i = 0; i < data.stages.size(); i++) {
            String stageName = data.name + "_stage" + (i + 1);
            KothSolo stageKoth = new KothSolo(stageName, data.clanPoints, data.stages.get(i));
            stageKoth.setCaptureTime(data.captureTime);
            plugin.getKothManager().addKoth(stageKoth);
            nextKoths.add(stageName);
        }

        // Crear el Conquest principal
        KothConquest conquest = new KothConquest(data.name, data.clanPoints, data.mainRegion, nextKoths, data.worldGuardRegion);
        conquest.setCaptureTime(data.captureTime);
        plugin.getKothManager().addKoth(conquest);

        // Eliminar los datos de creación
        conquestCreationMap.remove(player.getUniqueId());

        player.sendMessage(plugin.getMessageManager().getMessage("commands.conquest.created")
                .replace("%name%", data.name));
    }

    private void removeSelectionAxeFromPlayer(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && KothAxe.isSelectionAxe(item)) {
                player.getInventory().remove(item);
                break;
            }
        }
    }

    public boolean isCreatingConquest(UUID playerId) {
        return conquestCreationMap.containsKey(playerId);
    }
}