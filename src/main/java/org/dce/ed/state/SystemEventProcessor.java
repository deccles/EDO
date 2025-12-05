package org.dce.ed.state;

import java.util.List;
import java.util.Locale;

import org.dce.ed.SystemCache;
import org.dce.ed.exobiology.ExobiologyData;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.EliteLogEvent.FsdJumpEvent;
import org.dce.ed.logreader.EliteLogEvent.FssBodySignalsEvent;
import org.dce.ed.logreader.EliteLogEvent.FssDiscoveryScanEvent;
import org.dce.ed.logreader.EliteLogEvent.LocationEvent;
import org.dce.ed.logreader.EliteLogEvent.SaasignalsFoundEvent;
import org.dce.ed.logreader.EliteLogEvent.ScanEvent;
import org.dce.ed.logreader.EliteLogEvent.ScanOrganicEvent;

/**
 * Consumes Elite Dangerous journal events and mutates a SystemState.
 *
 * All GUI logic has been removed.
 * This engine may be invoked by Swing, CLI tools, or background processors.
 */
public class SystemEventProcessor {

    private final SystemState state;

    public SystemEventProcessor(SystemState state) {
        this.state = state;
    }

    /**
     * Entry point: consume any event and update SystemState.
     */
    public void handleEvent(EliteLogEvent event) {
        if (event instanceof LocationEvent) {
            LocationEvent e = (LocationEvent) event;
            enterSystem(e.getStarSystem(), e.getSystemAddress());
            return;
        }

        if (event instanceof FsdJumpEvent) {
            FsdJumpEvent e = (FsdJumpEvent) event;
            enterSystem(e.getStarSystem(), e.getSystemAddress());
            return;
        }

        if (event instanceof FssDiscoveryScanEvent) {
            handleFssDiscovery((FssDiscoveryScanEvent) event);
            return;
        }

        if (event instanceof ScanEvent) {
            handleScan((ScanEvent) event);
            return;
        }

        if (event instanceof SaasignalsFoundEvent) {
            handleSaaSignals((SaasignalsFoundEvent) event);
            return;
        }

        if (event instanceof FssBodySignalsEvent) {
            handleFssBodySignals((FssBodySignalsEvent) event);
            return;
        }

        if (event instanceof ScanOrganicEvent) {
            handleScanOrganic((ScanOrganicEvent) event);
            return;
        }
    }

    // ---------------------------------------------------------------------
    // System transition handling
    // ---------------------------------------------------------------------

    private void enterSystem(String name, long addr) {
        boolean sameName = name != null && name.equals(state.getSystemName());
        boolean sameAddr = addr != 0L && addr == state.getSystemAddress();

        if (sameName || sameAddr) {
            if (name != null) {
                state.setSystemName(name);
            }
            if (addr != 0L) {
                state.setSystemAddress(addr);
            }
            return;
        }

        // New system: clear old one
        state.setSystemName(name);
        state.setSystemAddress(addr);
        state.resetBodies();
        state.setTotalBodies(null);
        state.setNonBodyCount(null);
        state.setFssProgress(null);
    }

    // ---------------------------------------------------------------------
    // FSS Discovery (honk)
    // ---------------------------------------------------------------------

    private void handleFssDiscovery(FssDiscoveryScanEvent e) {
        if (state.getSystemName() == null) {
            state.setSystemName(e.getSystemName());
        }
        if (e.getSystemAddress() != 0L) {
            state.setSystemAddress(e.getSystemAddress());
        }

        state.setFssProgress(e.getProgress());
        state.setTotalBodies(e.getBodyCount());
        state.setNonBodyCount(e.getNonBodyCount());
    }

    // ---------------------------------------------------------------------
    // SCAN event (detailed body scan)
    // ---------------------------------------------------------------------

    private void handleScan(ScanEvent e) {
        if (isBeltOrRing(e.getBodyName())) {
            return;
        }

        BodyInfo info = state.getOrCreateBody(e.getBodyId());
        info.setBodyId(e.getBodyId());
        info.setName(e.getBodyName());
        info.setShortName(state.computeShortName(e.getBodyName()));

        info.setDistanceLs(e.getDistanceFromArrivalLs());
        info.setLandable(e.isLandable());
        info.setGravityMS(e.getSurfaceGravity());
        info.setAtmoOrType(chooseAtmoOrType(e));
        info.setHighValue(isHighValue(e));

        info.setPlanetClass(e.getPlanetClass());
        info.setAtmosphere(e.getAtmosphere());

        if (e.getSurfaceTemperature() != null) {
            info.setSurfaceTempK(e.getSurfaceTemperature());
        }

        if (e.getVolcanism() != null && !e.getVolcanism().isEmpty()) {
            info.setVolcanism(e.getVolcanism());
        }

        updatePredictions(info);
    }

    // ---------------------------------------------------------------------
    // SAASignalsFound (DSS results)
    // ---------------------------------------------------------------------

    private void handleSaaSignals(SaasignalsFoundEvent e) {
        BodyInfo info = state.getOrCreateBody(e.getBodyId());

        List<SaasignalsFoundEvent.Signal> signals = e.getSignals();
        if (signals != null) {
            for (SaasignalsFoundEvent.Signal s : signals) {
                String type = toLower(s.getType());
                String loc = toLower(s.getTypeLocalised());

                if (type.contains("biological") || loc.contains("biological")) {
                    info.setHasBio(true);
                } else if (type.contains("geological") || loc.contains("geological")) {
                    info.setHasGeo(true);
                }
            }
        }

        List<SaasignalsFoundEvent.Genus> genuses = e.getGenuses();
        if (genuses != null) {
            for (SaasignalsFoundEvent.Genus g : genuses) {
                String genusName = toLower(g.getGenusLocalised());
                if (genusName.isEmpty()) {
                    genusName = toLower(g.getGenus());
                }
                if (!genusName.isEmpty()) {
                    info.addObservedGenus(genusName);
                }
            }
        }

        updatePredictions(info);
    }

