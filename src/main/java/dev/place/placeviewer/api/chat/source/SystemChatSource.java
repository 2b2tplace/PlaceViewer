package dev.place.placeviewer.api.chat.source;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class SystemChatSource implements ChatSource {

    private SystemChatSource() {}

    @NotNull
    private static final SystemChatSource SYSTEM_CHAT_SOURCE = new SystemChatSource();

    @NotNull
    public static SystemChatSource system() {
        return SYSTEM_CHAT_SOURCE;
    }

    @Nullable
    public String name() {
        return null;
    }

    @NotNull
    public Component displayName() {
        return Component.text("");
    }

    @NotNull
    public Optional<UUID> uuid() {
        return Optional.empty();
    }

}
