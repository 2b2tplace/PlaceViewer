package dev.place.placeviewer.systems.chat.message;

import dev.place.placeviewer.systems.chat.source.PlayerChatSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class WhisperMessage implements ChatMessage {

    @NotNull
    private final String content;

    @NotNull
    private final PlayerChatSource source;

    @NotNull
    private UUID intendedReceiver;

    @NotNull
    private final String senderName;

    @NotNull
    private final String receiverName;

    private final boolean senderToReceiver;

    public WhisperMessage(final String content, final boolean senderToReceiver, @NotNull final Player sender, @NotNull final Player receiver) {
        this.content = Optional.ofNullable(content).orElse("");
        this.source = new PlayerChatSource(sender);
        this.intendedReceiver = senderToReceiver ? receiver.getUniqueId() : sender.getUniqueId();

        this.senderName = sender.getName();
        this.receiverName = receiver.getName();
        this.senderToReceiver = senderToReceiver;
    }

    private WhisperMessage(@NotNull final String content, final boolean senderToReceiver, @NotNull final String senderName,
                           @NotNull final PlayerChatSource source, @NotNull final String receiverName,
                           @NotNull final UUID intendedReceiver) {
        this.content = content;
        this.source = source;
        this.intendedReceiver = intendedReceiver;

        this.senderName = senderName;
        this.receiverName = receiverName;
        this.senderToReceiver = senderToReceiver;
    }

    @NotNull
    public static WhisperMessage of(@NotNull final String message, final boolean senderToReceiver, @NotNull final Player sender, @NotNull final Player receiver) {
        return new WhisperMessage(message, senderToReceiver, sender, receiver);
    }

    @NotNull
    public PlayerChatSource source() {
        return source;
    }

    @NotNull
    public UUID intendedReceiver() {
        return intendedReceiver;
    }

    public void intendedReceiver(@NotNull final UUID uuid) {
        this.intendedReceiver = uuid;
    }

    @NotNull
    public final Collection<UUID> recipients() {
        return Collections.singleton(intendedReceiver);
    }

    public void recipients(@NotNull final Collection<UUID> intendedReceivers) {
        if (intendedReceivers.size() != 1) throw new UnsupportedOperationException();
        intendedReceiver = intendedReceivers.stream().toList().getFirst();
    }

    public boolean shouldReceive(@NotNull final Player p) {
        return shouldReceive(p.getUniqueId());
    }

    public boolean shouldReceive(@NotNull final UUID uuid) {
        return intendedReceiver.equals(uuid);
    }

    public boolean systemMessage() {
        return false;
    }

    @NotNull
    private Component whisperPrefix() {
        final String prefix = (senderToReceiver ? "To " + receiverName : "From " + senderName) + ": ";
        return whisperClickEvent(whisperHoverEvent(ChatMessage.ofString(prefix).color(NamedTextColor.LIGHT_PURPLE)));
    }

    @NotNull
    private Component whisperHoverEvent(@NotNull final Component b) {
        return ChatMessage.hoverEvent(b, Component.text(senderToReceiver ? "Whisper to " + receiverName + " again" : "Reply to " + senderName)
                .style(Style.style(NamedTextColor.GRAY, TextDecoration.ITALIC)));
    }

    @NotNull
    private Component whisperClickEvent(@NotNull final Component b) {
        return ChatMessage.clickEvent(b, ClickEvent.Action.SUGGEST_COMMAND, senderToReceiver ? ("/w " + receiverName + " ") : ("/w " + senderName + " "));
    }

    @NotNull
    public Component send(@NotNull final CommandSender receiver) {
        if (!(receiver instanceof Player)) return ChatMessage.ofString(null);

        return whisperPrefix().append(ChatMessage.ofString(content).color(NamedTextColor.LIGHT_PURPLE));
    }

    @NotNull
    public String message() {
        return content;
    }

    public boolean senderToReceiver() {
        return senderToReceiver;
    }

    public String senderName() {
        return senderName;
    }

    public String receiverName() {
        return receiverName;
    }

    @NotNull
    public ChatMessage createCopy() {
        return new WhisperMessage(message(), senderToReceiver(), senderName(), source(), receiverName(), intendedReceiver());
    }

}
