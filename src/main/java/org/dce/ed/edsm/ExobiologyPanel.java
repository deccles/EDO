package org.dce.ed.edsm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import org.dce.ed.exobiology.ExobiologyData;
import org.dce.ed.exobiology.ExobiologyData.AtmosphereType;
import org.dce.ed.exobiology.ExobiologyData.BodyAttributes;
import org.dce.ed.exobiology.ExobiologyData.BioCandidate;
import org.dce.ed.exobiology.ExobiologyData.PlanetType;

/**
 * EDSM -> Exobiology prediction panel.
 *
 * Given a system name, queries EDSM /bodies, filters to landable
 * worlds with an atmosphere, and runs them through ExobiologyData
 * to show predicted biology per body.
 */
public class ExobiologyPanel extends JPanel {

    private final EdsmClient client;

    private final JTextField systemField;
    private final JButton searchButton;

    private final JPanel bodiesContainer;
    private final JScrollPane scroll;

    public ExobiologyPanel(EdsmClient client) {
        this.client = client;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        systemField = new JTextField(30);
        searchButton = new JButton("Query EDSM");

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        inputRow.add(new JLabel("System: "));
        inputRow.add(Box.createHorizontalStrut(4));
        inputRow.add(systemField);
        inputRow.add(Box.createHorizontalStrut(8));
        inputRow.add(searchButton);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(inputRow);

        add(north, BorderLayout.NORTH);

        bodiesContainer = new JPanel();
        bodiesContainer.setLayout(new BoxLayout(bodiesContainer, BoxLayout.Y_AXIS));
        bodiesContainer.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        scroll = new JScrollPane(bodiesContainer);
        add(scroll, BorderLayout.CENTER);

        searchButton.addActionListener(e -> performSearch());
    }

