/**
 * Prospector / mining log backends (Google Sheets, CSV) and shared rules for run boundaries.
 * <p>
 * <b>Run timing conventions (regression-tested; do not change casually)</b>
 * </p>
 * <ul>
 *   <li>{@link MiningRunNumberResolver} — how the next {@linkplain org.dce.ed.mining.ProspectorLogRow#getRun()
 *       run number} is chosen given existing rows (open vs closed runs, multi-row same run, etc.).</li>
 *   <li>{@link ProspectorMiningLogPolicy} — sheet upsert must not overwrite a non-blank run start; dock writes
 *       run end on exactly one canonical row per run (prefer asteroid {@code A} with start).</li>
 *   <li>{@link LocalCsvBackend#updateRunEndTime(String, int, java.time.Instant)} mirrors the same canonical-row
 *       rule as Google Sheets.</li>
 * </ul>
 * <p>
 * UI wiring in {@link org.dce.ed.MiningTabPanel} delegates run numbering to the resolver and documents
 * when {@code runStart} is set on newly written rows (first cargo batch of a run only).
 * </p>
 */
package org.dce.ed.mining;
