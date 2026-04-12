package org.dce.ed.tts;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TtsSprintfMaterialListTest {

    @Test
    void materialSplitsOnWhitespaceLikeSpecies() {
        TtsSprintf sp = new TtsSprintf(new PollyTtsCached());
        List<String> chunks = sp.formatToUtteranceChunks("Prospector found {material} at {n} percent.",
                "Low Temperature Diamonds", 12);
        assertTrue(chunks.contains("Low"));
        assertTrue(chunks.contains("Temperature"));
        assertTrue(chunks.contains("Diamonds"));
        assertTrue(chunks.contains("12"));
    }

    @Test
    void listStaysSingleChunkSoProsodyMatchesJoinWithAnd() {
        TtsSprintf sp = new TtsSprintf(new PollyTtsCached());
        List<String> chunks = sp.formatToUtteranceChunks("Prospector found {list} from {min} to {max} percent.",
                "Tritium and Platinum", 10, 90);
        assertTrue(chunks.stream().anyMatch(c -> c.contains("Tritium") && c.contains("Platinum")),
                "chunks=" + chunks);
    }
}
