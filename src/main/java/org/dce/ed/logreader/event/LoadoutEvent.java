package org.dce.ed.logreader.event;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

/**
 * Journal event: "Loadout".
 *
 * Parsed very completely, while keeping the raw JsonObject around so we don't
 * lose new fields that Frontier adds.
 */
public class LoadoutEvent extends EliteLogEvent {

    public static final class FuelCapacity {
        private final double main;
        private final double reserve;

        public FuelCapacity(double main, double reserve) {
            this.main = main;
            this.reserve = reserve;
        }

        public double getMain() {
            return main;
        }

        public double getReserve() {
            return reserve;
        }
    }

    public static final class Modifier {
        private final String label;
        private final double value;
        private final double originalValue;
        private final int lessIsGood;
        private final JsonObject raw;

        public Modifier(String label, double value, double originalValue, int lessIsGood, JsonObject raw) {
            this.label = label;
            this.value = value;
            this.originalValue = originalValue;
            this.lessIsGood = lessIsGood;
            this.raw = raw;
        }

        public String getLabel() {
            return label;
        }

        public double getValue() {
            return value;
        }

        public double getOriginalValue() {
            return originalValue;
        }

        public int getLessIsGood() {
            return lessIsGood;
        }

        public JsonObject getRaw() {
            return raw;
        }
    }

    public static final class Engineering {
        private final String engineer;
        private final long engineerId;
        private final long blueprintId;
        private final String blueprintName;
        private final int level;
        private final double quality;
        private final String experimentalEffect;
        private final String experimentalEffectLocalised;
        private final List<Modifier> modifiers;
        private final JsonObject raw;

        public Engineering(String engineer,
                           long engineerId,
                           long blueprintId,
                           String blueprintName,
                           int level,
                           double quality,
                           String experimentalEffect,
                           String experimentalEffectLocalised,
                           List<Modifier> modifiers,
                           JsonObject raw) {
            this.engineer = engineer;
            this.engineerId = engineerId;
            this.blueprintId = blueprintId;
            this.blueprintName = blueprintName;
            this.level = level;
            this.quality = quality;
            this.experimentalEffect = experimentalEffect;
            this.experimentalEffectLocalised = experimentalEffectLocalised;
            this.modifiers = (modifiers == null) ? Collections.emptyList() : Collections.unmodifiableList(modifiers);
            this.raw = raw;
        }

        public String getEngineer() {
            return engineer;
        }

        public long getEngineerId() {
            return engineerId;
        }

        public long getBlueprintId() {
            return blueprintId;
        }

        public String getBlueprintName() {
            return blueprintName;
        }

        public int getLevel() {
            return level;
        }

        public double getQuality() {
            return quality;
        }

        public String getExperimentalEffect() {
            return experimentalEffect;
        }

        public String getExperimentalEffectLocalised() {
            return experimentalEffectLocalised;
        }

        public List<Modifier> getModifiers() {
            return modifiers;
        }

        public JsonObject getRaw() {
            return raw;
        }
    }

    public static final class Module {
        private final String slot;
        private final String item;
        private final boolean on;
        private final int priority;
        private final double health;
        private final long value;

        private final Integer ammoInClip;
        private final Integer ammoInHopper;

        private final Engineering engineering;

        private final JsonObject raw;

        public Module(String slot,
                      String item,
                      boolean on,
                      int priority,
                      double health,
                      long value,
                      Integer ammoInClip,
                      Integer ammoInHopper,
                      Engineering engineering,
                      JsonObject raw) {
            this.slot = slot;
            this.item = item;
            this.on = on;
            this.priority = priority;
            this.health = health;
            this.value = value;
            this.ammoInClip = ammoInClip;
            this.ammoInHopper = ammoInHopper;
            this.engineering = engineering;
            this.raw = raw;
        }

        public String getSlot() {
            return slot;
        }

        public String getItem() {
            return item;
        }

        public boolean isOn() {
            return on;
        }

        public int getPriority() {
            return priority;
        }

        public double getHealth() {
            return health;
        }

        public long getValue() {
            return value;
        }

        public Integer getAmmoInClip() {
            return ammoInClip;
        }

        public Integer getAmmoInHopper() {
            return ammoInHopper;
        }

        public Engineering getEngineering() {
            return engineering;
        }

        public JsonObject getRaw() {
            return raw;
        }
    }

    private final String ship;
    private final int shipId;
    private final String shipName;
    private final String shipIdent;

    private final long hullValue;
    private final long modulesValue;
    private final double hullHealth;
    private final double unladenMass;

    private final int cargoCapacity;
    private final double maxJumpRange;

    private final FuelCapacity fuelCapacity;
    private final long rebuy;

    private final List<Module> modules;

    public LoadoutEvent(Instant timestamp,
                        JsonObject raw,
                        String ship,
                        int shipId,
                        String shipName,
                        String shipIdent,
                        long hullValue,
                        long modulesValue,
                        double hullHealth,
                        double unladenMass,
                        int cargoCapacity,
                        double maxJumpRange,
                        FuelCapacity fuelCapacity,
                        long rebuy,
                        List<Module> modules) {
        super(timestamp, EliteEventType.LOADOUT, raw);
        this.ship = ship;
        this.shipId = shipId;
        this.shipName = shipName;
        this.shipIdent = shipIdent;
        this.hullValue = hullValue;
        this.modulesValue = modulesValue;
        this.hullHealth = hullHealth;
        this.unladenMass = unladenMass;
        this.cargoCapacity = cargoCapacity;
        this.maxJumpRange = maxJumpRange;
        this.fuelCapacity = fuelCapacity;
        this.rebuy = rebuy;
        this.modules = (modules == null) ? Collections.emptyList() : Collections.unmodifiableList(modules);
    }

    public String getShip() {
        return ship;
    }

    public int getShipId() {
        return shipId;
    }

    public String getShipName() {
        return shipName;
    }

    public String getShipIdent() {
        return shipIdent;
    }

    public long getHullValue() {
        return hullValue;
    }

    public long getModulesValue() {
        return modulesValue;
    }

    public double getHullHealth() {
        return hullHealth;
    }

    public double getUnladenMass() {
        return unladenMass;
    }

    public int getCargoCapacity() {
        return cargoCapacity;
    }

    public double getMaxJumpRange() {
        return maxJumpRange;
    }

    public FuelCapacity getFuelCapacity() {
        return fuelCapacity;
    }

    public long getRebuy() {
        return rebuy;
    }

    public List<Module> getModules() {
        return modules;
    }

    @Override
    public String toString() {
        return "LoadoutEvent{ship=" + ship
                + ", shipId=" + shipId
                + ", shipIdent=" + shipIdent
                + ", cargoCapacity=" + cargoCapacity
                + ", modules=" + (modules != null ? modules.size() : 0)
                + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTimestamp(), ship, shipId, shipIdent, cargoCapacity);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LoadoutEvent other)) {
            return false;
        }
        return Objects.equals(getTimestamp(), other.getTimestamp())
                && Objects.equals(ship, other.ship)
                && shipId == other.shipId
                && Objects.equals(shipIdent, other.shipIdent)
                && cargoCapacity == other.cargoCapacity;
    }
}
