package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import dev.place.placeviewer.systems.region.DimensionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static dev.place.placeviewer.systems.command.CommandUtil.parseIntFromArgs;

@PlaceViewerCommand
public class TeleportCommand extends BukkitCommand {

    @NotNull
    private static final String TELEPORT_COMMAND = "teleport";

    public TeleportCommand() {
        super(
            TELEPORT_COMMAND,
            "Teleports you to a selected position",
            "/" + TELEPORT_COMMAND + " <x> <y> <z> [dimension] or /" + TELEPORT_COMMAND + " <x> <z>",
            List.of("tp")
        );
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        Integer x = null;
        Integer y = player.getLocation().getBlockY();
        Integer z = null;
        if (args.length == 2) {
            final CommandUtil.IntParseResult xResult = parseIntFromArgs(player, args, 0,
                arg -> "Invalid x coordinate: '" + args[0] + "'. The input must be a valid number.");

            final CommandUtil.IntParseResult zResult = parseIntFromArgs(player, args, 1,
                arg -> "Invalid z coordinate: '" + args[1] + "'. The input must be a valid number.");

            x = xResult.result();
            z = zResult.result();

            if (x == null || z == null) return true;
        } else if (args.length < 3 || args.length > 4) {
            sender.sendMessage(Component.text("Usage: " + getUsage(), NamedTextColor.RED));
            return true;
        } else {
            final CommandUtil.IntParseResult xResult = parseIntFromArgs(player, args, 0,
                arg -> "Invalid x coordinate: '" + arg + "'. The input must be a valid number.");

            final CommandUtil.IntParseResult yResult = parseIntFromArgs(player, args, 1,
                arg -> "Invalid y coordinate: '" + arg + "'. The input must be a valid number.");

            final CommandUtil.IntParseResult zResult = parseIntFromArgs(player, args, 2,
                arg -> "Invalid z coordinate: '" + arg + "'. The input must be a valid number.");

            x = xResult.result();
            y = yResult.result();
            z = zResult.result();

            if (x == null || y == null || z == null) return true;
        }
        World newWorld = player.getWorld();
        if (args.length == 4) {
            final Optional<World> world = DimensionCommand.parseWorld(sender, args[3]);
            if (world.isEmpty()) return true;
            newWorld = world.get();
        }
        player.teleport(new Location(newWorld, x, y, z, player.getYaw(), player.getPitch()));
        player.sendMessage(Component.text("You were teleported to "
            + x + " " + y + " " + z + " in dimension "
            + DimensionType.dimensionType(newWorld.getEnvironment())
            + ".", NamedTextColor.GOLD));

        return true;
    }

}
