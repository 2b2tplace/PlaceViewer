package dev.place.placeviewer.systems.listeners;

import dev.place.placeviewer.api.chat.message.ChatMessage;
import dev.place.placeviewer.api.event.PlayerJoinQuitBroadcastEvent;
import dev.place.placeviewer.api.event.PlayerPublicMessageEvent;
import dev.place.placeviewer.api.event.PlayerWhisperEvent;
import dev.place.placeviewer.api.event.ServerChatEvent;
import dev.place.placeviewer.systems.command.IgnoreCommand;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@PlaceViewerListener
public class IgnoreMessageListener implements Listener {

    @EventHandler
    public void onWhisperMessage(@NotNull final PlayerWhisperEvent event) {
        if (IgnoreCommand.ignored(event.sender().getUniqueId(), event.recipient().getUniqueId()))
            event.toMessage(null);
    }

    @EventHandler
    public void onPublicMessage(@NotNull final PlayerPublicMessageEvent event) {
        onSystemMessage(event);
    }

    @EventHandler
    public void onJoinQuitMessage(@NotNull final PlayerJoinQuitBroadcastEvent event) {
        onSystemMessage(event);
    }

    public <T extends ChatMessage> void onSystemMessage(@NotNull final ServerChatEvent<T> event) {
        event.message().ifPresent(m -> m.recipients().removeAll(IgnoreCommand.ignoredByUsers(event.source().getUniqueId())));
    }

}
