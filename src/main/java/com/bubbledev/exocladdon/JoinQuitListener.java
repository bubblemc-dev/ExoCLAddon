package com.bubbledev.exocladdon;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitListener implements Listener {

    private final GlowManager manager;
    public JoinQuitListener(GlowManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Reapply for the viewer himself (if they toggled previously)
        manager.reapplyFor(e.getPlayer());
        // Also make the joined player glow for online viewers in same clan
        manager.onPlayerJoin(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.onPlayerQuit(e.getPlayer());
    }
}
