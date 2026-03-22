package org.dce.ed.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.dce.ed.edsm.BodiesResponse;
import org.dce.ed.logreader.event.ScanEvent;

/**
 * Builds human-readable ring summary lines for the system table (ring composition + reserve).
 */
public final class RingSummaryFormatter {

    private static final Pattern RESERVE_AFTER_HYPHEN = Pattern.compile(
            "(?i)\\s+-\\s+(?=(pristine|depleted|motherlode)\\b|major\\b|common\\b|low\\s+resources\\b)");

    private RingSummaryFormatter() {
    }

    /**
     * One line per journal ring: "{ring class} · {reserve}" when reserve is known.
     */
    public static List<String> fromJournal(List<ScanEvent.RingInfo> rings, String reserveLevel) {
        if (rings == null || rings.isEmpty()) {
            return Collections.emptyList();
        }
        String res = humanizeReserve(reserveLevel);
        List<String> out = new ArrayList<>(rings.size());
        for (ScanEvent.RingInfo ri : rings) {
            if (ri == null) {
                continue;
            }
            String cls = humanizeRingClass(ri.getRingClass());
            if (cls.isEmpty()) {
                continue;
            }
            String line = !res.isEmpty() ? cls + " · " + res : cls;
            line = finalizeRingDisplayLine(line);
            if (!line.isEmpty()) {
                out.add(line);
            }
        }
        return out.isEmpty() ? Collections.emptyList() : out;
    }

    /**
     * EDSM/Spansh {@code Ring.type} strings (often just "Icy").
     * When {@code bodyReserveHumanized} is set from a journal scan, append it to lines that lack a reserve hint.
     */
    public static List<String> fromEdsmRings(List<BodiesResponse.Body.Ring> rings) {
        return fromEdsmRings(rings, null);
    }

    public static List<String> fromEdsmRings(List<BodiesResponse.Body.Ring> rings, String bodyReserveHumanized) {
        if (rings == null || rings.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> raw = new ArrayList<>();
        for (BodiesResponse.Body.Ring r : rings) {
            if (r != null && r.type != null && !r.type.trim().isEmpty()) {
                raw.add(r.type.trim());
            }
        }
        return finalizeAndEnrichRingLines(raw, bodyReserveHumanized);
    }

    /**
     * Finalize spacing/prefix stripping and append body reserve when API only gave composition (e.g. "Icy").
     */
    public static List<String> finalizeAndEnrichRingLines(List<String> lines, String bodyReserveHumanized) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> done = new ArrayList<>(lines.size());
        for (String line : lines) {
            String f = finalizeRingDisplayLine(line);
            if (!f.isEmpty()) {
                done.add(f);
            }
        }
        return appendReserveIfMissing(done, bodyReserveHumanized);
    }

    public static String humanizeReserve(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim();
        if (s.endsWith("Materials")) {
            s = s.substring(0, s.length() - "Materials".length());
        } else if (s.endsWith("Resources")) {
            s = s.substring(0, s.length() - "Resources".length());
        }
        s = s.trim();
        if (s.isEmpty()) {
            return "";
        }
        return capitalizeWords(s.replace('_', ' '));
    }

    static String humanizeRingClass(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim();
        if (s.contains(" ") || s.contains("-")) {
            return finalizeRingDisplayLine(capitalizeWords(s.replace('_', ' ')));
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(s.charAt(i - 1))) {
                sb.append(' ');
            }
            sb.append(c);
        }
        return finalizeRingDisplayLine(capitalizeWords(sb.toString().replace('_', ' ')));
    }

    /**
     * Strip leading ring letter + "Ring Class" (several API/journal shapes), normalize hyphenated reserve to " · ",
     * collapse odd spaces.
     */
    public static String finalizeRingDisplayLine(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String t = normalizeWs(s.trim());
        t = stripLeadingRingLetterAndClassPrefix(t);
        t = RESERVE_AFTER_HYPHEN.matcher(t).replaceFirst(" · ");
        return t.trim();
    }

    static List<String> appendReserveIfMissing(List<String> lines, String reserveHumanized) {
        if (lines == null || lines.isEmpty()) {
            return lines == null ? Collections.emptyList() : new ArrayList<>(lines);
        }
        if (reserveHumanized == null || reserveHumanized.isBlank()) {
            return new ArrayList<>(lines);
        }
        String res = reserveHumanized.trim();
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (looksLikeHasMiningReserve(line)) {
                out.add(line);
            } else {
                out.add(line + " · " + res);
            }
        }
        return out;
    }

    static boolean looksLikeHasMiningReserve(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String l = line.toLowerCase(Locale.ROOT);
        if (l.contains("pristine")) {
            return true;
        }
        if (l.contains("depleted")) {
            return true;
        }
        if (l.contains("motherlode")) {
            return true;
        }
        if (l.contains("low resources")) {
            return true;
        }
        if (l.matches(".*\\bmajor\\b.*")) {
            return true;
        }
        if (l.matches(".*\\bcommon\\b.*")) {
            return true;
        }
        return false;
    }

    private static String normalizeWs(String s) {
        String t = s.replace('\u00A0', ' ');
        return t.replaceAll("\\s+", " ").trim();
    }

    /**
     * Journal/API sometimes encode ring designation + "class" as one token (e.g. {@code ERingClassMetallic}),
     * or send a spaced phrase "E Ring Class Rocky - Pristine". Strip that leading designation.
     */
    static String stripLeadingRingLetterAndClassPrefix(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String t = normalizeWs(s.trim());
        for (int i = 0; i < 6; i++) {
            String before = t;
            t = t.replaceFirst("(?i)^[a-z]\\s+ring\\s+class\\s+", "");
            if (t.equals(before)) {
                t = t.replaceFirst("(?i)^[a-z]ring\\s+class\\s+", "");
            }
            if (t.equals(before)) {
                break;
            }
            t = t.trim();
        }
        return t.trim();
    }

    private static String capitalizeWords(String s) {
        String[] parts = s.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                out.append(p.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.toString();
    }
}
