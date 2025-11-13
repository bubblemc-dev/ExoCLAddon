package com.bubbledev.exocladdon;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.Locale;

public class GlowCommandInterceptor implements Listener {

    private final AddonSettings settings;
    private final GlowManager manager;

    public GlowCommandInterceptor(AddonSettings settings, GlowManager manager) {
        this.settings = settings;
        this.manager = manager;
    }

    @EventHandler
    public void onPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().trim();
        if (!msg.startsWith("/")) return;
        String withoutSlash = msg.substring(1);
        if (withoutSlash.isEmpty()) return;
        String[] parts = withoutSlash.split("\s+");
        if (parts.length < 2) return;

        String base = parts[0].toLowerCase(Locale.ROOT);
        if (!settings.getBaseCommandAliases().contains(base)) return;
        if (!parts[1].equalsIgnoreCase(settings.getGlowSubcommand())) return;

        event.setCancelled(true);
        Player p = event.getPlayer();
        String[] args = Arrays.copyOfRange(parts, 2, parts.length);
        String commandUsed = parts[0] + " " + parts[1];
        manager.handleToggleCommand(p, commandUsed, args);
    }
}
