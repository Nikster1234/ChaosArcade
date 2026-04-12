package bg.nikol.chaosarcade;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ArcadeMenu {
    private static final int SIZE = 27;
    private static final int STATUS_SLOT = 22;
    private static final int CLOSE_SLOT = 26;

    private final GameManager manager;
    private final ArenaRepository arenaRepository;

    public ArcadeMenu(GameManager manager, ArenaRepository arenaRepository) {
        this.manager = manager;
        this.arenaRepository = arenaRepository;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(), SIZE, ChatColor.GOLD + "" + ChatColor.BOLD + "ChaosArcade");

        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, simple(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + " "));
        }

        for (GameMode mode : GameMode.values()) {
            inventory.setItem(mode.getMenuSlot(), createModeItem(player, mode));
        }

        inventory.setItem(STATUS_SLOT, createStatusItem());
        inventory.setItem(CLOSE_SLOT, simple(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(inventory);
    }

    public boolean handleClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) {
            return false;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return true;
        }

        if (event.getRawSlot() == CLOSE_SLOT) {
            player.closeInventory();
            return true;
        }

        for (GameMode mode : GameMode.values()) {
            if (event.getRawSlot() == mode.getMenuSlot()) {
                manager.joinQueue(player, mode);
                open(player);
                return true;
            }
        }

        return true;
    }

    private ItemStack createModeItem(Player player, GameMode mode) {
        String state = manager.getModeState(mode);
        String nameColor = ChatColor.GREEN.toString();
        List<String> lore = new ArrayList<>();

        lore.add(ChatColor.GRAY + mode.getDescription());
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Players: " + ChatColor.WHITE + mode.getMinPlayers() + "-" + mode.getMaxPlayers());
        lore.add(ChatColor.GRAY + "Ready arenas: " + ChatColor.WHITE + arenaRepository.getReadyArenas(mode).size());

        switch (state) {
            case "RUNNING" -> {
                nameColor = ChatColor.RED.toString();
                lore.add(ChatColor.RED + "A match is already running.");
            }
            case "QUEUE" -> {
                nameColor = ChatColor.GOLD.toString();
                lore.add(ChatColor.GOLD + "Queue open: " + manager.getQueueSize() + " players");
                if (manager.isQueued(player.getUniqueId()) && manager.getQueuedMode() == mode) {
                    lore.add(ChatColor.YELLOW + "Click again to leave the queue.");
                } else {
                    lore.add(ChatColor.GREEN + "Click to join the queue.");
                }
            }
            case "SETUP" -> {
                nameColor = ChatColor.DARK_RED.toString();
                lore.add(ChatColor.RED + "No fully configured arena yet.");
                lore.add(ChatColor.GRAY + "Use /arcade arena " + mode.getCommandKey() + " ...");
            }
            default -> lore.add(ChatColor.GREEN + "Click to join the queue.");
        }

        if (manager.isPlaying(player.getUniqueId())) {
            lore.add(ChatColor.RED + "You are already in the current match.");
        }

        return simple(mode.getIcon(), nameColor + mode.getDisplayName(), lore.toArray(new String[0]));
    }

    private ItemStack createStatusItem() {
        List<String> lore = new ArrayList<>();

        if (manager.getActiveSession() != null) {
            lore.add(ChatColor.GRAY + "Mode: " + ChatColor.WHITE + manager.getActiveSession().getMode().getDisplayName());
            lore.add(ChatColor.GRAY + "Arena: " + ChatColor.WHITE + manager.getActiveSession().getArenaId());
            lore.add(ChatColor.GRAY + "Players: " + ChatColor.WHITE + manager.getActiveSession().getActivePlayers().size());
            return simple(Material.NETHER_STAR, ChatColor.GOLD + "Live Match", lore.toArray(new String[0]));
        }

        if (manager.getQueuedMode() != null && manager.getQueueSize() > 0) {
            lore.add(ChatColor.GRAY + "Mode: " + ChatColor.WHITE + manager.getQueuedMode().getDisplayName());
            lore.add(ChatColor.GRAY + "Queued: " + ChatColor.WHITE + manager.getQueueSize());
            return simple(Material.CLOCK, ChatColor.YELLOW + "Queue Status", lore.toArray(new String[0]));
        }

        lore.add(ChatColor.GRAY + "Choose a mini-game to start.");
        return simple(Material.BOOK, ChatColor.GREEN + "Idle", lore.toArray(new String[0]));
    }

    private ItemStack simple(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (loreLines.length > 0) {
                meta.setLore(List.of(loreLines));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static final class MenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
