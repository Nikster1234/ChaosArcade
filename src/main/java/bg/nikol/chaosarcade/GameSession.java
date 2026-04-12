package bg.nikol.chaosarcade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

public abstract class GameSession {
    protected final ChaosArcadePlugin plugin;
    protected final GameManager manager;
    protected final ArenaDefinition arena;
    protected final GameMode mode;
    protected final Set<UUID> activePlayers = new LinkedHashSet<>();
    protected final Random random = new Random();
    private final List<BukkitTask> trackedTasks = new ArrayList<>();
    private boolean running;

    protected GameSession(ChaosArcadePlugin plugin, GameManager manager, ArenaDefinition arena, GameMode mode, Collection<UUID> players) {
        this.plugin = plugin;
        this.manager = manager;
        this.arena = arena;
        this.mode = mode;
        this.activePlayers.addAll(players);
    }

    public final void start() {
        if (running) {
            return;
        }
        running = true;
        onStart();
    }

    public final void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        for (BukkitTask task : trackedTasks) {
            task.cancel();
        }
        trackedTasks.clear();
        onStop();
    }

    protected abstract void onStart();

    protected void onStop() {
    }

    public GameMode getMode() {
        return mode;
    }

    public String getArenaId() {
        return arena.getId();
    }

    public boolean isActive(UUID uniqueId) {
        return activePlayers.contains(uniqueId);
    }

    public Set<UUID> getActivePlayers() {
        return Set.copyOf(activePlayers);
    }

    protected BukkitTask repeat(long delayTicks, long periodTicks, Runnable runnable) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        trackedTasks.add(task);
        return task;
    }

    protected BukkitTask later(long delayTicks, Runnable runnable) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        trackedTasks.add(task);
        return task;
    }

    protected List<Player> getPlayers() {
        List<Player> players = new ArrayList<>();
        for (UUID uniqueId : activePlayers) {
            Player player = Bukkit.getPlayer(uniqueId);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    protected Player getPlayer(UUID uniqueId) {
        return Bukkit.getPlayer(uniqueId);
    }

    protected int getAliveCount() {
        return activePlayers.size();
    }

    protected void preparePlayer(Player player, Location spawn) {
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setExtraContents(null);
        player.getInventory().setItemInOffHand(null);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        heal(player);
        player.teleport(spawn.clone());
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
    }

    protected void heal(Player player) {
        double maxHealth = 20.0D;
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        }
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
    }

    protected void giveItems(Player player, ItemStack... items) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setExtraContents(null);
        player.getInventory().setItemInOffHand(null);
        for (ItemStack item : items) {
            if (item != null) {
                player.getInventory().addItem(item);
            }
        }
        player.updateInventory();
    }

    protected Location spawnForIndex(int index) {
        Location spawn = arena.getSpawnFor(index);
        if (spawn == null) {
            throw new IllegalStateException("Arena " + arena.getId() + " has no spawns.");
        }
        return spawn;
    }

    protected void eliminate(Player player, String message) {
        if (!activePlayers.remove(player.getUniqueId())) {
            return;
        }
        manager.restorePlayer(player, message);
    }

    protected void broadcast(String message) {
        for (Player player : getPlayers()) {
            player.sendMessage(plugin.prefix() + message);
        }
    }

    protected void actionBar(String message) {
        for (Player player : getPlayers()) {
            player.sendActionBar(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    protected void finish(String result) {
        manager.finishSession(this, result);
    }

    protected void finishWithWinner(Player winner, String suffix) {
        if (winner == null) {
            finish(ChatColor.YELLOW + "Match ended without a winner.");
            return;
        }
        finish(ChatColor.GOLD + winner.getName() + ChatColor.YELLOW + " " + suffix);
    }

    protected void checkForLastPlayer(String suffix) {
        if (activePlayers.size() == 1) {
            Player winner = getPlayer(activePlayers.iterator().next());
            finishWithWinner(winner, suffix);
        } else if (activePlayers.isEmpty()) {
            finish(ChatColor.YELLOW + "Nobody survived the match.");
        }
    }

    protected boolean isLethal(Player player, double damage) {
        return player.getHealth() - damage <= 0.0D;
    }

    protected void respawnInArena(Player player) {
        heal(player);
        player.teleport(spawnForIndex(random.nextInt(Math.max(1, activePlayers.size()))));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.3F);
    }

    public void handleLeave(Player player) {
        if (!isActive(player.getUniqueId())) {
            return;
        }
        eliminate(player, ChatColor.RED + "You left the match.");
        checkForLastPlayer("wins the match.");
    }

    public void handleQuit(Player player) {
        if (!isActive(player.getUniqueId())) {
            return;
        }
        eliminate(player, ChatColor.RED + player.getName() + " left the server.");
        checkForLastPlayer("wins the match.");
    }

    public void onMove(PlayerMoveEvent event) {
    }

    public void onDamage(EntityDamageEvent event) {
    }

    public void onDamageByEntity(EntityDamageByEntityEvent event) {
    }

    public boolean allowsDirectCombat() {
        return false;
    }

    protected Player resolveAttacker(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    public void onBlockBreak(BlockBreakEvent event) {
    }

    public void onBlockPlace(BlockPlaceEvent event) {
    }

    public void onInteract(PlayerInteractEvent event) {
    }

    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && isActive(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    public void onDrop(PlayerDropItemEvent event) {
        if (isActive(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    public void onToggleFlight(PlayerToggleFlightEvent event) {
    }
}
