package dev.place.placeviewer.systems.chat;

import dev.place.placeviewer.systems.chat.message.ChatMessage;
import dev.place.placeviewer.systems.chat.message.SystemMessage;
import dev.place.placeviewer.systems.event.chat.ServerChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@SuppressWarnings("unused")
public class ServerChat {

    private ServerChat() {}

    public static <T extends Event & ServerChatEvent & Cancellable> void submit(@NotNull final T ev) {
        Bukkit.getPluginManager().callEvent(ev);

        if (!ev.isCancelled()) ServerChat.broadcast(ev);
    }

    public static void broadcast(@NotNull final ServerChatEvent ev) {
        final ChatMessage message = ev.getMessage();
        if (message != null) ServerChat.publicMessage(message, message.onlineRecipients());
    }

    private static void sendPrivate(@NotNull final CommandSender sender, @Nullable final ChatMessage message) {
        if (message == null) return;
        final Component component = message.send(sender);
        sender.sendMessage(component);
    }

    private static void broadcast(@Nullable final ChatMessage message, @NotNull final Collection<? extends Player> recipients) {
        if (message == null) return;
        recipients.forEach(p -> sendPrivate(p, message));

        final Component component = message.send(Bukkit.getConsoleSender());
        final String messagePlain = PlainTextComponentSerializer.plainText().serialize(component);
        if (!messagePlain.isBlank()) Bukkit.getConsoleSender().sendMessage(message.send(Bukkit.getConsoleSender()));
    }

    public static void publicMessage(@Nullable final ChatMessage message) {
        publicMessage(message, Bukkit.getOnlinePlayers());
    }

    public static void coloredMessage(@NotNull final String message, @NotNull final TextColor color, @NotNull final CommandSender sender) {
        privateMessage(SystemMessage.system(ChatMessage.colorized(message, color)), sender);
    }

    public static void publicMessage(@Nullable final ChatMessage message, @NotNull final Collection<? extends Player> recipients) {
        broadcast(message, recipients);
    }

    public static void privateMessage(@Nullable final ChatMessage message, @NotNull final Player meantFor) {
        sendPrivate(meantFor, message);
    }

    public static void privateMessage(@NotNull final String message, @NotNull final Player meantFor) {
        if (!message.isEmpty()) meantFor.sendMessage(message);
    }

    public static void privateMessage(@Nullable final Component message, final CommandSender meantFor) {
        if (message != null)
            sendPrivate(meantFor, SystemMessage.system(message));
    }

    public static void privateMessage(@Nullable final ChatMessage message, final CommandSender meantFor) {
        sendPrivate(meantFor, message);
    }

    public static void privateMessage(@NotNull final String message, @NotNull final CommandSender meantFor) {
        if (!message.isEmpty()) meantFor.sendMessage(message);
    }

}
