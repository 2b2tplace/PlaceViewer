package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.api.chat.ServerChat;
import dev.place.placeviewer.api.settings.PersistentUserSettings;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@PlaceViewerCommand
public class IgnoreCommand extends BukkitCommand {

    @NotNull
    private static final IgnoredPlayerSettings IGNORE_LISTS = new IgnoredPlayerSettings();

    @NotNull
    private static final String IGNORE_COMMAND = "ignore";

    public IgnoreCommand() {
        super(
            IGNORE_COMMAND,
            "Ignore all chat messages sent by a player.",
            "/" + IGNORE_COMMAND + " <player>",
            List.of("i")
        );
    }

    public static void loadUserSettings() {
        IGNORE_LISTS.ignoreLists.clear();
        IGNORE_LISTS.ignoreLists.putAll(IGNORE_LISTS.loadUserSettings());
    }

    public static void saveUserSettings() {
        IGNORE_LISTS.saveUserSettings();
    }

    @NotNull
    public static Map<UUID, Set<UUID>> ignoreLists() {
        return IGNORE_LISTS.userSettings();
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        if (args.length != 1) {
            ServerChat.coloredMessage("Invalid usage. Ignore a player using " + getUsage(), NamedTextColor.GRAY, player);
            return true;
        }
        final String ignoreName = args[0];
        if (ignoreName.equals(player.getName())) {
            ServerChat.coloredMessage("You cannot ignore yourself.", NamedTextColor.GRAY, player);
            return true;
        }
        final UUID ignoredBy = player.getUniqueId();
        final Player target = Bukkit.getPlayer(ignoreName);
        if (target == null) {
            ServerChat.coloredMessage("Player '" + ignoreName + "' was not found.", NamedTextColor.GRAY, player);
            return true;
        }
        final UUID targetUUID = target.getUniqueId();
        final boolean ignored = toggleIgnored(targetUUID, ignoredBy);
        ServerChat.coloredMessage((ignored ? "I" : "Uni") + "gnored player " + ignoreName + ".", NamedTextColor.GOLD, player);
        return true;
    }

    public static boolean ignored(@NotNull final UUID user, @NotNull final UUID ignoredBy) {
        return Optional.ofNullable(ignoreLists().get(ignoredBy)).map(s -> s.contains(user)).orElse(false);
    }

    public static void ignore(@NotNull final UUID user, @NotNull final UUID ignoredBy, final boolean shouldIgnore) {
        final Set<UUID> ignored = ignoreLists().get(ignoredBy);
        if (shouldIgnore) {
            final Set<UUID> newIgnoreList = ignored == null ? new HashSet<>() : ignored;
            newIgnoreList.add(user);
            ignoreLists().put(ignoredBy, newIgnoreList);
            return;
        }
        if (ignored == null) return;

        ignored.remove(user);
        if (ignored.isEmpty()) ignoreLists().remove(ignoredBy); // dont save empty ignorelists
        else ignoreLists().put(ignoredBy, ignored);
    }

    @NotNull
    public static Set<UUID> ignoredByUsers(@NotNull final UUID user) {
        return ignoreLists().keySet().stream().filter(ignoredBy -> ignored(user, ignoredBy)).collect(Collectors.toSet());
    }

    public static boolean toggleIgnored(@NotNull final UUID user, @NotNull final UUID ignoredBy) {
        final boolean ignore = !ignored(user, ignoredBy);
        ignore(user, ignoredBy, ignore);
        return ignore;
    }

    public static class IgnoredPlayerSettings implements PersistentUserSettings<Set<UUID>> {

        @NotNull
        private final Map<UUID, Set<UUID>> ignoreLists = new ConcurrentHashMap<>();

        @NotNull
        public Map<UUID, Set<UUID>> userSettings() {
            return ignoreLists;
        }

        @NotNull
        public String container() {
            return "ignore-lists";
        }

        @NotNull
        public String settingsExtension() {
            return ".txt";
        }

        @Nullable
        public String saveSingleUser(@NotNull final UUID uuid, @NotNull final Set<UUID> uuids) {
            return uuids.isEmpty() ? null : uuids.stream().map(UUID::toString).collect(Collectors.joining("\n"));
        }

        @NotNull
        public Set<UUID> loadSingleUser(@NotNull final UUID uuid, @NotNull final String saved) {
            return Arrays.stream(saved.split("\n")).map(UUID::fromString).collect(Collectors.toSet());
        }
    }

}
