package dev.place.placeviewer.mixin;

import dev.place.placeviewer.systems.command.HelpCommand;
import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.entrypoint.PlaceViewerManager;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerCommand;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(SimpleCommandMap.class)
public abstract class MixinSimpleCommandMap {

    @Shadow
    public abstract boolean register(final String fallbackPrefix, final Command command);

    @Nullable
    @ModifyArg(method = "register(Ljava/lang/String;Ljava/lang/String;Lorg/bukkit/command/Command;)Z",
        at = @At(value = "INVOKE", target = "Lco/aikar/timings/TimingsManager;getCommandTiming(Ljava/lang/String;Lorg/bukkit/command/Command;)Lco/aikar/timings/Timing;"))
    private String registerTimingRemovePlugin(@Nullable final String pluginName) {
        if (Objects.equals(pluginName, PlaceViewer.COMMAND_FALLBACK_PREFIX)) return null;
        return pluginName;
    }

    @Inject(method = "setFallbackCommands", at = @At("HEAD"), cancellable = true)
    private void removeFallbackCommands(final CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "setDefaultCommands()V", at = @At("HEAD"))
    public void registerPlaceViewerCommands(final CallbackInfo ci) {
        PlaceViewerManager.findAnnotatedClassObjects(PlaceViewerCommand.class)
            .filter(o -> {
                if (!(o instanceof BukkitCommand)) {
                    PlaceViewer.LOGGER.error("Failed to register command {}", o.getClass().getName() + ", the class does not extend " + BukkitCommand.class.getName() + "; skipping.");
                    return false;
                }
                return true;
            })
            .map(o -> (BukkitCommand) o)
            .forEach(command -> {
                register(PlaceViewer.COMMAND_FALLBACK_PREFIX, command);
                HelpCommand.registerHelpPage(command);
                PlaceViewer.register(command);
            });
    }

}
