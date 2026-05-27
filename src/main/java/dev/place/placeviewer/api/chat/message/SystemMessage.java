package dev.place.placeviewer.api.chat.message;

import dev.place.placeviewer.api.chat.source.ChatSource;
import dev.place.placeviewer.api.chat.source.SystemChatSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

public class SystemMessage implements ChatMessage {

    @NotNull
    private final Component content;

    @NotNull
    private final Collection<UUID> intendedReceivers;

    public SystemMessage(final String content) {
        this.content = ChatMessage.ofString(content);
        this.intendedReceivers = new HashSet<>(Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList());
    }

    public SystemMessage(final String content, @NotNull final Collection<UUID> intendedReceivers) {
        this.content = ChatMessage.ofString(content);
        this.intendedReceivers = new HashSet<>(intendedReceivers);
    }

    public SystemMessage(final Component content) {
        this.content = Optional.ofNullable(content).orElse(ChatMessage.ofString(null));
        this.intendedReceivers = new HashSet<>(Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList());
    }

    public SystemMessage(final Component content, @NotNull final Collection<UUID> intendedReceivers) {
        this.content = Optional.ofNullable(content).orElse(ChatMessage.ofString(null));
        this.intendedReceivers = new HashSet<>(intendedReceivers);
    }

    @NotNull
    public static SystemMessage system(final String message) {
        return new SystemMessage(message);
    }

    @NotNull
    public static SystemMessage system(final String message, @NotNull final Collection<UUID> intendedReceivers) {
        return new SystemMessage(message, intendedReceivers);
    }

    @NotNull
    public static SystemMessage system(final Component message) {
        return new SystemMessage(message);
    }

    @NotNull
    public static SystemMessage system(final Component message, @NotNull final Collection<UUID> intendedReceivers) {
        return new SystemMessage(message, intendedReceivers);
    }

    @NotNull
    public Component send(@NotNull final CommandSender receiver) {
        return content;
    }

    public boolean systemMessage() {
        return true;
    }

    @NotNull
    public String message() {
        return PlainTextComponentSerializer.plainText().serialize(content);
    }

    @NotNull
    public Component content() {
        return content;
    }

    @NotNull
    public ChatSource source() {
        return SystemChatSource.system();
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

    @NotNull
    public ChatMessage createCopy() {
        return new SystemMessage(content(), recipients());
    }
}
