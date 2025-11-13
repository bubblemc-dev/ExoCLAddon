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
    private final String defaultCommandAlias;
    private final String glowSubcommand;
    private final String glowSubcommandDisplay;
    private final Map<ToggleAction, KeywordGroup> toggleKeywords;
    private final String toggleUsageHint;
    private final String permission;
    private final ChatColor glowColor;
    private final String usageTemplateRaw;
    private final String playerOnlyMessageRaw;
    private final String noPermissionMessageRaw;

    public AddonSettings(FileConfiguration configuration) {
        KeywordGroup baseAliases = readAliases(configuration.getStringList("command.base-aliases"), "clan");
        this.baseCommandAliases = baseAliases.unmodNormalized;
        this.defaultCommandAlias = baseAliases.primaryOr("clan");

        String glowSubRaw = configuration.getString("command.glow-subcommand", "glow");
        if (glowSubRaw == null || glowSubRaw.isBlank()) {
            glowSubRaw = "glow";
        }
        this.glowSubcommand = glowSubRaw.toLowerCase(Locale.ROOT);
        this.glowSubcommandDisplay = glowSubRaw.trim();
        this.permission = configuration.getString("command.permission", "exocladdon.glow");
        this.toggleKeywords = readToggleKeywords(configuration);
        this.toggleUsageHint = buildToggleUsageHint();
        this.glowColor = parseColor(configuration.getString("glow.color", "GREEN"));
        this.usageTemplateRaw = configuration.getString("messages.usage", "&7Использование: &f/{command} [{options}]");
        this.playerOnlyMessageRaw = configuration.getString("messages.player-only", "&cКоманда доступна только игрокам.");
        this.noPermissionMessageRaw = configuration.getString("messages.no-permission", "&cНедостаточно прав.");
    }

    public Set<String> getBaseCommandAliases() {
        return baseCommandAliases;
    }

    public String getDefaultCommandAlias() {
        return defaultCommandAlias;
    }

    public String getGlowSubcommand() {
        return glowSubcommand;
    }

    public String getGlowSubcommandDisplay() {
        return glowSubcommandDisplay;
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
        for (Map.Entry<ToggleAction, KeywordGroup> entry : toggleKeywords.entrySet()) {
            if (entry.getValue().normalized.contains(lowered)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String formatUsageMessage(String usedCommand) {
        String command = usedCommand;
        if (command == null || command.isBlank()) {
            command = getDefaultCommandAlias() + " " + getGlowSubcommandDisplay();
        }
        command = command.startsWith("/") ? command : "/" + command;
        String formatted = usageTemplateRaw.replace("{command}", command)
                .replace("{options}", toggleUsageHint);
        return colorize(formatted);
    }

    public String playerOnlyMessage() {
        return colorize(playerOnlyMessageRaw);
    }

    public String noPermissionMessage() {
        return colorize(noPermissionMessageRaw);
    }

    public Set<String> getToggleKeywords(ToggleAction action) {
        KeywordGroup group = toggleKeywords.get(action);
        if (group == null) {
            return Collections.emptySet();
        }
        return group.unmodNormalized;
    }

    public String getToggleUsageHint() {
        return toggleUsageHint;
    }

    private Map<ToggleAction, KeywordGroup> readToggleKeywords(FileConfiguration configuration) {
        Map<ToggleAction, KeywordGroup> map = new EnumMap<>(ToggleAction.class);
        map.put(ToggleAction.ENABLE, readAliases(configuration.getStringList("command.toggle-aliases.on"), "on", "enable", "true"));
        map.put(ToggleAction.DISABLE, readAliases(configuration.getStringList("command.toggle-aliases.off"), "off", "disable", "false"));
        map.put(ToggleAction.TOGGLE, readAliases(configuration.getStringList("command.toggle-aliases.toggle"), "toggle", "switch"));
        return map;
    }

    private KeywordGroup readAliases(List<String> values, String... defaults) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        ArrayList<String> display = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value == null) continue;
                String trimmed = value.trim();
                if (trimmed.isEmpty()) continue;
                String lowered = trimmed.toLowerCase(Locale.ROOT);
                if (normalized.add(lowered)) {
                    display.add(trimmed);
                }
            }
        }
        if (display.isEmpty()) {
            for (String def : defaults) {
                if (def == null) continue;
                String trimmed = def.trim();
                if (trimmed.isEmpty()) continue;
                String lowered = trimmed.toLowerCase(Locale.ROOT);
                if (normalized.add(lowered)) {
                    display.add(trimmed);
                }
            }
        }
        return new KeywordGroup(normalized, display);
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

    private String buildToggleUsageHint() {
        List<String> options = new ArrayList<>();
        addPrimary(options, ToggleAction.ENABLE, "on");
        addPrimary(options, ToggleAction.DISABLE, "off");
        addPrimary(options, ToggleAction.TOGGLE, "toggle");
        return String.join("|", options);
    }

    private void addPrimary(List<String> options, ToggleAction action, String fallback) {
        KeywordGroup group = toggleKeywords.get(action);
        String primary = group != null ? group.primaryOr(fallback) : fallback;
        if (primary != null && !primary.isBlank()) {
            options.add(primary);
        }
    }

    private static final class KeywordGroup {
        private final LinkedHashSet<String> normalized;
        private final List<String> display;
        private final Set<String> unmodNormalized;

        private KeywordGroup(LinkedHashSet<String> normalized, List<String> display) {
            this.normalized = normalized;
            this.display = display;
            this.unmodNormalized = Collections.unmodifiableSet(normalized);
        }

        private String primaryOr(String fallback) {
            if (!display.isEmpty()) {
                return display.get(0);
            }
            return fallback;
        }
    }
}
