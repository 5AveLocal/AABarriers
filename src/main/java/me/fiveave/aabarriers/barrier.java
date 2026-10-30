package me.fiveave.aabarriers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Interaction;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

import static me.fiveave.aabarriers.main.plugin;

public class barrier {

    private final Map<String, Map<BlockDisplay, Vector3f>> activePersistentArms = new HashMap<>();
    private final Map<String, Interaction> activeInteractions = new HashMap<>();
    private final Map<String, List<BukkitTask>> activeTempTasks = new HashMap<>();
    private final Map<String, Float> gateAngles = new HashMap<>();
    private final Map<String, BukkitTask> activeAnimationTasks = new HashMap<>();
    private final Map<String, Long> gateActionIds = new HashMap<>();

    public void activateBarrierArm(Location start, Location end, int rotSpeed, int duration) {
        metadata gate = new metadata(start, end);
        clearAndAdvanceSequence(gate.getKey());
        long currentActionId = gateActionIds.get(gate.getKey());

        int holdTime = duration - (2 * rotSpeed);
        if (holdTime < 0) holdTime = 0;

        List<BukkitTask> tasksList = new ArrayList<>();

        float currentAngleRad = gateAngles.getOrDefault(gate.getKey(), 0f);
        float targetAngleRad = (float) Math.toRadians(-90);
        float totalAngleDelta = Math.abs(targetAngleRad - currentAngleRad);
        float fullMovementRange = (float) Math.toRadians(90);

        int openTicks = Math.max(1, Math.round((totalAngleDelta / fullMovementRange) * rotSpeed));

        setPersistentGateStateInternal(gate, openTicks, true, currentActionId);

        main.gateStorage.dataconfig.set("gates." + gate.getKey() + ".state", "temp_processing");
        main.gateStorage.save();

        BukkitTask closeSchedule = new BukkitRunnable() {
            @Override
            public void run() {
                if (Objects.equals(gateActionIds.get(gate.getKey()), currentActionId)) {
                    setPersistentGateStateInternal(gate, rotSpeed, false, currentActionId);
                }
            }
        }.runTaskLater(plugin, 1L + openTicks + holdTime);
        tasksList.add(closeSchedule);

        BukkitTask cleanupSchedule = new BukkitRunnable() {
            @Override
            public void run() {
                if (Objects.equals(gateActionIds.get(gate.getKey()), currentActionId)) {
                    activeTempTasks.remove(gate.getKey());
                    forceResetGate(gate.getStart(), gate.getEnd());
                }
            }
        }.runTaskLater(plugin, 1L + openTicks + holdTime + rotSpeed);
        tasksList.add(cleanupSchedule);

        activeTempTasks.put(gate.getKey(), tasksList);
    }

    public void setPersistentGateState(Location start, Location end, int rotSpeed, boolean targetOpen) {
        metadata gate = new metadata(start, end);
        clearAndAdvanceSequence(gate.getKey());

        float currentAngleRad = gateAngles.getOrDefault(gate.getKey(), 0f);
        float targetAngleRad = targetOpen ? (float) Math.toRadians(-90) : 0f;
        float totalAngleDelta = Math.abs(targetAngleRad - currentAngleRad);
        float fullMovementRange = (float) Math.toRadians(90);

        int dynamicRotSpeed = Math.max(1, Math.round((totalAngleDelta / fullMovementRange) * rotSpeed));

        setPersistentGateStateInternal(gate, dynamicRotSpeed, targetOpen, gateActionIds.get(gate.getKey()));
    }

    private void setPersistentGateStateInternal(metadata gate, int calculatedSpeed, boolean targetOpen, long actionId) {
        if (gate.getWorld() == null) return;

        double thickness = 0.2;
        Location pivotLoc = gate.getStart().clone().add(1.5, 0.5, 0.5);
        Map<BlockDisplay, Vector3f> barrierParts = activePersistentArms.get(gate.getKey());

        if (barrierParts == null) {
            barrierParts = new HashMap<>();
            Map<Location, BlockData> originalBlocks = new HashMap<>();

            Interaction pivotInteraction = gate.getWorld().spawn(pivotLoc, Interaction.class, i -> {
                i.setInteractionWidth(0.01f);
                i.setInteractionHeight(0.01f);
            });
            activeInteractions.put(gate.getKey(), pivotInteraction);

            transformer.scanAndSpawn(gate, pivotLoc, thickness, barrierParts, originalBlocks);
            activePersistentArms.put(gate.getKey(), barrierParts);
            gateAngles.put(gate.getKey(), 0f);

            originalBlocks.forEach((loc, data) -> {
                String locKey = "gates." + gate.getKey() + ".blocks." + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
                main.gateStorage.dataconfig.set(locKey + ".type", loc.getBlock().getType().name());
                main.gateStorage.dataconfig.set(locKey + ".data", data.getAsString());
            });
            main.gateStorage.dataconfig.set("gates." + gate.getKey() + ".state", targetOpen ? "open" : "closed");
            main.gateStorage.save();
        }

        // --- Inverted Physics Check ---
        // Instantly remove physical barrier blocks when moving/opening to clear the path
        collisionmanager.setPhysicalBarriers(gate, Material.AIR);

        BukkitTask runningAnim = activeAnimationTasks.remove(gate.getKey());
        if (runningAnim != null) {
            runningAnim.cancel();
        }

        final Map<BlockDisplay, Vector3f> finalParts = barrierParts;
        float initialAngleRad = gateAngles.getOrDefault(gate.getKey(), 0f);
        float targetAngleRad = targetOpen ? (float) Math.toRadians(-90) : 0f;
        float totalAngleDelta = targetAngleRad - initialAngleRad;

        BukkitTask animationLoop = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!Objects.equals(gateActionIds.get(gate.getKey()), actionId)) {
                    this.cancel();
                    return;
                }

