package org.dce.ed.tools;

import org.dce.ed.OverlayPreferences;
import org.dce.ed.mining.GoogleSheetsBackend;
import org.dce.ed.mining.ProspectorLogRow;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ActiveRunInspector {

	private static final DateTimeFormatter TS_FMT =
		DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss").withZone(ZoneId.systemDefault());

	private record RunKey(int run, String commander) {}

	public static void main(String[] args) {
		String url = OverlayPreferences.getMiningGoogleSheetsUrl();
		if (url == null || url.isBlank()) {
			System.err.println("No mining Google Sheets URL configured in preferences.");
			System.exit(1);
		}

		String commanderFilter = OverlayPreferences.getMiningLogCommanderName();
		if (commanderFilter == null || commanderFilter.isBlank()) {
			commanderFilter = "-";
		}

		GoogleSheetsBackend backend = new GoogleSheetsBackend(url);
		List<ProspectorLogRow> rows = backend.loadRows();
		if (rows == null || rows.isEmpty()) {
			System.out.println("No rows.");
			return;
		}

		Map<RunKey, List<ProspectorLogRow>> byRun = new HashMap<>();
		for (ProspectorLogRow r : rows) {
			if (r == null) continue;
			String cmdr = r.getCommanderName();
			if (cmdr == null || cmdr.isBlank()) cmdr = "-";
			if (!cmdr.equalsIgnoreCase(commanderFilter)) continue;
			byRun.computeIfAbsent(new RunKey(r.getRun(), cmdr), k -> new ArrayList<>()).add(r);
		}
		if (byRun.isEmpty()) {
			System.out.println("No rows for commander " + commanderFilter);
			return;
		}

		RunKey latestKey = byRun.keySet().stream()
			.max(Comparator.comparingInt(k -> k.run))
			.orElse(null);
		if (latestKey == null) {
			System.out.println("No runs for commander " + commanderFilter);
			return;
		}

		List<ProspectorLogRow> latestRows = byRun.get(latestKey);
		if (latestRows == null || latestRows.isEmpty()) {
			System.out.println("No rows for latest run " + latestKey.run);
			return;
		}

		ProspectorLogRow canonical = latestRows.stream()
			.filter(r -> r.getRunStartTime() != null)
			.findFirst()
			.orElse(null);

		Instant start = canonical != null ? canonical.getRunStartTime() : null;
		Instant end = canonical != null ? canonical.getRunEndTime() : null;
		boolean active = (start != null && end == null);

		System.out.println("Commander: " + latestKey.commander);
		System.out.println("Run #:     " + latestKey.run);
		System.out.println("Start:     " + (start != null ? TS_FMT.format(start) : "(none)"));
		System.out.println("End:       " + (end != null ? TS_FMT.format(end) : "(none)"));
		System.out.println("Active:    " + active);
	}
}