    // ---------------------------------------------------------------------
    // FSSBodySignalsEvent (FSS version of DSS signals)
    // ---------------------------------------------------------------------

    private void handleFssBodySignals(FssBodySignalsEvent e) {
        BodyInfo info = state.getOrCreateBody(e.getBodyId());

        List<SaasignalsFoundEvent.Signal> signals = e.getSignals();
        if (signals != null) {
            for (SaasignalsFoundEvent.Signal s : signals) {
                String type = toLower(s.getType());
                String loc = toLower(s.getTypeLocalised());

                if (type.contains("biological") || loc.contains("biological")) {
                    info.setHasBio(true);
                } else if (type.contains("geological") || loc.contains("geological")) {
                    info.setHasGeo(true);
                }
            }
        }

        updatePredictions(info);
    }

    // ---------------------------------------------------------------------
    // ScanOrganic – the most important exobiology event
    // ---------------------------------------------------------------------

    private void handleScanOrganic(EliteLogEvent.ScanOrganicEvent e) {
    	// Tighten system address if needed
        if (e.getSystemAddress() != 0L && state.getSystemAddress() == 0L) {
            state.setSystemAddress(e.getSystemAddress());
        }

        // If we don't know the system name yet but have a body name, infer it
        state.setSystemNameIfEmptyFromBodyName(e.getBodyName());

        BodyInfo info = state.getOrCreateBody(e.getBodyId());
        info.setHasBio(true);

        // Make sure the body has a name / short name
        String bodyName = e.getBodyName();
        if (bodyName != null && !bodyName.isEmpty()) {
            if (info.getName() == null || info.getName().isEmpty()) {
                info.setName(bodyName);
            }
            if (info.getShortName() == null || info.getShortName().isEmpty()) {
                info.setShortName(state.computeShortName(bodyName));
            }
        }

        // --- Genus + species handling ---
        String genusName = firstNonBlank(e.getGenusLocalised(), e.getGenus());
        String speciesName = firstNonBlank(e.getSpeciesLocalised(), e.getSpecies());

        if (speciesName.contains(" ")) {
        	speciesName = speciesName.split(" ")[1];
        	System.out.println("Sketchy, is this the right place to do this?");
        }
        
        if (genusName != null && !genusName.isEmpty()) {
            // used for narrowing predictions
            info.addObservedGenusPrefix(genusName);

            // "truth" display name: "Bacterium Nebulus", "Stratum Tectonicas", etc.
            String displayName;
            if (speciesName != null && !speciesName.isEmpty()) {
                displayName = genusName + " " + speciesName;
            } else {
                displayName = genusName;
            }
            info.addObservedBioDisplayName(displayName);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return null;
    }

    
    private static String toLower(String s) {
        return (s == null) ? "" : s.toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.isEmpty()) {
            return a;
        }
        if (b != null && !b.isEmpty()) {
            return b;
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // Prediction handling
    // ---------------------------------------------------------------------

    private void updatePredictions(BodyInfo info) {
        ExobiologyData.BodyAttributes attrs = info.buildBodyAttributes();
        if (attrs == null) {
            return; // insufficient data
        }

        List<ExobiologyData.BioCandidate> candidates = ExobiologyData.predict(attrs);
        if (candidates == null || candidates.isEmpty()) {
            info.clearPredictions();
            return;
        }

        // If we know specific observed genera, filter predictions
        if (info.getObservedGenusPrefixes() != null && !info.getObservedGenusPrefixes().isEmpty()) {
            List<ExobiologyData.BioCandidate> filtered =
                    new java.util.ArrayList<>();

            for (ExobiologyData.BioCandidate cand : candidates) {
                String lower = toLower(cand.getDisplayName());
                boolean match = false;
                for (String genusPrefix : info.getObservedGenusPrefixes()) {
                    if (lower.startsWith(genusPrefix + " ")
                            || lower.equals(genusPrefix)) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    filtered.add(cand);
                }
            }

            if (!filtered.isEmpty()) {
                info.setPredictions(filtered);
                return;
            }
        }

        info.setPredictions(candidates);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------
    /**
     * Ensure this body has up-to-date exobiology predictions.
     * Called whenever we either:
     *  - process a detailed Scan, or
     *  - learn that the body has biological signals.
     */
    private boolean isBeltOrRing(String bodyName) {
        if (bodyName == null) {
            return false;
        }
        String n = bodyName.toLowerCase(Locale.ROOT);
        return n.contains("belt cluster")
                || n.contains("ring")
                || n.contains("belt ");
    }

    private String chooseAtmoOrType(ScanEvent e) {
        if (e.getAtmosphere() != null && !e.getAtmosphere().isEmpty()) {
            return e.getAtmosphere();
        }
        if (e.getPlanetClass() != null && !e.getPlanetClass().isEmpty()) {
            return e.getPlanetClass();
        }
        return e.getStarType() != null ? e.getStarType() : "";
    }

    private boolean isHighValue(ScanEvent e) {
        String pc = toLower(e.getPlanetClass());
        String tf = toLower(e.getTerraformState());
        return pc.contains("earth-like")
                || pc.contains("water world")
                || pc.contains("ammonia world")
                || tf.contains("terraformable");
    }

}
