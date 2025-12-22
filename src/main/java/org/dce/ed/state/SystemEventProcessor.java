package org.dce.ed.state;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.dce.ed.cache.CachedSystem;
import org.dce.ed.cache.SystemCache;
import org.dce.ed.edsm.BodiesResponse;
import org.dce.ed.edsm.EdsmClient;
import org.dce.ed.exobiology.BodyAttributes;
import org.dce.ed.exobiology.ExobiologyData;
import org.dce.ed.exobiology.ExobiologyData.BioCandidate;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.LiveJournalMonitor;
import org.dce.ed.logreader.event.BioScanPredictionEvent;
import org.dce.ed.logreader.event.FsdJumpEvent;
import org.dce.ed.logreader.event.FssAllBodiesFoundEvent;
import org.dce.ed.logreader.event.FssBodySignalsEvent;
import org.dce.ed.logreader.event.FssDiscoveryScanEvent;
import org.dce.ed.logreader.event.LocationEvent;
import org.dce.ed.logreader.event.SaasignalsFoundEvent;
import org.dce.ed.logreader.event.ScanEvent;
import org.dce.ed.logreader.event.ScanOrganicEvent;

/**
 * Consumes Elite Dangerous journal events and mutates a SystemState.
 *
 * All GUI logic has been removed.
 * This engine may be invoked by Swing, CLI tools, or background processors.
 */
public class SystemEventProcessor {

    private final SystemState state;
    private final SystemCache systemCache;
    private final EdsmClient edsmClient; // optional; may be null
	private String clientKey;

    public SystemEventProcessor(String clientKey, SystemState state) {
        this(clientKey, state, null);
    }

    public SystemEventProcessor(String clientKey, SystemState state, EdsmClient edsmClient) {
    	this.clientKey = clientKey;
        this.state = state;
        this.edsmClient = edsmClient;
        this.systemCache = SystemCache.getInstance();
    }

