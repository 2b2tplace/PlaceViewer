package dev.place.placeviewer.systems.listeners;

import dev.place.placeviewer.api.chat.ServerChat;
import dev.place.placeviewer.api.chat.message.ChatMessage;
import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.entrypoint.PlaceViewerConfig;
import dev.place.placeviewer.systems.entrypoint.annotate.PlaceViewerListener;
import dev.place.placeviewer.api.event.PlayerPublicMessageEvent;
import dev.place.placeviewer.api.event.PlayerWhisperEvent;
import dev.place.placeviewer.systems.util.LimitedSizeQueue;
import dev.place.placeviewer.systems.util.Ratelimiter;
import me.xuender.unidecode.Unidecode;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.*;

@PlaceViewerListener
public class AntiSpamListener implements Listener {

    @NotNull
    private final Ratelimiter<UUID> ratelimiter;

    @NotNull
    private final PlaceViewerConfig.AntiSpamConfig config;

    public AntiSpamListener() {
        config = PlaceViewer.config().antiSpamConfig();
        ratelimiter = new Ratelimiter<>(config.maxViolations(), config.perTicks());
    }

    @NotNull
    private static final Map<UUID, AntispamViolation> spamStreak = new ConcurrentHashMap<>();

    @EventHandler
    public void onChat(@NotNull final PlayerPublicMessageEvent event) {
        if (!config.filterPublicMessages()) return;

        event.message().ifPresent(message -> checkAntispam(event.getPlayer(), message, event));
    }

    @EventHandler
    public void onWhisper(@NotNull final PlayerWhisperEvent event) {
        if (!config.filterWhisperMessages()) return;

        event.toMessage().ifPresent(message -> checkAntispam(event.getPlayer(), message, event));
    }

    private class MessageFootprint {

        @NotNull
        private final String original;

        @NotNull
        private final List<String> sanitized;

        @NotNull
        private final List<String> urls;

        private final long timestamp;

        public MessageFootprint(@NotNull final String original, @NotNull final List<String> sanitized,
                                @NotNull final List<String> urls, final long timestamp) {
            this.original = original;
            this.sanitized = sanitized;
            this.urls = urls;
            this.timestamp = timestamp;
        }

        public double spamProbability(final long nextTime, final int totalURLAmount, final int smallestLevenshtein) {
            final double l = original.length();
            final double lambda = l / (double) config.lengthToleranceCharcount();
            final double upsilon = 1 + totalURLAmount / 2.0d;
            final double tau = tau((nextTime - timestamp) / (double) config.spamToleranceMillis());
            final double lTau = max(0, log(tau(smallestLevenshtein / 5.0d) / 2.0d));

            return tanh(upsilon * lambda + tau + lTau);
        }

        @NotNull
        public List<String> urls() {
            return urls;
        }

        public long timestamp() {
            return timestamp;
        }

        @NotNull
        public String footprint() {
            return String.join(" ", sanitized);
        }

    }

    private class AntispamViolation {
        private int currentTaskID;
        private int violationCount;

        @NotNull
        private final UUID uuid;

        public AntispamViolation(@NotNull final UUID uuid) {
            violationCount = 1;
            currentTaskID = -1;
            this.uuid = uuid;
        }

        public void violated() {
            violationCount++;
            if (currentTaskID != -1) Bukkit.getScheduler().cancelTask(currentTaskID);

            final Player p = Bukkit.getPlayer(uuid);
            if (p != null && violationCount > config.violationsBeforeKick()) {
                Bukkit.getScheduler().runTask(PlaceViewer.PLUGIN, () -> {
                    final Component kickMessage = config.kickMessage();
                    if (kickMessage == null) p.kick();
                    else p.kick(kickMessage);
                });
                PlaceViewer.LOGGER.warn("Player {} ({}) was kicked due to spamming in chat.", p.getName(), p.getUniqueId());
            }

            currentTaskID = Bukkit.getScheduler().runTaskLater(PlaceViewer.PLUGIN, () -> {
                violationCount = 0;
                currentTaskID = -1;
            }, config.perTicks()).getTaskId();
        }

    }

