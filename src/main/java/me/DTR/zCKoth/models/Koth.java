package me.DTR.zCKoth.models;

import me.DTR.zCKoth.ZCKoth;
import me.DTR.zCKoth.events.KothCaptureEvent;
import me.DTR.zCKoth.utils.KothCuboid;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class Koth implements ConfigurationSerializable {

    protected final String name;
    protected final KothType type;
    protected final int clanPoints;
    protected KothCuboid region;
    protected boolean active;
    protected UUID capturingPlayer;
    protected String capturingClan;
    protected int captureProgress;
    protected int captureTime;
    protected Location signLocation;

    public Koth(String name, KothType type, int clanPoints, KothCuboid region) {
        this.name = name;
        this.type = type;
        this.clanPoints = clanPoints;
        this.region = region;
        this.active = false;
        this.captureProgress = 0;
        this.captureTime = 300; // Default capture time in seconds
    }

    public Koth(Map<String, Object> map) {
        this.name = (String) map.get("name");
        this.type = KothType.valueOf((String) map.get("type"));
        this.clanPoints = (int) map.get("clanPoints");
        this.region = (KothCuboid) map.get("region");
        this.active = (boolean) map.get("active");
        this.captureTime = (int) map.get("captureTime");

        // Optional fields
        if (map.containsKey("signLocation")) {
            this.signLocation = (Location) map.get("signLocation");
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("type", type.toString());
        map.put("clanPoints", clanPoints);
        map.put("region", region);
        map.put("active", active);
        map.put("captureTime", captureTime);

        if (signLocation != null) {
            map.put("signLocation", signLocation);
        }

        return map;
    }

    // Implementación del método tick según lo solicitado
    public void tick() {
        if (!active) return;

        // Si es el Conquest principal, no procesar capturas
        if (this.getType() == KothType.CONQUEST) {
            return;
        }

        if (capturingPlayer != null) {
            Player player = Bukkit.getPlayer(capturingPlayer);

            // Check if player left the KOTH or went offline
            if (player == null || !isPlayerInKoth(player)) {
                if (type == KothType.CONQUEST) {
                    broadcast(ZCKoth.getInstance().getMessageManager().getMessage("conquest.left")
                            .replace("%player%", player != null ? player.getName() : "Unknown")
                            .replace("%koth%", name)
                            .replace("%stage%", String.valueOf(((KothConquest) this).getStage() + 1)));
                } else {
                    broadcast(ZCKoth.getInstance().getMessageManager().getMessage("koth.left")
                            .replace("%player%", player != null ? player.getName() : "Unknown")
                            .replace("%koth%", name));
                }

                captureProgress = 0;
                capturingPlayer = null;
                capturingClan = null;

                // Remover la barra de progreso si el jugador está online
                if (player != null) {
                    ZCKoth.getInstance().getProgressBarUtil().removeProgressBar(player);
                }

                return;
            }

            // Increment progress
            captureProgress++;

            // Calculate percentage for messages and progress bar
            double percentage = (double) captureProgress / captureTime;
            int percentageInt = (int) (percentage * 100);

            // Actualizar la barra de progreso
            String barTitle = ZCKoth.getInstance().getMessageManager().getMessageNoPrefix("progress-bar.title")
                    .replace("%koth%", name)
                    .replace("_", " ")
                    .replace("%progress%", String.valueOf(percentageInt));

            ZCKoth.getInstance().getProgressBarUtil().showProgressBar(
                    player,
                    barTitle,
                    percentage,
                    BarColor.RED);

            // Send progress messages at certain intervals
            if (percentageInt % 25 == 0 && captureProgress < captureTime && percentageInt > 0) {
                if (type == KothType.CONQUEST) {
                    broadcast(ZCKoth.getInstance().getMessageManager().getMessage("conquest.progress")
                            .replace("%player%", player.getName())
                            .replace("%koth%", name)
                            .replace("%stage%", String.valueOf(((KothConquest) this).getStage() + 1))
                            .replace("%progress%", String.valueOf(percentageInt)));
                } else {
                    broadcast(ZCKoth.getInstance().getMessageManager().getMessage("koth.progress")
                            .replace("%player%", player.getName())
                            .replace("%koth%", name)
                            .replace("%progress%", String.valueOf(percentageInt)));
                }
            }

            // Check if capture complete
            if (captureProgress >= captureTime) {
                // Remover la barra de progreso
                ZCKoth.getInstance().getProgressBarUtil().removeProgressBar(player);

                KothCaptureEvent event = new KothCaptureEvent(this, player);
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    // Mensaje de captura diferente según el tipo de KOTH
                    if (type == KothType.CONQUEST) {
                        broadcast(ZCKoth.getInstance().getMessageManager().getMessage("conquest.captured")
                                .replace("%player%", player.getName())
                                .replace("%koth%", name)
                                .replace("%stage%", String.valueOf(((KothConquest) this).getStage() + 1)));

                        // Log para diagnóstico
                        Bukkit.getLogger().info("[ZCKoth] Etapa " + (((KothConquest) this).getStage() + 1) +
                                " del Conquest " + name + " capturada por " + player.getName());
                    } else if (this instanceof KothSolo) {
                        // Para KOTH Solo, usar el nuevo método onCaptureComplete
                        ((KothSolo) this).onCaptureComplete(player);
                    } else {
                        broadcast(ZCKoth.getInstance().getMessageManager().getMessage("koth.captured")
                                .replace("%player%", player.getName())
                                .replace("%koth%", name));
                    }

                    // Award clan points if applicable
                    if (ZCKoth.getInstance().getUltimateClansHook() != null && capturingClan != null) {
                        ZCKoth.getInstance().getUltimateClansHook().addClanPoints(capturingClan, clanPoints);
                        broadcast(ZCKoth.getInstance().getMessageManager().getMessage("koth.clan.points")
                                .replace("%clan%", capturingClan)
                                .replace("%points%", String.valueOf(clanPoints)));
                    }

                    // Give rewards to player
                    giveRewards(player);

                    // Para Conquest, manejar el cambio de etapa
                    if (this instanceof KothConquest) {
                        Bukkit.getLogger().info("[ZCKoth] Llamando a activateNextKoth() para Conquest: " + name);
                        ((KothConquest) this).activateNextKoth();
                    } else {
                        // Para KOTH normal, terminar
                        endKoth();
                    }
                }
            }
        } else {
            // Check if any player is in the KOTH
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isPlayerInKoth(player)) {
                    capturingPlayer = player.getUniqueId();

                    // Get player's clan if UltimateClans is available
                    if (ZCKoth.getInstance().getUltimateClansHook() != null) {
                        capturingClan = ZCKoth.getInstance().getUltimateClansHook().getPlayerClan(player);
                    }

                    // Mensaje de captura diferente según el tipo de KOTH
                    if (type == KothType.CONQUEST) {
                        broadcast(ZCKoth.getInstance().getMessageManager().getMessage("conquest.capturing")
                                .replace("%player%", player.getName())
                                .replace("%koth%", name)
                                .replace("%stage%", String.valueOf(((KothConquest) this).getStage() + 1)));
                    } else {
                        broadcast(ZCKoth.getInstance().getMessageManager().getMessage("koth.capturing")
                                .replace("%player%", player.getName())
                                .replace("%koth%", name));
                    }

                    break;
                }
            }
        }
    }

    public abstract void handleCapture(Player player);

    public abstract void startKoth();

    public abstract void endKoth();

    public String getName() {
        return name;
    }

    public KothType getType() {
        return type;
    }

    public int getClanPoints() {
        return clanPoints;
    }

    public KothCuboid getRegion() {
        return region;
    }

    public void setRegion(KothCuboid region) {
        this.region = region;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getCapturingPlayer() {
        return capturingPlayer;
    }

    public void setCapturingPlayer(UUID capturingPlayer) {
        this.capturingPlayer = capturingPlayer;
    }

    public String getCapturingClan() {
        return capturingClan;
    }

    public void setCapturingClan(String capturingClan) {
        this.capturingClan = capturingClan;
    }

    public int getCaptureProgress() {
        return captureProgress;
    }

    public void setCaptureProgress(int captureProgress) {
        this.captureProgress = captureProgress;
    }

    public int getCaptureTime() {
        return captureTime;
    }

    public void setCaptureTime(int captureTime) {
        this.captureTime = captureTime;
    }

    public Location getSignLocation() {
        return signLocation;
    }

    public void setSignLocation(Location signLocation) {
        this.signLocation = signLocation;
    }

    public boolean isPlayerInKoth(Player player) {
        return region != null && region.isIn(player.getLocation());
    }

    protected void giveRewards(Player player) {
        ZCKoth.getInstance().getLootManager().giveRewards(player, this);
    }

    protected void broadcast(String message) {
        Bukkit.broadcastMessage(ZCKoth.getInstance().getMessageManager().colorize(message));
    }
}