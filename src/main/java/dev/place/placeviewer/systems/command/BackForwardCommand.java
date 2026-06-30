package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.place.placeviewer.systems.command.CommandUtil.*;

@PlaceViewerCommand
public class BackForwardCommand extends BukkitCommand {

    static final int MAX_HISTORY = 50;

    private static final Map<UUID, List<Location>> HISTORY = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> HISTORY_INDEX = new ConcurrentHashMap<>();
    public static final Set<UUID> NAVIGATING = ConcurrentHashMap.newKeySet();

    public BackForwardCommand() {
        super("back", "Teleport back to a previous location", "/back [n]", List.of("b", "forward", "f"));
    }

    public static void recordTeleport(@NotNull final UUID uuid, @NotNull final Location destination) {
        final List<Location> history = HISTORY.computeIfAbsent(uuid, k -> new ArrayList<>());
        final int index = HISTORY_INDEX.getOrDefault(uuid, history.size() - 1);

        if (index < history.size() - 1)
            history.subList(index + 1, history.size()).clear();

        history.add(destination.clone());

        while (history.size() > MAX_HISTORY)
            history.removeFirst();

        HISTORY_INDEX.put(uuid, history.size() - 1);
    }

    public static void clearHistory(@NotNull final UUID uuid) {
        HISTORY.remove(uuid);
        HISTORY_INDEX.remove(uuid);
        NAVIGATING.remove(uuid);
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        return switch (command) {
            case "back", "b" -> navigate(player, args, -1);
            case "forward", "f" -> navigate(player, args, 1);
            default -> false;
        };
    }

    private static boolean navigate(@NotNull final Player player, @NotNull final String @NotNull [] args, final int direction) {
        final UUID uuid = player.getUniqueId();
        final List<Location> history = HISTORY.get(uuid);

        if (history == null || history.size() < 2) {
            player.sendMessage(Component.text("No teleport history available.", NamedTextColor.RED));
            return true;
        }

        final int currentIndex = HISTORY_INDEX.getOrDefault(uuid, history.size() - 1);

        int steps = 1;
        if (args.length > 0) {
            final IntParseResult result = parseInt(player, args[0], arg -> "Invalid number: '" + arg + "'. The input must be a valid number.");
            if (result.result() == null) return true;
            steps = Math.max(1, result.result());
        }

        final int targetIndex = Math.clamp(currentIndex + direction * steps, 0, history.size() - 1);

        if (targetIndex == currentIndex) {
            final boolean isBack = direction < 0;
            player.sendMessage(Component.text("You are already at the " + (isBack ? "oldest" : "newest") + " position in your history.", NamedTextColor.GRAY));
            return true;
        }

        NAVIGATING.add(uuid);
        TeleportCommand.teleportSafely(player, history.get(targetIndex));
        NAVIGATING.remove(uuid);
        HISTORY_INDEX.put(uuid, targetIndex);

        final int stepsActual = Math.abs(targetIndex - currentIndex);
        final String directionStr = direction < 0 ? "back" : "forward";
        player.sendMessage(Component.text("Teleported " + directionStr + " " + stepsActual + " step" + (stepsActual != 1 ? "s" : "") + ".", NamedTextColor.GOLD));
        return true;
    }

}
