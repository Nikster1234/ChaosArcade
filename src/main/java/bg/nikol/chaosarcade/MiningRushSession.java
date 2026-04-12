package bg.nikol.chaosarcade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class MiningRushSession extends GameSession {
    private final CuboidRegion region;
    private final int durationSeconds;
    private final Map<UUID, Integer> scores = new HashMap<>();
    private RegionSnapshot snapshot;
    private int secondsLeft;

    public MiningRushSession(ChaosArcadePlugin plugin, GameManager manager, ArenaDefinition arena, List<UUID> players) {
        super(plugin, manager, arena, GameMode.MINING_RUSH, players);
        this.region = arena.getRegion();
        this.durationSeconds = plugin.getConfig().getInt("modes.MINING_RUSH.duration-seconds", 120);
    }

    @Override
    protected void onStart() {
        snapshot = RegionSnapshot.capture(region);
        populateMine();
        secondsLeft = durationSeconds;

        int index = 0;
        for (Player player : getPlayers()) {
            preparePlayer(player, spawnForIndex(index++));
            player.getInventory().addItem(createPickaxe());
            scores.put(player.getUniqueId(), 0);
        }

        repeat(0L, 20L, () -> {
            Player leader = getLeader();
            int leaderScore = leader == null ? 0 : scores.getOrDefault(leader.getUniqueId(), 0);

            for (Player player : getPlayers()) {
                player.sendActionBar(ChatColor.GOLD + "Your Score: "
                        + ChatColor.WHITE + scores.getOrDefault(player.getUniqueId(), 0)
                        + ChatColor.DARK_GRAY + " | "
                        + ChatColor.YELLOW + "Leader: "
                        + ChatColor.WHITE + leaderScore
                        + ChatColor.DARK_GRAY + " | "
                        + ChatColor.AQUA + secondsLeft + "s");
            }

            if (secondsLeft <= 0) {
                finishByScore();
                return;
            }

            secondsLeft--;
        });
    }

    @Override
    protected void onStop() {
        if (snapshot != null) {
            snapshot.restore();
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isActive(player.getUniqueId())) {
            return;
        }

        Block block = event.getBlock();
        if (!region.contains(block.getLocation())) {
            event.setCancelled(true);
            return;
        }

        int points = pointsFor(block.getType());
        event.setDropItems(false);
        event.setExpToDrop(0);

        if (points > 0) {
            int updated = scores.getOrDefault(player.getUniqueId(), 0) + points;
            scores.put(player.getUniqueId(), updated);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.2F);
            player.sendActionBar(ChatColor.GREEN + "+" + points + " points");
        }

        Block broken = block;
        later(10L, () -> broken.setType(randomOre(), false));
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isActive(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isActive(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            player.teleport(spawnForIndex(random.nextInt(Math.max(1, getAliveCount()))));
        }
    }

    private void populateMine() {
        region.forEachBlock(block -> block.setType(randomOre(), false));
    }

    private Material randomOre() {
        int roll = random.nextInt(100);
        if (roll < 38) {
            return Material.STONE;
        }
        if (roll < 53) {
            return Material.COAL_ORE;
        }
        if (roll < 66) {
            return Material.COPPER_ORE;
        }
        if (roll < 77) {
            return Material.IRON_ORE;
        }
        if (roll < 85) {
            return Material.REDSTONE_ORE;
        }
        if (roll < 91) {
            return Material.LAPIS_ORE;
        }
        if (roll < 96) {
            return Material.GOLD_ORE;
        }
        if (roll < 99) {
            return Material.EMERALD_ORE;
        }
        return Material.DIAMOND_ORE;
    }

    private int pointsFor(Material material) {
        return switch (material) {
            case COAL_ORE -> 1;
            case COPPER_ORE -> 2;
            case IRON_ORE -> 3;
            case REDSTONE_ORE, LAPIS_ORE -> 4;
            case GOLD_ORE -> 5;
            case EMERALD_ORE -> 7;
            case DIAMOND_ORE -> 10;
            default -> 0;
        };
    }

    private Player getLeader() {
        Player leader = null;
        int best = Integer.MIN_VALUE;
        for (Player player : getPlayers()) {
            int value = scores.getOrDefault(player.getUniqueId(), 0);
            if (value > best) {
                best = value;
                leader = player;
            }
        }
        return leader;
    }

    private void finishByScore() {
        Player winner = getLeader();
        if (winner == null) {
            finish(ChatColor.YELLOW + "Mining Rush ended without a winner.");
            return;
        }
        finish(ChatColor.GOLD + winner.getName()
                + ChatColor.YELLOW + " won Mining Rush with "
                + ChatColor.WHITE + scores.getOrDefault(winner.getUniqueId(), 0)
                + ChatColor.YELLOW + " points.");
    }

    private ItemStack createPickaxe() {
        ItemStack pickaxe = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta meta = pickaxe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Mining Rush Pickaxe");
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(0);
            }
            pickaxe.setItemMeta(meta);
        }
        return pickaxe;
    }
}
