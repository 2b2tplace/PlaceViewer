package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@PlaceViewerCommand
public class HelpCommand extends BukkitCommand {

    @NotNull
    private static final String HELP_COMMAND = "help";

    public HelpCommand() {
        super(
            HELP_COMMAND,
            "Shows the PlaceViewer help page",
            "/" + HELP_COMMAND,
            List.of("?")
        );
    }

    @NotNull
    private static final TextComponent.Builder HELP_PAGE = Component.text()
        .append(Component.text("--- PlaceViewer Help Page ---", NamedTextColor.GOLD));

    public static void registerHelpPage(@NotNull final BukkitCommand command) {
        final List<String> aliases = new ArrayList<>(command.getAliases());
        aliases.add(command.getName());

        HELP_PAGE.appendNewline()
            .append(Component.text(String.join(", ", aliases), NamedTextColor.GOLD))
            .append(Component.text(": " + command.getDescription(), NamedTextColor.GRAY))
            .appendNewline()
            .append(Component.text("Usage: " + command.getUsage(), NamedTextColor.GRAY));
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, final @NotNull String @NotNull [] args) {
        sender.sendMessage(HELP_PAGE);
        return true;
    }

}
