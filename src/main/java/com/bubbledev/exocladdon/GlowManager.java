package com.bubbledev.exocladdon;

import fr.skytasul.glowingentities.GlowingEntities;
import me.loving11ish.clans.api.ClansLiteAPI;
import me.loving11ish.clans.api.models.Clan;
import me.loving11ish.clans.api.models.ClanPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GlowManager {

    private final ExoCLAddonPlugin plugin;
    private final ClansLiteAPI clansAPI;
    private final GlowingEntities glowing;

    // players who enabled viewing glow of their clanmates
    private final Set<UUID> viewers = new HashSet<>();

    private final String META_KEY = "exocladdon_glow_viewer";

    public GlowManager(ExoCLAddonPlugin plugin, ClansLiteAPI clansAPI, GlowingEntities glowing) {
        this.plugin = plugin;
        this.clansAPI = clansAPI;
        this.glowing = glowing;
    }

    /* =====================
       Public API
       ===================== */

    public boolean handleToggleCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Команда только для игроков.");
            return true;
        }
        if (!p.hasPermission("exocladdon.glow")) {
            p.sendMessage("§cНедостаточно прав.");
            return true;
        }
        if (clansAPI == null) {
            p.sendMessage(color(plugin.cfg().getString("messages.no-clanslite")));
            return true;
        }
        boolean targetState;
        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            targetState = !viewers.contains(p.getUniqueId());
        } else if (args[0].equalsIgnoreCase("on")) {
            targetState = true;
        } else if (args[0].equalsIgnoreCase("off")) {
            targetState = false;
        } else {
            p.sendMessage("§7Использование: §f/" + (Objects.equals(sender.getName(), "CONSOLE") ? "clanglow" : "clanglow") + " §7[on|off|toggle]");
            return true;
        }
        if (targetState) {
            enableFor(p);
            p.sendMessage(color(plugin.cfg().getString("messages.toggled-on")));
        } else {
            disableFor(p);
            p.sendMessage(color(plugin.cfg().getString("messages.toggled-off")));
        }
        return true;
    }

    public void enableFor(@NotNull Player viewer) {
        if (clansAPI == null) return;
        Clan clan = clansAPI.getClanByBukkitPlayer(viewer);
        if (clan == null) {
            viewer.sendMessage(color(plugin.cfg().getString("messages.not-in-clan")));
            return;
        }
        viewers.add(viewer.getUniqueId());
        viewer.setMetadata(META_KEY, new FixedMetadataValue(plugin, true));
        // Apply green glow for each online clanmate (excluding self)
        for (Player mate : getOnlineClanmates(clan, viewer)) {
            try {
                glowing.setGlowing(mate, viewer, ChatColor.GREEN);
            } catch (Throwable t) {
                // Fallback: do nothing special
            }
        }
        saveTogglesAsync();
    }

    public void disableFor(@NotNull Player viewer) {
        if (!viewers.remove(viewer.getUniqueId())) return;
        // Remove glow from all currently online clanmates for this viewer
        if (clansAPI != null) {
            Clan clan = clansAPI.getClanByBukkitPlayer(viewer);
            if (clan != null) {
                for (Player mate : getOnlineClanmates(clan, viewer)) {
                    try {
                        glowing.unsetGlowing(mate, viewer);
                    } catch (Throwable ignored) {}
                }
            }
        }
        viewer.removeMetadata(META_KEY, plugin);
        saveTogglesAsync();
    }

    public void reapplyFor(@NotNull Player viewer) {
        if (!viewers.contains(viewer.getUniqueId())) return;
        enableFor(viewer);
    }

    public void onPlayerJoin(@NotNull Player joined) {
        if (clansAPI == null) return;
        // For each viewer toggled ON that shares the same clan, show green glow of the joined player *to that viewer*
        Clan joinedClan = clansAPI.getClanByBukkitPlayer(joined);
        if (joinedClan == null) return;
        Set<UUID> toggled = new HashSet<>(viewers);
        if (toggled.isEmpty()) return;
        for (UUID viewerId : toggled) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null) continue;
            Clan viewerClan = clansAPI.getClanByBukkitPlayer(viewer);
            if (viewerClan == null) continue;
            if (sameClan(joinedClan, viewerClan) && !viewer.equals(joined)) {
                try {
                    glowing.setGlowing(joined, viewer, ChatColor.GREEN);
                } catch (Throwable ignored) {}
            }
        }
    }

    public void onPlayerQuit(@NotNull Player quit) {
        // Remove any per-viewer glow towards quit (optional, GlowingEntities handles removals on disconnect)
        for (UUID viewerId : new HashSet<>(viewers)) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null) {
                try {
                    glowing.unsetGlowing(quit, viewer);
                } catch (Throwable ignored) {}
            }
        }
    }

    public void clearAllViewers() {
        for (UUID viewerId : new HashSet<>(viewers)) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null) continue;
            if (clansAPI != null) {
                Clan clan = clansAPI.getClanByBukkitPlayer(viewer);
                if (clan != null) {
                    for (Player mate : getOnlineClanmates(clan, viewer)) {
                        try {
                            glowing.unsetGlowing(mate, viewer);
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }
        viewers.clear();
    }

    /* =====================
       Helpers
       ===================== */

    private boolean sameClan(Clan a, Clan b) {
        try {
            return Objects.equals(a.getClanFinalName(), b.getClanFinalName());
        } catch (Throwable t) {
            return false;
        }
    }

    private List<Player> getOnlineClanmates(Clan clan, Player exclude) {
        List<Player> out = new ArrayList<>();
        try {
            // The API's getClanMembers() type may vary; handle UUID, ClanPlayer or String gracefully.
            Object rawMembers = clan.getClanMembers();
            if (rawMembers instanceof Iterable<?> it) {
                for (Object o : it) {
                    UUID uuid = null;
                    if (o instanceof UUID u) {
                        uuid = u;
                    } else if (o instanceof ClanPlayer cp) {
                        uuid = cp.getJavaUUID();
                    } else if (o instanceof String s) {
                        try { uuid = UUID.fromString(s); } catch (IllegalArgumentException ex) {
                            // maybe it's a name
                            Player byName = Bukkit.getPlayerExact(s);
                            if (byName != null && byName.isOnline()) {
                                if (!byName.getUniqueId().equals(exclude.getUniqueId())) out.add(byName);
                            }
                            continue;
                        }
                    }
                    if (uuid != null) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline() && !p.getUniqueId().equals(exclude.getUniqueId())) {
                            out.add(p);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return out;
    }

    private String color(String s) {
        if (s == null) return "";
        return s.replace("&", "§");
    }

    /* =====================
       Persistence
       ===================== */

    public void loadToggles() {
        File data = new File(plugin.getDataFolder(), "toggles.yml");
        if (!data.exists()) return;
        try {
            List<String> lines = java.nio.file.Files.readAllLines(data.toPath());
            for (String ln : lines) {
                ln = ln.trim();
                if (ln.isEmpty() || ln.startsWith("#")) continue;
                try {
                    viewers.add(UUID.fromString(ln));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    public void saveToggles() {
        File data = new File(plugin.getDataFolder(), "toggles.yml");
        data.getParentFile().mkdirs();
        try (java.io.PrintWriter pw = new java.io.PrintWriter(data, java.nio.charset.StandardCharsets.UTF_8)) {
            pw.println("# UUID игроков, у которых включена подсветка клана (/clan glow)");
            for (UUID id : viewers) pw.println(id.toString());
        } catch (IOException ignored) {}
    }

    private void saveTogglesAsync() {
        new BukkitRunnable() {
            @Override public void run() { saveToggles(); }
        }.runTaskAsynchronously(plugin);
    }
}
