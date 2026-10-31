package me.fiveave.aabarriers;

import org.bukkit.Location;
import org.bukkit.World;

public class metadata {
    private final Location start;
    private final Location end;
    private final String key;

    public metadata(Location start, Location end) {
        this.start = start;
        this.end = end;
        this.key = start.getBlockX() + "_" + start.getBlockY() + "_" + start.getBlockZ() + "_" +
                end.getBlockX() + "_" + end.getBlockY() + "_" + end.getBlockZ();
    }

    public Location getStart() {
        return start;
    }

    public Location getEnd() {
        return end;
    }

    public String getKey() {
        return key;
    }

    public World getWorld() {
        return start.getWorld();
    }
}
