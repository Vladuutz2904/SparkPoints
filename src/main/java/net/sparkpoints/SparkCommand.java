package net.sparkpoints;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SparkCommand implements CommandExecutor, TabCompleter {

    private final SparkPoints plugin;

    public SparkCommand(SparkPoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PointsManager m = plugin.getManager();

        if (command.getName().equalsIgnoreCase("spforall")) {
            return forAll(sender, args);
        }

        if (args.length == 0) {
            return balance(sender, null);
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help":
            case "ajutor":
                for (String line : plugin.msgList("help")) sender.sendMessage(line);
                sender.sendMessage(plugin.color("&8Plugin facut de &6Vladuutz &8- &7/sp credits"));
                return true;

            case "credits":
            case "about":
            case "creator":
                credits(sender);
                return true;

            case "balance":
            case "bal":
            case "vezi":
                return balance(sender, args.length > 1 ? args[1] : null);

            case "top":
                if (!sender.hasPermission("sparkpoints.top")) {
                    sender.sendMessage(plugin.msg("no-permission"));
                    return true;
                }
                List<PointsManager.TopEntry> top = m.getTop();
                sender.sendMessage(plugin.msg("top-header"));
                if (top.isEmpty()) {
                    sender.sendMessage(plugin.msg("top-empty"));
                } else {
                    for (int i = 0; i < Math.min(10, top.size()); i++) {
                        PointsManager.TopEntry t = top.get(i);
                        sender.sendMessage(plugin.msg("top-line",
                                "{pos}", String.valueOf(i + 1),
                                "{player}", t.name,
                                "{points}", m.format(t.points)));
                    }
                }
                return true;

            case "pay":
            case "trimite":
                return pay(sender, args);

            case "give":
            case "add":
            case "take":
            case "remove":
            case "set":
                return admin(sender, sub, args);

            case "reload":
                if (!sender.hasPermission("sparkpoints.admin")) {
                    sender.sendMessage(plugin.msg("no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                m.load();
                sender.sendMessage(plugin.msg("reloaded"));
                return true;

            default:
                // /sp <jucator>  -> scurtatura pentru balance
                return balance(sender, args[0]);
        }
    }

    // ---------- /sp [balance] [jucator] ----------

    private boolean balance(CommandSender sender, String targetName) {
        PointsManager m = plugin.getManager();

        if (targetName == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.msg("players-only"));
                return true;
            }
            if (!sender.hasPermission("sparkpoints.use")) {
                sender.sendMessage(plugin.msg("no-permission"));
                return true;
            }
            Player p = (Player) sender;
            sender.sendMessage(plugin.msg("balance-self", "{points}", m.format(m.get(p.getUniqueId()))));
            return true;
        }

        if (!sender.hasPermission("sparkpoints.others")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        UUID id = resolve(targetName);
        if (id == null) {
            sender.sendMessage(plugin.msg("player-not-found", "{player}", targetName));
            return true;
        }
        sender.sendMessage(plugin.msg("balance-other",
                "{player}", m.getName(id),
                "{points}", m.format(m.get(id))));
        return true;
    }

    // ---------- /sp pay <jucator> <suma> ----------

    private boolean pay(CommandSender sender, String[] args) {
        PointsManager m = plugin.getManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.msg("players-only"));
            return true;
        }
        if (!sender.hasPermission("sparkpoints.pay")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.msg("invalid-amount"));
            return true;
        }
        Player p = (Player) sender;
        UUID target = resolve(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found", "{player}", args[1]));
            return true;
        }
        if (target.equals(p.getUniqueId())) {
            sender.sendMessage(plugin.msg("pay-self"));
            return true;
        }
        long amount = parse(args[2]);
        if (amount <= 0) {
            sender.sendMessage(plugin.msg("invalid-amount"));
            return true;
        }
        if (!m.take(p.getUniqueId(), amount)) {
            sender.sendMessage(plugin.msg("pay-not-enough", "{points}", m.format(m.get(p.getUniqueId()))));
            return true;
        }
        m.add(target, amount);

        sender.sendMessage(plugin.msg("pay-sent", "{player}", m.getName(target), "{amount}", m.format(amount)));
        Player online = Bukkit.getPlayer(target);
        if (online != null) {
            online.sendMessage(plugin.msg("pay-received", "{player}", p.getName(), "{amount}", m.format(amount)));
        }
        return true;
    }

    // ---------- /sp give|take|set <jucator> <suma> ----------

    private boolean admin(CommandSender sender, String sub, String[] args) {
        PointsManager m = plugin.getManager();

        if (!sender.hasPermission("sparkpoints.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (args.length < 3) {
            for (String line : plugin.msgList("help")) sender.sendMessage(line);
            return true;
        }
        UUID id = resolve(args[1]);
        if (id == null) {
            sender.sendMessage(plugin.msg("player-not-found", "{player}", args[1]));
            return true;
        }
        long amount = parse(args[2]);
        boolean isSet = sub.equals("set");
        if (amount < 0 || (!isSet && amount == 0)) {
            sender.sendMessage(plugin.msg("invalid-amount"));
            return true;
        }

        boolean notify = plugin.getConfig().getBoolean("notify-target", true);
        Player online = Bukkit.getPlayer(id);
        String name = m.getName(id);

        if (sub.equals("give") || sub.equals("add")) {
            long total = m.add(id, amount);
            sender.sendMessage(plugin.msg("given", "{player}", name, "{amount}", m.format(amount), "{points}", m.format(total)));
            if (notify && online != null)
                online.sendMessage(plugin.msg("target-received", "{amount}", m.format(amount), "{points}", m.format(total)));

        } else if (sub.equals("take") || sub.equals("remove")) {
            if (!m.take(id, amount)) {
                sender.sendMessage(plugin.msg("not-enough-target", "{player}", name, "{points}", m.format(m.get(id))));
                return true;
            }
            long total = m.get(id);
            sender.sendMessage(plugin.msg("taken", "{player}", name, "{amount}", m.format(amount), "{points}", m.format(total)));
            if (notify && online != null)
                online.sendMessage(plugin.msg("target-lost", "{amount}", m.format(amount), "{points}", m.format(total)));

        } else { // set
            m.set(id, amount);
            sender.sendMessage(plugin.msg("set", "{player}", name, "{points}", m.format(amount)));
            if (notify && online != null)
                online.sendMessage(plugin.msg("target-set", "{points}", m.format(amount)));
        }
        return true;
    }

    // ---------- /sp credits ----------

    private void credits(CommandSender sender) {
        sender.sendMessage(plugin.color("&8&m-----&r &6&lSpark&e&lPoints &8&m-----"));
        sender.sendMessage(plugin.color("&7Versiune: &f" + plugin.getDescription().getVersion()));
        sender.sendMessage(plugin.color("&7Creat de: " + SparkPoints.CREATOR));
        sender.sendMessage(plugin.color("&7Discord: &f_vladuu_"));
        sender.sendMessage(plugin.color("&7Designer pentru servere de Minecraft: logouri, site-uri si store-uri."));
        sender.sendMessage(plugin.color("&8&m---------------------------"));
    }

    // ---------- /spforall <suma> ----------

    private boolean forAll(CommandSender sender, String[] args) {
        PointsManager m = plugin.getManager();

        if (!sender.hasPermission("sparkpoints.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.msg("forall-usage"));
            return true;
        }
        long amount = parse(args[0]);
        if (amount <= 0) {
            sender.sendMessage(plugin.msg("invalid-amount"));
            return true;
        }

        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            m.touch(p.getUniqueId(), p.getName());
            long total = m.add(p.getUniqueId(), amount);
            p.sendMessage(plugin.msg("forall-received",
                    "{player}", sender.getName(),
                    "{amount}", m.format(amount),
                    "{points}", m.format(total)));
            count++;
        }
        sender.sendMessage(plugin.msg("forall-sent",
                "{amount}", m.format(amount),
                "{count}", String.valueOf(count)));
        return true;
    }

    // ---------- utilitare ----------

    private long parse(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @SuppressWarnings("deprecation")
    private UUID resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        UUID known = plugin.getManager().findByName(name);
        if (known != null) return known;
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("help", "balance", "top", "pay", "credits"));
            if (sender.hasPermission("sparkpoints.admin")) subs.addAll(Arrays.asList("give", "take", "set", "reload"));
            for (String s : subs) if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2) {
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
        }
        return out;
    }
}
