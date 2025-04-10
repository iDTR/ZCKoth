package me.DTR.zCKoth.managers;

import me.DTR.zCKoth.ZCKoth;
import me.DTR.zCKoth.models.Koth;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootManager {

    private final ZCKoth plugin;
    private final File lootFile;
    private FileConfiguration lootConfig;
    private final Map<String, List<ItemStack>> kothLoot;
    private final Map<String, List<String>> kothCommands;

    private final int ITEMS_PER_PAGE = 36;

    public LootManager(ZCKoth plugin) {
        this.plugin = plugin;
        this.lootFile = new File(plugin.getDataFolder(), "loot.yml");
        this.kothLoot = new HashMap<>();
        this.kothCommands = new HashMap<>();

        // Create file if it doesn't exist
        if (!lootFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                lootFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create loot.yml file: " + e.getMessage());
            }
        }

        this.lootConfig = YamlConfiguration.loadConfiguration(lootFile);

        // Load loot data
        loadLoot();
    }

    private void loadLoot() {
        kothLoot.clear();
        kothCommands.clear();

        ConfigurationSection lootSection = lootConfig.getConfigurationSection("loot");
        if (lootSection == null) return;

        for (String kothName : lootSection.getKeys(false)) {
            ConfigurationSection kothSection = lootSection.getConfigurationSection(kothName);
            if (kothSection == null) continue;

            // Load items
            List<ItemStack> items = new ArrayList<>();
            ConfigurationSection itemsSection = kothSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    ItemStack item = itemsSection.getItemStack(key);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
            kothLoot.put(kothName, items);

            // Load commands
            List<String> commands = kothSection.getStringList("commands");
            kothCommands.put(kothName, commands);
        }
    }

    public void saveLoot() {
        lootConfig.set("loot", null);

        for (Map.Entry<String, List<ItemStack>> entry : kothLoot.entrySet()) {
            String kothName = entry.getKey();
            List<ItemStack> items = entry.getValue();

            for (int i = 0; i < items.size(); i++) {
                lootConfig.set("loot." + kothName + ".items." + i, items.get(i));
            }

            List<String> commands = kothCommands.getOrDefault(kothName, new ArrayList<>());
            lootConfig.set("loot." + kothName + ".commands", commands);
        }

        try {
            lootConfig.save(lootFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save loot.yml file: " + e.getMessage());
        }
    }

    public void openLootEditor(Player player, Koth koth, int page) {
        String kothName = koth.getName();
        List<ItemStack> items = kothLoot.getOrDefault(kothName, new ArrayList<>());

        int totalPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
        if (page > totalPages && totalPages > 0) page = totalPages;
        if (page < 1) page = 1;

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        // Versión corregida
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.GOLD.toString() + "KOTH Loot: " + kothName + " (Page " + page + "/" + (totalPages == 0 ? 1 : totalPages) + ")");

        // Add items
        for (int i = startIndex; i < endIndex; i++) {
            inv.setItem(i - startIndex, items.get(i));
        }

        // Add navigation and control items
        ItemStack prevPage = new ItemStack(Material.ARROW);
        org.bukkit.inventory.meta.ItemMeta prevMeta = prevPage.getItemMeta();
        prevMeta.setDisplayName(ChatColor.YELLOW.toString() + "Previous Page");
        prevPage.setItemMeta(prevMeta);
        inv.setItem(45, prevPage);

        ItemStack nextPage = new ItemStack(Material.ARROW);
        org.bukkit.inventory.meta.ItemMeta nextMeta = nextPage.getItemMeta();
        nextMeta.setDisplayName(ChatColor.YELLOW.toString() + "Next Page");
        nextPage.setItemMeta(nextMeta);
        inv.setItem(53, nextPage);

        ItemStack saveButton = new ItemStack(Material.EMERALD_BLOCK);
        org.bukkit.inventory.meta.ItemMeta saveMeta = saveButton.getItemMeta();
        saveMeta.setDisplayName(ChatColor.GREEN.toString() + "Save Loot");
        saveButton.setItemMeta(saveMeta);
        inv.setItem(49, saveButton);

        ItemStack cmdButton = new ItemStack(Material.COMMAND_BLOCK);
        org.bukkit.inventory.meta.ItemMeta cmdMeta = cmdButton.getItemMeta();
        cmdMeta.setDisplayName(ChatColor.GOLD.toString() + "Edit Commands");
        cmdButton.setItemMeta(cmdMeta);
        inv.setItem(48, cmdButton);

        player.openInventory(inv);

        // Registrar el editor de loot
        plugin.getLootListener().registerLootEditor(player, koth, page);
    }

    public void saveLootFromInventory(Player player, Inventory inventory, Koth koth, int page) {
        String kothName = koth.getName();
        List<ItemStack> items = new ArrayList<>(kothLoot.getOrDefault(kothName, new ArrayList<>()));

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        // Asegurarse de que la lista tenga tamaño adecuado
        while (items.size() <= startIndex + ITEMS_PER_PAGE) {
            items.add(null);
        }

        // Actualizar items existentes y añadir nuevos
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            ItemStack item = inventory.getItem(i);
            int itemIndex = startIndex + i;

            if (item != null && item.getType() != Material.AIR) {
                if (itemIndex < items.size()) {
                    items.set(itemIndex, item);
                } else {
                    items.add(item);
                }
            } else if (itemIndex < items.size()) {
                items.set(itemIndex, null);
            }
        }

        // Eliminar items nulos
        items.removeIf(item -> item == null || item.getType() == Material.AIR);

        // Guardar la lista actualizada
        kothLoot.put(kothName, items);

        // Guardar en el archivo
        saveLoot();

        // Mensaje de confirmación
        player.sendMessage(plugin.getMessageManager().getMessage("loot.saved")
                .replace("%koth%", kothName));
    }

    public void openCommandEditor(Player player, Koth koth) {
        String kothName = koth.getName();
        List<String> commands = kothCommands.getOrDefault(kothName, new ArrayList<>());

        String cmdStr = String.join("\n", commands);
        player.sendMessage(plugin.getMessageManager().getMessage("loot.commands.edit.header")
                .replace("%koth%", kothName));

        if (commands.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("loot.commands.empty"));
        } else {
            for (int i = 0; i < commands.size(); i++) {
                player.sendMessage(ChatColor.YELLOW.toString() + (i + 1) + ". " + ChatColor.WHITE.toString() + commands.get(i));
            }
        }

        player.sendMessage(plugin.getMessageManager().getMessage("loot.commands.edit.instructions"));
    }

    public void addCommand(String kothName, String command) {
        List<String> commands = kothCommands.getOrDefault(kothName, new ArrayList<>());
        commands.add(command);
        kothCommands.put(kothName, commands);
        saveLoot();
    }

    public void removeCommand(String kothName, int index) {
        List<String> commands = kothCommands.getOrDefault(kothName, new ArrayList<>());
        if (index >= 0 && index < commands.size()) {
            commands.remove(index);
            kothCommands.put(kothName, commands);
            saveLoot();
        }
    }

    public void clearCommands(String kothName) {
        kothCommands.put(kothName, new ArrayList<>());
        saveLoot();
    }

    public void giveRewards(Player player, Koth koth) {
        String kothName = koth.getName();

        // Give items
        List<ItemStack> items = kothLoot.getOrDefault(kothName, new ArrayList<>());
        for (ItemStack item : items) {
            if (player.getInventory().firstEmpty() == -1) {
                // Inventory is full, drop items at player's location
                player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
            } else {
                player.getInventory().addItem(item.clone());
            }
        }

        // Execute commands
        List<String> commands = kothCommands.getOrDefault(kothName, new ArrayList<>());
        for (String command : commands) {
            String cmd = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }
}