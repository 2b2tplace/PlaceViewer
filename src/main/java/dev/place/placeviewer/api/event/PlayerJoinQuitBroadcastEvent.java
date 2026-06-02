package dev.place.placeviewer.api.event;

import dev.place.placeviewer.api.chat.message.PublicChatMessage;
import dev.place.placeviewer.api.chat.message.SystemMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PlayerJoinQuitBroadcastEvent extends PlayerEvent implements Cancellable, ServerChatEvent<SystemMessage> {

    @Nullable
    private SystemMessage message;
    private boolean cancelled;
    private final boolean joinEvent;

    public PlayerJoinQuitBroadcastEvent(@NotNull final Player p, @NotNull final SystemMessage message, final boolean joinEvent) {
        super(p);
        this.message = message;
        this.joinEvent = joinEvent;
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
    public Optional<SystemMessage> message() {
        return Optional.ofNullable(message);
    }

    public void message(@Nullable final SystemMessage message) {
        this.message = message;
    }

    @NotNull
    public Optional<Component> component() {
        return message().map(SystemMessage::content);
    }

    public void component(@Nullable final Component component) {
        if (component == null || message == null) {
            message = null;
            return;
        }
        message = SystemMessage.system(component, message.recipients());
    }

    public boolean joinEvent() {
        return joinEvent;
    }

    public boolean quitEvent() {
        return !joinEvent;
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
