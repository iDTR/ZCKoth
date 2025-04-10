package me.DTR.zCKoth;

import me.DTR.zCKoth.commands.KothCommand;
import me.DTR.zCKoth.commands.ScheduleCommand;
import me.DTR.zCKoth.events.KothListeners;
import me.DTR.zCKoth.hooks.PlaceholderHook;
import me.DTR.zCKoth.hooks.UltimateClansHook;
import me.DTR.zCKoth.hooks.WorldGuardHook;
import me.DTR.zCKoth.listeners.LootListener;
import me.DTR.zCKoth.managers.ConfigManager;
import me.DTR.zCKoth.managers.KothManager;
import me.DTR.zCKoth.managers.LootManager;
import me.DTR.zCKoth.managers.MessageManager;
import me.DTR.zCKoth.managers.ScheduleManager;
import me.DTR.zCKoth.utils.KothCuboid;
import me.DTR.zCKoth.utils.MessageHandler;
import me.DTR.zCKoth.utils.PlayerMessageResetTask;
import me.DTR.zCKoth.utils.ProgressBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZCKoth extends JavaPlugin implements Listener {

    private static ZCKoth instance;
    private ConfigManager configManager;
    private KothManager kothManager;
    private LootManager lootManager;
    private MessageManager messageManager;
    private ScheduleManager scheduleManager;
    private WorldGuardHook worldGuardHook;
    private UltimateClansHook ultimateClansHook;
    private ProgressBarUtil progressBarUtil;
    private KothCommand commandHandler;
    //private MessageHandler messageHandler;
    private LootListener lootListener;

    @Override
    public void onEnable() {
        instance = this;
        //messageHandler = new MessageHandler();
        //PlayerMessageResetTask.start(this, messageHandler, 2400L); // 2 minutos


        // Registrar las clases serializables
        registerSerializableClasses();

        // Initialize managers
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        kothManager = new KothManager(this);
        lootManager = new LootManager(this);
        scheduleManager = new ScheduleManager(this);
        progressBarUtil = new ProgressBarUtil();

        // Initialize hooks
        setupHooks();

        // Register events
        getServer().getPluginManager().registerEvents(new KothListeners(this), this);

        // Inicializar y registrar el LootListener
        lootListener = new LootListener(this);
        getServer().getPluginManager().registerEvents(lootListener, this);

        // Registrar este listener para el evento de guardado del mundo
        getServer().getPluginManager().registerEvents(this, this);

        // Register commands
        commandHandler = new KothCommand(this);
        getCommand("zckoth").setExecutor(commandHandler);
        getCommand("zckoth").setTabCompleter(commandHandler);

        // Registrar comando de programación
        ScheduleCommand scheduleCommand = new ScheduleCommand(this);
        getCommand("zcschedule").setExecutor(scheduleCommand);
        getCommand("zcschedule").setTabCompleter(scheduleCommand);

        // Load KOTHs
        kothManager.loadKoths();

        getLogger().info("ZCKoth ha sido habilitado correctamente!");
    }

    private void registerSerializableClasses() {
        // Registrar todas las clases que necesitan ser serializadas
        ConfigurationSerialization.registerClass(KothCuboid.class);

        // Registrar cualquier otra clase personalizada que necesite ser serializada
        // ConfigurationSerialization.registerClass(OtraClase.class);
    }

    @Override
    public void onDisable() {
        // Save data
        if (kothManager != null) {
            kothManager.shutdown(); // Este método guardará los KOTHs y cancelará las tareas
        }

        if (lootManager != null) {
            lootManager.saveLoot(); // Asegurarse de guardar el loot
        }

        if (scheduleManager != null) {
            scheduleManager.shutdown(); // Detener las tareas del programador
        }

        // Clean up progress bars
        if (progressBarUtil != null) {
            progressBarUtil.removeAllBars();
        }

        getLogger().info("ZCKoth ha sido deshabilitado correctamente!");
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        // Guardar KOTHs cuando el mundo principal se guarda
        if (event.getWorld().equals(Bukkit.getWorlds().get(0))) {
            getLogger().info("Guardando KOTHs durante el guardado del mundo...");
            kothManager.saveKoths();
            lootManager.saveLoot();
            scheduleManager.saveSchedules();
        }
    }

    private void setupHooks() {
        // WorldGuard Hook
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook = new WorldGuardHook(this);
            getLogger().info("WorldGuard hook habilitado.");
        } else {
            getLogger().warning("WorldGuard no encontrado. Algunas funciones no estarán disponibles.");
        }

        // UltimateClans Hook
        if (Bukkit.getPluginManager().getPlugin("UltimateClans") != null) {
            ultimateClansHook = new UltimateClansHook(this);
            getLogger().info("UltimateClans hook habilitado.");
        } else {
            getLogger().warning("UltimateClans no encontrado. Algunas funciones no estarán disponibles.");
        }

        // PlaceholderAPI Hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI hook habilitado.");
        } else {
            getLogger().warning("PlaceholderAPI no encontrado. Los placeholders no estarán disponibles.");
        }
    }

    public static ZCKoth getInstance() {
        return instance;
    }


    public ConfigManager getConfigManager() {
        return configManager;
    }

    public KothManager getKothManager() {
        return kothManager;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ScheduleManager getScheduleManager() {
        return scheduleManager;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public UltimateClansHook getUltimateClansHook() {
        return ultimateClansHook;
    }

    public ProgressBarUtil getProgressBarUtil() {
        return progressBarUtil;
    }

    public KothCommand getCommandHandler() {
        return commandHandler;
    }

    public LootListener getLootListener() {
        return lootListener;
    }

    /**
     * Método auxiliar para crear una Location a partir de un string con formato "mundo,x,y,z"
     * Útil para serialización/deserialización
     */
    public static Location stringToLocation(String locationString) {
        if (locationString == null || locationString.isEmpty()) {
            return null;
        }

        String[] parts = locationString.split(",");
        if (parts.length < 4) {
            return null;
        }

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Método auxiliar para convertir una Location a un string con formato "mundo,x,y,z"
     * Útil para serialización/deserialización
     */
    public static String locationToString(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }

        return location.getWorld().getName() + "," +
                location.getX() + "," +
                location.getY() + "," +
                location.getZ();
    }
}