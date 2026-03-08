package dev.place.placeviewer.systems.flashback;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class EpochIndex {

    @NotNull
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @NotNull
    private static final DateFormat FORMAT = new SimpleDateFormat("dd MMM yyyy hh:mm:ss a z") {{
        setTimeZone(UTC);
    }};

    @NotNull
    private List<Date> dates;

    @NotNull
    private final List<Date> condensedDates;

    @NotNull
    private final Map<Integer, Map<Integer, Map<Integer, List<Date>>>> index = new TreeMap<>();

    public EpochIndex(@NotNull final List<Date> dates) {
        this.dates = new ArrayList<>(dates);
        condensedDates = new ArrayList<>();
        indexDates();
    }

    public EpochIndex() {
        dates = new ArrayList<>();
        condensedDates = new ArrayList<>();
    }

    @NotNull
    public static String format(@NotNull final Date date) {
        return FORMAT.format(date);
    }

    public void dates(final long @NotNull [] timestamps) {
        dates(Arrays.stream(timestamps)
            .mapToObj(timestamp -> Date.from(Instant.ofEpochMilli(timestamp)))
            .toList());
    }

    public void dates(@NotNull final List<Date> dates) {
        this.dates = new ArrayList<>(dates);
        indexDates();
    }

    private void indexDates() {
        Collections.sort(dates);
        index.clear();
        condensedDates.clear();

        final Calendar calendar = Calendar.getInstance(UTC);
        for (final Date date : dates) {
            calendar.setTime(date);
            final int year = calendar.get(Calendar.YEAR);
            final int month = calendar.get(Calendar.MONTH) + 1;
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            final int hour = calendar.get(Calendar.HOUR_OF_DAY);

            final List<Date> exactDates = index.computeIfAbsent(year, y -> new TreeMap<>())
                .computeIfAbsent(month, m -> new TreeMap<>())
                .computeIfAbsent(day, d -> new ArrayList<>());

            if (exactDates.stream().noneMatch(existing -> {
                calendar.setTime(existing);
                return hour == calendar.get(Calendar.HOUR_OF_DAY);
            })) {
                condensedDates.add(date);
                exactDates.add(date);
            }
        }
    }

    @NotNull
    public List<Date> dates() {
        return dates;
    }

    @NotNull
    public List<Date> condensedDates() {
        return condensedDates;
    }

    @NotNull
    public Map<Integer, Map<Integer, Map<Integer, List<Date>>>> get() {
        return index;
    }

    @NotNull
    public Map<Integer, Map<Integer, List<Date>>> get(final int year) {
        return index.getOrDefault(year, Map.of());
    }

    @NotNull
    public Map<Integer, List<Date>> get(final int year, final int month) {
        return get(year).getOrDefault(month, Map.of());
    }

    @NotNull
    public List<Date> get(final int year, final int month, final int day) {
        return get(year, month).getOrDefault(day, List.of());
    }

}
