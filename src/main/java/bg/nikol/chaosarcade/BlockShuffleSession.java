package bg.nikol.chaosarcade;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BlockShuffleSession extends GameSession {
    private final int roundSeconds;
    private final List<Material> blockPool;
    private int secondsLeft;
    private Material currentTarget;

    public BlockShuffleSession(ChaosArcadePlugin plugin, GameManager manager, ArenaDefinition arena, List<UUID> players) {
        super(plugin, manager, arena, GameMode.BLOCK_SHUFFLE, players);
        this.roundSeconds = plugin.getConfig().getInt("modes.BLOCK_SHUFFLE.round-seconds", 18);
        this.blockPool = loadBlockPool(plugin);
    }

    @Override
    protected void onStart() {
        int index = 0;
        for (Player player : getPlayers()) {
            preparePlayer(player, spawnForIndex(index++));
        }

        chooseNextTarget();

        repeat(0L, 20L, () -> {
            if (getAliveCount() <= 1) {
                checkForLastPlayer("wins Block Shuffle.");
                return;
            }

            actionBar("&aTarget: &f" + readable(currentTarget) + " &8| &e" + secondsLeft + "s");
            if (secondsLeft <= 0) {
                resolveRound();
                if (getAliveCount() <= 1) {
                    checkForLastPlayer("wins Block Shuffle.");
                    return;
                }
                chooseNextTarget();
                return;
            }

            secondsLeft--;
        });
    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isActive(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            eliminate(player, ChatColor.RED + "You fell off the map.");
            checkForLastPlayer("wins Block Shuffle.");
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

    private void chooseNextTarget() {
        currentTarget = blockPool.get(random.nextInt(blockPool.size()));
        secondsLeft = roundSeconds;

        for (Player player : getPlayers()) {
            player.sendTitle(ChatColor.GOLD + readable(currentTarget), ChatColor.YELLOW + "Stand on this block.", 5, 35, 10);
            player.getInventory().setItem(4, targetItem(currentTarget));
        }
    }

    private void resolveRound() {
        List<Player> players = new ArrayList<>(getPlayers());
        for (Player player : players) {
            Material underFeet = player.getLocation().clone().subtract(0.0D, 0.2D, 0.0D).getBlock().getType();
            Material below = player.getLocation().clone().subtract(0.0D, 1.0D, 0.0D).getBlock().getType();
            if (underFeet != currentTarget && below != currentTarget) {
                eliminate(player, ChatColor.RED + "You missed the " + readable(currentTarget) + " block.");
            }
        }

        broadcast(ChatColor.YELLOW + "Round finished. Target was " + ChatColor.GOLD + readable(currentTarget) + ChatColor.YELLOW + ".");
    }

    private List<Material> loadBlockPool(ChaosArcadePlugin plugin) {
        List<Material> pool = new ArrayList<>();
        for (String raw : plugin.getConfig().getStringList("modes.BLOCK_SHUFFLE.block-pool")) {
            try {
                pool.add(Material.valueOf(raw.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (pool.isEmpty()) {
            pool.add(Material.GRASS_BLOCK);
            pool.add(Material.STONE);
            pool.add(Material.DIRT);
        }
        return pool;
    }

    private ItemStack targetItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Target: " + ChatColor.WHITE + readable(material));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String readable(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
