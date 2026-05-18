package dev.place.placeviewer.systems.chat.source;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public interface ChatSource {

    @Nullable
    String name();

    @NotNull
    Component displayName();

    @NotNull
    Optional<UUID> uuid();

}
