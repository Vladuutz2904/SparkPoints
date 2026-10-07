package net.sparkpoints;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PointsManager {

    public static class TopEntry {
        public final UUID uuid;
        public final String name;
        public final long points;

        TopEntry(UUID uuid, String name, long points) {
            this.uuid = uuid;
            this.name = name;
            this.points = points;
        }
    }

    private final SparkPoints plugin;
    private final File file;
    private final Map<UUID, Long> points = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;

    private List<TopEntry> topCache = new ArrayList<>();
    private long topCacheTime = 0;

    public PointsManager(SparkPoints plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    // ---------- stocare ----------

    public void load() {
        points.clear();
        names.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yml.getConfigurationSection("players");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                points.put(id, sec.getLong(key + ".points", 0));
                names.put(id, sec.getString(key + ".name", "?"));
            } catch (IllegalArgumentException ignored) {
            }
        }
        plugin.getLogger().info("Incarcate datele pentru " + points.size() + " jucatori.");
    }

    public synchronized void save() {
        if (!dirty) return;
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : points.entrySet()) {
            String base = "players." + e.getKey();
            yml.set(base + ".name", names.getOrDefault(e.getKey(), "?"));
            yml.set(base + ".points", e.getValue());
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("Nu am putut salva data.yml: " + ex.getMessage());
        }
    }

    // ---------- API ----------

    public boolean hasAccount(UUID id) {
        return points.containsKey(id);
    }

    public long get(UUID id) {
        Long v = points.get(id);
        return v != null ? v : plugin.getConfig().getLong("starting-points", 0);
    }

    public void set(UUID id, long amount) {
        points.put(id, Math.max(0, amount));
        dirty = true;
    }

    public long add(UUID id, long amount) {
        long now = safeAdd(get(id), amount);
        set(id, now);
        return now;
    }

    /** Returneaza false daca jucatorul nu are destule puncte. */
    public boolean take(UUID id, long amount) {
        long cur = get(id);
        if (cur < amount) return false;
        set(id, cur - amount);
        return true;
    }

    public boolean has(UUID id, long amount) {
        return get(id) >= amount;
    }

    public void touch(UUID id, String name) {
        names.put(id, name);
        if (!points.containsKey(id)) {
            points.put(id, plugin.getConfig().getLong("starting-points", 0));
        }
        dirty = true;
    }

    public String getName(UUID id) {
        return names.getOrDefault(id, "?");
    }

    /** Cauta un UUID dupa nume printre jucatorii cunoscuti (case-insensitive). */
    public UUID findByName(String name) {
        for (Map.Entry<UUID, String> e : names.entrySet()) {
            if (e.getValue().equalsIgnoreCase(name)) return e.getKey();
        }
        return null;
    }

    private long safeAdd(long a, long b) {
        long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) return Long.MAX_VALUE;
        return r;
    }

    // ---------- formatare / top ----------

    public String format(long value) {
        String sep = plugin.getConfig().getString("number-separator", ".");
        return String.format(java.util.Locale.US, "%,d", value).replace(",", sep);
    }

    public synchronized List<TopEntry> getTop() {
        long ttl = plugin.getConfig().getLong("top-cache-seconds", 10) * 1000L;
        long now = System.currentTimeMillis();
        if (now - topCacheTime < ttl && !topCache.isEmpty()) return topCache;

        List<TopEntry> list = new ArrayList<>();
        for (Map.Entry<UUID, Long> e : points.entrySet()) {
            if (e.getValue() > 0) {
                list.add(new TopEntry(e.getKey(), names.getOrDefault(e.getKey(), "?"), e.getValue()));
            }
        }
        list.sort(Comparator.comparingLong((TopEntry t) -> t.points).reversed());
        topCache = list;
        topCacheTime = now;
        return list;
    }
}
