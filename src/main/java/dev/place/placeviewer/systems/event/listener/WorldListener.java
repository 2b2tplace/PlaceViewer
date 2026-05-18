package dev.place.placeviewer.systems.event.listener;

import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.jetbrains.annotations.NotNull;

@PlaceViewerListener
public class WorldListener implements Listener {

    @EventHandler
    public void onEntitySpawn(@NotNull final EntitySpawnEvent event) {
        if (event.getEntityType() != EntityType.PLAYER)
            event.setCancelled(true);
    }

}
