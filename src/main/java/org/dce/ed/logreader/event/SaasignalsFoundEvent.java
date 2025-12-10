package org.dce.ed.logreader.event;

import java.time.Instant;
import java.util.List;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

public class SaasignalsFoundEvent extends EliteLogEvent {
    private final String bodyName;
    private final long systemAddress;
    private final int bodyId;
    private final List<Signal> signals;
    private final List<Genus> genuses;

    public SaasignalsFoundEvent(Instant timestamp,
                                JsonObject rawJson,
                                String bodyName,
                                long systemAddress,
                                int bodyId,
                                List<Signal> signals,
                                List<Genus> genuses) {
        super(timestamp, EliteEventType.SAASIGNALS_FOUND, rawJson);
        this.bodyName = bodyName;
        this.systemAddress = systemAddress;
        this.bodyId = bodyId;
        this.signals = signals;
        this.genuses = genuses;
    }

    public String getBodyName() {
        return bodyName;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public int getBodyId() {
        return bodyId;
    }

    public List<Signal> getSignals() {
        return signals;
    }

    public List<Genus> getGenuses() {
        return genuses;
    }

    public static final class Signal {
        private final String type;
        private final String typeLocalised;
        private final int count;

        public Signal(String type, String typeLocalised, int count) {
            this.type = type;
            this.typeLocalised = typeLocalised;
            this.count = count;
        }

        public String getType() {
            return type;
        }

        public String getTypeLocalised() {
            return typeLocalised;
        }

        public int getCount() {
            return count;
        }
    }

    public static final class Genus {
        private final String genus;
        private final String genusLocalised;

        public Genus(String genus, String genusLocalised) {
            this.genus = genus;
            this.genusLocalised = genusLocalised;
        }

        public String getGenus() {
            return genus;
        }

        public String getGenusLocalised() {
            return genusLocalised;
        }
    }
}
