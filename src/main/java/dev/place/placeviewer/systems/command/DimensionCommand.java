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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@PlaceViewerCommand
public class DimensionCommand extends BukkitCommand {

    @NotNull
    private static final String DIMENSION_COMMAND = "dimension";

    public DimensionCommand() {
        super(
            DIMENSION_COMMAND,
            "Switches your current dimension",
            "/" + DIMENSION_COMMAND + " <dimension>",
            List.of("dim")
        );
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(Component.text("Expected 1 argument: the dimension to switch to. Select from any of the following: "
                + String.join(", ", worldNames()), NamedTextColor.RED));
            return true;
        }
        final Optional<World> world = parseWorld(sender, args[0]);
        if (world.isEmpty()) return true;

        if (player.getWorld() == world.get()) {
            sender.sendMessage(Component.text("You are already in dimension " + args[0] + "."));
            return true;
        }
        final World.Environment currentEnvironment = player.getWorld().getEnvironment();
        final World.Environment newEnvironment = world.get().getEnvironment();

        final double coordinateMultiplierXZ = switch (currentEnvironment) {
            case NETHER -> newEnvironment == World.Environment.NORMAL ? 8.0d : 1.0d;
            case NORMAL -> newEnvironment == World.Environment.NETHER ? 1.0d / 8.0d : 1.0d;
            default -> 1.0d;
        };
        final int maxHeight = newEnvironment == World.Environment.NETHER ? 128 : world.get().getMaxHeight();
        TeleportCommand.teleportSafely(player, new Location(world.get(),
            player.getX() * coordinateMultiplierXZ,
            Math.clamp(player.getY(), world.get().getMinHeight(), maxHeight),
            player.getZ() * coordinateMultiplierXZ
        ));
        return true;
    }

    @NotNull
    public List<String> tabComplete(@NotNull final CommandSender sender, @NotNull final String alias, final @NotNull String @NotNull [] args) throws IllegalArgumentException {
        return worldNames();
    }

    @NotNull
    public static Optional<World> parseWorld(@NotNull final CommandSender sender, @NotNull final String arg) {
        final Optional<DimensionType> newWorld = DimensionType.ofString(arg);
        if (newWorld.isEmpty()) {
            sender.sendMessage(Component.text("Invalid argument. Select from any of the following: "
                + String.join(", ", worldNames()), NamedTextColor.RED));
            return Optional.empty();
        }
        final Optional<World> world = newWorld.get().world();
        if (world.isEmpty())
            sender.sendMessage(Component.text("Internal server error: the world was not found on the viewer server. " +
                "Please contact one of the admins of this PlaceViewer server to fix the issue."));

        return world;
    }

    @NotNull
    private static final List<String> WORLD_NAMES = Arrays.stream(DimensionType.values())
        .map(DimensionType::toString)
        .toList();

    @NotNull
    private static List<String> worldNames() {
        return WORLD_NAMES;
    }

}
