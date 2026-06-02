package dev.place.placeviewer.api.event;

import dev.place.placeviewer.api.chat.message.WhisperMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PlayerWhisperEvent extends PlayerEvent implements Cancellable, ServerChatEvent<WhisperMessage> {

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

    @NotNull
    public Player source() {
        return player;
    }

    @NotNull
    public Player sender() {
        return player;
    }

    @NotNull
    public Player recipient() {
        return recipient;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    public Optional<WhisperMessage> fromMessage() {
        return Optional.ofNullable(fromMessage);
    }

    public void fromMessage(@Nullable final WhisperMessage fromMessage) {
        this.fromMessage = fromMessage;
    }

    @NotNull
    public Optional<WhisperMessage> message() {
        return Optional.ofNullable(fromMessage);
    }

    @NotNull
    public Optional<WhisperMessage> toMessage() {
        return Optional.ofNullable(toMessage);
    }

    public void toMessage(@Nullable final WhisperMessage toMessage) {
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

