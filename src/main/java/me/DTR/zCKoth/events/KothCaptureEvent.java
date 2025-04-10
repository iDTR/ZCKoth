package me.DTR.zCKoth.events;

import me.DTR.zCKoth.models.Koth;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class KothCaptureEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Koth koth;
    private final Player player;
    private boolean cancelled;

    public KothCaptureEvent(Koth koth, Player player) {
        this.koth = koth;
        this.player = player;
        this.cancelled = false;
    }

    public Koth getKoth() {
        return koth;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}