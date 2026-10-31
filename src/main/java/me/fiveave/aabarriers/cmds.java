package me.fiveave.aabarriers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

import static me.fiveave.aabarriers.main.*;

public class cmds implements CommandExecutor, TabCompleter {

    private final barrier barrierManager;
    private final warningsignal warningManager;

    public cmds(barrier barrierManager, warningsignal warningManager) {
        this.barrierManager = barrierManager;
        this.warningManager = warningManager;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aabarriers.admin")) {
            sender.sendMessage(AAB_HEAD + ChatColor.RED + "You do not have permission to execute this command!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(AAB_HEAD + ChatColor.RED + "Usage: /" + label + " [tempopen|open|close|reset|warn|reload] ...");
            return true;
        }

        String sub = args[0].toLowerCase();

        // Version 0.1.2: Processing structural hot-reload command routing profiles
        if (sub.equals("reload")) {
            plugin.reloadPluginConfigs();
            sender.sendMessage(AAB_HEAD + ChatColor.GREEN + "All configurations have been successfully reloaded!");
            return true;
        }

        // Warning sign tracking execution pipeline blocks
        if (sub.equals("warn")) {
            if (args.length < 6) {
                sender.sendMessage(AAB_HEAD + ChatColor.RED + "Usage: /" + label + " warn <on/off> <warn_preset> <x1> <y1> <z1> [x2] [y2] [z2]");
                return true;
            }
            String action = args[1].toLowerCase();
            if (!action.equals("on") && !action.equals("off")) {
                sender.sendMessage(AAB_HEAD + ChatColor.RED + "Action parameter must be 'on' or 'off'.");
                return true;
            }

            String presetName = args[2];
            World world = getTargetWorld(sender);
            if (world == null) {
                sender.sendMessage(AAB_HEAD + ChatColor.RED + "Error: Could not locate world context.");
                return true;
            }

            try {
                int x1 = parseCoordinate(sender, args[3], "x");
                int y1 = parseCoordinate(sender, args[4], "y");
                int z1 = parseCoordinate(sender, args[5], "z");
                Location light1 = new Location(world, x1, y1, z1);
                Location light2 = null;

                if (args.length >= 9) {
                    int x2 = parseCoordinate(sender, args[6], "x");
                    int y2 = parseCoordinate(sender, args[7], "y");
                    int z2 = parseCoordinate(sender, args[8], "z");
                    light2 = new Location(world, x2, y2, z2);
                }

                if (action.equals("on")) {
                    warningManager.startSignal(presetName, light1, light2);
                    sender.sendMessage(AAB_HEAD + ChatColor.GREEN + "Warning signal activated using preset: " + ChatColor.YELLOW + presetName);
                } else {
                    warningManager.stopSignal(light1, light2);
                    sender.sendMessage(AAB_HEAD + ChatColor.GREEN + "Warning signal deactivated.");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(AAB_HEAD + ChatColor.RED + "Input Error: Invalid parameter coordinates entered.");
            }
            return true;
        }

        // --- Standard barrier logic engine paths ---
        if (!sub.equals("tempopen") && !sub.equals("open") && !sub.equals("close") && !sub.equals("reset")) {
            sender.sendMessage(AAB_HEAD + ChatColor.RED + "Unknown action parameter.");
            return true;
        }

        int requiredArgs = sub.equals("reset") ? 7 : (sub.equals("tempopen") ? 9 : 8);
        if (args.length < requiredArgs) {
            if (sub.equals("reset")) {
                sender.sendMessage(AAB_HEAD + ChatColor.RED + "Correct Usage: /" + label + " reset <startX> <startY> <startZ> <endX> <endY> <endZ>");
            } else if (sub.equals("tempopen")) {
                sender.sendMessage(AAB_HEAD + ChatColor.RED + "Correct Usage: /" + label + " tempopen <startX> <startY> <startZ> <endX> <endY> <endZ> <rotSpeed> <duration>");
            } else {
                sender.sendMessage(AAB_HEAD + ChatColor.RED + "Correct Usage: /" + label + " " + sub + " <startX> <startY> <startZ> <endX> <endY> <endZ> <rotSpeed>");
            }
            return true;
        }

        World world = getTargetWorld(sender);
        if (world == null) {
            sender.sendMessage(AAB_HEAD + ChatColor.RED + "Error: Could not locate world context.");
            return true;
        }

        try {
            int startX = parseCoordinate(sender, args[1], "x");
            int startY = parseCoordinate(sender, args[2], "y");
            int startZ = parseCoordinate(sender, args[3], "z");
            int endX = parseCoordinate(sender, args[4], "x");
            int endY = parseCoordinate(sender, args[5], "y");
            int endZ = parseCoordinate(sender, args[6], "z");

            Location startLocation = new Location(world, startX, startY, startZ);
            Location endLocation = new Location(world, endX, endY, endZ);

            if (sub.equals("reset")) {
                barrierManager.forceResetGate(startLocation, endLocation);
                sender.sendMessage(AAB_HEAD + ChatColor.GREEN + "Forced structural reset completed.");
                return true;
            }

            int rotSpeed = Integer.parseInt(args[7]);
            if (rotSpeed <= 0) {
                sender.sendMessage(AAB_HEAD + ChatColor.RED + "Input Error: Rotation speed must be greater than 0!");
                return true;
            }

            if (sub.equals("tempopen")) {
                int duration = Integer.parseInt(args[8]);
                if (duration < (2 * rotSpeed)) {
                    sender.sendMessage(AAB_HEAD + ChatColor.RED + "Input Error: Total duration must be >= 2x rotation speed!");
                    return true;
                }
                barrierManager.activateBarrierArm(startLocation, endLocation, rotSpeed, duration);
            } else {
                barrierManager.setPersistentGateState(startLocation, endLocation, rotSpeed, sub.equals("open"));
            }

            sender.sendMessage(AAB_HEAD + ChatColor.GREEN + "Barrier arm action executed successfully.");
        } catch (NumberFormatException e) {
            sender.sendMessage(AAB_HEAD + ChatColor.RED + "Input Error: Invalid parameter coordinates or timing digits entered.");
        }

        return true;
    }

    private World getTargetWorld(CommandSender sender) {
        if (sender instanceof Player) return ((Player) sender).getWorld();
        if (sender instanceof org.bukkit.command.BlockCommandSender)
            return ((org.bukkit.command.BlockCommandSender) sender).getBlock().getWorld();
        return Bukkit.getWorlds().getFirst();
    }

    private int parseCoordinate(CommandSender sender, String arg, String axis) {
        if (arg.startsWith("~")) {
            Location loc = null;
            if (sender instanceof Player) loc = ((Player) sender).getLocation();
            else if (sender instanceof org.bukkit.command.BlockCommandSender)
                loc = ((org.bukkit.command.BlockCommandSender) sender).getBlock().getLocation();

            int base = 0;
            if (loc != null) {
                if (axis.equalsIgnoreCase("x")) base = loc.getBlockX();
                else if (axis.equalsIgnoreCase("y")) base = loc.getBlockY();
                else if (axis.equalsIgnoreCase("z")) base = loc.getBlockZ();
            }
            if (arg.length() == 1) return base;
            return base + Integer.parseInt(arg.substring(1));
        }
        return Integer.parseInt(arg);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        Block targetBlock = player.getTargetBlockExact(5);

        if (args.length == 1) {
            suggestions.addAll(Arrays.asList("tempopen", "open", "close", "reset", "warn", "reload"));
            StringUtil.copyPartialMatches(args[0], suggestions, completions);
            return completions;
        }

        String sub = args[0].toLowerCase();

        // Handle reload suggestions early
        if (sub.equals("reload")) {
            return Collections.emptyList();
        }

        if (sub.equals("warn")) {
            switch (args.length) {
                case 2 -> suggestions.addAll(Arrays.asList("on", "off"));
                case 3 -> {
                    if (warnPresets != null && warnPresets.dataconfig != null && warnPresets.dataconfig.contains("presets")) {
                        suggestions.addAll(Objects.requireNonNull(warnPresets.dataconfig.getConfigurationSection("presets")).getKeys(false));
                    } else {
                        suggestions.add("default");
                    }
                }
                case 4, 7 -> {
                    suggestions.add("~");
                    if (targetBlock != null) suggestions.add(String.valueOf(targetBlock.getX()));
                }
                case 5, 8 -> {
                    suggestions.add("~");
                    if (targetBlock != null) suggestions.add(String.valueOf(targetBlock.getY()));
                }
                case 6, 9 -> {
                    suggestions.add("~");
                    if (targetBlock != null) suggestions.add(String.valueOf(targetBlock.getZ()));
                }
            }
            StringUtil.copyPartialMatches(args[args.length - 1], suggestions, completions);
            return completions;
        }
        if (!sub.equals("tempopen") && !sub.equals("open") && !sub.equals("close") && !sub.equals("reset")) {
            return Collections.emptyList();
        }
        switch (args.length) {
            case 2, 5 -> {
                suggestions.add("~");
                if (targetBlock != null) suggestions.add(String.valueOf(targetBlock.getX()));
            }
            case 3, 6 -> {
                suggestions.add("~");
                if (targetBlock != null) suggestions.add(String.valueOf(targetBlock.getY()));
            }
            case 4, 7 -> {
                suggestions.add("~");
                if (targetBlock != null) suggestions.add(String.valueOf(targetBlock.getZ()));
            }
            case 8 -> {
                if (!sub.equals("reset")) suggestions.addAll(Arrays.asList("10", "20", "40", "[rotSpeed]"));
            }
            case 9 -> {
                if (sub.equals("tempopen")) {
                    suggestions.addAll(Arrays.asList("60", "120", "240", "[duration]"));
                }
            }
        }
        StringUtil.copyPartialMatches(args[args.length - 1], suggestions, completions);
        return completions;
    }
}