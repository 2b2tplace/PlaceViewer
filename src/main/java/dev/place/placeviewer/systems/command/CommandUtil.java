package dev.place.placeviewer.systems.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface CommandUtil {

    record IntParseResult(boolean hadParameter, @Nullable Integer result) {}

    @NotNull
    static IntParseResult parseIntFromArgs(@NotNull final Player player, @NotNull final String @NotNull [] args,
                                           final int index, @Nullable final Function<String, String> orErrorMessage) {
        if (index >= args.length || index < 0)
            return new IntParseResult(false, null);

        try {
            return new IntParseResult(true, Integer.parseInt(args[index]));
        } catch (final NumberFormatException e) {
            if (orErrorMessage != null)
                player.sendMessage(Component.text(orErrorMessage.apply(args[index]), NamedTextColor.RED));
            return new IntParseResult(true, null);
        }
    }

}
