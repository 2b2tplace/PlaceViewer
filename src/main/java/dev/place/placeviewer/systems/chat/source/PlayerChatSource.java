package dev.place.placeviewer.systems.chat.source;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class PlayerChatSource implements ChatSource {

    @Nullable
    private final UUID uuid;

    @NotNull
    private final String name;

    @NotNull
    private final Component displayName;

    public PlayerChatSource(@NotNull final Player p) {
        this.uuid = p.getUniqueId();
        this.name = p.getName();
        this.displayName = p.displayName();
    }

    @NotNull
    public String name() {
        return name;
    }

    @NotNull
    public Component displayName() {
        return displayName;
    }

    @NotNull
    public Optional<UUID> uuid() {
        return Optional.ofNullable(uuid);
    }

}
