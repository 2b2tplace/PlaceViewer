package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@PlaceViewerCommand
public class GameModeCommand extends BukkitCommand {

    @NotNull
    private static final String GAMEMODE_COMMAND = "gamemode";

    public GameModeCommand() {
        super(
            GAMEMODE_COMMAND,
            "Select or browse history of your current location",
            "/" + GAMEMODE_COMMAND + " <survival/creative/adventure/spectator>",
            List.of("gm")
        );
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, final @NotNull String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: " + usageMessage, NamedTextColor.RED));
            return true;
        }
        final String gameModeStr = args[0];
        final Optional<GameMode> newGameMode = Arrays.stream(GameMode.values())
            .filter(gm -> gm.name().equalsIgnoreCase(gameModeStr))
            .findAny();

        if (newGameMode.isEmpty()) {
            sender.sendMessage(Component.text("Usage: " + usageMessage, NamedTextColor.RED));
            return true;
        }
        player.setGameMode(newGameMode.get());
        player.sendMessage(Component.text("Set Game Mode to " + StringUtils.capitalize(newGameMode.get().name().toLowerCase()) + ".", NamedTextColor.GOLD));
        return true;
    }

    @NotNull
    public List<String> tabComplete(@NotNull final CommandSender sender, @NotNull final String alias, @NotNull final String @NotNull [] args) throws IllegalArgumentException {
        if (args.length > 1) return List.of();

        return Arrays.stream(GameMode.values())
            .map(Enum::name)
            .map(String::toLowerCase)
            .toList();
    }
}