    private void checkAntispam(@NotNull final Player p, @NotNull final ChatMessage message, @NotNull final Cancellable event) {
        final UUID uuid = p.getUniqueId();
        final double probability = spamProbability(p, message);
        if (probability < 0.65) return;

        if (!ratelimiter.maybeViolated(p.getUniqueId()) && probability < 0.95) return;

        final Component warnMessage = config.warnMessage();
        if (warnMessage != null)
            ServerChat.privateMessage(warnMessage, p);

        spamStreak.putIfAbsent(uuid, new AntispamViolation(uuid));
        spamStreak.get(uuid).violated();
        event.setCancelled(true);

        if (!config.silentDiscard()) return;
        ServerChat.privateMessage(message, p);
        message.recipients().removeIf(u -> !u.equals(uuid));
    }

    @NotNull
    private static final Pattern ENGLISH_STOPWORDS = Pattern.compile("\\b(i|me|my|myself|we|our|ours|ourselves|you|your|yours|yourself|yourselves|he|him|his" +
            "|himself|she|her|hers|herself|it|its|itself|they|them|their|theirs|themselves|what|which|who|whom|this|that|these|those|am|is|are|was|were|be|been|being" +
            "|have|has|had|having|do|does|did|doing|a|an|the|and|but|if|or|because|as|until|while|of|at|by|for|with|about|against|between|into|through|during|before" +
            "|after|above|below|to|from|up|down|in|out|on|off|over|under|again|further|then|once|here|there|when|where|why|how|all|any|both|each|few|more|most|other" +
            "|some|such|no|nor|not|only|own|same|so|than|too|very|s|t|can|will|just|don|should|now)\\b\\s?");

    @NotNull
    private static final Pattern NUMBERS = Pattern.compile("\\d");

    @NotNull
    private static final Pattern PUNCTUATION = Pattern.compile("\\p{Punct}");

    @NotNull
    private static final String PUNCTUATION_SPLIT_REGEX = "((?<=%s)|(?=%s))".formatted(PUNCTUATION.pattern(), PUNCTUATION.pattern());

    @NotNull
    private static final String URL_REGEX = "\\b(http://www\\.|https://www\\.|http://|https://)?[a-z0-9]+([\\-.][a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(/.*)?\\b";

    @NotNull
    private static final Function<String, String> removeStopwordsAndNumbers = s -> s.replaceAll(ENGLISH_STOPWORDS.pattern() + "|" + NUMBERS.pattern(), "");

    @NotNull
    private static final Map<UUID, LimitedSizeQueue<MessageFootprint>> footprints = new ConcurrentHashMap<>();

    public static double tau(final double x) {
        return exp(-abs(x) + 1);
    }

    @NotNull
    private static List<String> extractURLs(@NotNull final String text) {
        final List<String> containedUrls = new ArrayList<>();
        final Pattern pattern = Pattern.compile(URL_REGEX, Pattern.CASE_INSENSITIVE);
        final Matcher urlMatcher = pattern.matcher(text);

        while (urlMatcher.find()) {
            containedUrls.add(text.substring(urlMatcher.start(0),
                    urlMatcher.end(0)));
        }
        return containedUrls;
    }

    @NotNull
    private MessageFootprint messageFootprint(@NotNull final String message) {
        final long timestamp = System.currentTimeMillis();
        final List<String> urls = extractURLs(message);

        final String withoutLinks = message.replaceAll(URL_REGEX, "");
        final String decoded = Unidecode.decode(withoutLinks).toLowerCase(); // convert unicode characters into english alphabet equivalents

        final List<String> punctuationSplit = Arrays.stream(decoded.split(" "))
                .flatMap(s -> Arrays.stream(s.split(PUNCTUATION_SPLIT_REGEX))) // split any punctuation into its own element in the list for later
                .toList();

        final List<String> sanitized = new ArrayList<>(punctuationSplit.stream()
                .distinct()
                .map(removeStopwordsAndNumbers)
                .filter(s -> !s.isBlank())
                .toList());

        final List<String> punctuation = punctuationSplit.stream()
                .filter(s -> PUNCTUATION.matcher(s).matches())
                .toList();

        sanitized.removeAll(punctuation);
        sanitized.sort(Comparator.naturalOrder());

        return new MessageFootprint(message, sanitized, urls, timestamp);
    }

