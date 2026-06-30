package dev.place.placeviewer.systems.listeners;

import dev.place.placeviewer.systems.command.BackForwardCommand;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

@PlaceViewerListener
public class TeleportHistoryListener implements Listener {

    @EventHandler
    public void onTeleport(@NotNull final PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        if (BackForwardCommand.NAVIGATING.contains(player.getUniqueId())) return;

        final Location destination = event.getTo();
        if (destination == null) return;

        BackForwardCommand.recordTeleport(player.getUniqueId(), destination);
    }

    @EventHandler
    public void onQuit(@NotNull final PlayerQuitEvent event) {
        BackForwardCommand.clearHistory(event.getPlayer().getUniqueId());
    }

}
