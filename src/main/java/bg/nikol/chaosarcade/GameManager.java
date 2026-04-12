package bg.nikol.chaosarcade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class GameManager {
    private final ChaosArcadePlugin plugin;
    private final ArenaRepository arenaRepository;
    private final Random random = new Random();
    private final LinkedHashSet<UUID> queuedPlayers = new LinkedHashSet<>();
    private final Map<UUID, PlayerSnapshot> activeSnapshots = new HashMap<>();
    private ArcadeMenu menu;
    private GameMode queuedMode;
    private BukkitTask countdownTask;
    private int countdownRemaining;
    private GameSession activeSession;

    public GameManager(ChaosArcadePlugin plugin, ArenaRepository arenaRepository) {
        this.plugin = plugin;
        this.arenaRepository = arenaRepository;
    }

    public void setMenu(ArcadeMenu menu) {
        this.menu = menu;
    }

    public void openMenu(Player player) {
        if (menu != null) {
            menu.open(player);
        }
    }

    public boolean isQueued(UUID uniqueId) {
        return queuedPlayers.contains(uniqueId);
    }

    public boolean isPlaying(UUID uniqueId) {
        return activeSnapshots.containsKey(uniqueId);
    }

    public GameMode getQueuedMode() {
        return queuedMode;
    }

    public int getQueueSize() {
        return queuedPlayers.size();
    }

    public GameSession getActiveSession() {
        return activeSession;
    }

    public String getModeState(GameMode mode) {
        if (activeSession != null && activeSession.getMode() == mode) {
            return "RUNNING";
        }
        if (queuedMode == mode && !queuedPlayers.isEmpty()) {
            return "QUEUE";
        }
        if (arenaRepository.getReadyArenas(mode).isEmpty()) {
            return "SETUP";
        }
        return "READY";
    }

    public void joinQueue(Player player, GameMode mode) {
        if (!player.hasPermission("chaosarcade.use")) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "You do not have permission.");
            return;
        }

        if (activeSession != null) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "A " + activeSession.getMode().getDisplayName() + " match is already running.");
            return;
        }

        if (arenaRepository.getReadyArenas(mode).isEmpty()) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "This mode has no ready arena yet. Use /arcade arena " + mode.getCommandKey() + " ...");
            return;
        }

        if (queuedMode != null && queuedMode != mode && !queuedPlayers.isEmpty()) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "Queue is already open for " + queuedMode.getDisplayName() + ".");
            return;
        }

        if (queuedPlayers.remove(player.getUniqueId())) {
            if (queuedPlayers.isEmpty()) {
                cancelCountdown();
                queuedMode = null;
            } else if (queuedPlayers.size() < mode.getMinPlayers()) {
                cancelCountdown();
            }
            player.sendMessage(plugin.prefix() + ChatColor.YELLOW + "You left the " + mode.getDisplayName() + " queue.");
            return;
        }

        queuedMode = mode;
        queuedPlayers.add(player.getUniqueId());
        player.sendMessage(plugin.prefix() + ChatColor.GREEN + "You joined the " + mode.getDisplayName() + " queue.");
        broadcastQueue(ChatColor.AQUA + player.getName() + ChatColor.GRAY + " joined the queue "
                + ChatColor.DARK_GRAY + "(" + queuedPlayers.size() + "/" + mode.getMinPlayers() + "+)");

        if (queuedPlayers.size() >= mode.getMinPlayers()) {
            startCountdownIfNeeded();
        }
    }

    public void leave(Player player) {
        if (isQueued(player.getUniqueId())) {
            GameMode mode = queuedMode;
            queuedPlayers.remove(player.getUniqueId());
            player.sendMessage(plugin.prefix() + ChatColor.YELLOW + "You left the queue.");

            if (queuedPlayers.isEmpty()) {
                cancelCountdown();
                queuedMode = null;
            } else if (mode != null && queuedPlayers.size() < mode.getMinPlayers()) {
                cancelCountdown();
            }
            return;
        }

        if (activeSession != null && isPlaying(player.getUniqueId())) {
            activeSession.handleLeave(player);
            return;
        }

        player.sendMessage(plugin.prefix() + ChatColor.RED + "You are not in a queue or match.");
    }

    public void sendStatus(CommandSender sender) {
        if (activeSession != null) {
            sender.sendMessage(plugin.prefix() + ChatColor.GOLD + "Running: "
                    + ChatColor.WHITE + activeSession.getMode().getDisplayName()
                    + ChatColor.GRAY + " on arena "
                    + ChatColor.WHITE + activeSession.getArenaId()
                    + ChatColor.DARK_GRAY + " | "
                    + ChatColor.GRAY + "Players: "
                    + ChatColor.WHITE + activeSession.getActivePlayers().size());
            return;
        }

        if (queuedMode != null && !queuedPlayers.isEmpty()) {
            sender.sendMessage(plugin.prefix() + ChatColor.GOLD + "Queue: "
                    + ChatColor.WHITE + queuedMode.getDisplayName()
                    + ChatColor.DARK_GRAY + " | "
                    + ChatColor.GRAY + "Players: "
                    + ChatColor.WHITE + queuedPlayers.size()
                    + ChatColor.GRAY + " | Countdown: "
                    + ChatColor.WHITE + (countdownTask == null ? "waiting" : countdownRemaining + "s"));
            return;
        }

        sender.sendMessage(plugin.prefix() + ChatColor.GRAY + "No active queue or match.");
    }

    public void forceStart(CommandSender sender) {
        if (activeSession != null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "A match is already running.");
            return;
        }
        if (queuedMode == null || queuedPlayers.isEmpty()) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "No queue is active.");
            return;
        }
        if (queuedPlayers.size() < queuedMode.getMinPlayers()) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Need at least " + queuedMode.getMinPlayers() + " players.");
            return;
        }
        cancelCountdown();
        startMatch();
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Force-started the queue.");
    }

    public void stopActive(CommandSender sender) {
        if (activeSession != null) {
            finishSession(activeSession, ChatColor.RED + "Match was stopped by an admin.");
            if (sender != null) {
                sender.sendMessage(plugin.prefix() + ChatColor.YELLOW + "Stopped the active match.");
            }
            return;
        }
        if (queuedMode != null && !queuedPlayers.isEmpty()) {
            clearQueue(ChatColor.RED + "Queue was stopped by an admin.");
            if (sender != null) {
                sender.sendMessage(plugin.prefix() + ChatColor.YELLOW + "Stopped the active queue.");
            }
            return;
        }
        if (sender != null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Nothing is running.");
        }
    }

    public void reloadFromCommand(CommandSender sender) {
        stopActive(null);
        plugin.reloadConfig();
        arenaRepository.reload();
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "ChaosArcade reloaded.");
    }

    public void handleQuit(Player player) {
        if (isQueued(player.getUniqueId())) {
            queuedPlayers.remove(player.getUniqueId());
            if (queuedPlayers.isEmpty()) {
                cancelCountdown();
                queuedMode = null;
            } else if (queuedMode != null && queuedPlayers.size() < queuedMode.getMinPlayers()) {
                cancelCountdown();
            }
        }

        if (activeSession != null && isPlaying(player.getUniqueId())) {
            activeSession.handleQuit(player);
        }
    }

    public void restorePlayer(Player player, String message) {
        PlayerSnapshot snapshot = activeSnapshots.remove(player.getUniqueId());
        if (snapshot != null) {
            snapshot.restore(player);
            if (plugin.getConfig().getBoolean("settings.send-to-lobby-after-match", false) && arenaRepository.getLobby() != null) {
                player.teleport(arenaRepository.getLobby());
            }
        }
        if (message != null && !message.isBlank()) {
            player.sendMessage(plugin.prefix() + message);
        }
    }

    public void finishSession(GameSession session, String result) {
        if (session == null || activeSession != session) {
            return;
        }

        activeSession.shutdown();

        List<UUID> remaining = new ArrayList<>(activeSnapshots.keySet());
        for (UUID uniqueId : remaining) {
            Player player = Bukkit.getPlayer(uniqueId);
            if (player != null) {
                restorePlayer(player, ChatColor.YELLOW + "Match finished.");
            } else {
                activeSnapshots.remove(uniqueId);
            }
        }

        activeSession = null;

        String finalMessage = plugin.prefix() + result;
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(finalMessage);
        }
    }

    public void shutdown() {
        clearQueue(ChatColor.RED + "Plugin disabled.");
        if (activeSession != null) {
            finishSession(activeSession, ChatColor.RED + "Match stopped because the plugin was disabled.");
        }
    }

    private void startCountdownIfNeeded() {
        if (countdownTask != null || queuedMode == null) {
            return;
        }

        countdownRemaining = plugin.getConfig().getInt("settings.auto-countdown-seconds", 15);
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (queuedMode == null || queuedPlayers.size() < queuedMode.getMinPlayers()) {
                cancelCountdown();
                return;
            }

            if (countdownRemaining <= 0) {
                cancelCountdown();
                startMatch();
                return;
            }

            broadcastQueue(ChatColor.GOLD + queuedMode.getDisplayName()
                    + ChatColor.GRAY + " starts in "
                    + ChatColor.WHITE + countdownRemaining
                    + ChatColor.GRAY + "s.");
            countdownRemaining--;
        }, 0L, 20L);
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private void clearQueue(String message) {
        cancelCountdown();

        for (UUID uniqueId : queuedPlayers) {
            Player player = Bukkit.getPlayer(uniqueId);
            if (player != null) {
                player.sendMessage(plugin.prefix() + message);
            }
        }

        queuedPlayers.clear();
        queuedMode = null;
    }

    private void startMatch() {
        if (queuedMode == null) {
            return;
        }

        ArenaDefinition arena = arenaRepository.getRandomReadyArena(queuedMode, random);
        if (arena == null) {
            clearQueue(ChatColor.RED + "No ready arena exists for " + queuedMode.getDisplayName() + ".");
            return;
        }

        List<Player> participants = new ArrayList<>();
        List<UUID> overflow = new ArrayList<>();
        int count = 0;
        for (UUID uniqueId : queuedPlayers) {
            Player player = Bukkit.getPlayer(uniqueId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (count < queuedMode.getMaxPlayers()) {
                participants.add(player);
                count++;
            } else {
                overflow.add(uniqueId);
            }
        }

        if (participants.size() < queuedMode.getMinPlayers()) {
            clearQueue(ChatColor.RED + "Not enough online players left in the queue.");
            return;
        }

        for (Player player : participants) {
            activeSnapshots.put(player.getUniqueId(), PlayerSnapshot.capture(player));
        }

        for (UUID uniqueId : overflow) {
            Player player = Bukkit.getPlayer(uniqueId);
            if (player != null) {
                player.sendMessage(plugin.prefix() + ChatColor.YELLOW + "Queue was full, you were not moved into this round.");
            }
        }

        GameMode mode = queuedMode;
        queuedPlayers.clear();
        queuedMode = null;

        List<UUID> participantIds = participants.stream().map(Player::getUniqueId).toList();
        activeSession = createSession(mode, arena, participantIds);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(plugin.prefix() + ChatColor.GREEN + "Starting "
                    + mode.getDisplayName()
                    + ChatColor.GRAY + " on arena "
                    + ChatColor.WHITE + arena.getId()
                    + ChatColor.GRAY + " with "
                    + ChatColor.WHITE + participantIds.size()
                    + ChatColor.GRAY + " players.");
        }

        activeSession.start();
    }

    private void broadcastQueue(String message) {
        Set<UUID> snapshot = Set.copyOf(queuedPlayers);
        for (UUID uniqueId : snapshot) {
            Player player = Bukkit.getPlayer(uniqueId);
            if (player != null) {
                player.sendMessage(plugin.prefix() + message);
            }
        }
    }

    private GameSession createSession(GameMode mode, ArenaDefinition arena, List<UUID> players) {
        return switch (mode) {
            case TNT_TAG -> new TntTagSession(plugin, this, arena, players);
            case BLOCK_SHUFFLE -> new BlockShuffleSession(plugin, this, arena, players);
            case KING_OF_THE_HILL -> new KingOfTheHillSession(plugin, this, arena, players);
            case INFECTION -> new InfectionSession(plugin, this, arena, players);
            case COLLAPSE_ARENA -> new CollapseArenaSession(plugin, this, arena, players);
            case MINING_RUSH -> new MiningRushSession(plugin, this, arena, players);
            case ABILITY_BRAWL -> new AbilityBrawlSession(plugin, this, arena, players);
        };
    }
}
