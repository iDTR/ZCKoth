package me.DTR.zCKoth.models;

import me.DTR.zCKoth.ZCKoth;
import me.DTR.zCKoth.utils.KothCuboid;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

public class KothSolo extends Koth {

    private boolean captureCompleted = false;
    // Flag para indicar si este KOTH es parte de un Conquest y ya fue completado
    private boolean isCompletedConquestStage = false;

    public KothSolo(String name, int clanPoints, KothCuboid region) {
        super(name, KothType.SOLO, clanPoints, region);
    }

    public KothSolo(Map<String, Object> map) {
        super(map);
        this.isCompletedConquestStage = map.containsKey("isCompletedConquestStage") ?
                (boolean) map.get("isCompletedConquestStage") : false;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("isCompletedConquestStage", isCompletedConquestStage);
        return map;
    }

    @Override
    public void handleCapture(Player player) {
        // Primero verificar si este KOTH es una etapa completada de un Conquest
        if (isCompletedConquestStage) {
            // Buscar el Conquest al que pertenece
            for (Koth koth : ZCKoth.getInstance().getKothManager().getAllKoths()) {
                if (koth instanceof KothConquest) {
                    KothConquest conquest = (KothConquest) koth;
                    if (conquest.isActive() && conquest.getNextKoths().contains(name)) {
                        int stageIndex = conquest.getNextKoths().indexOf(name);
                        int currentStage = conquest.getStage();
                    }
                }
            }

            // Si llegamos aquí, es una etapa completada de un Conquest que ya no está activo
            return;
        }

        // Verificar si este KOTH es parte de un Conquest activo
        for (Koth koth : ZCKoth.getInstance().getKothManager().getAllKoths()) {
            if (koth instanceof KothConquest) {
                KothConquest conquest = (KothConquest) koth;
                if (conquest.isActive() && conquest.getNextKoths().contains(name)) {
                    int stageIndex = conquest.getNextKoths().indexOf(name);
                    int currentStage = conquest.getStage();

                    Bukkit.getLogger().info("[ZCKoth] Jugador " + player.getName() + " intenta entrar a etapa " +
                            (stageIndex + 1) + " del Conquest " + conquest.getName() +
                            ". Etapa actual: " + (currentStage + 1));

                    if (stageIndex < currentStage) {

                    } else if (stageIndex > currentStage) {
                        // Esta etapa aún no está activa
                        player.sendMessage(ZCKoth.getInstance().getMessageManager().getMessage("conquest.stage.not-active-yet")
                                .replace("%koth%", conquest.getName())
                                .replace("%stage%", String.valueOf(stageIndex + 1))
                                .replace("%current_stage%", String.valueOf(currentStage + 1)));
                        return;
                    }
                    // Si es la etapa actual, continuar con la captura
                    break;
                }
            }
        }

        // Este es un KOTH normal o la etapa actual del Conquest
        if (!active || capturingPlayer != null) return;

        capturingPlayer = player.getUniqueId();
        captureCompleted = false;

        // Get player's clan if UltimateClans is available
        if (ZCKoth.getInstance().getUltimateClansHook() != null) {
            capturingClan = ZCKoth.getInstance().getUltimateClansHook().getPlayerClan(player);
        }

        broadcast(ZCKoth.getInstance().getMessageManager().getMessage("koth.capturing")
                .replace("%player%", player.getName())
                .replace("%koth%", name));
    }

    @Override
    public void startKoth() {
        if (active) return;

        active = true;
        captureProgress = 0;
        capturingPlayer = null;
        capturingClan = null;
        captureCompleted = false;

        // Si este KOTH es activado directamente, no es una etapa completada
        isCompletedConquestStage = false;

        Bukkit.getLogger().info("[ZCKoth] ¡El KOTH " + name + " ha comenzado! ¡Ve a capturarlo!");
        broadcast(ZCKoth.getInstance().getMessageManager().getMessage("koth.started")
                .replace("%koth%", name));
    }

    // Este método es llamado cuando se completa la captura en tick()
    public void onCaptureComplete(Player player) {
        Bukkit.getLogger().info("[ZCKoth] onCaptureComplete llamado para " + name + " por " + player.getName());
        captureCompleted = true;

        // Verificar si este KOTH es parte de un Conquest
        boolean isPartOfConquest = false;

        Bukkit.getLogger().info("[ZCKoth] Buscando Conquests activos para " + name + "...");
        for (Koth koth : ZCKoth.getInstance().getKothManager().getAllKoths()) {
            Bukkit.getLogger().info("[ZCKoth] Verificando " + koth.getName() + ", tipo: " + koth.getType());

            if (koth instanceof KothConquest) {
                KothConquest conquest = (KothConquest) koth;
                Bukkit.getLogger().info("[ZCKoth] " + koth.getName() + " es un Conquest, activo: " + conquest.isActive());

                if (conquest.isActive()) {
                    Bukkit.getLogger().info("[ZCKoth] Lista de etapas de " + conquest.getName() + ": " + conquest.getNextKoths());
                    Bukkit.getLogger().info("[ZCKoth] ¿" + name + " está en la lista? " + conquest.getNextKoths().contains(name));

                    if (conquest.getNextKoths().contains(name)) {
                        // Este KOTH es parte de un Conquest activo
                        Bukkit.getLogger().info("[ZCKoth] El KOTH " + name + " es parte del Conquest " + conquest.getName());

                        // Marcar esta etapa como completada
                        isCompletedConquestStage = true;

                        // Notificar al Conquest que la etapa se ha completado
                        conquest.notifyStageComplete(name);
                        isPartOfConquest = true;
                        break;
                    }
                }
            }
        }

        if (!isPartOfConquest) {
            Bukkit.getLogger().info("[ZCKoth] " + name + " no es parte de ningún Conquest activo");
        }

        // Si no es parte de un Conquest, mostrar mensaje de captura normal
        if (!isPartOfConquest) {
            broadcast(ZCKoth.getInstance().getMessageManager().getMessage("koth.captured")
                    .replace("%player%", player.getName())
                    .replace("%koth%", name));
        }
    }

    @Override
    public void endKoth() {
        if (!active) return;

        Bukkit.getLogger().info("[ZCKoth] endKoth llamado para " + name);

        active = false;

        // Eliminar las barras de progreso para cualquier jugador
        if (capturingPlayer != null) {
            Player player = Bukkit.getPlayer(capturingPlayer);
            if (player != null) {
                ZCKoth.getInstance().getProgressBarUtil().removeProgressBar(player);
            }
        }

        captureProgress = 0;
        capturingPlayer = null;
        capturingClan = null;
        captureCompleted = false;

        // No resetear isCompletedConquestStage aquí, lo mantenemos para recordar que este KOTH
        // fue completado como parte de un Conquest

        broadcast(ZCKoth.getInstance().getMessageManager().getMessage("koth.ended")
                .replace("%koth%", name));
    }

    // Método para marcar este KOTH como etapa completada de un Conquest
    public void markAsCompletedConquestStage(boolean completed) {
        this.isCompletedConquestStage = completed;
    }

    // Método para verificar si este KOTH es una etapa completada de un Conquest
    public boolean isCompletedConquestStage() {
        return isCompletedConquestStage;
    }
}