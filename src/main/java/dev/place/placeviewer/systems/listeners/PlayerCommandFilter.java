package dev.place.placeviewer.systems.listeners;

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

import java.util.List;

@PlaceViewerListener
public class PlayerCommandFilter implements Listener {

    public static boolean isCommandDisabled(@NotNull final String commandName) {
        final List<String> allowedCommands = PlaceViewer.config().allowedCommands();
        final String stripped = PlaceViewer.stripCommandPrefix(commandName);
        return !allowedCommands.contains(commandName) && !allowedCommands.contains(stripped);
    }

    @EventHandler
    private void onCommandSend(@NotNull final PlayerCommandSendEvent event) {
        if (event.getPlayer().isOp()) return;

        event.getCommands().removeIf(PlayerCommandFilter::isCommandDisabled);
    }

    @EventHandler
    private void onCommandInvoke(@NotNull final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        if (player.isOp()) return;

        final String[] args = event.getMessage().split(" ");
        if (args.length == 0) return;

        final String command = args[0].startsWith("/") ? args[0].substring(1) : args[0];
        if (isCommandDisabled(command)) {
            player.sendMessage(Component.text("Unknown Command '" + command + "'. Use /help for all commands.", NamedTextColor.GRAY));
            event.setCancelled(true);
        }
    }

}
