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

        // Determine axis direction for correct initial spawning offset sizing
        boolean isXAxis = (maxX - minX) >= (maxZ - minZ);

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

                        // FIX: Calculate initial bounding offsets based on gate alignment direction
                        float offsetPad = (float) ((1.0 - thickness) / 2.0);
                        Vector3f blockOffset = isXAxis
                                ? new Vector3f(initialTranslation).add(0.0f, offsetPad, offsetPad)
                                : new Vector3f(initialTranslation).add(offsetPad, offsetPad, 0.0f);

                        Vector3f scale = isXAxis
                                ? new Vector3f(1.0f, (float) thickness, (float) thickness)
                                : new Vector3f((float) thickness, (float) thickness, 1.0f);

                        bd.setTransformation(new Transformation(blockOffset, new Quaternionf(), scale, new Quaternionf()));
                        bd.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
                        bd.setInterpolationDuration(1);
                        bd.setInterpolationDelay(0);
                    });

                    // --- Inside transformer.java -> scanAndSpawn loop ---
                    barrierParts.put(display, initialTranslation);

                    // --- CRITICAL FIX: Change block.setType(Material.AIR) to prevent neighbor state updates ---
                    block.setBlockData(org.bukkit.Bukkit.createBlockData(Material.AIR), false);
                }
            }
        }
    }

    public static void updateTransformations(Map<BlockDisplay, Vector3f> parts, Quaternionf rotationAngle, double thickness, boolean isXAxis) {
        parts.forEach((bd, initialTranslation) -> {
            Vector3f rotatedTranslation = new Vector3f(initialTranslation);
            rotationAngle.transform(rotatedTranslation);

            float offsetPad = (float) ((1.0 - thickness) / 2.0);
            Vector3f baseOffset = isXAxis
                    ? new Vector3f(0.0f, offsetPad, offsetPad)
                    : new Vector3f(offsetPad, offsetPad, 0.0f);

            rotationAngle.transform(baseOffset);
            Vector3f finalTranslation = rotatedTranslation.add(baseOffset);

            Vector3f scale = isXAxis
                    ? new Vector3f(1.0f, (float) thickness, (float) thickness)
                    : new Vector3f((float) thickness, (float) thickness, 1.0f);

            bd.setTransformation(new Transformation(finalTranslation, rotationAngle, scale, new Quaternionf()));
        });
    }

}
