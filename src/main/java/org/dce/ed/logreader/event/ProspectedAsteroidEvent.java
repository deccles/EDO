package org.dce.ed.logreader.event;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

/**
 * Journal event: ProspectedAsteroid
 */
public class ProspectedAsteroidEvent extends EliteLogEvent {

    public static final class MaterialProportion {

        private final String name;
        private final double proportion;

        public MaterialProportion(String name, double proportion) {
            this.name = name;
            this.proportion = proportion;
        }

        public String getName() {
            return name;
        }

        /**
         * Percent (0-100)
         */
        public double getProportion() {
            return proportion;
        }
    }

    private final List<MaterialProportion> materials;
    private final String motherlodeMaterial;

    public ProspectedAsteroidEvent(Instant timestamp,
                                  JsonObject rawJson,
                                  List<MaterialProportion> materials,
                                  String motherlodeMaterial) {
        super(timestamp, EliteEventType.PROSPECTED_ASTEROID, rawJson);
        this.materials = (materials == null) ? List.of() : List.copyOf(materials);
        this.motherlodeMaterial = motherlodeMaterial;
    }

    public List<MaterialProportion> getMaterials() {
        return Collections.unmodifiableList(materials);
    }

    public String getMotherlodeMaterial() {
        return motherlodeMaterial;
    }
}
