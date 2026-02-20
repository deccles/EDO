package org.dce.ed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.dce.ed.SystemTabPanel.Row;
import org.dce.ed.exobiology.ExobiologyData.BioCandidate;
import org.dce.ed.exobiology.ExobiologyData.SpeciesConstraint;
import org.dce.ed.state.BodyInfo;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BioTableBuilder}: FSS-only (predictions), DSS-only (observed genus),
 * DSS reveals genus we didn't predict (genus added to list), and filter behavior.
 */
class BioTableBuilderTest {

    private static final int BODY_ID = 7;

    @Test
    void dssRevealsGenusWeDidNotPredict_genusAppearsInList() {
        // FSS gave predictions for Bacterium only; then DSS reveals Tardigrade (not predicted).
        // The genus "Tardigrade" should appear in the built rows.
        BodyInfo body = new BodyInfo();
        body.setBodyId(BODY_ID);
        body.setHasBio(true);
        body.setObservedGenusPrefixes(new HashSet<>(Set.of("tardigrade")));
        body.setPredictions(new ArrayList<>(List.of(
                makeCandidate("Bacterium Acies"))));

        List<Row> rows = BioTableBuilder.buildRows(Collections.singletonList(body), false);

        List<String> bioLabels = bioRowLabels(rows);
        assertTrue(bioLabels.stream().anyMatch("Tardigrade"::equals),
                "DSS-only genus Tardigrade should appear in list; got: " + bioLabels);
    }

    @Test
    void predictionsOnly_noDss_noSampling_showsPredictedGenera() {
        BodyInfo body = new BodyInfo();
        body.setBodyId(BODY_ID);
        body.setHasBio(true);
        body.setObservedGenusPrefixes(null);
        body.setObservedBioDisplayNames(null);
        body.setPredictions(new ArrayList<>(List.of(
                makeCandidate("Bacterium Acies"),
                makeCandidate("Tardigrade Fragilis"))));

        List<Row> rows = BioTableBuilder.buildRows(Collections.singletonList(body), false);

        List<String> bioLabels = bioRowLabels(rows);
        assertTrue(bioLabels.stream().anyMatch(s -> s.contains("Bacterium")));
        assertTrue(bioLabels.stream().anyMatch(s -> s.contains("Tardigrade")));
    }

    @Test
    void dssOnly_observedGenusNoPredictions_showsGenusHeader() {
        BodyInfo body = new BodyInfo();
        body.setBodyId(BODY_ID);
        body.setHasBio(true);
        body.setObservedGenusPrefixes(new HashSet<>(Set.of("bacterium")));
        body.setObservedBioDisplayNames(null);
        body.setPredictions(null);

        List<Row> rows = BioTableBuilder.buildRows(Collections.singletonList(body), false);

        List<String> bioLabels = bioRowLabels(rows);
        assertTrue(bioLabels.stream().anyMatch(s -> "Bacterium".equals(s) || s.startsWith("Bacterium")),
                "Observed genus (DSS) should appear; got: " + bioLabels);
    }

    @Test
    void dssAndPredictions_bodyHasFilteredPredictionsOnlyObservedGenusShown() {
        // Simulates state after SystemEventProcessor has filtered predictions by observed genus:
        // only Bacterium remains on body; table should show only Bacterium.
        BodyInfo body = new BodyInfo();
        body.setBodyId(BODY_ID);
        body.setHasBio(true);
        body.setObservedGenusPrefixes(new HashSet<>(Set.of("bacterium")));
        body.setPredictions(new ArrayList<>(List.of(makeCandidate("Bacterium Acies"))));

        List<Row> rows = BioTableBuilder.buildRows(Collections.singletonList(body), false);

        List<String> bioLabels = bioRowLabels(rows);
        assertTrue(bioLabels.stream().anyMatch(s -> s.contains("Bacterium")),
                "Bacterium should appear; got: " + bioLabels);
        assertFalse(bioLabels.stream().anyMatch(s -> s.contains("Tardigrade")),
                "Only filtered genus should appear; got: " + bioLabels);
    }

    @Test
    void observedSpeciesNotPredicted_appearsInList() {
        BodyInfo body = new BodyInfo();
        body.setBodyId(BODY_ID);
        body.setHasBio(true);
        body.setObservedGenusPrefixes(new HashSet<>(Set.of("bacterium")));
        body.setObservedBioDisplayNames(new HashSet<>(Set.of("Bacterium Acies")));
        body.setPredictions(new ArrayList<>(List.of(makeCandidate("Bacterium Acies"))));

        List<Row> rows = BioTableBuilder.buildRows(Collections.singletonList(body), false);

        List<String> bioLabels = bioRowLabels(rows);
        assertTrue(bioLabels.stream().anyMatch(s -> s.contains("Acies")),
                "Confirmed species should appear; got: " + bioLabels);
    }

    private static List<String> bioRowLabels(List<Row> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .filter(r -> r.detail && r.bioText != null)
                .map(r -> r.bioText)
                .collect(Collectors.toList());
    }

    private static BioCandidate makeCandidate(String displayName) {
        String[] parts = displayName.split(" ", 2);
        String genus = parts.length > 0 ? parts[0] : "";
        String species = parts.length > 1 ? parts[1] : "";
        SpeciesConstraint sc = new SpeciesConstraint(genus, species, 1_000_000L, Collections.emptyList());
        return new BioCandidate(sc, 0.5, null);
    }
}