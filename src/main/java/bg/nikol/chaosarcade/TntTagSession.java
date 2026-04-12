package bg.nikol.chaosarcade;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TntTagSession extends GameSession {
    private final int fuseSeconds;
    private int secondsLeft;
    private UUID currentTagger;
    private long passCooldownUntil;

    public TntTagSession(ChaosArcadePlugin plugin, GameManager manager, ArenaDefinition arena, List<UUID> players) {
        super(plugin, manager, arena, GameMode.TNT_TAG, players);
        this.fuseSeconds = plugin.getConfig().getInt("modes.TNT_TAG.fuse-seconds", 60);
    }

    @Override
    protected void onStart() {
        int index = 0;
        for (Player player : getPlayers()) {
            preparePlayer(player, spawnForIndex(index++));
        }

        chooseNextTagger(null);
        secondsLeft = fuseSeconds;

        repeat(0L, 20L, () -> {
            if (getAliveCount() <= 1) {
                checkForLastPlayer("wins TNT Tag.");
                return;
            }

            Player tagger = currentTagger == null ? null : getPlayer(currentTagger);
            if (tagger == null || !tagger.isOnline()) {
                chooseNextTagger(null);
                tagger = getPlayer(currentTagger);
            }

            String name = tagger == null ? "None" : tagger.getName();
            actionBar("&cTagger: &f" + name + " &8| &e" + secondsLeft + "s");

            if (secondsLeft <= 0) {
                explodeTagger();
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
            player.teleport(spawnForIndex(random.nextInt(Math.max(1, getAliveCount()))));
        }
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player damager)) {
            return;
        }
        if (!isActive(victim.getUniqueId()) || !isActive(damager.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        if (!damager.getUniqueId().equals(currentTagger)) {
            return;
        }

        if (System.currentTimeMillis() < passCooldownUntil) {
            return;
        }

        chooseNextTagger(victim.getUniqueId());
        passCooldownUntil = System.currentTimeMillis() + 800L;

        broadcast(ChatColor.RED + damager.getName()
                + ChatColor.YELLOW + " passed the TNT to "
                + ChatColor.RED + victim.getName() + ChatColor.YELLOW + ".");
    }

    private void explodeTagger() {
        Player tagger = currentTagger == null ? null : getPlayer(currentTagger);
        if (tagger == null) {
            chooseNextTagger(null);
            secondsLeft = fuseSeconds;
            return;
        }

        Location location = tagger.getLocation();
        location.getWorld().createExplosion(location, 0.0F, false, false);
        eliminate(tagger, ChatColor.RED + "You exploded and got eliminated.");
        broadcast(ChatColor.RED + tagger.getName() + ChatColor.YELLOW + " exploded.");
        clearTaggerEffects();
        currentTagger = null;

        if (getAliveCount() <= 1) {
            checkForLastPlayer("wins TNT Tag.");
            return;
        }

        chooseNextTagger(null);
        secondsLeft = fuseSeconds;
    }

    private void chooseNextTagger(UUID forced) {
        clearTaggerEffects();

        UUID next = forced;
        if (next == null) {
            List<UUID> pool = new ArrayList<>(activePlayers);
            next = pool.get(random.nextInt(pool.size()));
        }

        currentTagger = next;
        Player player = getPlayer(currentTagger);
        if (player == null) {
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 60 * 10, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60 * 10, 0, false, false, true));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 0.8F);
        player.sendTitle(ChatColor.RED + "You have the TNT!", ChatColor.YELLOW + "Hit someone fast.", 5, 40, 10);
    }

    private void clearTaggerEffects() {
        if (currentTagger == null) {
            return;
        }
        Player current = getPlayer(currentTagger);
        if (current != null) {
            current.removePotionEffect(PotionEffectType.GLOWING);
            current.removePotionEffect(PotionEffectType.SPEED);
        }
    }
}
