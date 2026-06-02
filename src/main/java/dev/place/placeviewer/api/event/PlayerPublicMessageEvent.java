package dev.place.placeviewer.api.event;

import dev.place.placeviewer.api.chat.message.PublicChatMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PlayerPublicMessageEvent extends PlayerEvent implements Cancellable, ServerChatEvent<PublicChatMessage> {

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

    @NotNull
    public Player source() {
        return player;
    }

    @NotNull
    public Optional<PublicChatMessage> message() {
        return Optional.ofNullable(message);
    }

    @NotNull
    public Optional<Component> component() {
        return message().map(PublicChatMessage::component);
    }

    public void message(@Nullable final PublicChatMessage message) {
        this.message = message;
    }

    public void component(@Nullable final Component component) {
        if (component == null || message == null) {
            message = null;
            return;
        }
        message = PublicChatMessage.of(component, message.source(), message.recipients());
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
