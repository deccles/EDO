package org.dce.ed.tts;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "sprintf for voice" that expands templates like:
 *   "Leaving clonal colony range of {species}"
 *   "{n} signals found on planetary body {body}"
 *
 * The key design goal is: caller stays simple, while you can gradually move
 * toward a fully-cached speech library by controlling how templates are split
 * into utterance chunks.
 *
 * IMPORTANT: This class lives outside the TTS engine classes.
 * It only depends on PollyTtsCached's public speak()/speakBlocking().
 */
public class TtsSprintf {

    /**
     * Resolve a {tag} placeholder into one or more utterance chunks.
     * Each returned chunk is spoken separately (and therefore cached separately by PollyTtsCached).
     */
    @FunctionalInterface
    public interface TagResolver {
        List<String> resolve(String tag, Object value);
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

    private final PollyTtsCached tts;
    private final Map<String, TagResolver> resolvers = new HashMap<>();
    private final Locale locale;

    public TtsSprintf(PollyTtsCached tts) {
        this(tts, Locale.US);
    }

    public TtsSprintf(PollyTtsCached tts, Locale locale) {
        this.tts = Objects.requireNonNull(tts, "tts");
        this.locale = (locale == null) ? Locale.US : locale;

        // Default resolvers. Add/override with registerResolver().
        registerResolver("species", TtsSprintf::resolveSpeciesDefault);
        registerResolver("body", TtsSprintf::resolveBodyDefault);
        registerResolver("bodyId", TtsSprintf::resolveBodyDefault);

        registerResolver("n", TtsSprintf::resolveNumberDefault);
        registerResolver("num", TtsSprintf::resolveNumberDefault);
        registerResolver("number", TtsSprintf::resolveNumberDefault);
    }

