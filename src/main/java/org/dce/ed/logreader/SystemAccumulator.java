package org.dce.ed.logreader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.dce.ed.cache.CachedBody;
import org.dce.ed.logreader.event.FsdJumpEvent;
import org.dce.ed.logreader.event.FssAllBodiesFoundEvent;
import org.dce.ed.logreader.event.FssDiscoveryScanEvent;
import org.dce.ed.logreader.event.LocationEvent;
import org.dce.ed.logreader.event.SaasignalsFoundEvent.Genus;
import org.dce.ed.logreader.event.SaasignalsFoundEvent.Signal;
import org.dce.ed.logreader.event.ScanEvent;
import org.dce.ed.logreader.event.ScanOrganicEvent;
import org.dce.ed.state.BodyInfo;

public class SystemAccumulator {
    String systemName;
    long systemAddress;
    Integer totalBodies;
    Integer nonBodyCount;
    Double fssProgress;
    Boolean allBodiesFound;

    private final Map<Integer, BodyInfo> bodies = new HashMap<>();

    SystemAccumulator(String systemName, long systemAddress) {
        this.systemName = systemName;
        this.systemAddress = systemAddress;
    }

    void applyLocation(LocationEvent e) {
        if (e.getStarSystem() != null) {
            systemName = e.getStarSystem();
        }
        if (e.getSystemAddress() != 0L) {
            systemAddress = e.getSystemAddress();
        }
    }

    void applyFsdJump(FsdJumpEvent e) {
        if (e.getStarSystem() != null) {
            systemName = e.getStarSystem();
        }
        if (e.getSystemAddress() != 0L) {
            systemAddress = e.getSystemAddress();
        }
    }

    void applyFssDiscovery(FssDiscoveryScanEvent e) {
        fssProgress = e.getProgress();
        totalBodies = e.getBodyCount();
        nonBodyCount = e.getNonBodyCount();
        if (e.getSystemName() != null && !e.getSystemName().isEmpty()) {
            systemName = e.getSystemName();
        }
        if (e.getSystemAddress() != 0L) {
            systemAddress = e.getSystemAddress();
        }
    }

    void applyFssAllBodiesFound(FssAllBodiesFoundEvent e) {
        // Prefer system info from the event if present
        if (e.getSystemName() != null && !e.getSystemName().isEmpty()) {
            systemName = e.getSystemName();
        }
        if (e.getSystemAddress() != 0L) {
            systemAddress = e.getSystemAddress();
        }
        if (e.getBodyCount() > 0) {
            totalBodies = e.getBodyCount();
        }
        allBodiesFound = Boolean.TRUE;
    }

    void applyScan(ScanEvent e) {
        String bodyName = e.getBodyName();
        if (isBeltOrRing(bodyName)) {
            return;
        }
        int id = e.getBodyId();
        if (id < 0) {
            return;
        }

        BodyInfo info = getBodies().computeIfAbsent(id, ignored -> new BodyInfo());
        info.setBodyId(id);
        info.setName(bodyName);

        // Distance / landability / gravity
        info.setDistanceLs(e.getDistanceFromArrivalLs());
        info.setLandable(e.isLandable());
        info.setGravityMS(e.getSurfaceGravity());

        // Atmosphere / planet type used by prediction code
        info.setAtmoOrType(chooseAtmoOrType(e));

        // High-value flag for ELW / WW / AW / terraformable HMC etc.
        info.setHighValue(isHighValue(e));

        // Prediction-related attributes
        info.setPlanetClass(e.getPlanetClass());
        info.setAtmosphere(e.getAtmosphere());

        Double temp = e.getSurfaceTemperature();
        if (temp != null) {
            info.setSurfaceTempK(temp);
        }

        String volc = e.getVolcanism();
        if (volc != null && !volc.isEmpty()) {
            info.setVolcanism(volc);
        }
    }

    void applySignals(int bodyId, String bodyName, List<Signal> signals) {
        if (bodyId < 0 || signals == null || signals.isEmpty()) {
            return;
        }
        
        BodyInfo info = getBodies().computeIfAbsent(bodyId, ignored -> new BodyInfo());

        // Make sure the BodyInfo knows its real ID
        if (info.getBodyId() < 0) {
            info.setBodyId(bodyId);
        }
        info.setName(bodyName);
        
        for (Signal s : signals) {
            String type = toLower(s.getType());
            String loc = toLower(s.getTypeLocalised());
            if (type.contains("biological") || loc.contains("biological")) {
                info.setHasBio(true);
            } else if (type.contains("geological") || loc.contains("geological")) {
                info.setHasGeo(true);
            }
        }
    }

