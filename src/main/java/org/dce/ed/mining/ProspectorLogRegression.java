package org.dce.ed.mining;

import java.util.ArrayList;
import java.util.List;

/**
 * Linear regression of prospector log data: percentage (X) vs actual tons collected (Y),
 * matching the Mining tab scatter trend lines (per-commander OLS for the chart).
 * <p>
 * Live prospector <strong>surface</strong> tons on the Mining tab use a single global line (same for every
 * commander), hardcoded from the calibrated overlay trend tooltip (Villunus, n=69, R²≈0.87, pct 0…37.1%).
 */
public final class ProspectorLogRegression {

	/**
	 * Global surface-material estimate: {@code tons = slope × pct + intercept} for all commanders.
	 * Source: overlay scatter trend tooltip — Villunus series, Mar 2026 (n=69, R²=0.8696, pct axis 0…37.107).
	 */
	public static final double GLOBAL_SURFACE_TONS_SLOPE = 0.9745255779;
	/** Companion to {@link #GLOBAL_SURFACE_TONS_SLOPE}; same calibration. */
	public static final double GLOBAL_SURFACE_TONS_INTERCEPT = -0.7264508360;

	private ProspectorLogRegression() {
	}

	public static final class Result {
		public final boolean valid;
		public final double slope;
		public final double intercept;
		public final int n;
		public final double rSquared;

		private Result(boolean valid, double slope, double intercept, int n, double rSquared) {
			this.valid = valid;
			this.slope = slope;
			this.intercept = intercept;
			this.n = n;
			this.rSquared = rSquared;
		}
	}

	private static Result invalid() {
		return new Result(false, 0.0, 0.0, 0, Double.NaN);
	}

	/**
	 * Ordinary least squares through {@code (percent, difference)} points.
	 */
	public static Result compute(List<ProspectorLogRow> rows) {
		if (rows == null || rows.size() < 2) {
			return invalid();
		}
		int n = 0;
		double sumX = 0.0;
		double sumY = 0.0;
		double sumXX = 0.0;
		double sumXY = 0.0;
		for (ProspectorLogRow r : rows) {
			if (r == null) {
				continue;
			}
			double x = r.getPercent();
			double y = r.getDifference();
			if (Double.isNaN(x) || Double.isNaN(y)) {
				continue;
			}
			n++;
			sumX += x;
			sumY += y;
			sumXX += x * x;
			sumXY += x * y;
		}
		if (n < 2) {
			return new Result(false, 0.0, 0.0, n, Double.NaN);
		}
		double denom = n * sumXX - sumX * sumX;
		if (Math.abs(denom) < 1e-9) {
			return new Result(false, 0.0, 0.0, n, Double.NaN);
		}
		double slope = (n * sumXY - sumX * sumY) / denom;
		double intercept = (sumY - slope * sumX) / n;
		double yMean = sumY / n;
		double ssTot = 0.0;
		double ssRes = 0.0;
		for (ProspectorLogRow r : rows) {
			if (r == null) {
				continue;
			}
			double x = r.getPercent();
			double y = r.getDifference();
			if (Double.isNaN(x) || Double.isNaN(y)) {
				continue;
			}
			double yHat = slope * x + intercept;
			double d = y - yMean;
			ssTot += d * d;
			double e = y - yHat;
			ssRes += e * e;
		}
		double rSquared = (ssTot < 1e-12) ? 1.0 : (1.0 - ssRes / ssTot);
		return new Result(true, slope, intercept, n, rSquared);
	}

	/**
	 * Prefer regression for the given commander; if that has fewer than two points or is degenerate, use all rows.
	 */
	public static Result regressionForEstimate(String commander, List<ProspectorLogRow> allRows) {
		if (allRows == null || allRows.isEmpty()) {
			return invalid();
		}
		String c = commander != null ? commander.trim() : "";
		if (!c.isEmpty()) {
			List<ProspectorLogRow> forCmd = new ArrayList<>();
			for (ProspectorLogRow r : allRows) {
				if (r == null) {
					continue;
				}
				String rc = r.getCommanderName();
				if (c.equals(rc != null ? rc.trim() : "")) {
					forCmd.add(r);
				}
			}
			Result r = compute(forCmd);
			if (r.valid) {
				return r;
			}
		}
		return compute(allRows);
	}

	/**
	 * Estimated tons for one surface material at {@code percent}% using the global calibration line
	 * ({@link #GLOBAL_SURFACE_TONS_SLOPE}, {@link #GLOBAL_SURFACE_TONS_INTERCEPT}). Commander and history are
	 * ignored so estimates match one shared formula for everyone.
	 */
	@SuppressWarnings("unused")
	public static double estimateTonsForMaterialPercent(double percent, String commander, List<ProspectorLogRow> history) {
		return Math.max(0.0, GLOBAL_SURFACE_TONS_SLOPE * percent + GLOBAL_SURFACE_TONS_INTERCEPT);
	}

	/**
	 * Estimated tons for a core (motherlode) row: mean of historical core yields for this commander, else trend intercept, else a small default.
	 */
	public static double estimateCoreTons(String commander, List<ProspectorLogRow> history) {
		if (history == null || history.isEmpty()) {
			return 8.0;
		}
		String c = commander != null ? commander.trim() : "";
		List<ProspectorLogRow> coreRows = new ArrayList<>();
		for (ProspectorLogRow row : history) {
			if (row == null) {
				continue;
			}
			if (row.getCoreType() == null || row.getCoreType().isBlank()) {
				continue;
			}
			if (!c.isEmpty()) {
				String rc = row.getCommanderName();
				if (!c.equals(rc != null ? rc.trim() : "")) {
					continue;
				}
			}
			coreRows.add(row);
		}
		if (!coreRows.isEmpty()) {
			double avg = coreRows.stream().mapToDouble(ProspectorLogRow::getDifference).average().orElse(0);
			return Math.max(0.0, avg);
		}
		Result r = regressionForEstimate(commander, history);
		if (r.valid) {
			return Math.max(0.0, r.intercept);
		}
		return 8.0;
	}
}
