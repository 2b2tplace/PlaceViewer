package dev.place.placeviewer.systems.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface CommandUtil {

    record IntParseResult(boolean hadParameter, @Nullable Integer result) {}

    record DoubleParseResult(boolean hadParameter, @Nullable Double result) {}

    @NotNull
    static IntParseResult parseIntFromArgs(@NotNull final Player player, @NotNull final String @NotNull [] args,
                                           final int index, @Nullable final Function<String, String> orErrorMessage) {
        if (index >= args.length || index < 0)
            return new IntParseResult(false, null);

        return parseInt(player, args[index], orErrorMessage);
    }

    @NotNull
    static IntParseResult parseInt(@NotNull final Player player, @NotNull final String arg,
                                   @Nullable final Function<String, String> orErrorMessage) {
        try {
            return new IntParseResult(true, Integer.parseInt(arg));
        } catch (final NumberFormatException e) {
            if (orErrorMessage != null)
                player.sendMessage(Component.text(orErrorMessage.apply(arg), NamedTextColor.RED));
            return new IntParseResult(true, null);
        }
    }

    @NotNull
    static DoubleParseResult parseDoubleFromArgs(@NotNull final Player player, @NotNull final String @NotNull [] args,
                                           final int index, @Nullable final Function<String, String> orErrorMessage) {
        if (index >= args.length || index < 0)
            return new DoubleParseResult(false, null);

        return parseDouble(player, args[index], orErrorMessage);
    }

    @NotNull
    static DoubleParseResult parseDouble(@NotNull final Player player, @NotNull final String arg,
                                   @Nullable final Function<String, String> orErrorMessage) {
        try {
            return new DoubleParseResult(true, Double.parseDouble(arg));
        } catch (final NumberFormatException e) {
            if (orErrorMessage != null)
                player.sendMessage(Component.text(orErrorMessage.apply(arg), NamedTextColor.RED));
            return new DoubleParseResult(true, null);
        }
    }

}
