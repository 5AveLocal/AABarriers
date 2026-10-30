package me.fiveave.aabarriers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Objects;

public class transformer {

    public static void scanAndSpawn(metadata gate, Location pivotLoc, double thickness, Map<BlockDisplay, Vector3f> barrierParts, Map<Location, BlockData> originalBlocks) {
        int minX = Math.min(gate.getStart().getBlockX(), gate.getEnd().getBlockX());
        int maxX = Math.max(gate.getStart().getBlockX(), gate.getEnd().getBlockX());
        int minY = Math.min(gate.getStart().getBlockY(), gate.getEnd().getBlockY());
        int maxY = Math.max(gate.getStart().getBlockY(), gate.getEnd().getBlockY());
        int minZ = Math.min(gate.getStart().getBlockZ(), gate.getEnd().getBlockZ());
        int maxZ = Math.max(gate.getStart().getBlockZ(), gate.getEnd().getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = Objects.requireNonNull(gate.getWorld()).getBlockAt(x, y, z);
                    if (block.getType().isAir()) continue;

                    Location blockLoc = block.getLocation();
                    BlockData blockData = block.getBlockData();
                    originalBlocks.put(blockLoc.clone(), blockData.clone());

                    double relX = blockLoc.getX() - pivotLoc.getX();
                    double relY = blockLoc.getY() - pivotLoc.getY();
                    double relZ = blockLoc.getZ() - pivotLoc.getZ();
                    Vector3f initialTranslation = new Vector3f((float) relX, (float) relY, (float) relZ);

                    BlockData displayData = blockData.clone();
                    String matName = block.getType().name();

                    if (matName.endsWith("_STAINED_GLASS_PANE") || matName.endsWith("_STAINED_GLASS")) {
                        String colorPrefix = matName.contains("_STAINED_GLASS_PANE") ?
                                matName.substring(0, matName.indexOf("_STAINED_GLASS_PANE")) :
                                matName.substring(0, matName.indexOf("_STAINED_GLASS"));
                        Material concreteMat = Material.getMaterial(colorPrefix + "_CONCRETE");
                        if (concreteMat != null) displayData = concreteMat.createBlockData();
                    } else if (matName.equals("STAINED_GLASS") || matName.equals("STAINED_GLASS_PANE") || matName.equals("GLASS") || matName.equals("GLASS_PANE")) {
                        displayData = Material.WHITE_CONCRETE.createBlockData();
                    }

                    final BlockData finalizedDisplayData = displayData;
                    BlockDisplay display = gate.getWorld().spawn(pivotLoc, BlockDisplay.class, bd -> {
                        bd.setBlock(finalizedDisplayData);
                        Vector3f blockOffset = new Vector3f(initialTranslation).add(0.0f, (float) ((1.0 - thickness) / 2.0), (float) ((1.0 - thickness) / 2.0));
                        bd.setTransformation(new Transformation(blockOffset, new Quaternionf(), new Vector3f(1.0f, (float) thickness, (float) thickness), new Quaternionf()));
                        bd.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
                        bd.setInterpolationDuration(1);
                        bd.setInterpolationDelay(0);
                    });

                    barrierParts.put(display, initialTranslation);
                    block.setType(Material.AIR);
                }
            }
        }
    }

    public static void updateTransformations(Map<BlockDisplay, Vector3f> parts, Quaternionf rotationAngle, double thickness) {
        parts.forEach((bd, initialTranslation) -> {
            Vector3f rotatedTranslation = new Vector3f(initialTranslation);
            rotationAngle.transform(rotatedTranslation);
            Vector3f baseOffset = new Vector3f(0.0f, (float) ((1.0 - thickness) / 2.0), (float) ((1.0 - thickness) / 2.0));
            rotationAngle.transform(baseOffset);
            Vector3f finalTranslation = rotatedTranslation.add(baseOffset);
            bd.setTransformation(new Transformation(finalTranslation, rotationAngle, new Vector3f(1.0f, (float) thickness, (float) thickness), new Quaternionf()));
        });
    }
}