    @NotNull
    private static List<Long> zeroDifference(@NotNull final List<Long> from, final long min) {
        final List<Long> testFor = new ArrayList<>();
        for (int i = 0; i < from.size(); i++) {
            final long c = from.get(i);
            for (int j = 0; j < from.size(); j++) {
                if (j == i) continue;
                final long n = from.get(j);
                if (abs(n - c) < min) {
                    testFor.add(c);
                    testFor.add(n);
                }
            }
        }
        return testFor;
    }

    private static void addDeviations(@NotNull final Collection<Long> addTo, @NotNull final List<Long> from) {
        if (from.isEmpty()) return;
        for (int i = 0; i < from.size() - 1; i++) {
            final long c = from.get(i);
            final long n = from.get(i + 1);
            addTo.add(abs(c - n));
        }
    }

    @NotNull
    private static List<Long> zeroDeviation(@NotNull final LimitedSizeQueue<MessageFootprint> queue) {
        final List<Long> timeDelays = new ArrayList<>();
        addDeviations(timeDelays, queue.queue().stream().map(MessageFootprint::timestamp).toList());
        return zeroDifference(timeDelays, 50);
    }

    private static boolean hasZeroDeviation(@NotNull final LimitedSizeQueue<MessageFootprint> playerFootprints, final long timeSinceLast) {
        final List<Long> zeroDeviation = zeroDeviation(playerFootprints);
        return zeroDeviation.stream().anyMatch(z -> (abs(z - timeSinceLast) < 50));
    }

    @NotNull
    private static Optional<MessageFootprint> lastMessageFootprint(@NotNull final LimitedSizeQueue<MessageFootprint> queue) {
        return queue.isEmpty() ? Optional.empty() : Optional.ofNullable(queue.queue().get(queue.size() - 1));
    }

    private static double spamProbability(@NotNull final MessageFootprint lastFootprint, @NotNull final MessageFootprint footprint,
                                          @NotNull final LimitedSizeQueue<MessageFootprint> playerFootprints, final long timeSinceLast,
                                          final int smallestLevenshtein) {

        final boolean hasZeroDeviation = hasZeroDeviation(playerFootprints, timeSinceLast);
        final double spamProbabilityDefault = lastFootprint.spamProbability(footprint.timestamp(),
            footprint.urls.isEmpty() ? 0 : playerFootprints.stream().mapToInt(f -> f.urls().size()).sum(),
            smallestLevenshtein);

        return min(1.0d, hasZeroDeviation ? spamProbabilityDefault + 0.35 : spamProbabilityDefault);
    }

    private double spamProbability(@NotNull final Player p, @NotNull final ChatMessage message) {
        final UUID uuid = p.getUniqueId();
        final MessageFootprint footprint = messageFootprint(message.message());
        footprints.putIfAbsent(uuid, new LimitedSizeQueue<>(32));

        final LimitedSizeQueue<MessageFootprint> playerFootprints = footprints.get(uuid);
        final Optional<MessageFootprint> lastFootprint = lastMessageFootprint(playerFootprints);
        final long timeSinceLast = lastFootprint.map(f -> abs(footprint.timestamp() - f.timestamp())).orElse(Long.MAX_VALUE);

        final int smallestLevenshtein = playerFootprints.stream()
                .map(MessageFootprint::footprint)
                .mapToInt(f -> StringUtils.getLevenshteinDistance(footprint.footprint(), f))
                .min()
                .orElse(Integer.MAX_VALUE);

        footprints.get(uuid).add(footprint);

        return lastFootprint.map(f -> spamProbability(f, footprint, playerFootprints, timeSinceLast, smallestLevenshtein)).orElse(0.0d);
    }

}
