package org.dce.ed.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProspectorLogRegressionTest {

	@Test
	void estimateTonsUsesGlobalLineIgnoresHistory() {
		double withEmpty = ProspectorLogRegression.estimateTonsForMaterialPercent(20.0, "AnyCmdr", Collections.emptyList());
		double withNullHistory = ProspectorLogRegression.estimateTonsForMaterialPercent(20.0, "Other", null);
		assertEquals(withEmpty, withNullHistory, 1e-9);
		assertEquals(
				Math.max(0.0, ProspectorLogRegression.GLOBAL_SURFACE_TONS_SLOPE * 20.0 + ProspectorLogRegression.GLOBAL_SURFACE_TONS_INTERCEPT),
				withEmpty,
				1e-6);
	}

	@Test
	void estimateTonsClampsNegativeToZero() {
		double t = ProspectorLogRegression.estimateTonsForMaterialPercent(0.0, "", List.of());
		assertEquals(0.0, t, 1e-9);
		assertTrue(ProspectorLogRegression.GLOBAL_SURFACE_TONS_SLOPE * 0.1 + ProspectorLogRegression.GLOBAL_SURFACE_TONS_INTERCEPT < 0);
		double lowPct = ProspectorLogRegression.estimateTonsForMaterialPercent(0.1, "", List.of());
		assertEquals(0.0, lowPct, 1e-9);
	}
}
