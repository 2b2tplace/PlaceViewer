package dev.place.placeviewer.systems.event.listener;

import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.jetbrains.annotations.NotNull;

@PlaceViewerListener
public class PlayerCommandFilter implements Listener {

    @EventHandler
    private void onCommandSend(@NotNull final PlayerCommandSendEvent event) {
        if (event.getPlayer().isOp()) return;

        event.getCommands().removeIf(s -> !PlaceViewer.config().allowedCommands().contains(s));
    }

    @EventHandler
    private void onCommandInvoke(@NotNull final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        if (player.isOp()) return;

        final String[] args = event.getMessage().split(" ");
        if (args.length == 0) return;

        final String command = args[0].startsWith("/") ? args[0].substring(1) : args[0];
        if (!PlaceViewer.config().allowedCommands().contains(command)) {
            player.sendMessage(Component.text("Unknown Command '" + command + "'. Use /help for all commands.", NamedTextColor.GRAY));
            event.setCancelled(true);
        }
    }

}
