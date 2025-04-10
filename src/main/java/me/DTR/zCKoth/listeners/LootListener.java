package me.DTR.zCKoth.listeners;

import me.DTR.zCKoth.ZCKoth;
import me.DTR.zCKoth.models.Koth;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LootListener implements Listener {

    private final ZCKoth plugin;
    // Mapa para rastrear inventarios: UUID del jugador -> [Nombre del KOTH, Página actual]
    private final Map<UUID, String[]> lootEditors = new HashMap<>();

    public LootListener(ZCKoth plugin) {
        this.plugin = plugin;
    }

    // Método para registrar un nuevo editor de loot
    public void registerLootEditor(Player player, Koth koth, int page) {
        lootEditors.put(player.getUniqueId(), new String[] { koth.getName(), String.valueOf(page) });
    }

    // Método para eliminar un editor de loot
    public void unregisterLootEditor(Player player) {
        lootEditors.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Verificar si el jugador tiene un editor de loot abierto
        String[] editorInfo = lootEditors.get(player.getUniqueId());
        if (editorInfo == null) return;

        String kothName = editorInfo[0];
        int page = Integer.parseInt(editorInfo[1]);

        // Obtener el KOTH
        Koth koth = plugin.getKothManager().getKoth(kothName);
        if (koth == null) {
            player.closeInventory();
            player.sendMessage(plugin.getMessageManager().getMessage("commands.koth-not-found"));
            unregisterLootEditor(player);
            return;
        }

        // Manejar los clicks en los botones especiales
        if (event.getRawSlot() >= 45) { // Botones están en los slots 45-53
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // Botón de guardar
            if (event.getRawSlot() == 49 && clickedItem.getType() == Material.EMERALD_BLOCK) {
                plugin.getLootManager().saveLootFromInventory(player, event.getInventory(), koth, page);
                player.sendMessage(plugin.getMessageManager().getMessage("loot.saved")
                        .replace("%koth%", kothName));
                return;
            }

            // Botón de página anterior
            if (event.getRawSlot() == 45 && page > 1) {
                plugin.getLootManager().openLootEditor(player, koth, page - 1);
                // Actualizar la página en nuestro mapa
                lootEditors.put(player.getUniqueId(), new String[] { kothName, String.valueOf(page - 1) });
                return;
            }

            // Botón de página siguiente
            if (event.getRawSlot() == 53) {
                plugin.getLootManager().openLootEditor(player, koth, page + 1);
                // Actualizar la página en nuestro mapa
                lootEditors.put(player.getUniqueId(), new String[] { kothName, String.valueOf(page + 1) });
                return;
            }

            // Botón de comandos
            if (event.getRawSlot() == 48 && clickedItem.getType() == Material.COMMAND_BLOCK) {
                player.closeInventory();
                plugin.getLootManager().openCommandEditor(player, koth);
                unregisterLootEditor(player);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        // Eliminar el registro del editor cuando se cierra el inventario
        unregisterLootEditor(player);
    }
}