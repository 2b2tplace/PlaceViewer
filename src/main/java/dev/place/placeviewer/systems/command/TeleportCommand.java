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
import org.jetbrains.annotations.Nullable;

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
            "/" + TELEPORT_COMMAND + " <x> <y> <z> [dimension] or /" + TELEPORT_COMMAND + " <x> <z> [dimension] or /" + TELEPORT_COMMAND + " <player>",
            List.of("tp")
        );
    }

    @NotNull
    private static IntParseResult parseCoordinate(@NotNull final Player player, @NotNull final String @NotNull [] args, final int index, final char coordinate) {
        if (index < 0 || index >= args.length || !args[index].startsWith("~"))
            return parseInt(player, args[index], arg -> "Invalid " + coordinate + " coordinate: '" + arg + "'. The input must be a valid number.");

        final Location location = player.getLocation();
        final Integer coordinateValue = switch (coordinate) {
            case 'x' -> location.getBlockX();
            case 'y' -> location.getBlockY();
            case 'z' -> location.getBlockZ();
            default -> throw new IllegalArgumentException("Coordinate must be 'x', 'y', or 'z'");
        };
        final String remaining = args[index].substring(1);
        if (remaining.isEmpty())
            return new IntParseResult(true, coordinateValue);

        final IntParseResult delta = parseInt(player, remaining, arg -> "Invalid " + coordinate + " delta: '" + arg + "'. The input must be a valid number.");
        if (delta.result() == null)
            return new IntParseResult(true, null);

        return new IntParseResult(true, coordinateValue + delta.result());
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

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        Integer x;
        Integer y = player.getLocation().getBlockY();
        Integer z;
        World newWorld = player.getWorld();

        switch (args.length) {
            case 1 -> {
                teleportToPlayer(player, args[0]);
                return true;
            }
            case 2 -> {
                final IntParseResult xResult = parseCoordinate(player, args, 0, 'x');
                final IntParseResult zResult = parseCoordinate(player, args, 1, 'z');

                x = xResult.result();
                z = zResult.result();

                if (x == null || z == null) return true;
            }
            case 3 -> {
                final Optional<World> world = DimensionType.ofString(args[2]).flatMap(DimensionType::world);
                if (world.isPresent()) {
                    newWorld = world.get();

                    final IntParseResult xResult = parseCoordinate(player, args, 0, 'x');
                    final IntParseResult yResult = parseCoordinate(player, args, 1, 'z');

                    x = xResult.result();
                    z = yResult.result();

                    if (x == null || z == null) return true;
                    break;
                }
                final IntParseResult xResult = parseCoordinate(player, args, 0, 'x');
                final IntParseResult yResult = parseCoordinate(player, args, 1, 'y');
                final IntParseResult zResult = parseCoordinate(player, args, 2, 'z');

                x = xResult.result();
                y = yResult.result();
                z = zResult.result();

                if (x == null || y == null || z == null) return true;
            }
            case 4 -> {
                final IntParseResult xResult = parseCoordinate(player, args, 0, 'x');
                final IntParseResult yResult = parseCoordinate(player, args, 1, 'y');
                final IntParseResult zResult = parseCoordinate(player, args, 2, 'z');

                x = xResult.result();
                y = yResult.result();
                z = zResult.result();

                if (x == null || y == null || z == null) return true;

                final Optional<World> world = DimensionCommand.parseWorld(sender, args[3]);
                if (world.isEmpty()) return true;

                newWorld = world.get();
            }
            default -> {
                sender.sendMessage(Component.text("Usage: " + getUsage(), NamedTextColor.RED));
                return true;
            }
        }
        player.teleport(new Location(newWorld, x, y, z, player.getYaw(), player.getPitch()));
        player.sendMessage(Component.text("You were teleported to "
            + x + " " + y + " " + z + " in dimension "
            + DimensionType.dimensionType(newWorld.getEnvironment())
            + ".", NamedTextColor.GOLD));

        return true;
    }

}
