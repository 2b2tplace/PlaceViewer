package dev.place.placeviewer.systems.event.feature;

import dev.place.placeviewer.systems.chat.message.PublicChatMessage;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import dev.place.placeviewer.systems.event.chat.PlayerPublicMessageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@PlaceViewerListener
public class ChatGreentextFeature implements Listener {

    @EventHandler
    public void onChat(@NotNull final PlayerPublicMessageEvent ev) {
        final PublicChatMessage message = ev.getMessage();
        if (message == null || !message.message().startsWith(">")) return;

        ev.setMessage(PublicChatMessage.of(Component.text(message.message(), NamedTextColor.GREEN), message.source(), message.recipients()));
    }


}
