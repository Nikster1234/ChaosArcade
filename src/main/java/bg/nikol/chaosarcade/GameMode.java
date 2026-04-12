package bg.nikol.chaosarcade;

import java.util.Arrays;
import java.util.Locale;
import org.bukkit.Material;

public enum GameMode {
    TNT_TAG("TNT Tag", Material.TNT, 10, 2, 12, "Pass the TNT before the timer explodes."),
    BLOCK_SHUFFLE("Block Shuffle", Material.GRASS_BLOCK, 11, 2, 16, "Stand on the shown block or get eliminated."),
    KING_OF_THE_HILL("King of the Hill", Material.BEACON, 12, 2, 16, "Control the hill and score over time."),
    INFECTION("Infection", Material.ROTTEN_FLESH, 13, 2, 20, "Infected players convert the survivors."),
    COLLAPSE_ARENA("Collapse Arena", Material.SAND, 14, 2, 16, "Blocks disappear behind every step."),
    MINING_RUSH("Mining Rush", Material.DIAMOND_PICKAXE, 15, 2, 16, "Mine ores fast and stack points."),
    ABILITY_BRAWL("Ability Brawl", Material.BLAZE_ROD, 16, 2, 16, "Fight with shuffled special abilities.");

    private final String displayName;
    private final Material icon;
    private final int menuSlot;
    private final int minPlayers;
    private final int maxPlayers;
    private final String description;

    GameMode(String displayName, Material icon, int menuSlot, int minPlayers, int maxPlayers, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.menuSlot = menuSlot;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getMenuSlot() {
        return menuSlot;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getDescription() {
        return description;
    }

    public String getKey() {
        return name();
    }

    public String getCommandKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static GameMode fromInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String normalized = input.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return Arrays.stream(values())
                .filter(mode -> mode.name().equals(normalized))
                .findFirst()
                .orElse(null);
    }
}
