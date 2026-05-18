package dev.place.placeviewer.systems.event.chat;

import dev.place.placeviewer.systems.chat.message.ChatMessage;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Nullable;

public interface ServerChatEvent extends Cancellable {

    @Nullable
    ChatMessage getMessage();

}
