package bg.nikol.chaosarcade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class ArcadeCommand implements CommandExecutor, TabCompleter {
    private final ChaosArcadePlugin plugin;
    private final GameManager manager;
    private final ArenaRepository arenaRepository;

    public ArcadeCommand(ChaosArcadePlugin plugin, GameManager manager, ArenaRepository arenaRepository) {
        this.plugin = plugin;
        this.manager = manager;
        this.arenaRepository = arenaRepository;
    }

    public void register() {
        PluginCommand command = plugin.getCommand("arcade");
        if (command == null) {
            throw new IllegalStateException("Command arcade missing from plugin.yml");
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sendHelp(sender);
                return true;
            }
            manager.openMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(sender);
            case "join" -> handleJoin(sender, args);
            case "leave" -> handleLeave(sender);
            case "queue", "status" -> manager.sendStatus(sender);
            default -> {
                if (!sender.hasPermission("chaosarcade.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Unknown command.");
                    return true;
                }
                handleAdmin(sender, args);
            }
        }

        return true;
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can join queues.");
            return;
        }
        if (!sender.hasPermission("chaosarcade.use")) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /arcade join <mode>");
            return;
        }
        GameMode mode = GameMode.fromInput(args[1]);
        if (mode == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Unknown mode.");
            return;
        }
        manager.joinQueue(player, mode);
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can leave queues or matches.");
            return;
        }
        manager.leave(player);
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "setlobby" -> handleSetLobby(sender);
            case "reload" -> manager.reloadFromCommand(sender);
            case "forcestart" -> manager.forceStart(sender);
            case "stop" -> manager.stopActive(sender);
            case "arena" -> handleArena(sender, args);
            default -> sendHelp(sender);
        }
    }

    private void handleSetLobby(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can set the lobby.");
            return;
        }
        arenaRepository.setLobby(player.getLocation());
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Lobby saved.");
    }

    private void handleArena(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /arcade arena <mode> <action> ...");
            return;
        }

        GameMode mode = GameMode.fromInput(args[1]);
        if (mode == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Unknown mode.");
            return;
        }

        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "list" -> handleArenaList(sender, mode);
            case "create" -> handleArenaCreate(sender, mode, args);
            case "delete" -> handleArenaDelete(sender, mode, args);
            case "info" -> handleArenaInfo(sender, mode, args);
            case "setspawn" -> handleArenaSetSpawn(sender, mode, args);
            case "setcenter" -> handleArenaSetCenter(sender, mode, args);
            case "setradius" -> handleArenaSetRadius(sender, mode, args);
            case "setpos1" -> handleArenaSetPos1(sender, mode, args);
            case "setpos2" -> handleArenaSetPos2(sender, mode, args);
            default -> sender.sendMessage(plugin.prefix() + ChatColor.RED + "Unknown arena action.");
        }
    }

    private void handleArenaList(CommandSender sender, GameMode mode) {
        List<String> ids = arenaRepository.getArenaIds(mode);
        if (ids.isEmpty()) {
            sender.sendMessage(plugin.prefix() + ChatColor.GRAY + "No arenas for " + mode.getDisplayName() + ".");
            return;
        }
        sender.sendMessage(plugin.prefix() + ChatColor.GOLD + mode.getDisplayName() + ChatColor.GRAY + " arenas: " + ChatColor.WHITE + String.join(", ", ids));
    }

    private void handleArenaCreate(CommandSender sender, GameMode mode, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /arcade arena <mode> create <id>");
            return;
        }
        if (!arenaRepository.createArena(mode, args[3])) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Could not create arena. It may already exist.");
            return;
        }
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Created arena " + args[3] + " for " + mode.getDisplayName() + ".");
    }

    private void handleArenaDelete(CommandSender sender, GameMode mode, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /arcade arena <mode> delete <id>");
            return;
        }
        if (!arenaRepository.deleteArena(mode, args[3])) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Arena not found.");
            return;
        }
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Deleted arena " + args[3] + ".");
    }

    private void handleArenaInfo(CommandSender sender, GameMode mode, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /arcade arena <mode> info <id>");
            return;
        }
        ArenaDefinition arena = arenaRepository.getArena(mode, args[3]);
        if (arena == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Arena not found.");
            return;
        }

        sender.sendMessage(plugin.prefix() + ChatColor.GOLD + "Arena " + arena.getId() + ChatColor.GRAY + " for " + mode.getDisplayName());
        sender.sendMessage(ChatColor.GRAY + "Spawns: " + ChatColor.WHITE + arena.getSpawns().size());
        sender.sendMessage(ChatColor.GRAY + "Center: " + ChatColor.WHITE + formatLocation(arena.getCenter()));
        sender.sendMessage(ChatColor.GRAY + "Radius: " + ChatColor.WHITE + arena.getRadius());
        sender.sendMessage(ChatColor.GRAY + "Pos1: " + ChatColor.WHITE + formatLocation(arena.getPos1()));
        sender.sendMessage(ChatColor.GRAY + "Pos2: " + ChatColor.WHITE + formatLocation(arena.getPos2()));

        if (arena.getProblems().isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "Arena is ready.");
            return;
        }

        sender.sendMessage(ChatColor.RED + "Missing:");
        for (String problem : arena.getProblems()) {
            sender.sendMessage(ChatColor.RED + "- " + problem);
        }
    }

    private void handleArenaSetSpawn(CommandSender sender, GameMode mode, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can set spawns.");
            return;
        }
        if (args.length < 5) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /arcade arena <mode> setspawn <id> <index>");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(args[4]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Spawn index must be a number.");
            return;
        }

        if (!arenaRepository.setSpawn(mode, args[3], index, player.getLocation())) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Could not save spawn.");
            return;
        }
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Saved spawn " + index + " for arena " + args[3] + ".");
    }

    private void handleArenaSetCenter(CommandSender sender, GameMode mode, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can set centers.");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /arcade arena <mode> setcenter <id>");
            return;
        }
        if (!arenaRepository.setCenter(mode, args[3], player.getLocation())) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Could not save center.");
            return;
        }
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Center saved for arena " + args[3] + ".");
    }

    private void handleArenaSetRadius(CommandSender sender, GameMode mode, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /arcade arena <mode> setradius <id> <blocks>");
            return;
        }

        int radius;
        try {
            radius = Integer.parseInt(args[4]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Radius must be a number.");
            return;
        }

        if (!arenaRepository.setRadius(mode, args[3], radius)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Could not save radius.");
            return;
        }
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Radius saved for arena " + args[3] + ".");
    }

    private void handleArenaSetPos1(CommandSender sender, GameMode mode, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can set region positions.");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /arcade arena <mode> setpos1 <id>");
            return;
        }
        if (!arenaRepository.setPos1(mode, args[3], player.getLocation())) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Could not save pos1.");
            return;
        }
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Pos1 saved for arena " + args[3] + ".");
    }

    private void handleArenaSetPos2(CommandSender sender, GameMode mode, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can set region positions.");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /arcade arena <mode> setpos2 <id>");
            return;
        }
        if (!arenaRepository.setPos2(mode, args[3], player.getLocation())) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Could not save pos2.");
            return;
        }
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Pos2 saved for arena " + args[3] + ".");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.prefix() + ChatColor.GOLD + "ChaosArcade commands:");
        sender.sendMessage(ChatColor.YELLOW + "/arcade" + ChatColor.GRAY + " - Open the game selector");
        sender.sendMessage(ChatColor.YELLOW + "/arcade join <mode>");
        sender.sendMessage(ChatColor.YELLOW + "/arcade leave");
        sender.sendMessage(ChatColor.YELLOW + "/arcade queue");

        if (sender.hasPermission("chaosarcade.admin")) {
            sender.sendMessage(ChatColor.GOLD + "Admin:");
            sender.sendMessage(ChatColor.YELLOW + "/arcade setlobby");
            sender.sendMessage(ChatColor.YELLOW + "/arcade reload");
            sender.sendMessage(ChatColor.YELLOW + "/arcade forcestart");
            sender.sendMessage(ChatColor.YELLOW + "/arcade stop");
            sender.sendMessage(ChatColor.YELLOW + "/arcade arena <mode> create <id>");
            sender.sendMessage(ChatColor.YELLOW + "/arcade arena <mode> list");
            sender.sendMessage(ChatColor.YELLOW + "/arcade arena <mode> info <id>");
            sender.sendMessage(ChatColor.YELLOW + "/arcade arena <mode> setspawn <id> <index>");
            sender.sendMessage(ChatColor.YELLOW + "/arcade arena <mode> setcenter <id>");
            sender.sendMessage(ChatColor.YELLOW + "/arcade arena <mode> setradius <id> <blocks>");
            sender.sendMessage(ChatColor.YELLOW + "/arcade arena <mode> setpos1 <id>");
            sender.sendMessage(ChatColor.YELLOW + "/arcade arena <mode> setpos2 <id>");
        }
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "missing";
        }
        return location.getWorld().getName()
                + " "
                + location.getBlockX()
                + ","
                + location.getBlockY()
                + ","
                + location.getBlockZ();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>(List.of("help", "join", "leave", "queue"));
            if (sender.hasPermission("chaosarcade.admin")) {
                base.addAll(List.of("setlobby", "reload", "forcestart", "stop", "arena"));
            }
            return filter(base, args[0]);
        }

        if (args.length == 2 && ("join".equalsIgnoreCase(args[0]) || "arena".equalsIgnoreCase(args[0]))) {
            List<String> modes = new ArrayList<>();
            for (GameMode mode : GameMode.values()) {
                modes.add(mode.getCommandKey());
            }
            return filter(modes, args[1]);
        }

        if (args.length == 3 && "arena".equalsIgnoreCase(args[0])) {
            return filter(List.of("list", "create", "delete", "info", "setspawn", "setcenter", "setradius", "setpos1", "setpos2"), args[2]);
        }

        if (args.length == 4 && "arena".equalsIgnoreCase(args[0])) {
            GameMode mode = GameMode.fromInput(args[1]);
            if (mode == null) {
                return Collections.emptyList();
            }
            if ("create".equalsIgnoreCase(args[2])) {
                return filter(List.of("arena1"), args[3]);
            }
            return filter(arenaRepository.getArenaIds(mode), args[3]);
        }

        if (args.length == 5 && "arena".equalsIgnoreCase(args[0]) && "setspawn".equalsIgnoreCase(args[2])) {
            return filter(List.of("1", "2", "3", "4", "5", "6"), args[4]);
        }

        if (args.length == 5 && "arena".equalsIgnoreCase(args[0]) && "setradius".equalsIgnoreCase(args[2])) {
            return filter(List.of("5", "8", "10", "12"), args[4]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return options;
        }
        String lowered = prefix.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                filtered.add(option);
            }
        }
        return filtered;
    }
}
