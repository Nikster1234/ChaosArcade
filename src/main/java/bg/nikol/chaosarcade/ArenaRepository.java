package bg.nikol.chaosarcade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class ArenaRepository {
    private final ChaosArcadePlugin plugin;
    private final Map<GameMode, Map<String, ArenaDefinition>> arenas = new EnumMap<>(GameMode.class);

    public ArenaRepository(ChaosArcadePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        arenas.clear();
        for (GameMode mode : GameMode.values()) {
            arenas.put(mode, new LinkedHashMap<>());
        }

        ConfigurationSection arenasSection = plugin.getConfig().getConfigurationSection("arenas");
        if (arenasSection == null) {
            return;
        }

        for (String modeKey : arenasSection.getKeys(false)) {
            GameMode mode = GameMode.fromInput(modeKey);
            if (mode == null) {
                continue;
            }

            ConfigurationSection modeSection = arenasSection.getConfigurationSection(modeKey);
            if (modeSection == null) {
                continue;
            }

            for (String id : modeSection.getKeys(false)) {
                ConfigurationSection arenaSection = modeSection.getConfigurationSection(id);
                if (arenaSection == null) {
                    continue;
                }
                arenas.get(mode).put(id.toLowerCase(Locale.ROOT), ArenaDefinition.fromConfig(mode, id, arenaSection));
            }
        }
    }

    public Location getLobby() {
        Location location = plugin.getConfig().getLocation("lobby");
        return location == null ? null : location.clone();
    }

    public void setLobby(Location location) {
        plugin.getConfig().set("lobby", location);
        saveAndReload();
    }

    public List<ArenaDefinition> getArenas(GameMode mode) {
        return new ArrayList<>(arenas.getOrDefault(mode, Collections.emptyMap()).values());
    }

    public List<ArenaDefinition> getReadyArenas(GameMode mode) {
        List<ArenaDefinition> ready = new ArrayList<>();
        for (ArenaDefinition arena : getArenas(mode)) {
            if (arena.getProblems().isEmpty()) {
                ready.add(arena);
            }
        }
        return ready;
    }

    public ArenaDefinition getRandomReadyArena(GameMode mode, Random random) {
        List<ArenaDefinition> ready = getReadyArenas(mode);
        if (ready.isEmpty()) {
            return null;
        }
        return ready.get(random.nextInt(ready.size()));
    }

    public ArenaDefinition getArena(GameMode mode, String id) {
        if (mode == null || id == null) {
            return null;
        }
        return arenas.getOrDefault(mode, Collections.emptyMap()).get(id.toLowerCase(Locale.ROOT));
    }

    public List<String> getArenaIds(GameMode mode) {
        return new ArrayList<>(arenas.getOrDefault(mode, Collections.emptyMap()).keySet());
    }

    public boolean createArena(GameMode mode, String id) {
        if (mode == null || id == null || id.isBlank() || getArena(mode, id) != null) {
            return false;
        }
        String path = arenaPath(mode, id);
        plugin.getConfig().createSection(path);
        plugin.getConfig().set(path + ".radius", 5);
        saveAndReload();
        return true;
    }

    public boolean deleteArena(GameMode mode, String id) {
        if (getArena(mode, id) == null) {
            return false;
        }
        plugin.getConfig().set(arenaPath(mode, id), null);
        saveAndReload();
        return true;
    }

    public boolean setSpawn(GameMode mode, String id, int index, Location location) {
        if (getArena(mode, id) == null || index <= 0) {
            return false;
        }
        plugin.getConfig().set(arenaPath(mode, id) + ".spawns." + index, location);
        saveAndReload();
        return true;
    }

    public boolean setCenter(GameMode mode, String id, Location location) {
        if (getArena(mode, id) == null) {
            return false;
        }
        plugin.getConfig().set(arenaPath(mode, id) + ".center", location);
        saveAndReload();
        return true;
    }

    public boolean setPos1(GameMode mode, String id, Location location) {
        if (getArena(mode, id) == null) {
            return false;
        }
        plugin.getConfig().set(arenaPath(mode, id) + ".pos1", location);
        saveAndReload();
        return true;
    }

    public boolean setPos2(GameMode mode, String id, Location location) {
        if (getArena(mode, id) == null) {
            return false;
        }
        plugin.getConfig().set(arenaPath(mode, id) + ".pos2", location);
        saveAndReload();
        return true;
    }

    public boolean setRadius(GameMode mode, String id, int radius) {
        if (getArena(mode, id) == null || radius <= 0) {
            return false;
        }
        plugin.getConfig().set(arenaPath(mode, id) + ".radius", radius);
        saveAndReload();
        return true;
    }

    private String arenaPath(GameMode mode, String id) {
        return "arenas." + mode.getKey() + "." + id.toLowerCase(Locale.ROOT);
    }

    private void saveAndReload() {
        plugin.saveConfig();
        reload();
    }
}