    public void registerResolver(String tag, TagResolver resolver) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("tag is required");
        }
        Objects.requireNonNull(resolver, "resolver");
        resolvers.put(tag, resolver);
    }

    /**
     * Non-blocking: queues speech (delegates to PollyTtsCached.speak()).
     */
    public void speakf(String template, Object... args) {
        List<String> chunks = formatToUtteranceChunks(template, args);
        if (chunks.isEmpty()) {
            return;
        }

        tts.getPlaybackQueue().submit(() -> {
            try {
                speakAssembledBlocking(chunks);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void speakAssembledBlocking(List<String> chunks) throws Exception {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        // Build SSML with explicit <mark/> boundaries so Polly returns precise timestamps.
        SsmlPlan plan = buildSsmlWithMarks(chunks);

        // Ensure cached WAVs, preferring context-derived chunks when missing.
        List<Path> wavs = tts.ensureCachedWavsFromSsmlMarks(plan.chunkTexts, plan.ssml, plan.markNames);

        // Filter nulls (blank chunks) and play as a single continuous stream.
        List<Path> toPlay = new ArrayList<>();
        for (Path p : wavs) {
            if (p != null) {
                toPlay.add(p);
            }
        }

        if (!toPlay.isEmpty()) {
            tts.playCombinedWavsBlocking(toPlay);
        }
    }

    private static final class SsmlPlan {
        private final String ssml;
        private final List<String> markNames;
        private final List<String> chunkTexts;

        private SsmlPlan(String ssml, List<String> markNames, List<String> chunkTexts) {
            this.ssml = ssml;
            this.markNames = markNames;
            this.chunkTexts = chunkTexts;
        }
    }

    private SsmlPlan buildSsmlWithMarks(List<String> chunks) {
        List<String> markNames = new ArrayList<>(chunks.size());
        List<String> chunkTexts = new ArrayList<>(chunks.size());

        StringBuilder ssml = new StringBuilder();
        ssml.append("<speak>");

        for (int i = 0; i < chunks.size(); i++) {
            String c = chunks.get(i);
            if (c == null) {
                c = "";
            }

            String mark = "C" + i;
            markNames.add(mark);
            chunkTexts.add(c);

            ssml.append("<mark name=\"").append(mark).append("\"/>");

            // Always emit the chunk text (even if empty) so mark ordering stays aligned.
            // We normalize spacing here so Polly doesn't do weird pauses due to repeated whitespace.
            String escaped = escapeForSsml(c);
            if (!escaped.isBlank()) {
                ssml.append(escaped);
            }

            // Add a single space between chunks to keep natural phrasing.
            if (i + 1 < chunks.size()) {
                ssml.append(" ");
            }
        }

        ssml.append("</speak>");
        return new SsmlPlan(ssml.toString(), markNames, chunkTexts);
    }

    private static String escapeForSsml(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim().replaceAll("\s+", " ");
        // Minimal XML escaping for SSML text nodes.
        t = t.replace("&", "&amp;");
        t = t.replace("<", "&lt;");
        t = t.replace(">", "&gt;");
        t = t.replace("\"", "&quot;");
        t = t.replace("'", "&apos;");
        return t;
    }

    
    /**
     * Blocking: speaks the fully expanded chunks sequentially (delegates to PollyTtsCached.speakBlocking()).
     */
    public void speakfBlocking(String template, Object... args) throws Exception {
        List<String> chunks = formatToUtteranceChunks(template, args);
        for (String chunk : chunks) {
            tts.speakBlocking(chunk);
        }
    }

    /**
     * Named-arg version (lets you call with a map instead of positional arguments).
     */
    public void speakf(String template, Map<String, ?> argsByTag) {
        List<String> chunks = formatToUtteranceChunks(template, argsByTag);
        for (String chunk : chunks) {
            tts.speak(chunk);
        }
    }

    public void speakfBlocking(String template, Map<String, ?> argsByTag) throws Exception {
        List<String> chunks = formatToUtteranceChunks(template, argsByTag);
        for (String chunk : chunks) {
            tts.speakBlocking(chunk);
        }
    }

    /**
     * Positional-arg formatter.
     * Args are matched to placeholders in order of appearance.
     */
    public List<String> formatToUtteranceChunks(String template, Object... args) {
        Objects.requireNonNull(template, "template");

        List<String> out = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(template);

        int last = 0;
        int argIndex = 0;

        while (m.find()) {
            String literal = template.substring(last, m.start());
            addLiteralChunk(out, literal);

            String tag = m.group(1);

            Object value = null;
            if (args != null && argIndex < args.length) {
                value = args[argIndex];
            }
            argIndex++;

            addResolvedChunks(out, tag, value);

            last = m.end();
        }

        addLiteralChunk(out, template.substring(last));

        // Optional: you can enforce "all placeholders must have args"
        // by throwing if argIndex != countPlaceholders(template).
        return compact(out);
    }

    /**
     * Named-arg formatter.
     * Each placeholder pulls its value from the map by tag name.
     */
    public List<String> formatToUtteranceChunks(String template, Map<String, ?> argsByTag) {
        Objects.requireNonNull(template, "template");

        Map<String, ?> map = (argsByTag == null) ? Collections.emptyMap() : argsByTag;

        List<String> out = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(template);

        int last = 0;
        while (m.find()) {
            String literal = template.substring(last, m.start());
            addLiteralChunk(out, literal);

            String tag = m.group(1);
            Object value = map.get(tag);

            addResolvedChunks(out, tag, value);

            last = m.end();
        }

        addLiteralChunk(out, template.substring(last));
        return compact(out);
    }

    private void addLiteralChunk(List<String> out, String literal) {
        if (literal == null || literal.isBlank()) {
            return;
        }

        // Keep literal runs as a single chunk so your cache learns reusable phrases:
        // e.g. "Leaving clonal colony range of"
        String normalized = normalizeSpaces(literal);

        // Trim but preserve internal spacing.
        normalized = normalized.trim();
        if (!normalized.isEmpty()) {
            out.add(normalized);
        }
    }

    private void addResolvedChunks(List<String> out, String tag, Object value) {
        TagResolver resolver = resolvers.get(tag);
        List<String> chunks;

        if (resolver != null) {
            chunks = resolver.resolve(tag, value);
        } else {
            // Default: just stringify.
            chunks = defaultStringify(value);
        }

        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        for (String c : chunks) {
            if (c == null) {
                continue;
            }
            String s = normalizeSpaces(c).trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
    }

    // -----------------------
    // Default tag resolvers
    // -----------------------

    private static List<String> resolveSpeciesDefault(String tag, Object value) {
        if (value == null) {
            return List.of("unknown species");
        }

        // Common case: "Frutexa Acus" => ["Frutexa", "Acus"]
        String s = value.toString().trim();
        if (s.isEmpty()) {
            return List.of("unknown species");
        }

        // Split on whitespace, but keep each word as its own chunk (better reuse).
        String[] parts = s.split("\\s+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank()) {
                out.add(p);
            }
        }
        return out.isEmpty() ? List.of("unknown species") : out;
    }

    private static List<String> resolveNumberDefault(String tag, Object value) {
        if (value == null) {
            return List.of("zero");
        }

        if (value instanceof Number) {
            // For now: speak the number as a single chunk so Polly handles intonation.
            // Later: you can switch to digit-by-digit, or spell-out rules, etc.
            return List.of(stripTrailingDotZero(value.toString()));
        }

        String s = value.toString().trim();
        if (s.isEmpty()) {
            return List.of("zero");
        }
        return List.of(s);
    }

    private static List<String> resolveBodyDefault(String tag, Object value) {
        if (value == null) {
            return List.of("unknown body");
        }

        // Example values you might pass:
        //   "5f" => ["5", "f"]
        //   "A 5 f" => ["A", "5", "f"]
        //   12 => ["12"]
        String s = value.toString().trim();
        if (s.isEmpty()) {
            return List.of("unknown body");
        }

        // If already spaced, keep those tokens:
        if (s.contains(" ")) {
            List<String> out = new ArrayList<>();
            for (String p : s.split("\\s+")) {
                if (!p.isBlank()) {
                    out.add(p);
                }
            }
            return out.isEmpty() ? List.of("unknown body") : out;
        }

        // Otherwise split between digit/alpha transitions: "5f" => "5", "f"
        return splitAlphaNumericTransitions(s);
    }

    // -----------------------
    // Helpers
    // -----------------------

    private static List<String> defaultStringify(Object value) {
        if (value == null) {
            return List.of();
        }
        String s = value.toString();
        if (s == null || s.isBlank()) {
            return List.of();
        }
        return List.of(s.trim());
    }

    private static String normalizeSpaces(String s) {
        return s.replaceAll("\\s+", " ");
    }

    private static String stripTrailingDotZero(String s) {
        // "5.0" -> "5" (common when someone passes a double)
        if (s != null && s.endsWith(".0")) {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }

    private static List<String> splitAlphaNumericTransitions(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();

        Character prevType = null; // 'D' digit, 'A' alpha, 'O' other
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            char type;
            if (Character.isDigit(ch)) {
                type = 'D';
            } else if (Character.isLetter(ch)) {
                type = 'A';
            } else {
                type = 'O';
            }

            if (cur.length() == 0) {
                cur.append(ch);
                prevType = type;
                continue;
            }

            // Break on transitions digit<->alpha; keep other characters attached.
            if ((prevType == 'D' && type == 'A') || (prevType == 'A' && type == 'D')) {
                out.add(cur.toString());
                cur.setLength(0);
                cur.append(ch);
                prevType = type;
                continue;
            }

            cur.append(ch);
            prevType = type;
        }

        if (cur.length() > 0) {
            out.add(cur.toString());
        }

        // Final cleanup: trim empty
        List<String> cleaned = new ArrayList<>();
        for (String p : out) {
            if (p != null) {
                String t = p.trim();
                if (!t.isEmpty()) {
                    cleaned.add(t);
                }
            }
        }
        return cleaned.isEmpty() ? List.of(s) : cleaned;
    }

    private static List<String> compact(List<String> chunks) {
        // Remove exact duplicates that occur from weird spacing edge cases, but preserve order.
        // (You can delete this if you *want* duplicates.)
        List<String> out = new ArrayList<>();
        for (String c : chunks) {
            if (c == null) {
                continue;
            }
            String t = c.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * Returns placeholders in appearance order (useful for debugging / tooling).
     */
    public static List<String> listPlaceholders(String template) {
        if (template == null) {
            return List.of();
        }
        Matcher m = PLACEHOLDER.matcher(template);
        List<String> out = new ArrayList<>();
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    /**
     * Returns unique placeholder tags (useful if you want to ensure resolvers exist).
     */
    public static Set<String> uniquePlaceholderTags(String template) {
        return new LinkedHashSet<>(listPlaceholders(template));
    }
}
