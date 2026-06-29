package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import dev.place.placeviewer.systems.region.DimensionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static dev.place.placeviewer.systems.command.CommandUtil.*;

@PlaceViewerCommand
public class TeleportCommand extends BukkitCommand {

    @NotNull
    private static final String TELEPORT_COMMAND = "teleport";

    public TeleportCommand() {
        super(
            TELEPORT_COMMAND,
            "Teleports you to a selected position",
            "/" + TELEPORT_COMMAND + " <x> <y> <z> [yaw] [pitch] or /" + TELEPORT_COMMAND + " <x> <y> <z> [dimension] [yaw pitch] or /" + TELEPORT_COMMAND + " <x> <z> [dimension]",
            List.of("tp")
        );
    }

    @NotNull
    private static DoubleParseResult parseCoordinate(@NotNull final Player player, @NotNull final String @NotNull [] args, final int index, final char coordinate) {
        if (index < 0 || index >= args.length || !args[index].startsWith("~"))
            return parseDouble(player, args[index], arg -> "Invalid " + coordinate + " coordinate: '" + arg + "'. The input must be a valid number.");

        final double coordinateValue = switch (coordinate) {
            case 'x' -> player.getX();
            case 'y' -> player.getY();
            case 'z' -> player.getZ();
            default -> throw new IllegalArgumentException("Coordinate must be 'x', 'y', or 'z'");
        };
        final String remaining = args[index].substring(1);
        if (remaining.isEmpty())
            return new DoubleParseResult(true, coordinateValue);

        final DoubleParseResult delta = parseDouble(player, remaining, arg -> "Invalid " + coordinate + " delta: '" + arg + "'. The input must be a valid number.");
        if (delta.result() == null)
            return new DoubleParseResult(true, null);

        return new DoubleParseResult(true, coordinateValue + delta.result());
    }

    private static void teleportToPlayer(@NotNull final Player player, @NotNull final String target) {
        final Player targetPlayer = Bukkit.getServer().getPlayer(target);
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Player '" + target + "' was not found.", NamedTextColor.RED));
            return;
        }
        player.teleport(targetPlayer);
        player.sendMessage(Component.text("Teleported to player " + target + ".", NamedTextColor.GOLD));
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        final List<String> argsList = new ArrayList<>(Arrays.stream(args).toList());
        argsList.removeIf(s -> s.startsWith("@"));
        args = argsList.stream()
            .flatMap(s -> Arrays.stream(s.replace(",", " ").split(" ")))
            .filter(s -> !s.isBlank())
            .toList()
            .toArray(new String[0]);

