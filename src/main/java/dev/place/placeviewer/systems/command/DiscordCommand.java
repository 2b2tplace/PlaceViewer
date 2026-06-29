package dev.place.placeviewer.systems.command;

import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@PlaceViewerCommand
public class DiscordCommand extends BukkitCommand {

    public DiscordCommand() {
        super("discord", "Shows the Discord server link", "/discord", List.of());
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        final String url = PlaceViewer.config().discordUrl();
        if (url.isBlank()) {
            player.sendMessage(Component.text("Discord link is not configured.", NamedTextColor.RED));
            return true;
        }
        final Component link = Component.text(url)
            .color(NamedTextColor.AQUA)
            .decorate(TextDecoration.UNDERLINED)
            .clickEvent(ClickEvent.openUrl(url));
        player.sendMessage(Component.text("Discord: ", NamedTextColor.GOLD).append(link));
        return true;
    }

}
