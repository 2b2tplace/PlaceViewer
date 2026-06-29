package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.place.placeviewer.systems.entrypoint.PlaceViewer.PLUGIN;

@PlaceViewerCommand
public class TpaCommand extends BukkitCommand {

    private static final long REQUEST_TIMEOUT_TICKS = 1200L;

    private static final Map<UUID, UUID> PENDING_REQUESTS = new ConcurrentHashMap<>(); // target -> requester
    private static final Map<UUID, UUID> OUTGOING_REQUESTS = new ConcurrentHashMap<>(); // requester -> target
    private static final Map<UUID, Integer> PENDING_TASKS = new ConcurrentHashMap<>(); // requester -> expiry task id

    private static final Set<UUID> IGNORING_TPA = ConcurrentHashMap.newKeySet();

    public TpaCommand() {
        super("tpa", "Request to teleport to a player", "/tpa <player>", List.of("tpaccept", "tpdeny"));
    }

    public static boolean isIgnoring(@NotNull final UUID uuid) {
        return IGNORING_TPA.contains(uuid);
    }

    public static boolean toggleIgnore(@NotNull final UUID uuid) {
        if (IGNORING_TPA.contains(uuid)) {
            IGNORING_TPA.remove(uuid);
            return false;
        }
        IGNORING_TPA.add(uuid);
        return true;
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        return switch (command) {
            case "tpa" -> tpaCommand(player, args);
            case "tpaccept" -> tpAcceptCommand(player);
            case "tpdeny" -> tpDenyCommand(player);
            default -> false;
        };
    }

    private static boolean tpaCommand(@NotNull final Player requester, @NotNull final String @NotNull [] args) {
        if (args.length != 1) {
            requester.sendMessage(Component.text("Usage: /tpa <player>", NamedTextColor.RED));
            return true;
        }
        final Player target = Bukkit.getPlayer(args[0]);
        // a player who is ignoring requests is reported the same as a missing one, so their status is never leaked.
        final Component notFoundOrIgnoring = Component.text("Player '" + args[0] + "' was not found or is ignoring teleport requests.", NamedTextColor.RED);
        if (target == null) {
            requester.sendMessage(notFoundOrIgnoring);
            return true;
        }
        if (target.getUniqueId().equals(requester.getUniqueId())) {
            requester.sendMessage(Component.text("You cannot send a teleport request to yourself.", NamedTextColor.RED));
            return true;
        }
        if (isIgnoring(target.getUniqueId())) {
            requester.sendMessage(notFoundOrIgnoring);
            return true;
        }

        final UUID existingTarget = OUTGOING_REQUESTS.get(requester.getUniqueId());
        if (existingTarget != null) {
            final Player existing = Bukkit.getPlayer(existingTarget);
            final String existingName = existing != null ? existing.getName() : "another player";
            requester.sendMessage(Component.text("You already have a pending teleport request to " + existingName + ". Wait for it to be accepted, denied, or to expire.", NamedTextColor.RED));
            return true;
        }
        if (PENDING_REQUESTS.containsKey(target.getUniqueId())) {
            requester.sendMessage(Component.text(target.getName() + " already has a pending teleport request. Try again shortly.", NamedTextColor.RED));
            return true;
        }

        PENDING_REQUESTS.put(target.getUniqueId(), requester.getUniqueId());
        OUTGOING_REQUESTS.put(requester.getUniqueId(), target.getUniqueId());
        final int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(PLUGIN, () -> {
            if (PENDING_REQUESTS.remove(target.getUniqueId(), requester.getUniqueId())) {
                OUTGOING_REQUESTS.remove(requester.getUniqueId());
                PENDING_TASKS.remove(requester.getUniqueId());
                requester.sendMessage(Component.text("Your teleport request to " + target.getName() + " has expired.", NamedTextColor.GRAY));
                target.sendMessage(Component.text("Teleport request from " + requester.getName() + " has expired.", NamedTextColor.GRAY));
            }
        }, REQUEST_TIMEOUT_TICKS);
        PENDING_TASKS.put(requester.getUniqueId(), taskId);

        final Component acceptButton = Component.text("[Accept]", NamedTextColor.GREEN)
            .clickEvent(ClickEvent.runCommand("/tpaccept"))
            .hoverEvent(HoverEvent.showText(Component.text("Accept teleport request", NamedTextColor.GRAY)));
        final Component denyButton = Component.text("[Deny]", NamedTextColor.RED)
            .clickEvent(ClickEvent.runCommand("/tpdeny"))
            .hoverEvent(HoverEvent.showText(Component.text("Deny teleport request", NamedTextColor.GRAY)));

        target.sendMessage(Component.text(requester.getName(), NamedTextColor.GOLD)
            .append(Component.text(" wants to teleport to you. ", NamedTextColor.WHITE))
            .append(acceptButton)
            .append(Component.text(" ", NamedTextColor.WHITE))
            .append(denyButton));

        requester.sendMessage(Component.text("Teleport request sent to " + target.getName() + ".", NamedTextColor.GOLD));
        return true;
    }

    private static boolean tpAcceptCommand(@NotNull final Player target) {
        final UUID requesterUuid = PENDING_REQUESTS.remove(target.getUniqueId());
        if (requesterUuid == null) {
            target.sendMessage(Component.text("You have no pending teleport requests.", NamedTextColor.RED));
            return true;
        }
        clearRequest(requesterUuid);

        final Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester == null) {
            target.sendMessage(Component.text("The player who requested teleport has gone offline.", NamedTextColor.RED));
            return true;
        }

        TeleportCommand.teleportSafely(requester, target.getLocation());
        requester.sendMessage(Component.text("Teleport request accepted by " + target.getName() + ".", NamedTextColor.GOLD));
        target.sendMessage(Component.text("You accepted the teleport request from " + requester.getName() + ".", NamedTextColor.GOLD));
        return true;
    }

    private static boolean tpDenyCommand(@NotNull final Player target) {
        final UUID requesterUuid = PENDING_REQUESTS.remove(target.getUniqueId());
        if (requesterUuid == null) {
            target.sendMessage(Component.text("You have no pending teleport requests.", NamedTextColor.RED));
            return true;
        }
        clearRequest(requesterUuid);

        final Player requester = Bukkit.getPlayer(requesterUuid);
        target.sendMessage(Component.text("Teleport request denied.", NamedTextColor.GRAY));
        if (requester != null)
            requester.sendMessage(Component.text(target.getName() + " denied your teleport request.", NamedTextColor.GRAY));
        return true;
    }

    private static void clearRequest(@NotNull final UUID requesterUuid) {
        OUTGOING_REQUESTS.remove(requesterUuid);
        final Integer taskId = PENDING_TASKS.remove(requesterUuid);
        if (taskId != null)
            Bukkit.getScheduler().cancelTask(taskId);
    }

}
