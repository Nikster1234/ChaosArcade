package bg.nikol.chaosarcade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KingOfTheHillSession extends GameSession {
    private final Location center;
    private final int radius;
    private final int targetPoints;
    private final int pointsPerSecond;
    private final Map<UUID, Integer> scores = new HashMap<>();
    private BossBar bossBar;

    public KingOfTheHillSession(ChaosArcadePlugin plugin, GameManager manager, ArenaDefinition arena, List<UUID> players) {
        super(plugin, manager, arena, GameMode.KING_OF_THE_HILL, players);
        this.center = arena.getCenter();
        this.radius = arena.getRadius();
        this.targetPoints = plugin.getConfig().getInt("modes.KING_OF_THE_HILL.target-points", 100);
        this.pointsPerSecond = plugin.getConfig().getInt("modes.KING_OF_THE_HILL.points-per-second", 5);
    }

    @Override
    protected void onStart() {
        bossBar = Bukkit.createBossBar(ChatColor.GOLD + "King of the Hill", BarColor.YELLOW, BarStyle.SEGMENTED_10);

        int index = 0;
        for (Player player : getPlayers()) {
            preparePlayer(player, spawnForIndex(index++));
            equipLoadout(player);
            bossBar.addPlayer(player);
            scores.put(player.getUniqueId(), 0);
        }

        repeat(0L, 20L, this::tickScores);
    }

    @Override
    protected void onStop() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    @Override
    public boolean allowsDirectCombat() {
        return true;
    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isActive(player.getUniqueId())) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID || isLethal(player, event.getFinalDamage())) {
            event.setCancelled(true);
            respawnInArena(player);
        }
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !isActive(victim.getUniqueId())) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker != null && isActive(attacker.getUniqueId()) && isLethal(victim, event.getFinalDamage())) {
            event.setCancelled(true);
            respawnInArena(victim);
            heal(attacker);
            attacker.sendMessage(plugin.prefix() + ChatColor.GREEN + "You knocked out " + victim.getName() + ".");
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

    private void tickScores() {
        List<Player> contenders = getPlayers().stream().filter(this::isInsideHill).toList();

        if (contenders.size() == 1) {
            Player scorer = contenders.get(0);
            int newScore = scores.getOrDefault(scorer.getUniqueId(), 0) + pointsPerSecond;
            scores.put(scorer.getUniqueId(), newScore);
            scorer.sendActionBar(ChatColor.GOLD + "+" + pointsPerSecond + " hill points");

            if (newScore >= targetPoints) {
                finishWithWinner(scorer, "won King of the Hill.");
                return;
            }
        }

        Player leader = getLeader();
        int leaderScore = leader == null ? 0 : scores.getOrDefault(leader.getUniqueId(), 0);
        String hillState;
        if (contenders.isEmpty()) {
            hillState = ChatColor.GRAY + "Empty";
        } else if (contenders.size() == 1) {
            hillState = ChatColor.GOLD + contenders.get(0).getName();
        } else {
            hillState = ChatColor.RED + "Contested";
        }

        for (Player player : getPlayers()) {
            player.sendActionBar(ChatColor.YELLOW + "Hill: "
                    + hillState
                    + ChatColor.DARK_GRAY + " | "
                    + ChatColor.GREEN + "Your: "
                    + ChatColor.WHITE + scores.getOrDefault(player.getUniqueId(), 0)
                    + ChatColor.DARK_GRAY + " | "
                    + ChatColor.GOLD + "Leader: "
                    + ChatColor.WHITE + leaderScore);
        }

        if (bossBar != null) {
            String leaderName = leader == null ? "Nobody" : leader.getName();
            bossBar.setTitle(ChatColor.GOLD + "Leader: " + ChatColor.WHITE + leaderName + ChatColor.GRAY + " " + leaderScore + "/" + targetPoints);
            bossBar.setProgress(Math.max(0.0D, Math.min(1.0D, leaderScore / (double) targetPoints)));
        }
    }

    private boolean isInsideHill(Player player) {
        if (center == null || player.getWorld() == null || center.getWorld() == null) {
            return false;
        }
        if (!player.getWorld().getUID().equals(center.getWorld().getUID())) {
            return false;
        }
        return player.getLocation().distanceSquared(center) <= radius * radius;
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

    private ItemStack createSword() {
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Hill Blade");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            sword.setItemMeta(meta);
        }
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 1);
        return sword;
    }

    private void equipLoadout(Player player) {
        player.getInventory().addItem(createSword());
        player.getInventory().setArmorContents(new ItemStack[] {
                new ItemStack(Material.IRON_BOOTS),
                new ItemStack(Material.IRON_LEGGINGS),
                new ItemStack(Material.IRON_CHESTPLATE),
                new ItemStack(Material.IRON_HELMET)
        });
    }
}
