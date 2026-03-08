package dev.place.placeviewer.systems.event;

import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.jetbrains.annotations.NotNull;

@PlaceViewerListener
public class PlayerCommandFilter implements Listener {

    @EventHandler
    private void onCommandSend(@NotNull final PlayerCommandSendEvent event) {
        if (event.getPlayer().isOp()) return;

        event.getCommands().removeIf(s -> !PlaceViewer.config().allowedCommands().contains(s));
    }

}