    void applyGenuses(int bodyId, String bodyName, List<Genus> genuses) {
        if (bodyId < 0 || genuses == null || genuses.isEmpty()) {
            return;
        }

        BodyInfo info = getBodies().computeIfAbsent(bodyId, ignored -> new BodyInfo());

        // Same here: ensure the ID field isn’t left at -1
        if (info.getBodyId() < 0) {
            info.setBodyId(bodyId);
        }
        info.setName(bodyName);
        
        if (info.getObservedGenusPrefixes() == null) {
            info.setObservedGenusPrefixes(new HashSet<>());
        }
        for (Genus g : genuses) {
            String genusName = toLower(g.getGenusLocalised());
            if (genusName.isEmpty()) {
                genusName = toLower(g.getGenus());
            }
            if (!genusName.isEmpty()) {
                info.getObservedGenusPrefixes().add(genusName);
            }
        }
    }

    void applyScanOrganic(ScanOrganicEvent e) {
        String bodyName = e.getBodyName();
        int bodyId = e.getBodyId();

        BodyInfo info = findOrCreateBodyByIdOrName(bodyId, bodyName);
        if (info == null) {
            return;
        }

        info.setHasBio(true);

        // If the ScanOrganic has a body name and the scan body didn't, fill it in.
        if (bodyName != null && !bodyName.isEmpty()
                && (info.getName() == null || info.getName().isEmpty())) {
            info.setName(bodyName);
        }

        // Genus prefix (for narrowing predictions)
        String genusPrefix = toLower(e.getGenusLocalised());
        if (genusPrefix.isEmpty()) {
            genusPrefix = toLower(e.getGenus());
        }
        if (!genusPrefix.isEmpty()) {
            if (info.getObservedGenusPrefixes() == null) {
                info.setObservedGenusPrefixes(new HashSet<>());
            }
            info.getObservedGenusPrefixes().add(genusPrefix);
        }

        // Full display name (truth row): "Tussock Serrati", "Bacterium Nebulus", etc.
        String genusDisp = firstNonEmpty(e.getGenusLocalised(), e.getGenus());
        String speciesDisp = firstNonEmpty(e.getSpeciesLocalised(), e.getSpecies());
        String displayName = buildDisplayName(genusDisp, speciesDisp);

        if (displayName != null && !displayName.isEmpty()) {
            if (info.getObservedBioDisplayNames() == null) {
                info.setObservedBioDisplayNames(new HashSet<>());
            }
            info.getObservedBioDisplayNames().add(displayName);
        }
    }

    private BodyInfo findOrCreateBodyByIdOrName(int bodyId, String bodyName) {
        if (bodyId > 0) {
            BodyInfo existing = getBodies().get(bodyId);
            if (existing != null) {
                return existing;
            }
            BodyInfo info = new BodyInfo();
            info.setBodyId(bodyId);
            if (bodyName != null && !bodyName.isEmpty()) {
                info.setName(bodyName);
            }
            getBodies().put(bodyId, info);
            return info;
        }

        String name = bodyName != null ? bodyName.trim() : "";
        if (!name.isEmpty()) {
            for (BodyInfo b : getBodies().values()) {
                if (name.equals(b.getName())) {
                    return b;
                }
            }

            // >>> DEBUG: we are about to create a synthetic -1 body
            System.out.println(
                "[DEBUG] Creating synthetic bodyId=-1 for ScanOrganic in system '" + systemName
                + "' (" + systemAddress + ")"
                + " bodyId=" + bodyId
                + " bodyName='" + bodyName + "' "
                + debugDescribeBodies()
            );

            BodyInfo info = new BodyInfo();
            info.setBodyId(-1);
            info.setName(bodyName);
            getBodies().put(getBodies().size() + 1000000, info);
            return info;
        }

        return null;
    }

    private static String chooseAtmoOrType(ScanEvent e) {
        String atmo = e.getAtmosphere();
        if (atmo != null && !atmo.isEmpty()) {
            return atmo;
        }
        String pc = e.getPlanetClass();
        if (pc != null && !pc.isEmpty()) {
            return pc;
        }
        String star = e.getStarType();
        return star != null ? star : "";
    }

