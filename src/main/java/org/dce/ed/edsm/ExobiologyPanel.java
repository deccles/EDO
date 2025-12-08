package org.dce.ed.edsm;

import java.awt.BorderLayout;
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
    private final JTable table;
    private final DefaultTableModel model;

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

        model = new DefaultTableModel(
                new Object[]{"Body", "Planet type", "Atmosphere", "Gravity (g)", "Temp (K)", "Volcanism", "Predicted biology"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
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

        model.setRowCount(0);

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

            // Only landable bodies
            if (b.isLandable == null || !b.isLandable.booleanValue()) {
                continue;
            }

            // Require some kind of atmosphere
            String atmoRaw = b.atmosphereType;
            if (atmoRaw == null) {
                atmoRaw = "";
            }
            String atmoLower = atmoRaw.toLowerCase(Locale.ROOT);
            if (atmoLower.isEmpty()
                    || atmoLower.equals("none")
                    || atmoLower.contains("no atmosphere")) {
                continue;
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

            BodyAttributes attrs = new BodyAttributes(
                    planetType,
                    gravity,
                    atmoType,
                    tempK,
                    tempK,
                    hasVolcanism,
                    volcRaw
            );

            List<BioCandidate> predicted = ExobiologyData.predict(attrs);

            String predictedNames = "";
            if (predicted != null && !predicted.isEmpty()) {
                List<String> names = new ArrayList<String>();
                for (BioCandidate bc : predicted) {
                    names.add(bc.getDisplayName());
                }
                predictedNames = String.join(", ", names);
            }

            Object[] row = new Object[]{
                    b.name,
                    planetType,
                    atmoType,
                    gravity,
                    tempK,
                    hasVolcanism ? (volcRaw == null ? "Yes" : volcRaw) : (volcRaw == null ? "" : volcRaw),
                    predictedNames
            };

            model.addRow(row);
            UtilTable.autoSizeTableColumns(table);
        }
    }
}
