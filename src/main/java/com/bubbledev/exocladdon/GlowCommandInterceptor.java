package com.bubbledev.exocladdon;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class GlowCommandInterceptor implements Listener {

    private final ExoCLAddonPlugin plugin;
    private final GlowManager manager;

    public GlowCommandInterceptor(ExoCLAddonPlugin plugin, GlowManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().trim();
        // Accept: /clan glow [on|off|toggle]
        if (!msg.toLowerCase().startsWith("/clan ")) return;
        String[] parts = msg.split("\s+");
        if (parts.length >= 2 && parts[1].equalsIgnoreCase("glow")) {
            event.setCancelled(true); // intercept only our subcommand
            Player p = event.getPlayer();
            String[] args = new String[parts.length - 2];
            for (int i = 2; i < parts.length; i++) args[i-2] = parts[i];
            manager.handleToggleCommand(p, args);
        }
    }
}
