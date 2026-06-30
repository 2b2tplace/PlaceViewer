package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import dev.place.placeviewer.systems.region.RegionPool;
import dev.place.placeviewer.systems.region.epoch.Epoch;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@PlaceViewerCommand
public class OldCommand extends BukkitCommand {

    @NotNull
    private static final String OLD_COMMAND = "old";

    public OldCommand() {
        super(
            OLD_COMMAND,
            "Sets your map view mode to the oldest available snapshot",
            "/" + OLD_COMMAND,
            List.of()
        );
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        final RegionPool regionPool = PlaceViewer.regionPool();
        regionPool.currentEpoch(player.getUniqueId(), Epoch.snapshot(0L));
        regionPool.reloadAllChunks(player);
        player.sendMessage(Component.text("Your view mode has been set to the oldest available snapshot.", NamedTextColor.GOLD));
        return true;
    }

}
