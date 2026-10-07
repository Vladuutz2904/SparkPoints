package net.sparkpoints;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class SparkPoints extends JavaPlugin implements Listener {

    private static SparkPoints instance;
    private PointsManager manager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        manager = new PointsManager(this);
        manager.load();

        SparkCommand cmd = new SparkCommand(this);
        getCommand("sparkpoints").setExecutor(cmd);
        getCommand("sparkpoints").setTabCompleter(cmd);

        Bukkit.getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SparkExpansion(this).register();
            getLogger().info("PlaceholderAPI gasit - placeholderele %sparkpoints_*% sunt active.");
        } else {
            getLogger().warning("PlaceholderAPI nu e instalat - placeholderele pentru DeluxeMenus nu vor merge.");
        }

        long period = Math.max(10, getConfig().getLong("autosave-seconds", 300)) * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> manager.save(), period, period);
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.save();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        manager.touch(e.getPlayer().getUniqueId(), e.getPlayer().getName());
    }

    /** API pentru alte plugin-uri: SparkPoints.getInstance().getManager() */
    public static SparkPoints getInstance() {
        return instance;
    }

    public PointsManager getManager() {
        return manager;
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Ia un mesaj din config; pairs = "{cheie}", "valoare", ... */
    public String msg(String key, String... pairs) {
        FileConfiguration c = getConfig();
        String s = c.getString("messages." + key, "&cMesaj lipsa: " + key);
        s = s.replace("{prefix}", c.getString("messages.prefix", ""));
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            s = s.replace(pairs[i], pairs[i + 1]);
        }
        return color(s);
    }

    public List<String> msgList(String key) {
        return getConfig().getStringList("messages." + key).stream()
                .map(this::color).collect(Collectors.toList());
    }
}
