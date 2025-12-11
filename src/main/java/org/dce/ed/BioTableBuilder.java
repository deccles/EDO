package org.dce.ed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.dce.ed.SystemTabPanel.Row;
import org.dce.ed.exobiology.ExobiologyData;
import org.dce.ed.state.BodyInfo;

final class BioTableBuilder {

    private BioTableBuilder() {
        // utility
    }

    static List<Row> buildRows(Collection<BodyInfo> bodies) {
        List<BodyInfo> sorted = new ArrayList<>(bodies);
        sorted.sort(Comparator.comparingDouble(b -> Double.isNaN(b.getDistanceLs())
                ? Double.MAX_VALUE
                : b.getDistanceLs()));

        List<Row> rows = new ArrayList<>();

        for (BodyInfo b : sorted) {
            rows.add(Row.body(b));

            if (!b.hasBio()) {
                continue;
            }

            // 1) Start from whatever predictions we already have
            List<ExobiologyData.BioCandidate> preds = b.getPredictions();

            // If there are no predictions yet, try a one-shot calculation here
            if (preds == null || preds.isEmpty()) {
                ExobiologyData.BodyAttributes attrs = b.buildBodyAttributes();
                if (attrs != null) {
                    List<ExobiologyData.BioCandidate> base = ExobiologyData.predict(attrs);
                    if (base != null && !base.isEmpty()) {
                        // If we know which genera are present, narrow to those
                        Set<String> genusPrefixes = b.getObservedGenusPrefixes();
                        if (genusPrefixes != null && !genusPrefixes.isEmpty()) {
                            Set<String> lower = new HashSet<>();
                            for (String g : genusPrefixes) {
                                if (g != null && !g.isEmpty()) {
                                    lower.add(g.toLowerCase(Locale.ROOT));
                                }
                            }

                            List<ExobiologyData.BioCandidate> filtered = new ArrayList<>();
                            for (ExobiologyData.BioCandidate cand : base) {
                                String nameLower = cand.getDisplayName()
                                                       .toLowerCase(Locale.ROOT);
                                boolean matches = false;
                                for (String prefix : lower) {
                                    if (nameLower.startsWith(prefix + " ")
                                            || nameLower.equals(prefix)) {
                                        matches = true;
                                        break;
                                    }
                                }
                                if (matches) {
                                    filtered.add(cand);
                                }
                            }

                            preds = filtered.isEmpty() ? base : filtered;
                        } else {
                            preds = base;
                        }
                    }
                }
            }

            Set<String> genusPrefixes = b.getObservedGenusPrefixes();
            Set<String> observedNamesRaw = b.getObservedBioDisplayNames();

            Set<String> observedGenusLower = new HashSet<>();
            if (genusPrefixes != null) {
                for (String gp : genusPrefixes) {
                    if (gp != null && !gp.isEmpty()) {
                        observedGenusLower.add(gp.toLowerCase(Locale.ROOT));
                    }
                }
            }

            boolean hasGenusPrefixes = genusPrefixes != null && !genusPrefixes.isEmpty();
            boolean hasObservedNames = observedNamesRaw != null && !observedNamesRaw.isEmpty();
            boolean hasPreds = preds != null && !preds.isEmpty();

            // If literally nothing but "hasBio", show a generic message
            if (!hasGenusPrefixes && !hasObservedNames && !hasPreds) {
                rows.add(Row.bio(b.getBodyId(),
                        "Biological signals detected",
                        ""));
                continue;
            }

            //
            // CASE A: Predictions only, no genus info from scan yet.
            //   -> collapse by genus: "Genus (n)" with max value.
            //
            if (!hasGenusPrefixes && !hasObservedNames) {

                class BioRowData {
                    final String name;
                    final Long cr;

                    BioRowData(String name, Long cr) {
                        this.name = name;
                        this.cr = cr;
                    }
                }

                List<BioRowData> bioRows = new ArrayList<>();

                if (preds != null) {
                    for (ExobiologyData.BioCandidate cand : preds) {
                        String name = canonicalBioName(cand.getDisplayName());
                        Long cr = cand.getEstimatedPayout(true);
                        bioRows.add(new BioRowData(name, cr));
                    }
                }

                if (bioRows.isEmpty()) {
                    rows.add(Row.bio(b.getBodyId(),
                            "Biological signals detected",
                            ""));
                    continue;
                }

                // Sort by value desc, then genus, then full name
                bioRows.sort((a, bRow) -> {
                    String aName = (a.name != null) ? a.name : "";
                    String bName = (bRow.name != null) ? bRow.name : "";

                    String aGenus = firstWord(aName);
                    String bGenus = firstWord(bName);

                    long aVal = (a.cr != null) ? a.cr : Long.MIN_VALUE;
                    long bVal = (bRow.cr != null) ? bRow.cr : Long.MIN_VALUE;

                    int cmp = Long.compare(bVal, aVal);
                    if (cmp != 0) {
                        return cmp;
                    }

                    cmp = aGenus.compareToIgnoreCase(bGenus);
                    if (cmp != 0) {
                        return cmp;
                    }

                    return aName.compareToIgnoreCase(bName);
                });

                // Collapse by genus: "Genus (n)" with max CR
                class GenusSummary {
                    int count = 0;
                    Long maxCr = null;
                }

                Map<String, GenusSummary> byGenus = new LinkedHashMap<>();

                for (BioRowData br : bioRows) {
                    String genus = firstWord(br.name);
                    GenusSummary summary = byGenus.get(genus);
                    if (summary == null) {
                        summary = new GenusSummary();
                        byGenus.put(genus, summary);
                    }
                    summary.count++;
                    if (br.cr != null) {
                        if (summary.maxCr == null || br.cr > summary.maxCr) {
                            summary.maxCr = br.cr;
                        }
                    }
                }

                for (Map.Entry<String, GenusSummary> e : byGenus.entrySet()) {
                    String genus = e.getKey();
                    GenusSummary summary = e.getValue();

                    String label = summary.count > 1
                            ? genus + " (" + summary.count + ")"
                            : genus;

                    String valueText = "";
                    if (summary.maxCr != null) {
                        long millions = Math.round(summary.maxCr / 1_000_000.0);
                        valueText = String.format(Locale.US, "%dM Cr", millions);
                    }

                    rows.add(Row.bio(b.getBodyId(), label, valueText));
                }

                continue;
            }

            //
            // CASE B: We have genus info and/or observed species.
            //   -> Expand by genus, then species.
            //   Rules:
            //     - For a genus with confirmed species: show ONLY confirmed species rows
            //       (truth replaces predictions for that genus).
            //     - For a genus with no confirmed species: show predicted species.
            //

            // Predictions indexed by canonical name and genus
            Map<String, ExobiologyData.BioCandidate> predictedByCanonName = new LinkedHashMap<>();
            Map<String, List<ExobiologyData.BioCandidate>> predictedByGenus = new LinkedHashMap<>();
            if (preds != null) {
                for (ExobiologyData.BioCandidate cand : preds) {
                    String canon = canonicalBioName(cand.getDisplayName());
                    predictedByCanonName.put(canon, cand);
                    String genusKey = firstWord(canon).toLowerCase(Locale.ROOT);
                    predictedByGenus
                            .computeIfAbsent(genusKey, g -> new ArrayList<>())
                            .add(cand);
                }
            }

            // Confirmed species, grouped by genus, using canonical names
            Map<String, List<String>> confirmedByGenus = new LinkedHashMap<>();
            if (observedNamesRaw != null) {
                for (String rawName : observedNamesRaw) {
                    if (rawName == null || rawName.isEmpty()) {
                        continue;
                    }
                    String canon = canonicalBioName(rawName);
                    String genusKey = firstWord(canon).toLowerCase(Locale.ROOT);
                    confirmedByGenus
                            .computeIfAbsent(genusKey, g -> new ArrayList<>())
                            .add(canon);
                }
            }

            // Build genus order:
            //  - first: order from observed genus prefixes
            //  - then any extra genera we only have from predictions / confirmed species
            List<String> genusOrder = new ArrayList<>();
            if (genusPrefixes != null) {
                for (String gp : genusPrefixes) {
                    if (gp == null || gp.isEmpty()) {
                        continue;
                    }
                    String key = gp.toLowerCase(Locale.ROOT);
                    if (!genusOrder.contains(key)) {
                        genusOrder.add(key);
                    }
                }
            }
            for (String key : predictedByGenus.keySet()) {
                if (!genusOrder.contains(key)) {
                    genusOrder.add(key);
                }
            }
            for (String key : confirmedByGenus.keySet()) {
                if (!genusOrder.contains(key)) {
                    genusOrder.add(key);
                }
            }

            if (genusOrder.isEmpty()) {
                // Should be rare, but fall back to generic message
                rows.add(Row.bio(b.getBodyId(),
                        "Biological signals detected",
                        ""));
                continue;
            }

            for (String genusKey : genusOrder) {
                List<ExobiologyData.BioCandidate> predictedForGenus = predictedByGenus.get(genusKey);
                List<String> confirmedForGenus = confirmedByGenus.get(genusKey);

                boolean hasAnySpecies =
                        (confirmedForGenus != null && !confirmedForGenus.isEmpty()) ||
                        (predictedForGenus != null && !predictedForGenus.isEmpty());

                // If we *only* know the genus name and have no species/predictions,
                // show a single genus row (very rare).
                if (!hasAnySpecies) {
                    String displayGenus;
                    // Fall back to capitalized key (since we have no examples)
                    if (genusKey.isEmpty()) {
                        displayGenus = genusKey;
                    } else {
                        displayGenus = Character.toUpperCase(genusKey.charAt(0))
                                + genusKey.substring(1);
                    }
                    rows.add(Row.bio(b.getBodyId(), displayGenus, ""));
                    continue;
                }

                // From here on, we have at least one species (predicted or confirmed),
                // so we do NOT add a genus-only header line.
                // This is what removes the "Bacterium" header in stages 2 and 3.

                // If we have confirmed species for this genus, they REPLACE predictions.
                if (confirmedForGenus != null && !confirmedForGenus.isEmpty()) {
                    class SpeciesRow {
                        final String name;
                        final Long cr;

                        SpeciesRow(String name, Long cr) {
                            this.name = name;
                            this.cr = cr;
                        }
                    }

                    List<SpeciesRow> speciesRows = new ArrayList<>();
                    for (String canonName : confirmedForGenus) {
                        ExobiologyData.BioCandidate cand = predictedByCanonName.get(canonName);
                        Long cr = (cand != null) ? cand.getEstimatedPayout(true) : null;
                        speciesRows.add(new SpeciesRow(canonName, cr));
                    }

                    speciesRows.sort((a, bRow) -> {
                        long aVal = (a.cr != null) ? a.cr : Long.MIN_VALUE;
                        long bVal = (bRow.cr != null) ? bRow.cr : Long.MIN_VALUE;
                        int cmp = Long.compare(bVal, aVal);
                        if (cmp != 0) {
                            return cmp;
                        }
                        return a.name.compareToIgnoreCase(bRow.name);
                    });

                    for (SpeciesRow sr : speciesRows) {
                        String valueText = "";
                        if (sr.cr != null) {
                            long millions = Math.round(sr.cr / 1_000_000.0);
                            valueText = String.format(Locale.US, "%dM Cr", millions);
                        }

                        Row bio = Row.bio(b.getBodyId(), sr.name, valueText);
                        // Keep the green styling you liked.
                        bio.setObservedGenusHeader(true);
                        rows.add(bio);
                    }
                } else if (predictedForGenus != null && !predictedForGenus.isEmpty()) {
                    // No confirmed species for this genus -> show predicted species
                    predictedForGenus.sort((c1, c2) -> {
                        long v1 = c1.getEstimatedPayout(true);
                        long v2 = c2.getEstimatedPayout(true);
                        int cmp = Long.compare(v2, v1);
                        if (cmp != 0) {
                            return cmp;
                        }
                        String n1 = canonicalBioName(c1.getDisplayName());
                        String n2 = canonicalBioName(c2.getDisplayName());
                        return n1.compareToIgnoreCase(n2);
                    });

                    for (ExobiologyData.BioCandidate cand : predictedForGenus) {
                        String name = canonicalBioName(cand.getDisplayName());
                        long cr = cand.getEstimatedPayout(true);
                        long millions = Math.round(cr / 1_000_000.0);
                        String valueText = String.format(Locale.US, "%dM Cr", millions);
                        rows.add(Row.bio(b.getBodyId(), name, valueText));
                    }
                }
            }
        }

        return rows;
    }

    private static String canonicalBioName(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return s;
        }

        String[] parts = s.split("\\s+");
        // Collapse "Genus Genus Species..." -> "Genus Species..."
        if (parts.length >= 3 && parts[0].equalsIgnoreCase(parts[1])) {
            StringBuilder sb = new StringBuilder(parts[0]);
            for (int i = 2; i < parts.length; i++) {
                sb.append(' ').append(parts[i]);
            }
            return sb.toString();
        }

        return s;
    }

    private static String firstWord(String s) {
        if (s == null) {
            return "";
        }
        String[] parts = s.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }
}
