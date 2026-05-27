package dev.place.placeviewer.systems.chat.message;

import dev.place.placeviewer.systems.chat.source.ChatSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.regex.Pattern;

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

    @NotNull
    static Component ofString(@Nullable final String s) {
        return Component.text(Optional.ofNullable(s).orElse(""));
    }

    @NotNull
    static Component hoverEvent(@NotNull final String s, @Nullable final Component hoverEventText) {
        return hoverEvent(ofString(s), hoverEventText);
    }

    @NotNull
    static Component hoverEvent(@NotNull final Component component, @Nullable final Component hoverEventText) {
        return hoverEventText == null ? component : component.hoverEvent(HoverEvent.showText(hoverEventText));
    }

    @NotNull
    static Component clickEvent(@NotNull final String s, @NotNull final ClickEvent.Action action, @Nullable final String clickEventText) {
        return clickEvent(ofString(s), action, clickEventText);
    }

    @NotNull
    static Component clickEvent(@NotNull final Component builder, @NotNull final ClickEvent.Action action, @Nullable final String clickEventText) {
        return clickEventText == null ? builder : builder.clickEvent(ClickEvent.clickEvent(action, clickEventText));
    }

    @NotNull
    static Component nameWithWhisperClick(@NotNull final ChatSource source) {
        final Component name = source.displayName();
        return clickEvent(name, ClickEvent.Action.SUGGEST_COMMAND, "/w " + source.name() + " ");
    }

    @NotNull
    Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");

    @NotNull
    static Component createClickableURLComponent(@NotNull final Component component) {
        return component.replaceText(builder -> builder
            .match(URL_PATTERN)
            .replacement((match, text) -> {
                final String url = match.group(1);

                return Component.text(url)
                    .clickEvent(ClickEvent.openUrl(url))
                    .hoverEvent(HoverEvent.showText(
                        Component.text("Click to open " + url + " in your Browser\n")
                            .append(Component.text("Click links at your own risk. Don't click " +
                                "anything suspicious. Use common sense.").color(NamedTextColor.YELLOW))
                    ));
            }));
    }

    @NotNull
    static Component nameWithWhisperHover(@NotNull final Component b, @NotNull final ChatSource source) {
        return hoverEvent(b, Component.text("Whisper to " + source.name()));
    }

    @NotNull
    static Component nameWithWhisperSuggestion(@NotNull final ChatSource source) {
        return nameWithWhisperHover(nameWithWhisperClick(source), source);
    }

    @NotNull
    static Component itemAsComponent(@NotNull final ItemStack itemStack) {
        return itemStack.displayName().hoverEvent(itemStack.asHoverEvent());
    }

    @NotNull
    static Component plainText(@NotNull final String s) {
        return clickEvent(hoverEvent(s, null), ClickEvent.Action.SUGGEST_COMMAND, null);
    }

    @NotNull
    static Component colorized(@NotNull final String message, @NotNull final TextColor color) {
        return plainText(message).color(color);
    }

    default boolean shouldShowWhisperAction(@NotNull final CommandSender receiver, @NotNull final ChatSource source) {
        return receiver instanceof final Player p && !p.getUniqueId().equals(source.uuid().orElse(null));
    }
    
}
