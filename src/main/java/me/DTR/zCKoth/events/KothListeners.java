package me.DTR.zCKoth.events;

import me.DTR.zCKoth.ZCKoth;
import me.DTR.zCKoth.models.Koth;
import me.DTR.zCKoth.models.KothConquest;
import me.DTR.zCKoth.models.KothType;
import me.DTR.zCKoth.utils.KothAxe;
import me.DTR.zCKoth.utils.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KothListeners implements Listener {

    private final ZCKoth plugin;
    // Mapa para rastrear los cooldowns de mensajes por jugador y tipo de mensaje
    private final Map<UUID, Map<String, Long>> playerMessageCooldowns = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 5000; // 5 segundos de cooldown

    public KothListeners(ZCKoth plugin) {
        this.plugin = plugin;
    }

    // Método auxiliar para verificar si podemos mostrar un mensaje
    private boolean canShowMessage(Player player, String messageType) {
        UUID playerUUID = player.getUniqueId();

        // Obtener el mapa de cooldowns para este jugador
        Map<String, Long> cooldowns = playerMessageCooldowns.computeIfAbsent(playerUUID, k -> new HashMap<>());

        // Verificar si hay un cooldown para este tipo de mensaje
        long now = System.currentTimeMillis();
        Long lastTime = cooldowns.get(messageType);

        if (lastTime == null || now - lastTime > MESSAGE_COOLDOWN_MS) {
            // Actualizar el tiempo del último mensaje
            cooldowns.put(messageType, now);
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && KothAxe.isSelectionAxe(item)) {
            event.setCancelled(true);

            if (!player.hasPermission("zckoth.axe")) {
                return;
            }

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                KothAxe.setFirstPoint(player.getUniqueId(), event.getClickedBlock().getLocation());
                player.sendMessage(plugin.getMessageManager().getMessage("axe.first-point"));
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                KothAxe.setSecondPoint(player.getUniqueId(), event.getClickedBlock().getLocation());
                player.sendMessage(plugin.getMessageManager().getMessage("axe.second-point"));

                // Verificar si está en proceso de selección de conquest
                if (plugin.getCommandHandler().isCreatingConquest(player.getUniqueId())) {
                    // Continuar con el proceso de selección de conquest
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        plugin.getCommandHandler().continueConquestSelection(player);
                    }, 5L);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if the player has moved to a different block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location destination = event.getTo();

        // Primero encontrar todos los KOTHs activos
        for (Koth koth : plugin.getKothManager().getAllKoths()) {
            // Si no está activo o no es el destino, ignorarlo
            if (!koth.isActive() || !koth.getRegion().isIn(destination)) {
                continue;
            }

            // Si es un KOTH normal, proceder normalmente
            if (koth.getType() != KothType.CONQUEST && !isPartOfConquest(koth.getName())) {
                handleRegularKothEntry(player, koth);
                return;
            }

            // Si es un Conquest o parte de uno, necesitamos verificar etapas
            KothConquest activeConquest = getActiveConquest();
            if (activeConquest == null) {
                // Si no hay Conquest activo, tratar como KOTH normal
                handleRegularKothEntry(player, koth);
                return;
            }

            // Ahora verificamos si este KOTH es parte del Conquest
            if (koth.getType() == KothType.CONQUEST) {
                // Si es el conquest principal, permitir la entrada
                handleRegularKothEntry(player, koth);
                return;
            }

            // Verificar si es una etapa del conquest basado en el nombre
            List<String> stageNames = activeConquest.getNextKoths();
            int currentStage = activeConquest.getStage();

            if (!stageNames.contains(koth.getName())) {
                // No es parte del conquest activo, tratar como KOTH normal
                handleRegularKothEntry(player, koth);
                return;
            }

            // Obtener la posición de esta etapa en el conquest
            int stageIndex = stageNames.indexOf(koth.getName());

            // Verificar si es la etapa actual, anterior o futura
            if (stageIndex < currentStage) {
                // Es una etapa anterior - verificar cooldown para evitar spam
                /*String cooldownKey = "already-captured-" + activeConquest.getName() + "-" + stageIndex;
                if (canShowMessage(player, cooldownKey)) {

                    plugin.getMessageHandler().handlePlayerMessage(player, "already-captured", plugin.getMessageManager().getMessage("conquest.stage.already-captured")
                            .replace("%koth%", activeConquest.getName())
                            .replace("%stage%", String.valueOf(stageIndex + 1))
                            .replace("%current_stage%", String.valueOf(currentStage + 1)));

                }*/

                // Cancelar el movimiento
                event.setCancelled(true);
                return;
            } else if (stageIndex > currentStage) {
                // Es una etapa futura - verificar cooldown para evitar spam
                String cooldownKey = "not-active-yet-" + activeConquest.getName() + "-" + stageIndex;
                /*if (canShowMessage(player, cooldownKey)) {

                    plugin.getMessageHandler().handlePlayerMessage(player, "not-active", plugin.getMessageManager().getMessage("conquest.stage.not-active-yet")
                            .replace("%koth%", activeConquest.getName())
                            .replace("%stage%", String.valueOf(stageIndex + 1))
                            .replace("%current_stage%", String.valueOf(currentStage + 1)));

                }*/

                // Cancelar el movimiento
                event.setCancelled(true);
                return;
            } else {
                // Es la etapa actual, permitir la entrada
                handleRegularKothEntry(player, koth);
                return;
            }
        }

        // Si llegamos aquí, el jugador no está entrando a ningún KOTH,
        // verificar si está saliendo de uno
        Koth fromKoth = null;
        for (Koth koth : plugin.getKothManager().getAllKoths()) {
            if (koth.isActive() && koth.isPlayerInKoth(player)) {
                fromKoth = koth;
                break;
            }
        }

        // Player left a KOTH
        if (fromKoth != null) {
            if (fromKoth.getCapturingPlayer() != null &&
                    fromKoth.getCapturingPlayer().equals(player.getUniqueId())) {
                // Reset capture progress
                fromKoth.setCaptureProgress(0);
                fromKoth.setCapturingPlayer(null);
                fromKoth.setCapturingClan(null);

                // Remover la barra de progreso
                plugin.getProgressBarUtil().removeProgressBar(player);

                player.sendMessage(plugin.getMessageManager().getMessage("koth.left")
                        .replace("%koth%", fromKoth.getName()));
            }
        }
    }

    // Método para manejar la entrada a un KOTH normal
    private void handleRegularKothEntry(Player player, Koth koth) {
        // Player entered a KOTH
        koth.handleCapture(player);
    }

    // Obtener el Conquest activo actual, si existe
    private KothConquest getActiveConquest() {
        for (Koth koth : plugin.getKothManager().getAllKoths()) {
            if (koth.isActive() && koth instanceof KothConquest) {
                return (KothConquest) koth;
            }
        }
        return null;
    }

    // Verificar si un nombre de KOTH parece ser parte de un Conquest basado en su nombre
    private boolean isPartOfConquest(String kothName) {
        return kothName.contains("_stage");
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Skip for players with bypass permission
        if (player.hasPermission("zckoth.bypass")) {
            return;
        }

        // Check if player is in any active KOTH
        Koth koth = plugin.getKothManager().getKothByPlayer(player);
        if (koth == null || !koth.isActive()) {
            return;
        }

        // Get the command
        String command = event.getMessage().substring(1).split(" ")[0].toLowerCase();

        // Check if the command is blocked
        List<String> blockedCommands = plugin.getConfigManager().getConfig().getStringList("blocked-commands");
        for (String blockedCmd : blockedCommands) {
            if (command.equalsIgnoreCase(blockedCmd)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getMessageManager().getMessage("commands.blocked-in-koth"));
                return;
            }
        }
    }

    @EventHandler
    public void onKothCapture(KothCaptureEvent event) {
        // This event can be used by other plugins to hook into KOTH captures
        // For example, to give rewards, broadcast messages, etc.
    }
}