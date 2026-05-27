package dev.place.placeviewer.systems.event.feature;

import dev.place.placeviewer.api.chat.message.ChatMessage;
import dev.place.placeviewer.api.event.PlayerPublicMessageEvent;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@PlaceViewerListener
public class ExtraChatFeature implements Listener {

    @EventHandler
    public void onGreentextChat(@NotNull final PlayerPublicMessageEvent event) {
        event.message().ifPresent(message -> {
            if (!message.message().startsWith(">")) return;

            event.component(message.component().color(NamedTextColor.GREEN));
        });
    }

    @EventHandler
    public void onURLChat(@NotNull final PlayerPublicMessageEvent event) {
        event.component().ifPresent(component -> event.component(ChatMessage.createClickableURLComponent(component)));
    }

}
