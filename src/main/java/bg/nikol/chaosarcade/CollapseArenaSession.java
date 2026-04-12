package bg.nikol.chaosarcade;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class CollapseArenaSession extends GameSession {
    private final CuboidRegion region;
    private final int removeDelayTicks;
    private final Set<String> scheduledBlocks = new HashSet<>();
    private RegionSnapshot snapshot;

    public CollapseArenaSession(ChaosArcadePlugin plugin, GameManager manager, ArenaDefinition arena, List<UUID> players) {
        super(plugin, manager, arena, GameMode.COLLAPSE_ARENA, players);
        this.region = arena.getRegion();
        this.removeDelayTicks = plugin.getConfig().getInt("modes.COLLAPSE_ARENA.remove-delay-ticks", 8);
    }

    @Override
    protected void onStart() {
        snapshot = RegionSnapshot.capture(region);

        int index = 0;
        for (Player player : getPlayers()) {
            preparePlayer(player, spawnForIndex(index++));
        }

        repeat(0L, 10L, () -> {
            actionBar("&ePlayers Left: &f" + getAliveCount());
            for (Player player : getPlayers()) {
                if (player.getLocation().getY() < region.getMinY() - 2) {
                    eliminate(player, ChatColor.RED + "You fell out of the arena.");
                    checkForLastPlayer("wins Collapse Arena.");
                }
            }
        });
    }

    @Override
    protected void onStop() {
        if (snapshot != null) {
            snapshot.restore();
        }
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isActive(player.getUniqueId())) {
            return;
        }
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        scheduleCollapse(event.getFrom().clone().subtract(0.0D, 1.0D, 0.0D).getBlock());
        scheduleCollapse(event.getTo().clone().subtract(0.0D, 1.0D, 0.0D).getBlock());
    }

    private void scheduleCollapse(Block block) {
        if (block.getType() == Material.AIR) {
            return;
        }
        if (!region.contains(block.getLocation())) {
            return;
        }

        String key = key(block);
        if (!scheduledBlocks.add(key)) {
            return;
        }

        later(removeDelayTicks, () -> {
            scheduledBlocks.remove(key);
            if (region.contains(block.getLocation()) && block.getType() != Material.AIR) {
                block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5D, 0.5D, 0.5D), 20, block.getBlockData());
                block.setType(Material.AIR, false);
            }
        });
    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isActive(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            eliminate(player, ChatColor.RED + "You fell into the void.");
            checkForLastPlayer("wins Collapse Arena.");
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        if (isActive(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isActive(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private String key(Block block) {
        return block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
