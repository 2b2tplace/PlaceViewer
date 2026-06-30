package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@PlaceViewerCommand
public class IgnoreTpaCommand extends BukkitCommand {

    public IgnoreTpaCommand() {
        super("ignoretpa", "Toggle teleport requests from other players", "/ignoretpa", List.of());
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        final boolean nowIgnoring = TpaCommand.toggleIgnore(player.getUniqueId());
        player.sendMessage(Component.text(
            nowIgnoring ? "You will no longer receive teleport requests." : "You will now receive teleport requests again.",
            NamedTextColor.GOLD
        ));
        return true;
    }

}
