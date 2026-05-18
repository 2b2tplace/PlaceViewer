package dev.place.placeviewer.systems.chat.source;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class ConsoleChatSource implements ChatSource {

    private final String fakeName;

    public ConsoleChatSource(@NotNull final String fakeName) {
        this.fakeName = fakeName;
    }

    @NotNull
    public Optional<UUID> uuid() {
        return Optional.empty();
    }

    @NotNull
    public String name() {
        return fakeName;
    }

    @NotNull
    public Component displayName() {
        return Component.text(fakeName);
    }

}
