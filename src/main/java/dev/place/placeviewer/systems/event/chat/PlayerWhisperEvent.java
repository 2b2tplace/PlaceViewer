package dev.place.placeviewer.systems.event.chat;

import dev.place.placeviewer.systems.chat.message.ChatMessage;
import dev.place.placeviewer.systems.chat.message.WhisperMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerWhisperEvent extends PlayerEvent implements Cancellable, ServerChatEvent {

    @Nullable
    private WhisperMessage fromMessage;

    @Nullable
    private WhisperMessage toMessage;

    @NotNull
    private final Player recipient;
    private boolean cancelled;

    public PlayerWhisperEvent(@NotNull final Player sender, @NotNull final Player recipient, @NotNull final WhisperMessage fromMessage, @NotNull final WhisperMessage toMessage) {
        super(sender);
        this.fromMessage = fromMessage;
        this.toMessage = toMessage;
        this.recipient = recipient;
    }

    public Player getSender() {
        return getPlayer();
    }

    @NotNull
    public Player getRecipient() {
        return recipient;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Nullable
    public WhisperMessage getFromMessage() {
        return fromMessage;
    }

    public void setFromMessage(@Nullable final WhisperMessage fromMessage) {
        this.fromMessage = fromMessage;
    }

    @Nullable
    public ChatMessage getMessage() {
        return fromMessage;
    }

    @Nullable
    public WhisperMessage getToMessage() {
        return toMessage;
    }

    public void setToMessage(@Nullable final WhisperMessage toMessage) {
        this.toMessage = toMessage;
    }

    @NotNull
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

}

