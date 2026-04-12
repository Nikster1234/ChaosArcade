package bg.nikol.chaosarcade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class AbilityBrawlSession extends GameSession {
    private final int targetKills;
    private final int rollSeconds;
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, AbilityType> abilities = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private BossBar bossBar;
    private int secondsUntilRoll;

    public AbilityBrawlSession(ChaosArcadePlugin plugin, GameManager manager, ArenaDefinition arena, List<UUID> players) {
        super(plugin, manager, arena, GameMode.ABILITY_BRAWL, players);
        this.targetKills = plugin.getConfig().getInt("modes.ABILITY_BRAWL.target-kills", 8);
        this.rollSeconds = plugin.getConfig().getInt("modes.ABILITY_BRAWL.roll-seconds", 20);
    }

    @Override
    protected void onStart() {
        bossBar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + "Ability Brawl", BarColor.PURPLE, BarStyle.SEGMENTED_10);

        int index = 0;
        for (Player player : getPlayers()) {
            preparePlayer(player, spawnForIndex(index++));
            player.getInventory().addItem(createSword());
            kills.put(player.getUniqueId(), 0);
            bossBar.addPlayer(player);
        }

        rollAbilities();
        secondsUntilRoll = rollSeconds;

        repeat(0L, 20L, () -> {
            Player leader = getLeader();
            int leaderKills = leader == null ? 0 : kills.getOrDefault(leader.getUniqueId(), 0);

            if (bossBar != null) {
                String leaderName = leader == null ? "Nobody" : leader.getName();
                bossBar.setTitle(ChatColor.LIGHT_PURPLE + "Leader: " + ChatColor.WHITE + leaderName + ChatColor.GRAY + " " + leaderKills + "/" + targetKills);
                bossBar.setProgress(Math.max(0.0D, Math.min(1.0D, leaderKills / (double) targetKills)));
            }

            for (Player player : getPlayers()) {
                AbilityType ability = abilities.get(player.getUniqueId());
                player.sendActionBar(ChatColor.LIGHT_PURPLE + "Ability: "
                        + ChatColor.WHITE + (ability == null ? "None" : ability.displayName)
                        + ChatColor.DARK_GRAY + " | "
                        + ChatColor.YELLOW + "Kills: "
                        + ChatColor.WHITE + kills.getOrDefault(player.getUniqueId(), 0)
                        + ChatColor.DARK_GRAY + " | "
                        + ChatColor.AQUA + secondsUntilRoll + "s");
            }

            if (secondsUntilRoll <= 0) {
                rollAbilities();
                secondsUntilRoll = rollSeconds;
                return;
            }

            secondsUntilRoll--;
        });
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
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isActive(player.getUniqueId())) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() == Action.PHYSICAL) {
            return;
        }
        if (event.getItem() == null || event.getItem().getType() != Material.BLAZE_ROD) {
            return;
        }

        event.setCancelled(true);
        AbilityType ability = abilities.get(player.getUniqueId());
        if (ability == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long readyAt = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) {
            long left = Math.max(1L, (readyAt - now) / 1000L);
            player.sendMessage(plugin.prefix() + ChatColor.RED + "Ability cooldown: " + left + "s");
            return;
        }

        useAbility(player, ability);
        cooldowns.put(player.getUniqueId(), now + ability.cooldownSeconds * 1000L);
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
        if (attacker == null || !isActive(attacker.getUniqueId()) || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        if (isLethal(victim, event.getFinalDamage())) {
            event.setCancelled(true);
            respawnInArena(victim);

            int newKills = kills.getOrDefault(attacker.getUniqueId(), 0) + 1;
            kills.put(attacker.getUniqueId(), newKills);

            attacker.sendMessage(plugin.prefix() + ChatColor.GREEN + "Kill secured on " + victim.getName() + ". Total: " + newKills);
            victim.sendMessage(plugin.prefix() + ChatColor.RED + "You were taken out by " + attacker.getName() + ".");

            if (newKills >= targetKills) {
                finishWithWinner(attacker, "won Ability Brawl.");
            }
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

    private void rollAbilities() {
        for (Player player : getPlayers()) {
            AbilityType ability = AbilityType.values()[random.nextInt(AbilityType.values().length)];
            abilities.put(player.getUniqueId(), ability);
            player.getInventory().setItem(0, createAbilityItem(ability));
            player.sendTitle(ChatColor.LIGHT_PURPLE + ability.displayName, ChatColor.YELLOW + ability.description, 5, 30, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.4F);
        }
        broadcast(ChatColor.LIGHT_PURPLE + "Abilities were reshuffled.");
    }

    private void useAbility(Player player, AbilityType ability) {
        switch (ability) {
            case DASH -> useDash(player);
            case FIREBALL -> useFireball(player);
            case LEAP -> useLeap(player);
            case FROST_PULSE -> useFrostPulse(player);
        }
    }

    private void useDash(Player player) {
        Vector velocity = player.getLocation().getDirection().normalize().multiply(1.8D).setY(0.35D);
        player.setVelocity(velocity);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0.0D, 1.0D, 0.0D), 20, 0.3D, 0.3D, 0.3D, 0.02D);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8F, 1.5F);
    }

    private void useFireball(Player player) {
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield(0.0F);
        fireball.setIsIncendiary(false);
        fireball.setVelocity(player.getLocation().getDirection().multiply(1.2D));
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0F, 1.0F);
    }

    private void useLeap(Player player) {
        Vector velocity = player.getLocation().getDirection().normalize().multiply(0.8D).setY(1.0D);
        player.setVelocity(velocity);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 3, 0, false, false, true));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.3F);
    }

    private void useFrostPulse(Player player) {
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0.0D, 1.0D, 0.0D), 50, 2.0D, 1.0D, 2.0D, 0.02D);
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.9F, 1.6F);

        for (Player target : getPlayers()) {
            if (target.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            if (target.getLocation().distanceSquared(player.getLocation()) <= 16.0D) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 3, 2, false, true, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 2, 0, false, true, true));
            }
        }
    }

    private Player getLeader() {
        Player leader = null;
        int best = Integer.MIN_VALUE;
        for (Player player : getPlayers()) {
            int value = kills.getOrDefault(player.getUniqueId(), 0);
            if (value > best) {
                best = value;
                leader = player;
            }
        }
        return leader;
    }

    private ItemStack createSword() {
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Brawl Blade");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            sword.setItemMeta(meta);
        }
        return sword;
    }

    private ItemStack createAbilityItem(AbilityType ability) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + ability.displayName);
            meta.setLore(List.of(
                    ChatColor.GRAY + ability.description,
                    ChatColor.GRAY + "Cooldown: " + ChatColor.WHITE + ability.cooldownSeconds + "s",
                    ChatColor.YELLOW + "Right click to use"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private enum AbilityType {
        DASH("Dash", "Burst forward through the arena.", 6),
        FIREBALL("Fireball", "Launch a fast explosive projectile.", 8),
        LEAP("Sky Leap", "Jump high and dive back in.", 7),
        FROST_PULSE("Frost Pulse", "Slow nearby enemies.", 10);

        private final String displayName;
        private final String description;
        private final int cooldownSeconds;

        AbilityType(String displayName, String description, int cooldownSeconds) {
            this.displayName = displayName;
            this.description = description;
            this.cooldownSeconds = cooldownSeconds;
        }
    }
}