    private static boolean isHighValue(ScanEvent e) {
        String pc = toLower(e.getPlanetClass());
        String tf = toLower(e.getTerraformState());
        if (pc.contains("earth-like")) {
            return true;
        }
        if (pc.contains("ammonia world")) {
            return true;
        }
        if (pc.contains("water world")) {
            return true;
        }
        if (tf.contains("terraformable")) {
            return true;
        }
        return false;
    }

    private static boolean isBeltOrRing(String bodyName) {
        if (bodyName == null) {
            return false;
        }
        String lower = bodyName.toLowerCase(Locale.ROOT);
        return lower.contains("belt cluster")
                || lower.contains("belt ")
                || lower.contains(" ring");
    }

    private static String toLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static String firstNonEmpty(String a, String b) {
        if (!isEmpty(a)) {
            return a;
        }
        if (!isEmpty(b)) {
            return b;
        }
        return null;
    }

    private static String buildDisplayName(String genusDisp, String speciesDisp) {
        if ((genusDisp == null || genusDisp.isEmpty())
                && (speciesDisp == null || speciesDisp.isEmpty())) {
            return null;
        }
        if (genusDisp == null || genusDisp.isEmpty()) {
            return speciesDisp;
        }
        if (speciesDisp == null || speciesDisp.isEmpty()) {
            return genusDisp;
        }
        return genusDisp + " " + speciesDisp;
    }

    List<CachedBody> toCachedBodies() {
    	
    	if (systemName.equals("Antal"))
    		System.out.println("Found it");
        List<CachedBody> list = new ArrayList<>();
        for (BodyInfo b : getBodies().values()) {
            if (b.getBodyId() >= 0
                    && b.getDistanceLs() == Double.NaN
                    && !b.isLandable()
                    && b.getPlanetClass() == null
                    && b.getAtmoOrType() == null
                    && (b.isHasBio() || b.isHasGeo())) {
                System.out.println(
                    "[DEBUG] Partial body in " + systemName
                    + " addr=" + systemAddress
                    + " bodyId=" + b.getBodyId()
                    + " name='" + b.getName() + "'"
                    + " hasBio=" + b.isHasBio()
                    + " hasGeo=" + b.isHasGeo()
                );
            }
            CachedBody cb = new CachedBody();
            cb.name = b.getName();
            cb.bodyId = b.getBodyId();
            cb.distanceLs = b.getDistanceLs();
            cb.gravityMS = b.getGravityMS();
            cb.landable = b.isLandable();
            cb.hasBio = b.isHasBio();
            cb.hasGeo = b.isHasGeo();
            cb.highValue = b.isHighValue();

            cb.planetClass = b.getPlanetClass();
            cb.atmosphere = b.getAtmosphere();
            cb.atmoOrType = b.getAtmoOrType();
            cb.surfaceTempK = b.getSurfaceTempK();
            cb.volcanism = b.getVolcanism();
            cb.discoveryCommander = b.getDiscoveryCommander();

            if (b.getObservedGenusPrefixes() != null
                    && !b.getObservedGenusPrefixes().isEmpty()) {
                cb.observedGenusPrefixes = new HashSet<>(b.getObservedGenusPrefixes());
            }

            if (b.getObservedBioDisplayNames() != null
                    && !b.getObservedBioDisplayNames().isEmpty()) {
                cb.observedBioDisplayNames = new HashSet<>(b.getObservedBioDisplayNames());
            }

            list.add(cb);
        }
        return list;
    }

	public boolean bodiesIsEmpty() {
		return getBodies().isEmpty();
	}

	private Map<Integer, BodyInfo> getBodies() {
		return bodies;
	}
	
	private String debugDescribeBodies() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("bodies={");
	    boolean first = true;
	    for (Map.Entry<Integer, BodyInfo> e : getBodies().entrySet()) {
	        if (!first) {
	            sb.append(", ");
	        }
	        first = false;
	        BodyInfo b = e.getValue();
	        sb.append("key=").append(e.getKey())
	          .append(" id=").append(b.getBodyId())
	          .append(" name='").append(b.getName()).append("'");
	    }
	    sb.append("}");
	    return sb.toString();
	}

}