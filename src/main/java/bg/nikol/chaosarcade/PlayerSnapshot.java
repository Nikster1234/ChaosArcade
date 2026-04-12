package bg.nikol.chaosarcade;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class PlayerSnapshot {
    private final Location location;
    private final org.bukkit.GameMode gameMode;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int level;
    private final float exp;
    private final boolean allowFlight;
    private final boolean flying;
    private final ItemStack[] storageContents;
    private final ItemStack[] armorContents;
    private final ItemStack[] extraContents;
    private final ItemStack offHand;
    private final List<PotionEffect> effects;

    private PlayerSnapshot(
            Location location,
            org.bukkit.GameMode gameMode,
            double health,
            int foodLevel,
            float saturation,
            int level,
            float exp,
            boolean allowFlight,
            boolean flying,
            ItemStack[] storageContents,
            ItemStack[] armorContents,
            ItemStack[] extraContents,
            ItemStack offHand,
            List<PotionEffect> effects) {
        this.location = location;
        this.gameMode = gameMode;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.level = level;
        this.exp = exp;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.storageContents = cloneItems(storageContents);
        this.armorContents = cloneItems(armorContents);
        this.extraContents = cloneItems(extraContents);
        this.offHand = offHand == null ? null : offHand.clone();
        this.effects = new ArrayList<>(effects);
    }

    public static PlayerSnapshot capture(Player player) {
        return new PlayerSnapshot(
                player.getLocation().clone(),
                player.getGameMode(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getLevel(),
                player.getExp(),
                player.getAllowFlight(),
                player.isFlying(),
                player.getInventory().getStorageContents(),
                player.getInventory().getArmorContents(),
                player.getInventory().getExtraContents(),
                player.getInventory().getItemInOffHand(),
                new ArrayList<>(player.getActivePotionEffects()));
    }

    public void restore(Player player) {
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setExtraContents(null);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        player.setGameMode(gameMode);
        player.setAllowFlight(allowFlight);
        player.setFlying(allowFlight && flying);
        player.teleport(location.clone());
        player.getInventory().setStorageContents(cloneItems(storageContents));
        player.getInventory().setArmorContents(cloneItems(armorContents));
        player.getInventory().setExtraContents(cloneItems(extraContents));
        player.getInventory().setItemInOffHand(offHand == null ? null : offHand.clone());

        double maxHealth = 20.0D;
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        }
        player.setHealth(Math.max(1.0D, Math.min(health, maxHealth)));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setLevel(level);
        player.setExp(exp);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);

        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }

        player.updateInventory();
    }

    private static ItemStack[] cloneItems(ItemStack[] items) {
        if (items == null) {
            return null;
        }

        ItemStack[] clone = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            clone[i] = items[i] == null ? null : items[i].clone();
        }
        return clone;
    }
}
