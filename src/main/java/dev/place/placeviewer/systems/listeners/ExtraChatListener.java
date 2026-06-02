package dev.place.placeviewer.systems.listeners;

import dev.place.placeviewer.api.chat.message.ChatMessage;
import dev.place.placeviewer.api.event.PlayerJoinQuitBroadcastEvent;
import dev.place.placeviewer.api.event.PlayerPublicMessageEvent;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    @EventHandler
    public void onJoinQuit(@NotNull final PlayerJoinQuitBroadcastEvent event) {
        final Player player = event.getPlayer();
        final String name = player.getName();

        final boolean joinEvent = event.joinEvent();
        if (!joinEvent) {
            event.component(Component.text(name + " left the PlaceViewer server.", NamedTextColor.GRAY));
            return;
        }
        if (!player.hasPlayedBefore()) {
            event.component(Component.text(name + " joined the PlaceViewer server for the first time.", NamedTextColor.GRAY));
            return;
        }
        event.component(Component.text(name + " joined the PlaceViewer server.", NamedTextColor.GRAY));
    }

    @EventHandler
    public void onURLChat(@NotNull final PlayerPublicMessageEvent event) {
        event.component().ifPresent(component -> event.component(ChatMessage.createClickableURLComponent(component)));
    }

}
