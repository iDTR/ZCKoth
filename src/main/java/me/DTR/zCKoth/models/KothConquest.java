package me.DTR.zCKoth.models;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import me.DTR.zCKoth.ZCKoth;
import me.DTR.zCKoth.utils.KothCuboid;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KothConquest extends Koth {

    private final List<String> nextKoths;
    private final String worldGuardEntryRegion;
    private int stage = 0;

    public KothConquest(String name, int clanPoints, KothCuboid region, List<String> nextKoths, String worldGuardEntryRegion) {
        super(name, KothType.CONQUEST, clanPoints, region);
        this.nextKoths = nextKoths;
        this.worldGuardEntryRegion = worldGuardEntryRegion;
    }

    @SuppressWarnings("unchecked")
    public KothConquest(Map<String, Object> map) {
        super(map);
        this.nextKoths = (List<String>) map.get("nextKoths");
        this.worldGuardEntryRegion = (String) map.get("worldGuardEntryRegion");
        this.stage = map.containsKey("stage") ? (int) map.get("stage") : 0;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("nextKoths", nextKoths);
        map.put("worldGuardEntryRegion", worldGuardEntryRegion);
        map.put("stage", stage);
        return map;
    }

    @Override
    public void handleCapture(Player player) {
        // Si es el Conquest principal, NO permitir capturas directas
        // En su lugar, mostrar un mensaje informativo y direccionar al jugador a la etapa actual
        if (this.getType() == KothType.CONQUEST) {
            // Verificar si hay un mensaje para este caso
            String message = ZCKoth.getInstance().getMessageManager().getMessage("conquest.use-stages");
            if (message.contains("Missing message")) {
            } else {

            }

            // Dirigir al jugador a la etapa actual
            if (stage < nextKoths.size()) {
                String currentStageName = nextKoths.get(stage);
                Koth currentStage = ZCKoth.getInstance().getKothManager().getKoth(currentStageName);
                if (currentStage != null && currentStage.isActive()) {
                    String hintMessage = ZCKoth.getInstance().getMessageManager().getMessage("conquest.current-stage-hint");
                    if (hintMessage.contains("Missing message")) {

                    }
                }
            }

            return;
        }

        // Si llegamos aquí, es una etapa individual del Conquest
        // This method is called when a player enters the koth
        if (!active || capturingPlayer != null) return;

        capturingPlayer = player.getUniqueId();

        // Get player's clan if UltimateClans is available
        if (ZCKoth.getInstance().getUltimateClansHook() != null) {
            capturingClan = ZCKoth.getInstance().getUltimateClansHook().getPlayerClan(player);
        }

        broadcast(ZCKoth.getInstance().getMessageManager().getMessage("conquest.capturing")
                .replace("%player%", player.getName())
                .replace("%koth%", name)
                .replace("%stage%", String.valueOf(stage + 1)));
    }

    // El resto del código de KothConquest.java permanece igual...

    /**
     * Verifica y corrige el estado de activación de todas las etapas del Conquest
     * para asegurar que sólo la etapa actual esté activa
     */
    public void enforceActiveStage() {
        // No hacer nada si el Conquest no está activo
        if (!active) return;

        // Verificar cada KOTH en la secuencia
        for (int i = 0; i < nextKoths.size(); i++) {
            // Solo la etapa actual debe estar activa
            boolean shouldBeActive = (i == stage);

            String kothName = nextKoths.get(i);
            Koth koth = ZCKoth.getInstance().getKothManager().getKoth(kothName);

            if (koth != null) {
                // Si debería estar inactivo pero está activo, desactivarlo
                if (!shouldBeActive && koth.isActive()) {
                    Bukkit.getLogger().warning("[ZCKoth] Detectada etapa incorrecta activa: " + kothName + ", desactivando.");
                    koth.setActive(false);
                    koth.setCaptureProgress(0);
                    koth.setCapturingPlayer(null);
                    koth.setCapturingClan(null);

                    // Remover barras de progreso para jugadores en este KOTH
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (koth.isPlayerInKoth(online)) {
                            ZCKoth.getInstance().getProgressBarUtil().removeProgressBar(online);
                        }
                    }
                }
                // Si debería estar activo pero está inactivo, activarlo
                else if (shouldBeActive && !koth.isActive()) {
                    Bukkit.getLogger().warning("[ZCKoth] Etapa actual inactiva: " + kothName + ", activando.");
                    koth.setActive(true);
                }
            }
        }
    }

    @Override
    public void startKoth() {
        if (active) return;

        active = true;

        // Reiniciar el estado si es un inicio manual
        if (captureProgress == 0 && capturingPlayer == null && stage == 0) {
            // Desactivar todas las etapas al inicio para evitar problemas
            for (String kothName : nextKoths) {
                Koth koth = ZCKoth.getInstance().getKothManager().getKoth(kothName);
                if (koth != null) {
                    //Bukkit.getLogger().info("[ZCKoth] Desactivando etapa: " + kothName);
                    koth.setActive(false);
                    koth.setCaptureProgress(0);
                    koth.setCapturingPlayer(null);
                    koth.setCapturingClan(null);
                }
            }
        }

        captureProgress = 0;
        capturingPlayer = null;
        capturingClan = null;

        // Registrar información para diagnóstico
        Bukkit.getLogger().info("[ZCKoth] Iniciando Conquest: " + name + ", Etapa: " + (stage + 1));
        ZCKoth.getInstance().getWorldGuardHook().setRegionFlag(worldGuardEntryRegion, "entry", "allow");

        // Activar solamente el KOTH de la etapa actual
        if (stage < nextKoths.size()) {
            String currentKothName = nextKoths.get(stage);
            Koth currentKoth = ZCKoth.getInstance().getKothManager().getKoth(currentKothName);

            if (currentKoth != null) {
                //Bukkit.getLogger().info("[ZCKoth] Activando etapa " + (stage + 1) + ": " + currentKothName);

                // Asegurarse de que esté desactivado primero
                if (currentKoth.isActive()) {
                    currentKoth.endKoth();
                }

                // Iniciar el KOTH
                currentKoth.setActive(true);
                currentKoth.setCaptureProgress(0);
                currentKoth.setCapturingPlayer(null);
                currentKoth.setCapturingClan(null);

                // Anunciar inicio
                broadcast(ZCKoth.getInstance().getMessageManager().getMessage("koth.started")
                        .replace("%koth%", currentKothName)
                        .replace("_", " "));
            }
        }

        broadcast(ZCKoth.getInstance().getMessageManager().getMessage("conquest.started")
                .replace("%koth%", name)
                .replace("%stage%", String.valueOf(stage + 1))
                .replace("_", " "));
    }

    @Override
    public void endKoth() {
        if (!active) return;

        // Registrar información para diagnóstico
        Bukkit.getLogger().info("[ZCKoth] Finalizando Conquest: " + name + ", Etapa: " + (stage + 1));


        // Finalizar explícitamente todos los KOTHs de la secuencia
        for (String kothName : nextKoths) {
            Koth koth = ZCKoth.getInstance().getKothManager().getKoth(kothName);
            if (koth != null) {
                // Forzar desactivación
                //Bukkit.getLogger().info("[ZCKoth] Finalizando etapa: " + kothName);
                koth.setActive(false);
                koth.setCaptureProgress(0);
                koth.setCapturingPlayer(null);
                koth.setCapturingClan(null);


                // Remover barras de progreso para jugadores en este KOTH
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if(ZCKoth.getInstance().getWorldGuardHook().isInRegion(online.getLocation(), worldGuardEntryRegion)){
                        Bukkit.dispatchCommand(online,"spawn");
                    }

                    if (koth.isPlayerInKoth(online)) {
                        ZCKoth.getInstance().getProgressBarUtil().removeProgressBar(online);
                        Bukkit.dispatchCommand(online,"spawn");
                    }
                }
            }
        }

        active = false;
        stage = 0;

        // Eliminar las barras de progreso para cualquier jugador
        if (capturingPlayer != null) {
            Player player = Bukkit.getPlayer(capturingPlayer);
            if (player != null) {
                ZCKoth.getInstance().getProgressBarUtil().removeProgressBar(player);
                Bukkit.dispatchCommand(player,"spawn");
            }
        }

        captureProgress = 0;
        capturingPlayer = null;
        capturingClan = null;

        broadcast(ZCKoth.getInstance().getMessageManager().getMessage("conquest.ended")
                .replace("%koth%", name));
    }

    public void activateNextKoth() {
        // Desactivar TODOS los KOTHs excepto el nuevo que vamos a activar
        for (String kothName : nextKoths) {
            // No desactivar el que vamos a activar
            if (stage + 1 < nextKoths.size() && kothName.equals(nextKoths.get(stage + 1))) {
                continue;
            }

            Koth stageKoth = ZCKoth.getInstance().getKothManager().getKoth(kothName);
            if (stageKoth != null) {
                //Bukkit.getLogger().info("[ZCKoth] Forzando desactivación de: " + kothName);

                // Desactivar por completo este KOTH
                stageKoth.setActive(false);
                stageKoth.setCaptureProgress(0);
                stageKoth.setCapturingPlayer(null);
                stageKoth.setCapturingClan(null);

                // Finalizar el KOTH usando su propio método
                if (stageKoth.isActive()) {
                    stageKoth.endKoth();
                }

                // Asegurarse una segunda vez que está desactivado
                stageKoth.setActive(false);

                // Remover barras de progreso para jugadores en este KOTH
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (stageKoth.isPlayerInKoth(online)) {
                        ZCKoth.getInstance().getProgressBarUtil().removeProgressBar(online);
                    }
                }
            }
        }

        // Reset current koth state
        captureProgress = 0;
        capturingPlayer = null;
        capturingClan = null;

        // Increment stage
        stage++;

        // Check if there are more stages
        if (stage < nextKoths.size()) {
            // Ensure conquest is active
            active = true;

            // Activate next koth
            String nextKothName = nextKoths.get(stage);
            Koth nextKoth = ZCKoth.getInstance().getKothManager().getKoth(nextKothName);

            if (nextKoth != null) {
                // Asegurarse de que está desactivado primero
                if (nextKoth.isActive()) {
                    nextKoth.endKoth();
                }

                // Activar manualmente
                nextKoth.setActive(true);
                nextKoth.setCaptureProgress(0);
                nextKoth.setCapturingPlayer(null);
                nextKoth.setCapturingClan(null);

                // Anunciar inicio
                broadcast(ZCKoth.getInstance().getMessageManager().getMessage("koth.started")
                        .replace("%koth%", nextKothName));

                ZCKoth.getInstance().getWorldGuardHook().setRegionFlag(worldGuardEntryRegion, "entry", "allow");

                broadcast(ZCKoth.getInstance().getMessageManager().getMessage("conquest.next")
                        .replace("%koth%", nextKothName)
                        .replace("%stage%", String.valueOf(stage + 1)));

                //Bukkit.getLogger().info("[ZCKoth] Etapa " + (stage + 1) + " activada correctamente.");
            } else {
                Bukkit.getLogger().warning("[ZCKoth] No se pudo encontrar la etapa: " + nextKothName);
                broadcast(ZCKoth.getInstance().getMessageManager().getMessage("conquest.next-error")
                        .replace("%koth%", nextKothName));

                // End the conquest
                endKoth();
            }
        } else {
            Bukkit.getLogger().info("[ZCKoth] Todas las etapas completadas. Finalizando Conquest.");
            // Final stage completed, disable WorldGuard entry region if available
            if (ZCKoth.getInstance().getWorldGuardHook() != null && worldGuardEntryRegion != null) {
                ZCKoth.getInstance().getWorldGuardHook().setRegionFlag(worldGuardEntryRegion, "entry", "deny");
                broadcast(ZCKoth.getInstance().getMessageManager().getMessage("conquest.completed")
                        .replace("%koth%", name));
            }

            // End the conquest
            endKoth();
        }

        // Asegurarse que solo la etapa actual esté activa
        enforceActiveStage();
    }

    public void notifyStageComplete(String stageName) {

        // Verificar si es la etapa actual
        int stageIndex = nextKoths.indexOf(stageName);
        if (stageIndex != stage) {
            return;
        }

        activateNextKoth();
    }

    public List<String> getNextKoths() {
        return new ArrayList<>(nextKoths);
    }

    public String getWorldGuardEntryRegion() {
        return worldGuardEntryRegion;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }
}