package dev.place.placeviewer.api.event;

import dev.place.placeviewer.api.chat.message.ChatMessage;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface ServerChatEvent<T extends ChatMessage> extends Cancellable {

    @NotNull
    Optional<T> message();

}
