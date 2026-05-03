package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import dev.place.placeviewer.systems.flashback.EpochPool;
import dev.place.placeviewer.systems.flashback.Epoch;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@PlaceViewerCommand
public class NowCommand extends BukkitCommand {

    @NotNull
    private static final String NOW_COMMAND = "now";

    public NowCommand() {
        super(
            NOW_COMMAND,
            "Sets your map view mode back to the latest snapshot",
            "/" + NOW_COMMAND,
            List.of()
        );
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        final EpochPool epochPool = PlaceViewer.epochPool();
        epochPool.currentEpoch(player.getUniqueId(), Epoch.now());
        epochPool.reloadAllChunks(player);
        player.sendMessage(Component.text("Your view mode has been reset to the latest snapshot.", NamedTextColor.GOLD));
        return true;
    }

}
