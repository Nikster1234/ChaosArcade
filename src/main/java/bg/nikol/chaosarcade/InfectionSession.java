package bg.nikol.chaosarcade;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class InfectionSession extends GameSession {
    private final int durationSeconds;
    private final Set<UUID> infectedPlayers = new HashSet<>();
    private int secondsLeft;

    public InfectionSession(ChaosArcadePlugin plugin, GameManager manager, ArenaDefinition arena, List<UUID> players) {
        super(plugin, manager, arena, GameMode.INFECTION, players);
        this.durationSeconds = plugin.getConfig().getInt("modes.INFECTION.duration-seconds", 150);
    }

    @Override
    protected void onStart() {
        int index = 0;
        for (Player player : getPlayers()) {
            preparePlayer(player, spawnForIndex(index++));
            player.getInventory().setItem(0, trackerItem(ChatColor.AQUA + "Survivor", Material.IRON_NUGGET));
        }

        Player first = getPlayers().get(random.nextInt(getPlayers().size()));
        infectedPlayers.add(first.getUniqueId());
        stylePlayer(first);
        first.sendTitle(ChatColor.RED + "You are infected!", ChatColor.YELLOW + "Touch survivors to turn them.", 5, 40, 10);

        secondsLeft = durationSeconds;
        broadcast(ChatColor.RED + first.getName() + ChatColor.YELLOW + " is the first infected.");

        repeat(0L, 20L, () -> {
            int survivors = getSurvivorCount();
            actionBar("&cInfected: &f" + infectedPlayers.size() + " &8| &aSurvivors: &f" + survivors + " &8| &e" + secondsLeft + "s");

            if (survivors <= 0) {
                finish(ChatColor.RED + "The infection took over the whole lobby.");
                return;
            }

            if (secondsLeft <= 0) {
                if (survivors == 1) {
                    Player winner = getPlayers().stream()
                            .filter(player -> !infectedPlayers.contains(player.getUniqueId()))
                            .findFirst()
                            .orElse(null);
                    finishWithWinner(winner, "survived the infection.");
                } else {
                    finish(ChatColor.GREEN + "Survivors held out and won the infection round.");
                }
                return;
            }

            secondsLeft--;
        });
    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isActive(player.getUniqueId())) {
            event.setCancelled(true);
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

        if (!infectedPlayers.contains(damager.getUniqueId()) || infectedPlayers.contains(victim.getUniqueId())) {
            return;
        }

        infectedPlayers.add(victim.getUniqueId());
        stylePlayer(victim);
        broadcast(ChatColor.RED + victim.getName() + ChatColor.YELLOW + " was infected by " + ChatColor.RED + damager.getName() + ChatColor.YELLOW + ".");

        if (getSurvivorCount() <= 0) {
            finish(ChatColor.RED + "The infection took over the whole lobby.");
        }
    }

    private int getSurvivorCount() {
        return getAliveCount() - infectedPlayers.size();
    }

    private void stylePlayer(Player player) {
        player.getInventory().setItem(0, trackerItem(ChatColor.RED + "Infected", Material.ROTTEN_FLESH));
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.GLOWING);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60 * 10, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 60 * 10, 0, false, false, true));
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0F, 1.0F);
        player.sendTitle(ChatColor.RED + "Infected", ChatColor.YELLOW + "Touch the survivors.", 5, 35, 10);
    }

    private ItemStack trackerItem(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
