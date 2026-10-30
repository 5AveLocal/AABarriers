package me.fiveave.aabarriers;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class collisionmanager {

    public static void setPhysicalBarriers(metadata gate, Material material) {
        World world = gate.getWorld();
        if (world == null) return;

        int minX = Math.min(gate.getStart().getBlockX(), gate.getEnd().getBlockX());
        int maxX = Math.max(gate.getStart().getBlockX(), gate.getEnd().getBlockX());
        int minY = Math.min(gate.getStart().getBlockY(), gate.getEnd().getBlockY());
        int maxY = Math.max(gate.getStart().getBlockY(), gate.getEnd().getBlockY());
        int minZ = Math.min(gate.getStart().getBlockZ(), gate.getEnd().getBlockZ());
        int maxZ = Math.max(gate.getStart().getBlockZ(), gate.getEnd().getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (material == Material.AIR && block.getType() != Material.BARRIER) {
                        continue;
                    }
                    block.setType(material, false);
                }
            }
        }
    }
}
