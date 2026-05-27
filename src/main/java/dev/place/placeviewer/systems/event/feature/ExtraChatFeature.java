package dev.place.placeviewer.systems.event.feature;

import dev.place.placeviewer.systems.chat.message.ChatMessage;
import dev.place.placeviewer.systems.chat.message.PublicChatMessage;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import dev.place.placeviewer.systems.event.chat.PlayerPublicMessageEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@PlaceViewerListener
public class ExtraChatFeature implements Listener {

    @EventHandler
    public void onGreentextChat(@NotNull final PlayerPublicMessageEvent ev) {
        final PublicChatMessage message = ev.getMessage();
        if (message == null || !message.message().startsWith(">")) return;

        ev.setComponent(message.component().color(NamedTextColor.GREEN));
    }

    @EventHandler
    public void onURLChat(@NotNull final PlayerPublicMessageEvent ev) {
        final PublicChatMessage message = ev.getMessage();
        if (message == null) return;

        ev.setComponent(ChatMessage.createClickableURLComponent(message.component()));
    }


}
