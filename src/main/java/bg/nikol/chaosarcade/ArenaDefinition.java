package bg.nikol.chaosarcade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class ArenaDefinition {
    private final GameMode mode;
    private final String id;
    private final List<Location> spawns;
    private final Location center;
    private final Location pos1;
    private final Location pos2;
    private final int radius;

    public ArenaDefinition(GameMode mode, String id, List<Location> spawns, Location center, Location pos1, Location pos2, int radius) {
        this.mode = mode;
        this.id = id;
        this.spawns = new ArrayList<>(spawns);
        this.center = center == null ? null : center.clone();
        this.pos1 = pos1 == null ? null : pos1.clone();
        this.pos2 = pos2 == null ? null : pos2.clone();
        this.radius = radius;
    }

    public static ArenaDefinition fromConfig(GameMode mode, String id, ConfigurationSection section) {
        List<Location> spawns = new ArrayList<>();
        ConfigurationSection spawnsSection = section.getConfigurationSection("spawns");
        if (spawnsSection != null) {
            spawnsSection.getKeys(false).stream()
                    .sorted(Comparator.comparingInt(ArenaDefinition::parseSpawnIndex))
                    .forEach(key -> {
                        Location location = spawnsSection.getLocation(key);
                        if (location != null) {
                            spawns.add(location);
                        }
                    });
        }

        return new ArenaDefinition(
                mode,
                id,
                spawns,
                section.getLocation("center"),
                section.getLocation("pos1"),
                section.getLocation("pos2"),
                Math.max(0, section.getInt("radius")));
    }

    private static int parseSpawnIndex(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }

    public GameMode getMode() {
        return mode;
    }

    public String getId() {
        return id;
    }

    public List<Location> getSpawns() {
        List<Location> clone = new ArrayList<>();
        for (Location location : spawns) {
            clone.add(location.clone());
        }
        return clone;
    }

    public Location getCenter() {
        return center == null ? null : center.clone();
    }

    public Location getPos1() {
        return pos1 == null ? null : pos1.clone();
    }

    public Location getPos2() {
        return pos2 == null ? null : pos2.clone();
    }

    public int getRadius() {
        return radius;
    }

    public Location getSpawnFor(int index) {
        if (spawns.isEmpty()) {
            return null;
        }
        return spawns.get(Math.floorMod(index, spawns.size())).clone();
    }

    public CuboidRegion getRegion() {
        return CuboidRegion.fromLocations(pos1, pos2);
    }

    public List<String> getProblems() {
        List<String> problems = new ArrayList<>();

        if (spawns.size() < mode.getMinPlayers()) {
            problems.add("Needs at least " + mode.getMinPlayers() + " spawns.");
        }

        switch (mode) {
            case KING_OF_THE_HILL -> {
                if (center == null) {
                    problems.add("Center is missing.");
                }
                if (radius <= 0) {
                    problems.add("Radius must be above 0.");
                }
            }
            case COLLAPSE_ARENA, MINING_RUSH -> {
                if (pos1 == null || pos2 == null) {
                    problems.add("Region positions are missing.");
                } else if (getRegion() == null) {
                    problems.add("Region positions must be in the same world.");
                }
            }
            default -> {
            }
        }

        return problems;
    }
}
