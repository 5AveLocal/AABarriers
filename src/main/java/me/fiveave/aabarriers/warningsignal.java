package me.fiveave.aabarriers;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static me.fiveave.aabarriers.main.plugin;
import static me.fiveave.aabarriers.main.warnPresets;

public class warningsignal {

    private final Map<String, List<BukkitTask>> activeSignalTasks = new HashMap<>();

    public void startSignal(String presetName, Location light1, Location light2) {
        String key = generateSignalKey(light1, light2);
        if (activeSignalTasks.containsKey(key)) return;

        List<BukkitTask> associatedTasks = new ArrayList<>();

        String basePath = "presets." + presetName;
        if (warnPresets == null || warnPresets.dataconfig == null || !warnPresets.dataconfig.contains(basePath)) {
            basePath = "presets.default";
        }

        int interval = 10;
        List<String> rawSoundList = new ArrayList<>();

        if (warnPresets != null && warnPresets.dataconfig != null) {
            interval = warnPresets.dataconfig.getInt(basePath + ".flash_interval_ticks", 10);
            rawSoundList = warnPresets.dataconfig.getStringList(basePath + ".sounds");
        }

        BlockDisplay display1 = findBlockDisplayAt(light1);
        BlockDisplay display2 = (light2 != null) ? findBlockDisplayAt(light2) : null;

        // Visual loop handling both entity glow options and physical Lightable block toggle states
        BukkitTask flashTask = new BukkitRunnable() {
            boolean phaseToggle = false;

            @Override
            public void run() {
                phaseToggle = !phaseToggle;

                // Process primary coordinate target
                if (display1 != null) {
                    display1.setGlowing(phaseToggle);
                    display1.setGlowColorOverride(phaseToggle ? Color.RED : null);
                }
                togglePhysicalBlockLight(light1, phaseToggle);

                // Process secondary coordinate target if defined
                if (display2 != null) {
                    display2.setGlowing(!phaseToggle);
                    display2.setGlowColorOverride(!phaseToggle ? Color.RED : null);
                }
                if (light2 != null) {
                    togglePhysicalBlockLight(light2, !phaseToggle);
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
        associatedTasks.add(flashTask);

        if (!rawSoundList.isEmpty()) {
            final List<String> finalSounds = rawSoundList;
            BukkitTask soundTask = new BukkitRunnable() {
                @Override
                public void run() {
                    executeSoundCascade(key, light1, finalSounds, 0);
                }
            }.runTaskTimer(plugin, 0L, calculateTotalSoundSequenceDuration(rawSoundList));
            associatedTasks.add(soundTask);
        }

        activeSignalTasks.put(key, associatedTasks);
    }

    public void stopSignal(Location light1, Location light2) {
        String key = generateSignalKey(light1, light2);
        List<BukkitTask> tasks = activeSignalTasks.remove(key);
        if (tasks != null) {
            tasks.forEach(BukkitTask::cancel);
        }

        BlockDisplay d1 = findBlockDisplayAt(light1);
        if (d1 != null) {
            d1.setGlowing(false);
            d1.setGlowColorOverride(null);
        }
        togglePhysicalBlockLight(light1, false); // Turn off physical block lights immediately

        if (light2 != null) {
            BlockDisplay d2 = findBlockDisplayAt(light2);
            if (d2 != null) {
                d2.setGlowing(false);
                d2.setGlowColorOverride(null);
            }
            togglePhysicalBlockLight(light2, false);
        }
    }

    public void stopAllSignals() {
        new HashSet<>(activeSignalTasks.keySet()).forEach(key -> {
            List<BukkitTask> tasks = activeSignalTasks.remove(key);
            if (tasks != null) tasks.forEach(BukkitTask::cancel);
        });
    }

    /**
     * Safely updates physical blocks (Redstone Lamps, Torches, etc.) that support the Lightable interface.
     */
    private void togglePhysicalBlockLight(Location loc, boolean lit) {
        Block block = loc.getBlock();
        BlockData data = block.getBlockData();
        if (data instanceof Lightable lightable) {
            lightable.setLit(lit);
            block.setBlockData(lightable, false); // Update block data silently without triggering physics break cascades
        }
    }

    private void executeSoundCascade(String trackingKey, Location loc, List<String> sounds, int index) {
        if (index >= sounds.size() || !activeSignalTasks.containsKey(trackingKey)) return;

        try {
            String[] split = sounds.get(index).split(",");
            String soundName = split[0].trim().toUpperCase();
            float volume = Float.parseFloat(split[1].trim());
            float pitch = Float.parseFloat(split[2].trim());
            long nextDelay = Long.parseLong(split[3].trim());

            World world = loc.getWorld();
            if (world != null) {
                //noinspection deprecation
                world.playSound(loc, Sound.valueOf(soundName), volume, pitch);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    executeSoundCascade(trackingKey, loc, sounds, index + 1);
                }
            }.runTaskLater(plugin, nextDelay);

        } catch (Exception ignored) {
        }
    }

    private long calculateTotalSoundSequenceDuration(List<String> sounds) {
        long totalTicks = 0;
        for (String entry : sounds) {
            try {
                totalTicks += Long.parseLong(entry.split(",")[3].trim());
            } catch (Exception e) {
                totalTicks += 10;
            }
        }
        return Math.max(1L, totalTicks);
    }

    private BlockDisplay findBlockDisplayAt(Location loc) {
        World w = loc.getWorld();
        if (w == null) return null;
        return w.getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), 0.6, 0.6, 0.6).stream()
                .filter(e -> e instanceof BlockDisplay)
                .map(e -> (BlockDisplay) e)
                .findFirst()
                .orElse(null);
    }

    public String generateSignalKey(Location l1, Location l2) {
        if (l2 == null) {
            return "warn_" + l1.getBlockX() + "_" + l1.getBlockY() + "_" + l1.getBlockZ();
        }
        String k1 = l1.getBlockX() + "_" + l1.getBlockY() + "_" + l1.getBlockZ();
        String k2 = l2.getBlockX() + "_" + l2.getBlockY() + "_" + l2.getBlockZ();
        return (k1.compareTo(k2) <= 0) ? "warn_" + k1 + "_" + k2 : "warn_" + k2 + "_" + k1;
    }
}
