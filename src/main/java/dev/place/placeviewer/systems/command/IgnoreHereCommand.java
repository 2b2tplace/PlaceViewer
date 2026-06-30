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
public class IgnoreHereCommand extends BukkitCommand {

    public IgnoreHereCommand() {
        super("ignorehere", "Toggle /here broadcasts from other players", "/ignorehere", List.of());
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        final boolean nowIgnoring = HereCommand.toggleIgnore(player.getUniqueId());
        player.sendMessage(Component.text(
            nowIgnoring ? "You will no longer see /here broadcasts." : "You will now see /here broadcasts again.",
            NamedTextColor.GOLD
        ));
        return true;
    }

}
