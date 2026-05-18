package dev.place.placeviewer.systems.chat.message;

import dev.place.placeviewer.systems.chat.source.ChatSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessage {

    @NotNull
    Component send(@NotNull final CommandSender receiver);
    
    @NotNull
    ChatMessage createCopy();

    @NotNull
    String message();

    @NotNull
    ChatSource source();

    @NotNull
    Collection<UUID> recipients();

    void recipients(@NotNull final Collection<UUID> uuids);

    @NotNull
    default Collection<? extends Player> onlineRecipients() {
        return recipients().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();
    }

    boolean shouldReceive(@NotNull final Player p);
    boolean shouldReceive(@NotNull final UUID uuid);

    boolean systemMessage();

    static Component ofString(@Nullable final String s) {
        return Component.text(Optional.ofNullable(s).orElse(""));
    }

    static Component hoverEvent(@NotNull final String s, @Nullable final Component hoverEventText) {
        return hoverEvent(ofString(s), hoverEventText);
    }

    static Component hoverEvent(@NotNull final Component component, @Nullable final Component hoverEventText) {
        return hoverEventText == null ? component : component.hoverEvent(HoverEvent.showText(hoverEventText));
    }

    static Component clickEvent(@NotNull final String s, @NotNull final ClickEvent.Action action, @Nullable final String clickEventText) {
        return clickEvent(ofString(s), action, clickEventText);
    }

    static Component clickEvent(@NotNull final Component builder, @NotNull final ClickEvent.Action action, @Nullable final String clickEventText) {
        return clickEventText == null ? builder : builder.clickEvent(ClickEvent.clickEvent(action, clickEventText));
    }

    private static Component nameWithWhisperClick(@NotNull final ChatSource source) {
        final Component name = source.displayName();
        return clickEvent(name, ClickEvent.Action.SUGGEST_COMMAND, "/w " + source.name() + " ");
    }

    private static Component nameWithWhisperHover(@NotNull final Component b, @NotNull final ChatSource source) {
        return hoverEvent(b, Component.text("Whisper to " + source.name()));
    }

    static Component nameWithWhisperSuggestion(@NotNull final ChatSource source) {
        return nameWithWhisperHover(nameWithWhisperClick(source), source);
    }

    static Component itemAsComponent(@NotNull final ItemStack itemStack) {
        return itemStack.displayName().hoverEvent(itemStack.asHoverEvent());
    }

    static Component plainText(@NotNull final String s) {
        return clickEvent(hoverEvent(s, null), ClickEvent.Action.SUGGEST_COMMAND, null);
    }

    static Component colorized(@NotNull final String message, @NotNull final TextColor color) {
        return plainText(message).color(color);
    }

    default boolean shouldShowWhisperAction(@NotNull final CommandSender receiver, @NotNull final ChatSource source) {
        return receiver instanceof final Player p && !p.getUniqueId().equals(source.uuid().orElse(null));
    }
    
}