                if (tick >= calculatedSpeed) {
                    this.cancel();
                    gateAngles.put(gate.getKey(), targetAngleRad);

                    // --- Solidify Barriers On Complete Close ---
                    // Only place solid blocks when the gate arm returns flat down to 0 degrees
                    if (targetAngleRad == 0f) {
                        collisionmanager.setPhysicalBarriers(gate, Material.BARRIER);
                    }

                    if (main.gateStorage.dataconfig.contains("gates." + gate.getKey())) {
                        String currentState = main.gateStorage.dataconfig.getString("gates." + gate.getKey() + ".state");
                        if (!"temp_processing".equals(currentState)) {
                            main.gateStorage.dataconfig.set("gates." + gate.getKey() + ".state", targetOpen ? "open" : "closed");
                            main.gateStorage.save();
                        }
                    }
                    return;
                }
                tick++;

                float progress = (float) tick / calculatedSpeed;
                float dynamicCurrentAngle = initialAngleRad + (totalAngleDelta * progress);
                gateAngles.put(gate.getKey(), dynamicCurrentAngle);

                Quaternionf rotationAngle = new Quaternionf().rotationAxis(dynamicCurrentAngle, 0f, 0f, 1f);
                transformer.updateTransformations(finalParts, rotationAngle, thickness);
            }
        }.runTaskTimer(plugin, 1L, 1L);

        activeAnimationTasks.put(gate.getKey(), animationLoop);
    }

    public void forceResetGate(Location start, Location end) {
        metadata gate = new metadata(start, end);
        clearAndAdvanceSequence(gate.getKey());
        gateAngles.remove(gate.getKey());

        Map<BlockDisplay, Vector3f> components = activePersistentArms.remove(gate.getKey());
        if (components != null) {
            components.keySet().forEach(BlockDisplay::remove);
        }
        Interaction interaction = activeInteractions.remove(gate.getKey());
        if (interaction != null) {
            interaction.remove();
        }

        collisionmanager.setPhysicalBarriers(gate, Material.AIR);

        String blockSectionPath = "gates." + gate.getKey() + ".blocks";
        if (main.gateStorage.dataconfig.contains(blockSectionPath)) {
            var blockSection = main.gateStorage.dataconfig.getConfigurationSection(blockSectionPath);
            if (blockSection != null) {
                for (String compositeCoord : blockSection.getKeys(false)) {
                    String[] coords = compositeCoord.split("_");
                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);
                    int z = Integer.parseInt(coords[2]);

                    String rawBlockDataStr = main.gateStorage.dataconfig.getString(blockSectionPath + "." + compositeCoord + ".data");
                    if (rawBlockDataStr != null && gate.getWorld() != null) {
                        Block b = gate.getWorld().getBlockAt(x, y, z);
                        b.setBlockData(plugin.getServer().createBlockData(rawBlockDataStr), true);
                    }
                }
            }
        }

        main.gateStorage.dataconfig.set("gates." + gate.getKey(), null);
        main.gateStorage.save();
    }

    private void clearAndAdvanceSequence(String gateKey) {
        List<BukkitTask> associatedTasks = activeTempTasks.remove(gateKey);
        if (associatedTasks != null) {
            associatedTasks.forEach(BukkitTask::cancel);
        }

        BukkitTask runningAnim = activeAnimationTasks.remove(gateKey);
        if (runningAnim != null) {
            runningAnim.cancel();
        }

        long nextId = gateActionIds.getOrDefault(gateKey, 0L) + 1L;
        gateActionIds.put(gateKey, nextId);
    }

    public void clearAllActiveDisplays() {
        activePersistentArms.values().forEach(map -> map.keySet().forEach(BlockDisplay::remove));
        activeInteractions.values().forEach(Interaction::remove);
        activeTempTasks.values().forEach(list -> list.forEach(BukkitTask::cancel));
        activeAnimationTasks.values().forEach(BukkitTask::cancel);
        gateAngles.clear();
        gateActionIds.clear();
    }
}