    /**
     * Entry point: consume any event and update SystemState.
     */
    public void handleEvent(EliteLogEvent event) {
        if (event instanceof LocationEvent) {
            LocationEvent e = (LocationEvent) event;
            enterSystem(e.getStarSystem(), e.getSystemAddress(), e.getStarPos());
            return;
        }

        if (event instanceof FsdJumpEvent) {
            FsdJumpEvent e = (FsdJumpEvent) event;
            enterSystem(e.getStarSystem(), e.getSystemAddress(), e.getStarPos());
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

        if (event instanceof FssAllBodiesFoundEvent) {
            handleFssAllBodiesFound((FssAllBodiesFoundEvent) event);
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

    private void enterSystem(String name, long addr, double[] starPos) {
        boolean sameName = name != null && name.equals(state.getSystemName());
        boolean sameAddr = addr != 0L && addr == state.getSystemAddress();

        if (sameName || sameAddr) {
            if (name != null) {
                state.setSystemName(name);
            }
            if (addr != 0L) {
                state.setSystemAddress(addr);
            }
            if (starPos != null) {
                state.setStarPos(starPos);
            }
            return;
        }

        // New system: clear old one
        state.setSystemName(name);
        state.setSystemAddress(addr);
        state.setStarPos(starPos);

        state.resetBodies();
        state.setTotalBodies(null);
        state.setNonBodyCount(null);
        state.setFssProgress(null);
        state.setAllBodiesFound(null);

        // 1) Load from local cache (fast; gives you a body list even when FSS won't re-fire)
        CachedSystem cs = systemCache.get(addr, name);
        if (cs != null) {
            systemCache.loadInto(state, cs);
        }

        // 2) Enrich from EDSM (best-effort; fills legacy gaps)
        if (edsmClient != null && name != null && !name.isEmpty()) {
            try {
                BodiesResponse edsmBodies = edsmClient.showBodies(name);
                if (edsmBodies != null) {
                    systemCache.mergeBodiesFromEdsm(state, edsmBodies);
                }
            } catch (Exception ex) {
                // best-effort only; never block system state updates
                ex.printStackTrace();
            }
        }
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

        BodyInfo info = getOrCreateBody(e.getBodyId(), e.getBodyName());

        if (info.getStarPos() == null && state.getStarPos() != null) {
            info.setStarPos(state.getStarPos());
        }
        
        info.setBodyId(e.getBodyId());
        info.setBodyName(e.getBodyName());
        info.setStarSystem(e.getStarSystem());

        info.setBodyShortName(state.computeShortName(e.getStarSystem(), e.getBodyName()));

        info.setDistanceLs(e.getDistanceFromArrivalLs());
        info.setLandable(e.isLandable());
        info.setGravityMS(e.getSurfaceGravity());

        Double pPa = e.getSurfacePressure();
        if (pPa != null && !Double.isNaN(pPa)) {
            // Journal SurfacePressure is in Pascals; store in atmospheres for rules/UI
            info.setSurfacePressure(pPa / 101325.0);
        }

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

        state.getBodies().put(e.getBodyId(), info);
        
        for (BodyInfo body : state.getBodies().values()) {
        	updatePredictions(body);
        }
    }

    // ---------------------------------------------------------------------
    // SAASignalsFound (DSS results)
    // ---------------------------------------------------------------------

    private void handleSaaSignals(SaasignalsFoundEvent e) {
    	BodyInfo info = getOrCreateBody(e.getBodyId(), e.getBodyName());

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
            updatePredictions(info);
        }


    }

    // ---------------------------------------------------------------------
    // FSSBodySignalsEvent (FSS version of DSS signals)
    // ---------------------------------------------------------------------

    private void handleFssBodySignals(FssBodySignalsEvent e) {
    	BodyInfo info = getOrCreateBody(e.getBodyId(), e.getBodyName());

        List<SaasignalsFoundEvent.Signal> signals = e.getSignals();
        if (signals != null) {
            for (SaasignalsFoundEvent.Signal s : signals) {
                String type = toLower(s.getType());
                String loc = toLower(s.getTypeLocalised());

                if (type.contains("biological") || loc.contains("biological")) {
                    info.setHasBio(true);
                    info.setNumberOfBioSignals(s.getCount());
                    
                } else if (type.contains("geological") || loc.contains("geological")) {
                    info.setHasGeo(true);
                }
            }

        }
    }

    // ---------------------------------------------------------------------
    // FSSAllBodiesFound – all bodies in system discovered
    // ---------------------------------------------------------------------

    private void handleFssAllBodiesFound(FssAllBodiesFoundEvent e) {
        if (state.getSystemName() == null) {
            state.setSystemName(e.getSystemName());
        }
        if (e.getSystemAddress() != 0L) {
            state.setSystemAddress(e.getSystemAddress());
        }

        if (state.getTotalBodies() == null && e.getBodyCount() > 0) {
            state.setTotalBodies(e.getBodyCount());
        }

        state.setAllBodiesFound(Boolean.TRUE);
    }

    // ---------------------------------------------------------------------
    // ScanOrganic – the most important exobiology event
    // ---------------------------------------------------------------------

    private void handleScanOrganic(ScanOrganicEvent e) {
        if (e.getSystemAddress() != 0L && state.getSystemAddress() == 0L) {
            state.setSystemAddress(e.getSystemAddress());
        }

        BodyInfo info = getOrCreateBody(e.getBodyId(), e.getBodyName());
        info.setHasBio(true);

        CachedSystem system = SystemCache.getInstance().get(e.getSystemAddress(), null);

        String bodyName = e.getBodyName();
        if (bodyName != null && !bodyName.isEmpty()) {
            if (info.getBodyName() == null || info.getBodyName().isEmpty()) {
                info.setBodyName(bodyName);
            }
            if (info.getShortName() == null || info.getShortName().isEmpty()) {
                info.setBodyShortName(state.computeShortName(system.systemName, bodyName));
            }
        }

        String genusName = firstNonBlank(e.getGenusLocalised(), e.getGenus());
        String speciesName = firstNonBlank(e.getSpeciesLocalised(), e.getSpecies());

        if (speciesName.startsWith(genusName + " ")) {
            speciesName = speciesName.replace(genusName,"").trim(); 
        }

        if (genusName != null && !genusName.isEmpty()) {
            info.addObservedGenusPrefix(genusName);

            String displayName;
            if (speciesName != null && !speciesName.isEmpty()) {
                displayName = genusName + " " + speciesName;
            } else {
                displayName = genusName;
            }
            info.addObservedBioDisplayName(displayName);
            info.recordBioSample(displayName, e.getScanType());
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

    // ---------------------------------------------------------------------
    // Prediction handling
    // ---------------------------------------------------------------------

    private void updatePredictions(BodyInfo info) {
        if (!info.hasBio()) {
            return;
        }

        // If DSS/SAASignalsFound gave us observed genus, use it to FILTER existing predictions
        // even if we already predicted earlier.
        if (info.getPredictions() != null && !info.getPredictions().isEmpty()
                && info.getObservedGenusPrefixes() != null && !info.getObservedGenusPrefixes().isEmpty()) {

            List<BioCandidate> filtered = new java.util.ArrayList<>();

            for (BioCandidate cand : info.getPredictions()) {
                String display = toLower(cand.getDisplayName());

                // Take the "genus" as the first token of the prediction display name.
                // e.g. "Bacterium ..." -> "bacterium"
                String predictedGenus = display;
                int idx = display.indexOf(' ');
                if (idx > 0) {
                    predictedGenus = display.substring(0, idx).trim();
                }

                boolean match = false;
                for (String observed : info.getObservedGenusPrefixes()) {
                    String obs = toLower(observed).trim();

                    // Normalize a bit (some events/strings are inconsistent: bacterium/bacteria, stratum/strata)
                    obs = normalizeGenus(obs);
                    String pred = normalizeGenus(predictedGenus);

                    if (pred.equals(obs)) {
                        match = true;
                        break;
                    }
                }

                if (match) {
                    filtered.add(cand);
                }
            }

            // If we found any matches, replace the list with only observed-genus matches.
            // If we found none, fall through and allow fresh prediction computation below.
            if (!filtered.isEmpty()) {
                info.setPredictions(filtered);
                return;
            }
        }

        // If we already have predictions and no observed-genus filtering changed anything, keep them.
        if (info.getPredictions() != null && !info.getPredictions().isEmpty()) {
            return;
        }

        BodyAttributes attrs = info.buildBodyAttributes();
        if (attrs == null) {
            return;
        }

        List<BioCandidate> candidates = ExobiologyData.predict(attrs);
        if (candidates == null || candidates.isEmpty()) {
            info.clearPredictions();
            return;
        }

        // If we have observed genus, filter fresh candidates too.
        if (info.getObservedGenusPrefixes() != null && !info.getObservedGenusPrefixes().isEmpty()) {
            List<BioCandidate> filtered = new java.util.ArrayList<>();

            for (BioCandidate cand : candidates) {
                String display = toLower(cand.getDisplayName());
                String predictedGenus = display;
                int idx = display.indexOf(' ');
                if (idx > 0) {
                    predictedGenus = display.substring(0, idx).trim();
                }

                boolean match = false;
                for (String observed : info.getObservedGenusPrefixes()) {
                    String obs = normalizeGenus(toLower(observed).trim());
                    String pred = normalizeGenus(predictedGenus);

                    if (pred.equals(obs)) {
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

    private static String normalizeGenus(String s) {
        if (s == null) {
            return "";
        }

        String x = s.trim().toLowerCase();

        // Common genus plural/singular inconsistencies you will see in various sources
        if (x.equals("bacteria")) {
            return "bacterium";
        }
        if (x.equals("strata")) {
            return "stratum";
        }

        return x;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

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
        if (e.getStarType() != null) {
            return e.getStarType();
        }
        return "";
    }

    private boolean isHighValue(ScanEvent e) {
        String pc = toLower(e.getPlanetClass());
        String tf = toLower(e.getTerraformState());
        return pc.contains("earth-like")
                || pc.contains("water world")
                || pc.contains("ammonia world")
                || tf.contains("terraformable");
    }
    
    private static int tempBodyKey(String bodyName) {
        if (bodyName == null) {
            return Integer.MIN_VALUE;
        }

        // Force negative so we never collide with real bodyIds (which are >= 0 in practice).
        int h = bodyName.trim().toLowerCase(Locale.ROOT).hashCode();
        return h | 0x80000000;
    }

    private BodyInfo getOrCreateBody(int bodyId, String bodyName) {
        if (bodyId >= 0) {
            // If we previously created a temp entry for this same body name, migrate it now.
            migrateTempBodyIfPresent(bodyId, bodyName);
            return state.getOrCreateBody(bodyId);
        }

        int key = tempBodyKey(bodyName);
        return state.getOrCreateBody(key);
    }

    private void migrateTempBodyIfPresent(int realBodyId, String bodyName) {
        if (bodyName == null || bodyName.isBlank()) {
            return;
        }

        int tmpKey = tempBodyKey(bodyName);

        BodyInfo tmp = state.getBodies().get(tmpKey);
        if (tmp == null) {
            return;
        }

        BodyInfo real = state.getBodies().get(realBodyId);

        if (real == null) {
            // Move temp -> real
            state.getBodies().remove(tmpKey);
            tmp.setBodyId(realBodyId);
            state.getBodies().put(realBodyId, tmp);
            return;
        }

        // Real already exists: merge a few fields then drop temp
        // (Keep it minimal; merge only "additive" fields so we don't overwrite good data.)

        if (real.getBodyName() == null && tmp.getBodyName() != null) {
            real.setBodyName(tmp.getBodyName());
        }
        if (real.getStarSystem() == null && tmp.getStarSystem() != null) {
            real.setStarSystem(tmp.getStarSystem());
        }
        if (Double.isNaN(real.getDistanceLs()) && !Double.isNaN(tmp.getDistanceLs())) {
            real.setDistanceLs(tmp.getDistanceLs());
        }
        if (real.getGravityMS() == null && tmp.getGravityMS() != null) {
            real.setGravityMS(tmp.getGravityMS());
        }
        if (real.getSurfaceTempK() == null && tmp.getSurfaceTempK() != null) {
            real.setSurfaceTempK(tmp.getSurfaceTempK());
        }
        if (real.getSurfacePressure() == null && tmp.getSurfacePressure() != null) {
            real.setSurfacePressure(tmp.getSurfacePressure());
        }

        if (!real.hasBio() && tmp.hasBio()) {
            real.setHasBio(true);
        }
        if (!real.hasGeo() && tmp.hasGeo()) {
            real.setHasGeo(true);
        }
        if (!real.isLandable() && tmp.isLandable()) {
            real.setLandable(true);
        }

        if (real.getObservedGenusPrefixes() == null || real.getObservedGenusPrefixes().isEmpty()) {
            if (tmp.getObservedGenusPrefixes() != null && !tmp.getObservedGenusPrefixes().isEmpty()) {
                real.setObservedGenusPrefixes(new java.util.HashSet<>(tmp.getObservedGenusPrefixes()));
            }
        } else if (tmp.getObservedGenusPrefixes() != null) {
            for (String g : tmp.getObservedGenusPrefixes()) {
                real.addObservedGenus(g);
            }
        }

        if (tmp.getObservedBioDisplayNames() != null) {
            for (String n : tmp.getObservedBioDisplayNames()) {
                real.addObservedBioDisplayName(n);
            }
        }

        // If temp had predictions and real doesn't yet, copy them
        if ((real.getPredictions() == null || real.getPredictions().isEmpty())
                && tmp.getPredictions() != null
                && !tmp.getPredictions().isEmpty()) {
            real.setPredictions(tmp.getPredictions());
        }

        state.getBodies().remove(tmpKey);
    }

}
