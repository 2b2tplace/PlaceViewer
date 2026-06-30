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
public class DonateCommand extends BukkitCommand {

    public DonateCommand() {
        super("donate", "Shows the donation link", "/donate", List.of());
    }

    public boolean execute(@NotNull final CommandSender sender, @NotNull final String command, @NotNull final String @NotNull [] args) {
        final String url = PlaceViewer.config().donateUrl();
        if (url.isBlank()) {
            player.sendMessage(Component.text("Donation link is not configured.", NamedTextColor.RED));
            return true;
        }
        final Component link = Component.text(url)
            .color(NamedTextColor.AQUA)
            .decorate(TextDecoration.UNDERLINED)
            .clickEvent(ClickEvent.openUrl(url));
        player.sendMessage(Component.text("Support us: ", NamedTextColor.GOLD).append(link));
        return true;
    }

}
