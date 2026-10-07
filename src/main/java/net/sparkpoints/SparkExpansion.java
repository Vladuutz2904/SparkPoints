package net.sparkpoints;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.List;

/**
 * Placeholdere (folosibile in DeluxeMenus, scoreboard, tab etc.):
 *
 *  %sparkpoints_points%              -> 1250
 *  %sparkpoints_points_formatted%    -> 1.250
 *  %sparkpoints_top_1_name%          -> numele de pe locul 1
 *  %sparkpoints_top_1_points%        -> 1250
 *  %sparkpoints_top_1_points_formatted%
 */
public class SparkExpansion extends PlaceholderExpansion {

    private final SparkPoints plugin;

    public SparkExpansion(SparkPoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "sparkpoints";
    }

    @Override
    public String getAuthor() {
        return "Spark";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        PointsManager m = plugin.getManager();
        String p = params.toLowerCase();

        if (p.equals("points") || p.equals("balance")) {
            return player == null ? "0" : String.valueOf(m.get(player.getUniqueId()));
        }
        if (p.equals("points_formatted") || p.equals("balance_formatted")) {
            return player == null ? "0" : m.format(m.get(player.getUniqueId()));
        }

        // top_<pozitie>_<name|points|points_formatted>
        if (p.startsWith("top_")) {
            String[] parts = p.split("_", 3);
            if (parts.length < 3) return null;
            int pos;
            try {
                pos = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return null;
            }
            List<PointsManager.TopEntry> top = m.getTop();
            boolean exists = pos >= 1 && pos <= top.size();
            switch (parts[2]) {
                case "name":
                    return exists ? top.get(pos - 1).name : "-";
                case "points":
                    return exists ? String.valueOf(top.get(pos - 1).points) : "0";
                case "points_formatted":
                    return exists ? m.format(top.get(pos - 1).points) : "0";
                default:
                    return null;
            }
        }
        return null;
    }
}
