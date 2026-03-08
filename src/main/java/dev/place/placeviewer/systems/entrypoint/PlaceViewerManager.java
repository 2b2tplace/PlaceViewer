package dev.place.placeviewer.systems.entrypoint;

import co.aikar.timings.TimedEventExecutor;
import com.google.common.collect.Sets;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

public class PlaceViewerManager {

    @NotNull
    public static final Reflections REFLECTIONS = new Reflections("dev.place.placeviewer");

    @NotNull
    public static Stream<Class<?>> findAnnotatedClasses(@NotNull final Class<? extends Annotation> annotation) {
        return REFLECTIONS.getTypesAnnotatedWith(annotation).stream();
    }

    @NotNull
    public static Stream<?> findAnnotatedClassObjects(@NotNull final Class<? extends Annotation> annotation) {
        return findAnnotatedClasses(annotation)
            .map(clazz -> {
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (final ReflectiveOperationException e) {
                    PlaceViewer.LOGGER.error("Error while loading {}: {}", clazz.getSimpleName(), e.getMessage(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull);
    }

    public static void registerAll() {
        findAnnotatedClassObjects(PlaceViewerListener.class)
            .filter(o -> {
                if (!(o instanceof Listener)) {
                    PlaceViewer.LOGGER.error("Failed to register listener {}", o.getClass().getName() + ", the class does not implement " + Listener.class.getName() + "; skipping.");
                    return false;
                }
                return true;
            })
            .map(o -> (Listener) o)
            .forEach(PlaceViewerManager::registerEvents);
    }

    public static void registerEvents(@NotNull final Listener listener) {
        for (final Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : createRegisteredListeners(listener).entrySet())
            getEventListeners(getRegistrationClass(entry.getKey())).registerAll(entry.getValue());
    }

    @NotNull
    private static HandlerList getEventListeners(@NotNull final Class<? extends Event> type) {
        try {
            final Method method = getRegistrationClass(type).getDeclaredMethod("getHandlerList");
            method.setAccessible(true);
            return (HandlerList) method.invoke(null);
        } catch (final Exception e) {
            throw new IllegalPluginAccessException(e.toString());
        }
    }

    @NotNull
    private static Class<? extends Event> getRegistrationClass(@NotNull final Class<? extends Event> clazz) {
        try {
            clazz.getDeclaredMethod("getHandlerList");
            return clazz;
        } catch (final NoSuchMethodException e) {
            if (clazz.getSuperclass() != null
                && !clazz.getSuperclass().equals(Event.class)
                && Event.class.isAssignableFrom(clazz.getSuperclass())) {
                return getRegistrationClass(clazz.getSuperclass().asSubclass(Event.class));
            }
            throw new IllegalPluginAccessException("Unable to find handler list for event " + clazz.getName() + ". Static getHandlerList method required!");
        }
    }

    @NotNull
    public static Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(@NotNull final Listener listener) {
        final Plugin plugin = PlaceViewer.PLUGIN;
        final Map<Class<? extends Event>, Set<RegisteredListener>> result = new HashMap<>();

        final Set<Method> methods;
        try {
            final Class<?> listenerClazz = listener.getClass();
            methods = Sets.union(
                Set.of(listenerClazz.getMethods()),
                Set.of(listenerClazz.getDeclaredMethods())
            );
        } catch (final NoClassDefFoundError e) {
            plugin.getLogger().severe("Failed to register events for " + listener.getClass() + " because " + e.getMessage() + " does not exist.");
            return result;
        }
        for (final Method method : methods) {
            final EventHandler eh = method.getAnnotation(EventHandler.class);
            if (eh == null || method.isBridge() || method.isSynthetic())
                continue;

            final Class<?> checkClass;
            if (method.getParameterTypes().length != 1 || !Event.class.isAssignableFrom(checkClass = method.getParameterTypes()[0])) {
                plugin.getLogger().severe(plugin.getPluginMeta().getDisplayName() + " attempted to register an invalid EventHandler method signature \"" + method.toGenericString() + "\" in " + listener.getClass());
                continue;
            }
            final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
            method.setAccessible(true);

            final Set<RegisteredListener> eventSet = result.computeIfAbsent(eventClass, k -> new HashSet<>());

            final EventExecutor executor = new TimedEventExecutor(EventExecutor.create(method, eventClass), plugin, method, eventClass);
            eventSet.add(new RegisteredListener(listener, executor, eh.priority(), plugin, eh.ignoreCancelled()));
        }
        return result;
    }

}
