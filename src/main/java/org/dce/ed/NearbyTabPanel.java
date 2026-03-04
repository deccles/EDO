package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.dce.ed.cache.CachedBody;
import org.dce.ed.cache.CachedSystem;
import org.dce.ed.cache.SystemCache;
import org.dce.ed.edsm.BodiesResponse;
import org.dce.ed.edsm.SphereSystemsResponse;
import org.dce.ed.exobiology.BodyAttributes;
import org.dce.ed.exobiology.ExobiologyData;
import org.dce.ed.exobiology.ExobiologyData.AtmosphereType;
import org.dce.ed.exobiology.ExobiologyData.BioCandidate;
import org.dce.ed.exobiology.ExobiologyData.PlanetType;
import org.dce.ed.state.SystemState;
import org.dce.ed.ui.EdoUi;
import org.dce.ed.util.EdsmClient;

/**
 * Nearby tab: sphere search around current system, exobiology prediction on landable planets,
 * table of high-value systems. Respects overlay color and transparency (pass-through mode).
 */
public class NearbyTabPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final SystemTabPanel systemTabPanel;
    private final EdsmClient edsmClient = new EdsmClient();

    private final JLabel headerLabel;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JScrollPane scrollPane;

    private final AtomicBoolean firstShowDone = new AtomicBoolean(false);
    private volatile boolean refreshRequested;

    public NearbyTabPanel(SystemTabPanel systemTabPanel) {
        this.systemTabPanel = systemTabPanel;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(EdoUi.Internal.TRANSPARENT);

        headerLabel = new JLabel("Nearby (exobiology)");
        headerLabel.setForeground(EdoUi.User.MAIN_TEXT);
        headerLabel.setHorizontalAlignment(SwingConstants.LEFT);
        headerLabel.setOpaque(false);
        Font base = OverlayPreferences.getUiFont();
        headerLabel.setFont(base.deriveFont(Font.BOLD, OverlayPreferences.getUiFontSize() + 2));

        tableModel = new DefaultTableModel(new Object[]{"System", "Planets", "Value (cr)", "ValueCr"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setOpaque(false);
        table.setBackground(EdoUi.Internal.TRANSPARENT);
        table.setGridColor(EdoUi.Internal.TRANSPARENT);
        table.setForeground(EdoUi.User.MAIN_TEXT);
        table.setDefaultEditor(Object.class, null);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.getTableHeader().setForeground(EdoUi.User.MAIN_TEXT);
        table.getTableHeader().setBackground(EdoUi.User.BACKGROUND);
        table.getColumnModel().getColumn(3).setMinWidth(0);
        table.getColumnModel().getColumn(3).setMaxWidth(0);
        table.getColumnModel().getColumn(3).setWidth(0);
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            {
                setOpaque(false);
                setForeground(EdoUi.User.MAIN_TEXT);
            }

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                if (c instanceof JLabel) {
                    ((JLabel) c).setOpaque(false);
                    ((JLabel) c).setBackground(EdoUi.Internal.TRANSPARENT);
                    long minValueCr = (long) (OverlayPreferences.getNearbyMinValueMillionCredits() * 1_000_000);
                    boolean highValue = false;
                    if (tableModel.getRowCount() > row && tableModel.getColumnCount() > 3) {
                        Object valObj = tableModel.getValueAt(row, 3);
                        if (valObj instanceof Number) {
                            highValue = ((Number) valObj).longValue() >= minValueCr;
                        }
                    }
                    ((JLabel) c).setForeground(highValue ? EdoUi.User.SUCCESS : EdoUi.User.MAIN_TEXT);
                }
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);

        scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(EdoUi.Internal.TRANSPARENT);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(headerLabel, BorderLayout.WEST);
        add(north, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Called when the tab is first shown. Runs sphere search and populates the table if we have a current system.
     */
    public void onTabFirstShown() {
        if (firstShowDone.compareAndSet(false, true)) {
            refreshInBackground();
        }
    }

    /**
     * Called when the user jumps to another system. Refreshes the table for the new current system.
     */
    public void onCurrentSystemChanged(String systemName, long systemAddress) {
        refreshRequested = true;
        refreshInBackground();
    }

    private void refreshInBackground() {
        SystemState state = systemTabPanel != null ? systemTabPanel.getState() : null;
        String centerName = state != null ? state.getSystemName() : null;
        if (centerName == null || centerName.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                tableModel.addRow(new Object[]{"—", "No current system", "—"});
            });
            return;
        }

        int radiusLy = OverlayPreferences.getNearbySphereRadiusLy();
        long minValueCr = (long) (OverlayPreferences.getNearbyMinValueMillionCredits() * 1_000_000);

        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                List<Object[]> rows = new ArrayList<>();
                try {
                    SphereSystemsResponse[] systems = edsmClient.sphereSystemsLocal(centerName.trim(), radiusLy);
                    if (systems == null || systems.length == 0) {
                        return rows;
                    }
                    SystemCache cache = SystemCache.getInstance();
                    for (SphereSystemsResponse sys : systems) {
                        if (sys == null || sys.name == null || sys.name.isEmpty()) {
                            continue;
                        }
                        CachedSystem cs = cache.get(0L, sys.name);
                        List<BodyValue> bodyValues = new ArrayList<>();
                        if (cs != null && cs.bodies != null) {
                            double[] starPos = cs.starPos != null ? cs.starPos : new double[3];
                            for (CachedBody cb : cs.bodies) {
                                if (!cb.landable) {
                                    continue;
                                }
                                if (isExcluded(cb)) {
                                    continue;
                                }
                                if (!hasAtmosphere(cb)) {
                                    continue;
                                }
                                List<BioCandidate> preds = cb.predictions != null && !cb.predictions.isEmpty()
                                        ? cb.predictions
                                        : predictFromCachedBody(cb, starPos, cs.systemName);
                                if (preds == null || preds.isEmpty()) {
                                    continue;
                                }
                                boolean firstBonus = !Boolean.TRUE.equals(cb.wasFootfalled);
                                long maxVal = 0;
                                for (BioCandidate bc : preds) {
                                    if (bc != null) {
                                        long v = bc.getEstimatedPayout(firstBonus);
                                        if (v > maxVal) {
                                            maxVal = v;
                                        }
                                    }
                                }
                                if (maxVal > 0) {
                                    bodyValues.add(new BodyValue(cb.name, maxVal));
                                }
                            }
                        } else {
                            try {
                                BodiesResponse bodiesResp = edsmClient.showBodies(sys.name);
                                if (bodiesResp != null && bodiesResp.bodies != null && bodiesResp.coords != null) {
                                    double x = bodiesResp.coords.x != null ? bodiesResp.coords.x : 0;
                                    double y = bodiesResp.coords.y != null ? bodiesResp.coords.y : 0;
                                    double z = bodiesResp.coords.z != null ? bodiesResp.coords.z : 0;
                                    double[] starPos = new double[]{x, y, z};
                                    for (BodiesResponse.Body b : bodiesResp.bodies) {
                                        if (b == null || b.isLandable == null || !b.isLandable) {
                                            continue;
                                        }
                                        if (!hasAtmosphereEdsm(b)) {
                                            continue;
                                        }
                                        List<BioCandidate> preds = predictFromEdsmBody(b, bodiesResp, starPos);
                                        if (preds == null || preds.isEmpty()) {
                                            continue;
                                        }
                                        boolean firstBonus = true;
                                        long maxVal = 0;
                                        for (BioCandidate bc : preds) {
                                            if (bc != null) {
                                                long v = bc.getEstimatedPayout(firstBonus);
                                                if (v > maxVal) {
                                                    maxVal = v;
                                                }
                                            }
                                        }
                                        if (maxVal > 0) {
                                            bodyValues.add(new BodyValue(b.name, maxVal));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // skip this system
                            }
                        }
                        if (bodyValues.isEmpty()) {
                            continue;
                        }
                        long systemTotal = 0;
                        List<String> names = new ArrayList<>();
                        for (BodyValue bv : bodyValues) {
                            systemTotal += bv.valueCr;
                            names.add(bv.bodyName);
                        }
                        rows.add(new Object[]{
                                sys.name,
                                String.join(", ", names),
                                String.format(Locale.ROOT, "%,d", systemTotal),
                                Long.valueOf(systemTotal)
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                rows.sort(Comparator.comparingLong((Object[] row) -> ((Long) row[3]).longValue()).reversed());
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> result = get();
                    SwingUtilities.invokeLater(() -> {
                        tableModel.setRowCount(0);
                        if (result == null || result.isEmpty()) {
                            tableModel.addRow(new Object[]{"—", "No systems with exobiology in range", "—", Long.valueOf(0L)});
                        } else {
                            for (Object[] row : result) {
                                tableModel.addRow(row);
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    SwingUtilities.invokeLater(() -> {
                        tableModel.setRowCount(0);
                        tableModel.addRow(new Object[]{"—", "Interrupted", "—", Long.valueOf(0L)});
                    });
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    SwingUtilities.invokeLater(() -> {
                        tableModel.setRowCount(0);
                        tableModel.addRow(new Object[]{"—", "Error: " + cause.getMessage(), "—", Long.valueOf(0L)});
                    });
                }
            }
        }.execute();
    }

    private static boolean isExcluded(CachedBody cb) {
        if (Boolean.TRUE.equals(cb.wasFootfalled)) {
            return true;
        }
        if (cb.bioSampleCountsByDisplayName != null && !cb.bioSampleCountsByDisplayName.isEmpty()) {
            return true;
        }
        return false;
    }

    private static boolean hasAtmosphere(CachedBody cb) {
        String a = cb.atmosphere != null ? cb.atmosphere : (cb.atmoOrType != null ? cb.atmoOrType : "");
        return hasAtmosphereString(a);
    }

    private static boolean hasAtmosphereEdsm(BodiesResponse.Body b) {
        String a = b.atmosphereType != null ? b.atmosphereType : "";
        return hasAtmosphereString(a);
    }

    private static boolean hasAtmosphereString(String a) {
        if (a == null || a.trim().isEmpty()) {
            return false;
        }
        String lower = a.toLowerCase(Locale.ROOT);
        return !lower.equals("none") && !lower.contains("no atmosphere");
    }

    private static List<BioCandidate> predictFromCachedBody(CachedBody cb, double[] starPos, String systemName) {
        if (starPos == null || starPos.length < 3) {
            starPos = new double[]{0, 0, 0};
        }
        PlanetType pt = ExobiologyData.parsePlanetType(cb.planetClass);
        String atmoRaw = cb.atmoOrType != null ? cb.atmoOrType : (cb.atmosphere != null ? cb.atmosphere : "");
        AtmosphereType at = ExobiologyData.parseAtmosphere(atmoRaw);
        double gravity = (cb.gravityMS != null && !Double.isNaN(cb.gravityMS)) ? cb.gravityMS / 9.80665 : 0.0;
        double tempK = cb.surfaceTempK != null ? cb.surfaceTempK : 0.0;
        boolean hasVolc = cb.volcanism != null && !cb.volcanism.isEmpty()
                && !cb.volcanism.toLowerCase(Locale.ROOT).startsWith("no volcanism");
        BodyAttributes attrs = new BodyAttributes(
                cb.name,
                systemName != null ? systemName : (cb.starSystem != null ? cb.starSystem : ""),
                starPos,
                pt,
                gravity,
                at,
                tempK,
                tempK,
                cb.surfacePressure,
                hasVolc,
                cb.volcanism
        );
        List<BioCandidate> pred = ExobiologyData.predict(attrs);
        return pred != null ? pred : Collections.emptyList();
    }

    private static List<BioCandidate> predictFromEdsmBody(BodiesResponse.Body b, BodiesResponse bodies, double[] starPos) {
        if (bodies.coords == null) {
            return Collections.emptyList();
        }
        double x = bodies.coords.x != null ? bodies.coords.x : 0;
        double y = bodies.coords.y != null ? bodies.coords.y : 0;
        double z = bodies.coords.z != null ? bodies.coords.z : 0;
        double[] coords = new double[]{x, y, z};
        PlanetType pt = ExobiologyData.parsePlanetType(b.subType);
        String atmoRaw = b.atmosphereType != null ? b.atmosphereType : "";
        AtmosphereType at = ExobiologyData.parseAtmosphere(atmoRaw);
        double gravity = b.gravity != null ? b.gravity : 0.0;
        double tempK = b.surfaceTemperature != null ? b.surfaceTemperature : 0.0;
        boolean hasVolc = b.volcanismType != null && !b.volcanismType.isEmpty()
                && !b.volcanismType.toLowerCase(Locale.ROOT).startsWith("no volcanism");
        BodyAttributes attrs = new BodyAttributes(
                b.name,
                bodies.name != null ? bodies.name : "",
                coords,
                pt,
                gravity,
                at,
                tempK,
                tempK,
                b.getSurfacePressure(),
                hasVolc,
                b.volcanismType
        );
        List<BioCandidate> pred = ExobiologyData.predict(attrs);
        return pred != null ? pred : Collections.emptyList();
    }

    private static final class BodyValue {
        final String bodyName;
        final long valueCr;

        BodyValue(String bodyName, long valueCr) {
            this.bodyName = bodyName;
            this.valueCr = valueCr;
        }
    }

    public void applyOverlayBackground(Color bg) {
        if (bg == null) {
            bg = EdoUi.Internal.TRANSPARENT;
        }
        boolean opaque = bg.getAlpha() >= 255;
        setOpaque(opaque);
        setBackground(bg);
        table.setOpaque(false);
        table.setBackground(EdoUi.Internal.TRANSPARENT);
        table.getTableHeader().setOpaque(!opaque);
        if (!opaque) {
            table.getTableHeader().setBackground(EdoUi.Internal.TRANSPARENT);
        } else {
            table.getTableHeader().setBackground(EdoUi.User.BACKGROUND);
        }
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(EdoUi.Internal.TRANSPARENT);
        headerLabel.setOpaque(false);
        headerLabel.setBackground(EdoUi.Internal.TRANSPARENT);
        repaint();
    }

    public void applyUiFont(Font font) {
        if (font != null && headerLabel != null) {
            headerLabel.setFont(font.deriveFont(Font.BOLD, font.getSize() + 2));
        }
        if (table != null) {
            table.setFont(font != null ? font : table.getFont());
            table.getTableHeader().setFont(font != null ? font.deriveFont(Font.BOLD) : table.getTableHeader().getFont());
        }
        revalidate();
        repaint();
    }

    public void applyUiFontPreferences() {
        Font font = OverlayPreferences.getUiFont();
        applyUiFont(font);
    }
}
