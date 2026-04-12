package bg.nikol.chaosarcade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class ChaosArcadePlugin extends JavaPlugin {
    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ArenaRepository arenaRepository = new ArenaRepository(this);
        arenaRepository.reload();

        gameManager = new GameManager(this, arenaRepository);
        ArcadeMenu arcadeMenu = new ArcadeMenu(gameManager, arenaRepository);
        gameManager.setMenu(arcadeMenu);

        ArcadeCommand command = new ArcadeCommand(this, gameManager, arenaRepository);
        command.register();

        Bukkit.getPluginManager().registerEvents(new ArcadeListener(gameManager, arcadeMenu), this);
        getLogger().info("ChaosArcade enabled.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
    }

    public String prefix() {
        return ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "ChaosArcade" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
    }
}
