package org.dce.ed.mining;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Shared, testable rules for how prospector mining logs treat <strong>run start</strong> and
 * <strong>run end</strong> times in Google Sheets (and the same ideas elsewhere).
 * <p>
 * See {@link MiningRunNumberResolver} for run <em>number</em> selection. This class covers:
 * </p>
 * <ul>
 *   <li><strong>Upsert run start (column N / index 13):</strong> never overwrite a non-blank cell — cargo
 *       updates after a new undock must not replace the original trip start.</li>
 *   <li><strong>Run end placement:</strong> only one sheet row per run should receive end time on dock —
 *       prefer asteroid {@code A} with a start time, else the first data row with a start time.</li>
 * </ul>
 * <p>
 * Regressions here have shipped when logic was inlined in {@code GoogleSheetsBackend} without re-reading
 * these invariants; keep changes here and in tests together.
 * </p>
 */
public final class ProspectorMiningLogPolicy {

    private ProspectorMiningLogPolicy() {
    }

    /**
     * When upserting an existing sheet row, write run start into column 13 only if the incoming row carries
     * a start instant and the cell is still empty. Preserves the canonical start time across later cargo upserts.
     */
    public static boolean shouldWriteRunStartOnUpsertExistingRow(String existingStartCellText, Instant incomingRunStart) {
        if (incomingRunStart == null) {
            return false;
        }
        String s = existingStartCellText != null ? existingStartCellText.trim() : "";
        return s.isEmpty();
    }

    /**
     * Finds the 1-based data row index in {@code values} (row {@code 0} is the header) that should receive
     * run end time on dock, or {@code -1} if none.
     * <p>
     * Matches {@link org.dce.ed.mining.GoogleSheetsBackend#updateRunEndTime} scan order.
     * </p>
     */
    public static int findDataRowIndexForCanonicalRunEnd(List<List<Object>> values, int run, String commander) {
        if (values == null || values.size() < 2) {
            return -1;
        }
        String cmdr = commander != null ? commander : "";
        int preferA = findFirstMatching(values, run, cmdr, true);
        if (preferA >= 0) {
            return preferA;
        }
        return findFirstMatching(values, run, cmdr, false);
    }

    private static int findFirstMatching(List<List<Object>> values, int run, String cmdr, boolean requireAsteroidA) {
        for (int i = 1; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row == null || row.size() < 13) {
                continue;
            }
            int rowRun = parseInt(row.get(0), 0);
            String rowCommander = str(row.get(12));
            String rowStart = row.size() > 13 ? str(row.get(13)) : "";
            if (rowRun != run || !Objects.equals(rowCommander, cmdr) || rowStart.isEmpty()) {
                continue;
            }
            if (requireAsteroidA) {
                String asteroid = str(row.get(1));
                if (!"A".equalsIgnoreCase(asteroid)) {
                    continue;
                }
            }
            return i;
        }
        return -1;
    }

    private static String str(Object o) {
        return o != null ? o.toString().trim() : "";
    }

    private static int parseInt(Object o, int def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
