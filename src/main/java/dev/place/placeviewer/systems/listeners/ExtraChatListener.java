package dev.place.placeviewer.systems.listeners;

import dev.place.placeviewer.api.chat.ServerChat;
import dev.place.placeviewer.api.chat.message.ChatMessage;
import dev.place.placeviewer.api.event.PlayerJoinQuitBroadcastEvent;
import dev.place.placeviewer.api.event.PlayerPublicMessageEvent;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import dev.place.placeviewer.systems.region.DimensionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@PlaceViewerListener
public class ExtraChatListener implements Listener {

    @EventHandler
    public void onGreentextChat(@NotNull final PlayerPublicMessageEvent event) {
        event.message().ifPresent(message -> {
            if (!message.message().startsWith(">")) return;

            event.component(message.component().color(NamedTextColor.GREEN));
        });
    }

    @NotNull
    private static Location spawn() {
        final World world = DimensionType.OVERWORLD.world().orElseThrow();
        return new Location(world, 0, world.getMaxHeight(), 0);
    }

    private static void sendWelcomeSplashText(@NotNull final Player player) {
        ServerChat.coloredMessage("Welcome to the 2b2t Wayback Machine!", NamedTextColor.GOLD, player);
        ServerChat.coloredMessage("Use /help to see all available commands.", NamedTextColor.GRAY, player);
        ServerChat.coloredMessage("You can use /fb to browse through historical snapshots of the map.", NamedTextColor.GRAY, player);
    }

    @EventHandler
    public void onJoinQuit(@NotNull final PlayerJoinQuitBroadcastEvent event) {
        final Player player = event.getPlayer();
        final String name = player.getName();

        final boolean joinEvent = event.joinEvent();
        if (!joinEvent) {
            event.component(Component.text(name + " left the server.", NamedTextColor.GRAY));
            return;
        }
        if (!player.hasPlayedBefore()) {
            event.component(Component.text(name + " joined the server for the first time.", NamedTextColor.GRAY));
            player.teleport(spawn());
            sendWelcomeSplashText(player);
            return;
        }
        event.component(Component.text(name + " joined the server.", NamedTextColor.GRAY));
    }

    @EventHandler
    public void onURLChat(@NotNull final PlayerPublicMessageEvent event) {
        event.component().ifPresent(component -> event.component(ChatMessage.createClickableURLComponent(component)));
    }

}
