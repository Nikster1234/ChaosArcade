package bg.nikol.chaosarcade;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class RegionSnapshot {
    private final List<SavedBlock> blocks;

    public RegionSnapshot(List<SavedBlock> blocks) {
        this.blocks = blocks;
    }

    public static RegionSnapshot capture(CuboidRegion region) {
        List<SavedBlock> blocks = new ArrayList<>();
        region.forEachBlock(block -> blocks.add(new SavedBlock(block.getLocation(), block.getBlockData().getAsString())));
        return new RegionSnapshot(blocks);
    }

    public void restore() {
        for (SavedBlock block : blocks) {
            if (block.location().getWorld() == null) {
                continue;
            }
            block.location().getBlock().setBlockData(Bukkit.createBlockData(block.blockData()), false);
        }
    }

    public record SavedBlock(Location location, String blockData) {
    }
}
