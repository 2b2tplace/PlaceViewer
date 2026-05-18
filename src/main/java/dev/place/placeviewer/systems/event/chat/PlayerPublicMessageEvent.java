package dev.place.placeviewer.systems.event.chat;

import dev.place.placeviewer.systems.chat.message.PublicChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerPublicMessageEvent extends PlayerEvent implements Cancellable, ServerChatEvent {

    @Nullable
    private PublicChatMessage message;
    private boolean cancelled;

    public PlayerPublicMessageEvent(@NotNull final Player p, @NotNull final PublicChatMessage message) {
        super(p);
        this.message = message;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Nullable
    public PublicChatMessage getMessage() {
        return message;
    }

    public void setMessage(@Nullable final PublicChatMessage message) {
        this.message = message;
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
