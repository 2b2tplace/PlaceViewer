package dev.place.placeviewer.systems.chat.message;

import dev.place.placeviewer.systems.chat.source.ChatSource;
import dev.place.placeviewer.systems.chat.source.PlayerChatSource;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

public class PublicChatMessage implements ChatMessage {

    @NotNull
    protected final Component content;

    @NotNull
    protected final ChatSource source;

    @NotNull
    private final Collection<UUID> intendedReceivers;

    public PublicChatMessage(@Nullable final Component content, @NotNull final ChatSource source) {
        this(content, source, Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList());
    }

    public PublicChatMessage(@Nullable final Component content, @NotNull final ChatSource source, @NotNull final Collection<UUID> intendedReceivers) {
        this.content = Optional.ofNullable(content).orElse(Component.text(""));
        this.source = source;
        this.intendedReceivers = new HashSet<>(intendedReceivers);
    }

    @NotNull
    public static PublicChatMessage of(final String message, @NotNull final ChatSource source) {
        return new PublicChatMessage(Component.text(message), source);
    }

    @NotNull
    public static PublicChatMessage of(final String message, @NotNull final ChatSource source, @NotNull final Collection<UUID> intendedReceivers) {
        return new PublicChatMessage(Component.text(message), source, intendedReceivers);
    }

    @NotNull
    public static PublicChatMessage of(final Component message, @NotNull final ChatSource source) {
        return new PublicChatMessage(message, source);
    }

    @NotNull
    public static PublicChatMessage of(final Component message, @NotNull final ChatSource source, @NotNull final Collection<UUID> intendedReceivers) {
        return new PublicChatMessage(message, source, intendedReceivers);
    }

    @NotNull
    public static PublicChatMessage of(final AsyncChatEvent ev) {
        return PublicChatMessage.of(ev.message(), new PlayerChatSource(ev.getPlayer()));
    }

    @NotNull
    private Component createChatPrefix(final boolean showWhisperAction, @NotNull final ChatSource source) {
        return ChatMessage.plainText("<")
                .append(showWhisperAction
                        ? ChatMessage.nameWithWhisperSuggestion(source)
                        : source.displayName())
                .append(ChatMessage.plainText("> "));
    }

    @NotNull
    public Component send(@NotNull final CommandSender receiver) {
        final boolean showWhisperAction = shouldShowWhisperAction(receiver, source);
        final Component prefix = createChatPrefix(showWhisperAction, source);

        return prefix.append(component());
    }

    @NotNull
    public ChatMessage createCopy() {
        return new PublicChatMessage(component(), source(), recipients());
    }

    @NotNull
    public Component component() {
        return content;
    }

    @NotNull
    public String message() {
        return LegacyComponentSerializer.legacyAmpersand().serialize(content);
    }

    @NotNull
    public ChatSource source() {
        return source;
    }

    @NotNull
    public final Collection<UUID> recipients() {
        return intendedReceivers;
    }

    public void recipients(@NotNull final Collection<UUID> intendedReceivers) {
        this.intendedReceivers.clear();
        this.intendedReceivers.addAll(intendedReceivers);
    }

    public boolean shouldReceive(@NotNull final Player p) {
        return shouldReceive(p.getUniqueId());
    }

    public boolean shouldReceive(@NotNull final UUID uuid) {
        return intendedReceivers.contains(uuid);
    }

    public boolean systemMessage() {
        return false;
    }

}
