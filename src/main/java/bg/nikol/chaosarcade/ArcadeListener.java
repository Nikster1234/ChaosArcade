package bg.nikol.chaosarcade;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;

public class ArcadeListener implements Listener {
    private final GameManager manager;
    private final ArcadeMenu menu;

    public ArcadeListener(GameManager manager, ArcadeMenu menu) {
        this.manager = manager;
        this.menu = menu;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        menu.handleClick(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        GameSession session = manager.getActiveSession();
        if (session != null && manager.isPlaying(event.getPlayer().getUniqueId())) {
            session.onMove(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        GameSession session = manager.getActiveSession();
        if (session != null && manager.isPlaying(player.getUniqueId())) {
            session.onDamage(event);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        GameSession session = manager.getActiveSession();
        if (session != null) {
            if (event.getEntity() instanceof Player victim) {
                Player attacker = resolveAttacker(event.getDamager());
                if (attacker != null
                        && manager.isPlaying(victim.getUniqueId())
                        && manager.isPlaying(attacker.getUniqueId())
                        && session.allowsDirectCombat()) {
                    event.setCancelled(false);
                }
            }
            session.onDamageByEntity(event);
        }
    }

    private Player resolveAttacker(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        GameSession session = manager.getActiveSession();
        if (session != null && manager.isPlaying(event.getPlayer().getUniqueId())) {
            session.onBlockBreak(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        GameSession session = manager.getActiveSession();
        if (session != null && manager.isPlaying(event.getPlayer().getUniqueId())) {
            session.onBlockPlace(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        GameSession session = manager.getActiveSession();
        if (session != null && manager.isPlaying(event.getPlayer().getUniqueId())) {
            session.onInteract(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevel(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        GameSession session = manager.getActiveSession();
        if (session != null && manager.isPlaying(player.getUniqueId())) {
            session.onFoodLevelChange(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        GameSession session = manager.getActiveSession();
        if (session != null && manager.isPlaying(event.getPlayer().getUniqueId())) {
            session.onDrop(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        GameSession session = manager.getActiveSession();
        if (session != null && manager.isPlaying(event.getPlayer().getUniqueId())) {
            session.onToggleFlight(event);
        }
    }
}
