package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import dev.place.placeviewer.systems.region.RegionPool;
import dev.place.placeviewer.systems.region.epoch.EpochIndex;
import dev.place.placeviewer.systems.region.Region;
import dev.place.placeviewer.systems.region.pos.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static dev.place.placeviewer.systems.command.CommandUtil.*;

@PlaceViewerCommand
public class FlashbackCommand extends BukkitCommand {

    @NotNull
    private static final String FLASHBACK_COMMAND = "flashback",
        FLASHBACK_COMMAND_PREFIXED = PlaceViewer.prependCommandPrefix(FLASHBACK_COMMAND),
        FLASHBACK_ARGUMENT_SELECT = "select",
        FLASHBACK_ARGUMENT_BROWSE = "browse";

    public FlashbackCommand() {
        super(
            FLASHBACK_COMMAND,
            "Select or browse history of your current location",
            "/" + FLASHBACK_COMMAND + " browse [year] [month] [day] or /" + FLASHBACK_COMMAND + " select <timestamp>",
            List.of("fb")
        );
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        final Location location = player.getLocation();
        final Position regionPos = Position.regionPosition(location);
        final Position chunkPos = Position.chunkPosition(location);

        final CompletableFuture<Region> future = PlaceViewer.regionPool().regionAt(regionPos);
        if (future == null) {
            sender.sendMessage(Component.text("Region is currently not loaded. Please report this issue to the developers of PlaceViewer.", NamedTextColor.RED));
            return true;
        }
        future.thenAccept(region -> {
            final EpochIndex epochIndex = region == null ? null : region.epochIndex(chunkPos);

            if (epochIndex == null || epochIndex.condensedDates().isEmpty()) {
                player.sendMessage(Component.text("No recorded data was found at your current position.", NamedTextColor.GRAY));
                return;
            }
            if (args.length == 0) {
                browseYears(player, epochIndex);
                return;
            }
            final String mode = args[0];
            switch (mode) {
                case FLASHBACK_ARGUMENT_SELECT -> selectTimestamp(player, args);
                case FLASHBACK_ARGUMENT_BROWSE -> browseFromArgs(player, args, epochIndex);
            }
        });
        return true;
    }

    @NotNull
    private static final String COLLAPSED_PREFIX = "  ⏷ ", OPENED_PREFIX = " ⏵ ";

