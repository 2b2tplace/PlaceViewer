package dev.place.placeviewer.systems.util;

import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Ratelimiter<T> {

    private final Map<T, Integer> violatedCount = new ConcurrentHashMap<>();
    private final Map<T, Integer> bukkitTasks = new ConcurrentHashMap<>();

    private int MAX_AMOUNT;
    private long PER_TICKS;

    public Ratelimiter() {
        super();
    }

    public Ratelimiter(final int maxAmount, final long perTicks) {
        super();
        this.MAX_AMOUNT = maxAmount;
        this.PER_TICKS = perTicks;
    }

    public int maxAmount() {
        return MAX_AMOUNT;
    }

    public long perTicks() {
        return PER_TICKS;
    }

    public void maxAmount(final int maxAmount) {
        this.MAX_AMOUNT = maxAmount;
    }

    public void perTicks(final long perTicks) {
        this.PER_TICKS = perTicks;
    }

    public int violationCount(@NotNull final T obj) {
        return violatedCount.getOrDefault(obj, -1);
    }

    public void resetViolationCount(@NotNull final T obj) {
        violatedCount.remove(obj);
        bukkitTasks.remove(obj);
    }

    public boolean maybeViolated(@NotNull final T obj)  {
        violatedCount.putIfAbsent(obj, 0);
        violatedCount.merge(obj, 1, Integer::sum);

        if (!bukkitTasks.containsKey(obj) && PER_TICKS >= 0) {
            bukkitTasks.put(obj, Bukkit.getScheduler()
                    .runTaskLaterAsynchronously(PlaceViewer.PLUGIN, () -> resetViolationCount(obj), PER_TICKS)
                    .getTaskId()
            );
        }

        return violationCount(obj) >= MAX_AMOUNT;
    }

}
