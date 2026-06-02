package dev.place.placeviewer.systems.event.listener;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import dev.place.placeviewer.api.chat.ServerChat;
import dev.place.placeviewer.api.chat.message.PublicChatMessage;
import dev.place.placeviewer.api.chat.message.SystemMessage;
import dev.place.placeviewer.api.event.PlayerJoinQuitBroadcastEvent;
import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import dev.place.placeviewer.api.event.PlayerPublicMessageEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

@PlaceViewerListener
public class PlayerTrafficListener implements Listener {

    @EventHandler
    public void onJoin(@NotNull final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final SystemMessage joinMessage = new SystemMessage(event.joinMessage());
        event.joinMessage(null);

        final PlayerJoinQuitBroadcastEvent broadcastEvent = new PlayerJoinQuitBroadcastEvent(player, joinMessage, true);
        ServerChat.submit(broadcastEvent);
    }

    @EventHandler
    public void onQuit(@NotNull final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final SystemMessage quitMessage = new SystemMessage(event.quitMessage());
        event.quitMessage(null);

        final PlayerJoinQuitBroadcastEvent broadcastEvent = new PlayerJoinQuitBroadcastEvent(player, quitMessage, false);
        ServerChat.submit(broadcastEvent);
    }

    @EventHandler
    public void onPing(@NotNull final PaperServerListPingEvent event) {
        if (!PlaceViewer.config().hideOnlinePlayers()) return;

        event.setHidePlayers(true);
        event.setNumPlayers(0);
        event.setMaxPlayers(0);
    }

    @EventHandler
    public void onChat(@NotNull final AsyncChatEvent event) {
        final PublicChatMessage message = PublicChatMessage.of(event);
        event.setCancelled(true);

        final PlayerPublicMessageEvent broadcastEvent = new PlayerPublicMessageEvent(event.getPlayer(), message);
        Bukkit.getScheduler().runTask(PlaceViewer.PLUGIN, () -> ServerChat.submit(broadcastEvent));
    }

}