    @NotNull
    private static Component browseButton(@NotNull final Component text, @Nullable final String hoverText, @Nullable final String clickCommandArgs) {
        Component mainComponent = text;
        if (hoverText != null)
            mainComponent = mainComponent.hoverEvent(HoverEvent.showText(Component.text(hoverText, NamedTextColor.GOLD)));

        if (clickCommandArgs != null) {
            final String command = clickCommandArgs.isBlank() ? "/" + FLASHBACK_COMMAND_PREFIXED : "/" + FLASHBACK_COMMAND_PREFIXED + " " + clickCommandArgs;
            mainComponent = mainComponent.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, command));
        }
        return mainComponent;
    }

    @NotNull
    private static Component directoryButton(@NotNull final String text, final boolean collapsed,
                                             @Nullable final String hoverText,
                                             @Nullable final String clickCommandArgs) {
        final Component prefix = Component.text(collapsed ? COLLAPSED_PREFIX : OPENED_PREFIX, NamedTextColor.DARK_GRAY);
        return prefix.append(browseButton(Component.text(text, collapsed ? NamedTextColor.GRAY : NamedTextColor.GOLD), hoverText, clickCommandArgs));
    }

    @NotNull
    private static Component directoryDateButton() {
        return directoryDateButton(false, null);
    }

    @NotNull
    private static Component directoryDateButton(final boolean collapsed, @Nullable final Integer year) {
        return directoryDateButton(collapsed, year, null);
    }

    @NotNull
    private static Component directoryDateButton(final boolean collapsed, @Nullable final Integer year, @Nullable final Integer month) {
        return directoryDateButton(collapsed, year, month, null);
    }

    @NotNull
    private static Component directoryDateButton(final boolean collapsed, @Nullable final Integer year, @Nullable final Integer month, @Nullable final Integer day) {
        if (year == null && month == null && day == null)
            return directoryButton("Browse history", collapsed, "Browse history", "");

        final String monthName = month != null ? Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) : null;

        final String formattedDate;
        final String commandArgs;
        if (month != null && day != null) {
            formattedDate = day + " " + monthName + " " + year;
            commandArgs = year + " " + month + " " + day;
        } else if (month != null) {
            formattedDate = monthName + " " + year;
            commandArgs = year + " " + month;
        } else {
            formattedDate = String.valueOf(year);
            commandArgs = String.valueOf(year);
        }
        return directoryButton(
            formattedDate, collapsed,
            "Browse history for " + formattedDate, FLASHBACK_ARGUMENT_BROWSE + " " + commandArgs
        );
    }

    private static void browseDates(@NotNull final Player player, @NotNull final EpochIndex epochIndex,
                                    @NotNull final Integer yearArg, @NotNull final Integer monthArg, @NotNull final Integer dayArg) {
        final TextComponent.Builder componentBuilder = Component.text();
        componentBuilder.append(directoryDateButton());
        componentBuilder.append(directoryDateButton(false, yearArg));
        componentBuilder.append(directoryDateButton(false, yearArg, monthArg));
        componentBuilder.append(directoryDateButton(false, yearArg, monthArg, dayArg));
        epochIndex.get(yearArg, monthArg, dayArg).forEach(date -> componentBuilder
            .appendNewline()
            .append(browseButton(
                Component.text("    " + EpochIndex.format(date), NamedTextColor.GRAY),
                "Select this snapshot for historical view (" + EpochIndex.format(date) + ")",
                 FLASHBACK_ARGUMENT_SELECT + " " + date.getTime()
            )));

        player.sendMessage(componentBuilder);
    }

    private static void browseDays(@NotNull final Player player, @NotNull final EpochIndex epochIndex,
                                   @NotNull final Integer yearArg, @NotNull final Integer monthArg) {
        final TextComponent.Builder componentBuilder = Component.text();
        componentBuilder.append(directoryDateButton());
        componentBuilder.append(directoryDateButton(false, yearArg));
        componentBuilder.append(directoryDateButton(false, yearArg, monthArg));
        epochIndex.get(yearArg, monthArg).forEach((day, dayMap) -> componentBuilder
            .appendNewline()
            .append(directoryDateButton(true, yearArg, monthArg, day)));

        player.sendMessage(componentBuilder);
    }

    private static void browseMonths(@NotNull final Player player, @NotNull final EpochIndex epochIndex,
                                     @NotNull final Integer yearArg) {
        final TextComponent.Builder componentBuilder = Component.text();
        componentBuilder.append(directoryDateButton());
        componentBuilder.append(directoryDateButton(false, yearArg));
        epochIndex.get(yearArg).forEach((month, monthMap) -> componentBuilder
            .appendNewline()
            .append(directoryDateButton(true, yearArg, month)));

        player.sendMessage(componentBuilder);
    }

    private static void browseYears(@NotNull final Player player, @NotNull final EpochIndex epochIndex) {
        final TextComponent.Builder componentBuilder = Component.text();
        componentBuilder.append(directoryDateButton());
        epochIndex.get().forEach((year, yearMap) -> componentBuilder
            .appendNewline()
            .append(directoryDateButton(true, year)));

        player.sendMessage(componentBuilder);
    }

    private static void browseFromArgs(@NotNull final Player player, @NotNull final String @NotNull [] args,
                                       @NotNull final EpochIndex epochIndex) {
        final IntParseResult yearResult = parseIntFromArgs(player, args, 1,
            arg -> "Invalid year selected: '" + arg + "'. The input must be a valid number.");

        final IntParseResult monthResult = parseIntFromArgs(player, args, 2,
            arg -> "Invalid month selected: '" + arg + "'. The input must be a valid number.");

        final IntParseResult dayResult = parseIntFromArgs(player, args, 3,
            arg -> "Invalid day selected: '" + arg + "'. The input must be a valid number.");

        final Integer yearArg  = yearResult.result();
        final Integer monthArg = monthResult.result();
        final Integer dayArg   = dayResult.result();

        if (yearArg != null && monthArg != null && dayArg != null) {
            browseDates(player, epochIndex, yearArg, monthArg, dayArg);
            return;
        }
        if (yearArg != null && monthArg != null) {
            browseDays(player, epochIndex, yearArg, monthArg);
            return;
        }
        if (yearArg != null) {
            browseMonths(player, epochIndex, yearArg);
            return;
        }
        browseYears(player, epochIndex);
    }

    private static void selectTimestamp(@NotNull final Player player, @NotNull final String @NotNull [] args) {
        if (args.length != 2) {
            player.sendMessage(Component.text("Expected a unix timestamp (UTC, milliseconds). Usage: /flashback set <timestamp>", NamedTextColor.RED));
            return;
        }
        final long timestamp;
        try {
            timestamp = Long.parseLong(args[1]);
        } catch (final NumberFormatException e) {
            player.sendMessage(Component.text("Invalid timestamp selected: '" + args[1] + "'. The input must be a valid number.", NamedTextColor.RED));
            return;
        }
        final Date date = Date.from(Instant.ofEpochMilli(timestamp));
        final RegionPool regionPool = PlaceViewer.regionPool();
        regionPool.currentEpoch(player, date);
        regionPool.reloadAllChunks(player);

        player.sendMessage(Component.text("Using Flashback to see history from " + EpochIndex.format(date) + " (" + timestamp + "). " +
            "Use '/now' to reset to the latest view.", NamedTextColor.GOLD));
    }

}
