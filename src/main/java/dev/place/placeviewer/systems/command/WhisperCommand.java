package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.chat.ServerChat;
import dev.place.placeviewer.systems.chat.message.WhisperMessage;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import dev.place.placeviewer.systems.event.chat.PlayerWhisperEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@PlaceViewerCommand
public class WhisperCommand extends BukkitCommand {

    @NotNull
    private static final String WHISPER_COMMAND = "w", REPLY_COMMAND = "r", LAST_WHISPER_COMMAND = "l";

    public WhisperCommand() {
        super(
            WHISPER_COMMAND,
            "Send a private message to a player",
            "/" + WHISPER_COMMAND + " <player> <message> or reply with /" + REPLY_COMMAND + " <message> or message the last person with /" + LAST_WHISPER_COMMAND + " <message>",
            List.of("w", "whisper", "t", "tell", "msg", "r", "reply", "l", "last", "lastmsg")
        );
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player p)) {
            sender.sendMessage(Component.text("Only players can use chatting features and commands."));
            return false;
        }
        return switch (command) {
            case "w", "whisper", "t", "tell", "msg" ->
                    whisperPlayerCommand(p, elementOrNull(args, 0), restArgument(args, 1));
            case "r", "reply" ->
                    replyPlayerCommand(p, restArgument(args, 0));
            case "l", "last", "lastmsg" ->
                    lastWhisperPlayerCommand(p, restArgument(args, 0));
            default -> false;
        };
    }

    private static final Map<UUID, UUID> replyToPlayersMap = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> lastMessagePlayersMap = new ConcurrentHashMap<>();

    private void sendPrivateMessage(@NotNull final Player sender, @NotNull final Player receiver, @NotNull final String message) {
        final WhisperMessage fromSender = WhisperMessage.of(message, true, sender, receiver);
        final WhisperMessage toReceiver = WhisperMessage.of(message, false, sender, receiver);

        final PlayerWhisperEvent whisperEvent = new PlayerWhisperEvent(sender, receiver, fromSender, toReceiver);
        Bukkit.getPluginManager().callEvent(whisperEvent);
        if (whisperEvent.isCancelled()) return;

        if (whisperEvent.getFromMessage() != null) ServerChat.privateMessage(whisperEvent.getFromMessage(), sender);
        if (whisperEvent.getToMessage() != null)   ServerChat.privateMessage(whisperEvent.getToMessage(), receiver);

        replyToPlayersMap.put(receiver.getUniqueId(), sender.getUniqueId());
        lastMessagePlayersMap.put(sender.getUniqueId(), receiver.getUniqueId());
    }

    private boolean whisperPlayerCommand(@NotNull final Player sender, @Nullable final String playername, @Nullable final String message) {
        if (playername == null || message == null) {
            ServerChat.coloredMessage("Invalid usage. Whispering to a player: /w <player> <message>", NamedTextColor.GRAY, sender);
            return false;
        }
        final Player receiver = Bukkit.getPlayer(playername);
        if (receiver == null) {
            ServerChat.coloredMessage("Player '" + playername + "' is not online. Did you type their username correctly?", NamedTextColor.GRAY, sender);
            return false;
        }
        if (receiver.getUniqueId().equals(sender.getUniqueId())) {
            ServerChat.coloredMessage("You cannot whisper to yourself.", NamedTextColor.GRAY, sender);
            return false;
        }
        sendPrivateMessage(sender, receiver, message);
        return true;
    }

    private boolean replyPlayerCommand(@NotNull final Player sender, @Nullable final String message) {
        if (message == null) {
            ServerChat.coloredMessage("Invalid usage. Replying to a player: /r <message>", NamedTextColor.GRAY, sender);
            return false;
        }
        final UUID replyTo = replyToPlayersMap.get(sender.getUniqueId());
        if (replyTo == null) {
            ServerChat.coloredMessage("Noone has messaged you yet.", NamedTextColor.GRAY, sender);
            return false;
        }
        final Player receiver = Bukkit.getPlayer(replyTo);
        if (receiver == null) {
            ServerChat.coloredMessage("The person that last messaged you has logged off already.", NamedTextColor.GRAY, sender);
            return false;
        }
        sendPrivateMessage(sender, receiver, message);
        return true;
    }

    private boolean lastWhisperPlayerCommand(@NotNull final Player sender, @Nullable final String message) {
        if (message == null) {
            ServerChat.coloredMessage("Invalid usage. Messaging a player again: /l <message>", NamedTextColor.GRAY, sender);
            return false;
        }
        final UUID lastMessaged = lastMessagePlayersMap.get(sender.getUniqueId());
        if (lastMessaged == null) {
            ServerChat.coloredMessage("You haven't talked to anyone yet.", NamedTextColor.GRAY, sender);
            return false;
        }
        final Player receiver = Bukkit.getPlayer(lastMessaged);
        if (receiver == null) {
            ServerChat.coloredMessage("The person you messaged last has logged off already.", NamedTextColor.GRAY, sender);
            return false;
        }
        sendPrivateMessage(sender, receiver, message);
        return true;
    }

    @Nullable
    private static <T> T elementOrNull(@NotNull final T[] arr, final int index) {
        return index >= 0 && index < arr.length ? arr[index] : null;
    }

    @Nullable
    private static String restArgument(@NotNull final String[] split, final int afterIndex) {
        if (afterIndex < 0 || afterIndex > split.length) return null;
        return String.join(" ", Arrays.stream(split).toList().subList(afterIndex, split.length));
    }

}
