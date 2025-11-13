package com.bubbledev.exocladdon;

import fr.skytasul.glowingentities.GlowingEntities;
import me.loving11ish.clans.api.ClansLiteAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private static Main instance;
    private ClansLiteAPI clansAPI;
    private GlowingEntities glowing;
    private GlowManager glowManager;
    private AddonSettings settings;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        settings = new AddonSettings(getConfig());

        // Ensure ClansLite exists and fetch API via services manager (per official wiki)
        if (!Bukkit.getPluginManager().isPluginEnabled("ClansLite")) {
            getLogger().severe("ClansLite not found. Disabling features.");
        }
        RegisteredServiceProvider<ClansLiteAPI> rsp = Bukkit.getServicesManager().getRegistration(ClansLiteAPI.class);
        if (rsp != null) {
            clansAPI = rsp.getProvider();
        } else {
            getLogger().severe("ClansLiteAPI service not registered. Features will be disabled.");
        }

        // Init GlowingEntities (per README: must be after a world is loaded - default POSTWORLD load is fine)
        glowing = new GlowingEntities(this);

        // Manager
        glowManager = new GlowManager(this, settings, clansAPI, glowing);
        glowManager.loadToggles();

        // Listeners & command
        Bukkit.getPluginManager().registerEvents(new GlowCommandInterceptor(settings, glowManager), this);
        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(glowManager), this);
        getCommand("clanglow").setExecutor((sender, cmd, lbl, args) -> {
            String commandUsed = (lbl == null || lbl.isBlank()) ? cmd.getName() : lbl;
            return glowManager.handleToggleCommand(sender, commandUsed, args);
        });

        getLogger().info("ExoCLAddon enabled.");
    }

    @Override
    public void onDisable() {
        if (glowManager != null) {
            glowManager.clearAllViewers();
            glowManager.saveToggles();
        }
        if (glowing != null) {
            glowing.disable();
        }
        instance = null;
    }

    public static Main getInstance() {
        return instance;
    }

    public FileConfiguration cfg() {
        return getConfig();
    }

    public AddonSettings settings() {
        return settings;
    }
}