        Double x;
        Double y = player.getY();
        Double z;
        World newWorld = player.getWorld();
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        switch (args.length) {
//            case 1 -> {
//                teleportToPlayer(player, args[0]);
//                return true;
//            }
            case 2 -> {
                final DoubleParseResult xResult = parseCoordinate(player, args, 0, 'x');
                final DoubleParseResult zResult = parseCoordinate(player, args, 1, 'z');

                x = xResult.result();
                z = zResult.result();

                if (x == null || z == null) return true;
            }
            case 3 -> {
                final Optional<World> world = DimensionType.ofString(args[2]).flatMap(DimensionType::world);
                if (world.isPresent()) {
                    newWorld = world.get();

                    final DoubleParseResult xResult = parseCoordinate(player, args, 0, 'x');
                    final DoubleParseResult yResult = parseCoordinate(player, args, 1, 'z');

                    x = xResult.result();
                    z = yResult.result();

                    if (x == null || z == null) return true;
                    break;
                }
                final DoubleParseResult xResult = parseCoordinate(player, args, 0, 'x');
                final DoubleParseResult yResult = parseCoordinate(player, args, 1, 'y');
                final DoubleParseResult zResult = parseCoordinate(player, args, 2, 'z');

                x = xResult.result();
                y = yResult.result();
                z = zResult.result();

                if (x == null || y == null || z == null) return true;
            }
            case 4 -> {
                final DoubleParseResult xResult = parseCoordinate(player, args, 0, 'x');
                final DoubleParseResult yResult = parseCoordinate(player, args, 1, 'y');
                final DoubleParseResult zResult = parseCoordinate(player, args, 2, 'z');

                x = xResult.result();
                y = yResult.result();
                z = zResult.result();

                if (x == null || y == null || z == null) return true;

                // if arg[3] parses as a number its yaw otherwise treat as dimension
                final DoubleParseResult yawResult = parseDouble(player, args[3], null);
                if (yawResult.result() != null) {
                    yaw = yawResult.result().floatValue();
                } else {
                    final Optional<World> world = DimensionCommand.parseWorld(sender, args[3]);
                    if (world.isEmpty()) return true;
                    newWorld = world.get();
                }
            }
            case 5 -> {
                final DoubleParseResult xResult = parseCoordinate(player, args, 0, 'x');
                final DoubleParseResult yResult = parseCoordinate(player, args, 1, 'y');
                final DoubleParseResult zResult = parseCoordinate(player, args, 2, 'z');
                final DoubleParseResult yawResult = parseDouble(player, args[3], arg -> "Invalid yaw: '" + arg + "'. The input must be a valid number.");
                final DoubleParseResult pitchResult = parseDouble(player, args[4], arg -> "Invalid pitch: '" + arg + "'. The input must be a valid number.");

                x = xResult.result();
                y = yResult.result();
                z = zResult.result();

                if (x == null || y == null || z == null || yawResult.result() == null || pitchResult.result() == null) return true;

                yaw = yawResult.result().floatValue();
                pitch = pitchResult.result().floatValue();
            }
            case 6 -> {
                final DoubleParseResult xResult = parseCoordinate(player, args, 0, 'x');
                final DoubleParseResult yResult = parseCoordinate(player, args, 1, 'y');
                final DoubleParseResult zResult = parseCoordinate(player, args, 2, 'z');
                final DoubleParseResult yawResult = parseDouble(player, args[4], arg -> "Invalid yaw: '" + arg + "'. The input must be a valid number.");
                final DoubleParseResult pitchResult = parseDouble(player, args[5], arg -> "Invalid pitch: '" + arg + "'. The input must be a valid number.");

                x = xResult.result();
                y = yResult.result();
                z = zResult.result();

                if (x == null || y == null || z == null || yawResult.result() == null || pitchResult.result() == null) return true;

                final Optional<World> world = DimensionCommand.parseWorld(sender, args[3]);
                if (world.isEmpty()) return true;

                newWorld = world.get();
                yaw = yawResult.result().floatValue();
                pitch = pitchResult.result().floatValue();
            }
            default -> {
                sender.sendMessage(Component.text("Usage: " + getUsage(), NamedTextColor.RED));
                return true;
            }
        }
        teleportSafely(player, new Location(newWorld, x, y, z, yaw, pitch));
        player.sendMessage(Component.text("You were teleported to "
            + x + " " + y + " " + z + " in dimension "
            + DimensionType.dimensionType(newWorld.getEnvironment())
            + ".", NamedTextColor.GOLD));

        return true;
    }

    private static final double MAX_COORDINATE = 29999999.0d;

    public static boolean teleportSafely(@NotNull final Player player, @NotNull final Location target) {
        double x = target.x();
        double z = target.z();

        if (x > MAX_COORDINATE)
            x = MAX_COORDINATE;

        if (x < -MAX_COORDINATE)
            x = -MAX_COORDINATE;

        if (z > MAX_COORDINATE)
            z = MAX_COORDINATE;

        if (z < -MAX_COORDINATE)
            z = -MAX_COORDINATE;

        return player.teleport(new Location(target.getWorld(), x, target.y(), z, target.getYaw(), target.getPitch()));
    }

}