    private void performSearch() {
        String systemName = systemField.getText();
        if (systemName == null) {
            return;
        }

        systemName = systemName.trim();
        if (systemName.isEmpty()) {
            return;
        }

        bodiesContainer.removeAll();
        bodiesContainer.revalidate();
        bodiesContainer.repaint();

        BodiesResponse bodies;
        try {
            bodies = client.showBodies(systemName);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        if (bodies == null || bodies.bodies == null || bodies.bodies.isEmpty()) {
            return;
        }

        for (BodiesResponse.Body b : bodies.bodies) {

            List<String> edoPredictions = getEdoPredictions(bodies, b);
            if (edoPredictions.isEmpty()) {
                continue;
            }

            JPanel section = buildBodySection(systemName, b, edoPredictions);
            bodiesContainer.add(section);
            bodiesContainer.add(Box.createVerticalStrut(10));
        }

        SwingUtilities.invokeLater(() -> {
            bodiesContainer.revalidate();
            bodiesContainer.repaint();
            scroll.getVerticalScrollBar().setValue(0);
        });
    }

    private JPanel buildBodySection(String systemName, BodiesResponse.Body b, List<String> edoPredictions) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));

        String title = b.name;
        if (systemName != null && !systemName.trim().isEmpty()) {
            title = b.name.replace(systemName, "").trim();
            if (title.isEmpty()) {
                title = b.name;
            }
        }

        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title);
        border.setTitleFont(border.getTitleFont().deriveFont(border.getTitleFont().getStyle() | java.awt.Font.BOLD));
        section.setBorder(border);

        JTable planetTable = new JTable(buildPlanetInfoModel(b));
        planetTable.setFillsViewportHeight(true);
        planetTable.setRowSelectionAllowed(false);
        planetTable.setCellSelectionEnabled(false);
        planetTable.setShowGrid(true);
        UtilTable.autoSizeTableColumns(planetTable);

        JScrollPane planetScroll = new JScrollPane(planetTable);
        planetScroll.setBorder(BorderFactory.createTitledBorder("Planetary info"));
        planetScroll.setPreferredSize(new Dimension(10, Math.min(260, (planetTable.getRowCount() + 1) * planetTable.getRowHeight() + 28)));
        planetScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        section.add(planetScroll);

        section.add(Box.createVerticalStrut(8));

        JTable bioTable = new JTable(buildBiologyModel(edoPredictions));
        bioTable.setFillsViewportHeight(true);
        bioTable.setRowSelectionAllowed(false);
        bioTable.setCellSelectionEnabled(false);
        bioTable.setShowGrid(true);
        UtilTable.autoSizeTableColumns(bioTable);

        JScrollPane bioScroll = new JScrollPane(bioTable);
        bioScroll.setBorder(BorderFactory.createTitledBorder("Biology"));
        bioScroll.setPreferredSize(new Dimension(10, Math.min(180, (bioTable.getRowCount() + 1) * bioTable.getRowHeight() + 28)));
        bioScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        section.add(bioScroll);

        return section;
    }

    private DefaultTableModel buildPlanetInfoModel(BodiesResponse.Body b) {
        DefaultTableModel m = new DefaultTableModel(new Object[]{"Field", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        addIfPresent(m, "Name", b.name);
        addIfPresent(m, "Type", b.type);
        addIfPresent(m, "Sub-type", b.subType);
        addIfPresent(m, "EDSM body id", Long.valueOf(b.id));

        if (b.discovery != null) {
            addIfPresent(m, "Discovered by", b.discovery.commander);
            addIfPresent(m, "Discovery date", b.discovery.date);
        }

        addIfPresent(m, "Distance to arrival (ls)", b.distanceToArrival);
        addIfPresent(m, "Landable", b.isLandable);
        addIfPresent(m, "Atmosphere", b.atmosphereType);
        addIfPresent(m, "Terraforming", b.terraformingState);
        addIfPresent(m, "Volcanism", b.volcanismType);

        addIfPresent(m, "Surface temperature (K)", b.surfaceTemperature);
        addIfPresent(m, "Surface pressure (atm)", b.getSurfacePressure());
        addIfPresent(m, "Gravity (g)", b.gravity);
        addIfPresent(m, "Surface gravity", b.surfaceGravity);
        addIfPresent(m, "Earth masses", b.earthMasses);
        addIfPresent(m, "Radius", b.radius);

        addIfPresent(m, "Orbital period", b.orbitalPeriod);
        addIfPresent(m, "Semi-major axis", b.semiMajorAxis);
        addIfPresent(m, "Orbital eccentricity", b.orbitalEccentricity);
        addIfPresent(m, "Orbital inclination", b.orbitalInclination);
        addIfPresent(m, "Argument of periapsis", b.argOfPeriapsis);

        addIfPresent(m, "Rotational period", b.rotationalPeriod);
        addIfPresent(m, "Tidally locked", b.rotationalPeriodTidallyLocked);
        addIfPresent(m, "Axial tilt", b.axialTilt);

        // Star-only fields (but harmless if null)
        addIfPresent(m, "Main star", b.isMainStar);
        addIfPresent(m, "Scoopable", b.isScoopable);
        addIfPresent(m, "Age (Myr)", b.age);
        addIfPresent(m, "Luminosity", b.luminosity);
        addIfPresent(m, "Absolute magnitude", b.absoluteMagnitude);
        addIfPresent(m, "Solar masses", b.solarMasses);
        addIfPresent(m, "Solar radius", b.solarRadius);

        if (b.rings != null && !b.rings.isEmpty()) {
            addIfPresent(m, "Rings", Integer.valueOf(b.rings.size()));
            int idx = 1;
            for (BodiesResponse.Body.Ring r : b.rings) {
                String ringName = r.name;
                if (ringName == null || ringName.trim().isEmpty()) {
                    ringName = "Ring " + idx;
                }
                String details = safe(r.type);
                if (r.mass != null) {
                    details = details + "  mass=" + fmt(r.mass);
                }
                if (r.innerRadius != null || r.outerRadius != null) {
                    details = details + "  radius=" + fmt(r.innerRadius) + "-" + fmt(r.outerRadius);
                }
                addIfPresent(m, "  " + ringName, details.trim());
                idx++;
            }
        }

        return m;
    }

    private DefaultTableModel buildBiologyModel(List<String> edoPredictions) {
        DefaultTableModel m = new DefaultTableModel(new Object[]{"EDO Prediction", "BioScan Predictions"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (String p : edoPredictions) {
            m.addRow(new Object[]{p, ""});
        }

        return m;
    }

    private static List<String> getEdoPredictions(BodiesResponse bodies, BodiesResponse.Body b) {
        // Only landable bodies with an atmosphere are eligible for predictions.
        if (b == null || b.isLandable == null || !b.isLandable.booleanValue()) {
            return new ArrayList<>();
        }

        String atmoRaw = safe(b.atmosphereType);
        String atmoLower = atmoRaw.toLowerCase(Locale.ROOT);
        if (atmoLower.isEmpty()
                || atmoLower.equals("none")
                || atmoLower.contains("no atmosphere")) {
            return new ArrayList<>();
        }

        PlanetType planetType = ExobiologyData.parsePlanetType(b.subType);
        AtmosphereType atmoType = ExobiologyData.parseAtmosphere(atmoRaw);

        double gravity = 0.0;
        if (b.gravity != null) {
            gravity = b.gravity.doubleValue();
        }

        double tempK = 0.0;
        if (b.surfaceTemperature != null) {
            tempK = b.surfaceTemperature.doubleValue();
        }

        String volcRaw = b.volcanismType;
        boolean hasVolcanism = false;
        if (volcRaw != null) {
            String v = volcRaw.toLowerCase(Locale.ROOT).trim();
            if (!v.isEmpty()
                    && !v.startsWith("no volcanism")) {
                hasVolcanism = true;
            }
        }

        double starPos[] = new double[] { bodies.coords.x, bodies.coords.y, bodies.coords.z };
        		
        BodyAttributes attrs = new BodyAttributes(
                b.name,
                bodies.name,
                starPos,
                planetType,
                gravity,
                atmoType,
                tempK,
                tempK,
                b.getSurfacePressure(),
                hasVolcanism,
                volcRaw
        );

        List<BioCandidate> predicted = ExobiologyData.predict(attrs);
        if (predicted == null || predicted.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> out = new ArrayList<>();
        for (BioCandidate bc : predicted) {
            if (bc == null) {
                continue;
            }
            String name = safe(bc.getDisplayName()).trim();
            if (name.isEmpty()) {
                continue;
            }
            out.add(name);
        }

        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private static void addIfPresent(DefaultTableModel model, String field, Object value) {
        if (value == null) {
            return;
        }

        String s;
        if (value instanceof Double) {
            s = fmt((Double) value);
        } else {
            s = String.valueOf(value);
        }

        s = safe(s);
        if (s.trim().isEmpty()) {
            return;
        }

        model.addRow(new Object[]{field, s});
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String fmt(Double d) {
        if (d == null) {
            return "";
        }
        // Keep it readable; avoid scientific notation for common EDSM fields.
        return String.format(Locale.ROOT, "%.6f", d.doubleValue()).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
