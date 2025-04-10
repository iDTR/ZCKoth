package me.DTR.zCKoth.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public class MessageHandler {
    private final Map<String, Set<UUID>> notifiedPlayersByMessage = new HashMap<>();

    public void handlePlayerMessage(Player player, String messageKey, String message) {
        notifiedPlayersByMessage.putIfAbsent(messageKey, new HashSet<>());

        Set<UUID> notifiedPlayers = notifiedPlayersByMessage.get(messageKey);
        if (!notifiedPlayers.contains(player.getUniqueId())) {
            player.sendMessage(message);
            notifiedPlayers.add(player.getUniqueId());
        }
    }

    public void resetNotifiedPlayers(String messageKey) {
        notifiedPlayersByMessage.remove(messageKey);
    }

    public void resetAllNotifiedPlayers() {
        notifiedPlayersByMessage.clear();
    }
}
