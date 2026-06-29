package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@PlaceViewerCommand
public class HereCommand extends BukkitCommand {

    @NotNull
    private static final Set<UUID> IGNORING_HERE = ConcurrentHashMap.newKeySet();

    public HereCommand() {
        super("here", "Broadcast your current coordinates to all players", "/here", List.of());
    }

    public static boolean isIgnoring(@NotNull final UUID uuid) {
        return IGNORING_HERE.contains(uuid);
    }

    public static boolean toggleIgnore(@NotNull final UUID uuid) {
        if (IGNORING_HERE.contains(uuid)) {
            IGNORING_HERE.remove(uuid);
            return false;
        }
        IGNORING_HERE.add(uuid);
        return true;
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        final Location loc = player.getLocation();
        final int x = loc.getBlockX();
        final int y = loc.getBlockY();
        final int z = loc.getBlockZ();

        final Component coords = Component.text("[" + x + ", " + y + ", " + z + "]", NamedTextColor.YELLOW)
            .clickEvent(ClickEvent.runCommand("/tp " + x + " " + y + " " + z))
            .hoverEvent(HoverEvent.showText(Component.text("Click to teleport", NamedTextColor.GRAY)));

        final Component message = Component.text(player.getName(), NamedTextColor.GOLD)
            .append(Component.text(" is at ", NamedTextColor.WHITE))
            .append(coords);

        Bukkit.getOnlinePlayers().stream()
            .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
            .filter(p -> !isIgnoring(p.getUniqueId()))
            .forEach(p -> p.sendMessage(message));

        player.sendMessage(Component.text("Your location was broadcast to all players.", NamedTextColor.GRAY));
        return true;
    }

}
