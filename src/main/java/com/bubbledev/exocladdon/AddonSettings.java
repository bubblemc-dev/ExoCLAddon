package com.bubbledev.exocladdon;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * Encapsulates configurable behaviour of the addon so that command parsing and
 * glow rendering can be customised without touching the code.
 */
public class AddonSettings {

    public enum ToggleAction {
        ENABLE,
        DISABLE,
        TOGGLE
    }

    private final Set<String> baseCommandAliases;
    private final String glowSubcommand;
    private final Map<ToggleAction, Set<String>> toggleKeywords;
    private final String permission;
    private final ChatColor glowColor;
    private final String usageTemplateRaw;
    private final String playerOnlyMessageRaw;
    private final String noPermissionMessageRaw;

    public AddonSettings(FileConfiguration configuration) {
        this.baseCommandAliases = Collections.unmodifiableSet(readAliases(configuration.getStringList("command.base-aliases"), "clan"));
        this.glowSubcommand = configuration.getString("command.glow-subcommand", "glow").toLowerCase(Locale.ROOT);
        this.permission = configuration.getString("command.permission", "exocladdon.glow");
        this.toggleKeywords = readToggleKeywords(configuration);
        this.glowColor = parseColor(configuration.getString("glow.color", "GREEN"));
        this.usageTemplateRaw = configuration.getString("messages.usage", "&7Использование: &f/{command} [on|off|toggle]");
        this.playerOnlyMessageRaw = configuration.getString("messages.player-only", "&cКоманда доступна только игрокам.");
        this.noPermissionMessageRaw = configuration.getString("messages.no-permission", "&cНедостаточно прав.");
    }

    public Set<String> getBaseCommandAliases() {
        return baseCommandAliases;
    }

    public String getDefaultCommandAlias() {
        return baseCommandAliases.iterator().next();
    }

    public String getGlowSubcommand() {
        return glowSubcommand;
    }

    public String getPermission() {
        return permission;
    }

    public ChatColor getGlowColor() {
        return glowColor;
    }

    public ToggleAction parseToggleArgument(String argument) {
        if (argument == null || argument.isBlank()) {
            return ToggleAction.TOGGLE;
        }
        String lowered = argument.toLowerCase(Locale.ROOT);
        for (Map.Entry<ToggleAction, Set<String>> entry : toggleKeywords.entrySet()) {
            if (entry.getValue().contains(lowered)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String formatUsageMessage(String usedCommand) {
        String command = usedCommand;
        if (command == null || command.isBlank()) {
            command = getDefaultCommandAlias() + " " + getGlowSubcommand();
        }
        command = command.startsWith("/") ? command : "/" + command;
        return colorize(usageTemplateRaw.replace("{command}", command));
    }

    public String playerOnlyMessage() {
        return colorize(playerOnlyMessageRaw);
    }

    public String noPermissionMessage() {
        return colorize(noPermissionMessageRaw);
    }

    private Map<ToggleAction, Set<String>> readToggleKeywords(FileConfiguration configuration) {
        Map<ToggleAction, Set<String>> map = new EnumMap<>(ToggleAction.class);
        map.put(ToggleAction.ENABLE, readAliases(configuration.getStringList("command.toggle-aliases.on"), "on", "enable", "true"));
        map.put(ToggleAction.DISABLE, readAliases(configuration.getStringList("command.toggle-aliases.off"), "off", "disable", "false"));
        map.put(ToggleAction.TOGGLE, readAliases(configuration.getStringList("command.toggle-aliases.toggle"), "toggle", "switch"));
        return map;
    }

    private Set<String> readAliases(List<String> values, String... defaults) {
        Set<String> cleaned = new LinkedHashSet<>();
        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                if (value == null) continue;
                String trimmed = value.trim();
                if (trimmed.isEmpty()) continue;
                cleaned.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }
        if (cleaned.isEmpty()) {
            for (String def : defaults) {
                cleaned.add(def.toLowerCase(Locale.ROOT));
            }
        }
        return cleaned;
    }

    private ChatColor parseColor(String name) {
        if (name == null) {
            return ChatColor.GREEN;
        }
        try {
            return ChatColor.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ChatColor.GREEN;
        }
    }

    private String colorize(String value) {
        if (value == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
