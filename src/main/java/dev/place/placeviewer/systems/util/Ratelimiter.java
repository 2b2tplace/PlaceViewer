package dev.place.placeviewer.systems.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Ratelimiter<T> {

    private final Map<T, Integer> violatedCount = new ConcurrentHashMap<>();
    public final Map<T, ExpiringFlag> expiringFlags = new ConcurrentHashMap<>();

    private int maxAmount;
    private long perMillis;

    public Ratelimiter(final int maxAmount, final long perMillis) {
        super();
        this.maxAmount = maxAmount;
        this.perMillis = perMillis;
    }

    public int maxAmount() {
        return maxAmount;
    }

    public long perMillis() {
        return perMillis;
    }

    public void maxAmount(final int maxAmount) {
        this.maxAmount = maxAmount;
    }

    public void perMillis(final long perMillis) {
        this.perMillis = perMillis;
    }

    public int violationCount(@NotNull final T obj) {
        return violatedCount.getOrDefault(obj, 0);
    }

    public void reset(@NotNull final T obj) {
        violatedCount.remove(obj);
        expiringFlags.remove(obj);
    }

    public boolean isViolation(@Nullable final Integer count, @Nullable final ExpiringFlag expiringFlag) {
        if (count == null || expiringFlag == null) return false;

        return !expiringFlag.expired() && count >= maxAmount;
    }

    public boolean peek(@NotNull final T obj) {
        final Integer count = violatedCount.get(obj);
        final ExpiringFlag expiringFlag = expiringFlags.get(obj);

        return isViolation(count, expiringFlag);
    }

    public boolean expired(@NotNull final T obj) {
        final ExpiringFlag expiringFlag = expiringFlags.get(obj);
        return expiringFlag == null || expiringFlag.expired();
    }

    public boolean test(@NotNull final T obj)  {
        violatedCount.putIfAbsent(obj, 0);
        violatedCount.merge(obj, 1, Integer::sum);
        expiringFlags.putIfAbsent(obj, new ExpiringFlag(perMillis));

        final ExpiringFlag expiringFlag = expiringFlags.get(obj);
        final boolean countViolation = isViolation(violationCount(obj), expiringFlag);

        expiringFlag.reset();

        return countViolation;
    }

}
