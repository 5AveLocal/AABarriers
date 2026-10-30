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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static me.fiveave.aabarriers.main.AAB_HEAD;

public class cmds implements CommandExecutor, TabCompleter {

    private final barrier barrierManager;

    public cmds(barrier barrierManager) {
        this.barrierManager = barrierManager;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Permission Check
        if (!sender.hasPermission("aabarriers.admin")) {
            sender.sendMessage(AAB_HEAD + ChatColor.RED + "You do not have permission to execute this command!");
            return true;
        }

        // 2. Base Validation Check
        if (args.length == 0) {
            sender.sendMessage(AAB_HEAD + ChatColor.RED + "Usage: /" + label + " [tempopen|open|close|reset] ...");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (!sub.equals("tempopen") && !sub.equals("open") && !sub.equals("close") && !sub.equals("reset")) {
            sender.sendMessage(AAB_HEAD + ChatColor.RED + "Unknown action parameter.");
            return true;
        }

        // 3. Dynamic Argument Length Check
        // reset needs 7 items, open/close needs 8 items, tempopen needs 9 items
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

        // 4. Determine target environment world context
        World world;
        if (sender instanceof Player) {
            world = ((Player) sender).getWorld();
        } else if (sender instanceof org.bukkit.command.BlockCommandSender) {
            world = ((org.bukkit.command.BlockCommandSender) sender).getBlock().getWorld();
        } else {
            world = Bukkit.getWorlds().getFirst(); // Fallback for console executions
        }

        if (world == null) {
            sender.sendMessage(AAB_HEAD + ChatColor.RED + "Error: Could not locate world context.");
            return true;
        }

        // 5. Parse Arguments securely with proper sub-command array offsets (+1 shift)
        try {
            int startX = parseCoordinate(sender, args[1], "x");
            int startY = parseCoordinate(sender, args[2], "y");
            int startZ = parseCoordinate(sender, args[3], "z");
            int endX = parseCoordinate(sender, args[4], "x");
            int endY = parseCoordinate(sender, args[5], "y");
            int endZ = parseCoordinate(sender, args[6], "z");

            Location startLocation = new Location(world, startX, startY, startZ);
            Location endLocation = new Location(world, endX, endY, endZ);

            // Execute immediate structural data storage reset if triggered
            if (sub.equals("reset")) {
                barrierManager.forceResetGate(startLocation, endLocation);
                sender.sendMessage(AAB_HEAD + ChatColor.GREEN + "Forced structural reset completed. Physical block parameters restored from configuration files.");
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
                // Persistent Open or Close toggle logic mapping
                barrierManager.setPersistentGateState(startLocation, endLocation, rotSpeed, sub.equals("open"));
            }

            sender.sendMessage(AAB_HEAD + ChatColor.GREEN + "Barrier arm action executed successfully.");

        } catch (NumberFormatException e) {
            sender.sendMessage(AAB_HEAD + ChatColor.RED + "Input Error: Invalid parameter coordinates or timing digits entered.");
        }

        return true;
    }

    /**
     * Helper utility method to parse absolute numbers or relative tilde (~) values
     */
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

    // =========================================================================
    // Tab Completer Mechanism (Subcommand Routing)
    // =========================================================================
    @SuppressWarnings("NullableProblems")
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        Block targetBlock = player.getTargetBlockExact(5);

        if (args.length == 1) {
            suggestions.addAll(Arrays.asList("tempopen", "open", "close", "reset"));
            StringUtil.copyPartialMatches(args[0], suggestions, completions);
            return completions;
        }

        String sub = args[0].toLowerCase();
        if (!sub.equals("tempopen") && !sub.equals("open") && !sub.equals("close") && !sub.equals("reset")) {
            return Collections.emptyList();
        }

        switch (args.length) {
            case 2: // startX
                suggestions.add("~");
                if (targetBlock != null) {
                    suggestions.add(String.valueOf(targetBlock.getX()));
                    suggestions.add(targetBlock.getX() + " " + targetBlock.getY() + " " + targetBlock.getZ());
                }
                break;
            case 3: // startY
                suggestions.add("~");
                if (targetBlock != null) suggestions.add(String.valueOf(targetBlock.getY()));
                break;
            case 4: // startZ
                suggestions.add("~");
                if (targetBlock != null) suggestions.add(String.valueOf(targetBlock.getZ()));
                break;
            case 5: // endX
                suggestions.add("~");
                if (targetBlock != null) {
                    suggestions.add(String.valueOf(targetBlock.getX()));
                    suggestions.add(targetBlock.getX() + " " + targetBlock.getY() + " " + targetBlock.getZ());
                }
                break;
            case 6: // endY
                suggestions.add("~");
                if (targetBlock != null) suggestions.add(String.valueOf(targetBlock.getY()));
                break;
            case 7: // endZ
                suggestions.add("~");
                if (targetBlock != null) suggestions.add(String.valueOf(targetBlock.getZ()));
                break;
            case 8: // rotSpeed (Only applicable to moving variants)
                if (!sub.equals("reset")) suggestions.addAll(Arrays.asList("10", "20", "40", "[rotSpeed (Ticks)]"));
                break;
            case 9: // duration (Only applicable to tempopen)
                if (sub.equals("tempopen")) {
                    suggestions.addAll(Arrays.asList("60", "120", "240", "[duration (Ticks)]"));
                    try {
                        int speed = Integer.parseInt(args[7]);
                        suggestions.add(String.valueOf(speed * 2));
                    } catch (NumberFormatException ignored) {
                    }
                }
                break;
        }

        StringUtil.copyPartialMatches(args[args.length - 1], suggestions, completions);
        completions.sort((s1, s2) -> {
            if (s1.startsWith("[") || s1.startsWith("<")) return 1;
            if (s2.startsWith("[") || s2.startsWith("<")) return -1;
            return s1.compareToIgnoreCase(s2);
        });

        return completions;
    }
}
