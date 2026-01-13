#!/usr/bin/env python3
import ast
from pathlib import Path

RULESETS_DIR = Path("../../git/EDMC-BioScan/src/bio_scan/bio_data/rulesets")


def load_catalog(text: str):
    """Extract the top-level `catalog` dict from a ruleset .py file."""
    mod = ast.parse(text)
    for node in mod.body:
        if isinstance(node, ast.AnnAssign) and getattr(node.target, "id", None) == "catalog":
            return ast.literal_eval(node.value)
    return {}


def map_body_type(bt: str) -> str:
    """Map ruleset body_type strings -> PlanetType enum constant."""
    mapping = {
        "Rocky body": "PlanetType.ROCKY",
        "High metal content body": "PlanetType.HIGH_METAL",
        "Metal rich body": "PlanetType.METAL_RICH",
        "Rocky ice body": "PlanetType.ROCKY_ICE",
        "Icy body": "PlanetType.ICY",
    }
    return mapping[bt]


def map_atmo(a: str):
    """Map ruleset atmosphere keys -> AtmosphereType enum constant, or None for BioScan 'Any'."""
    mapping = {
        "Any": None,  # handled specially: must have an atmosphere but type does not matter
        "None": "AtmosphereType.NONE",
        "No atmosphere": "AtmosphereType.NONE",

        "CarbonDioxide": "AtmosphereType.CO2",
        "CarbonDioxideRich": "AtmosphereType.CO2_RICH",

        "Methane": "AtmosphereType.METHANE",
        "MethaneRich": "AtmosphereType.METHANE_RICH",

        "Nitrogen": "AtmosphereType.NITROGEN",
        "NitrogenRich": "AtmosphereType.NITROGEN_RICH",

        "Oxygen": "AtmosphereType.OXYGEN",
        "OxygenRich": "AtmosphereType.OXYGEN_RICH",

        "Neon": "AtmosphereType.NEON",
        "NeonRich": "AtmosphereType.NEON_RICH",

        "Argon": "AtmosphereType.ARGON",
        "ArgonRich": "AtmosphereType.ARGON_RICH",

        "Water": "AtmosphereType.WATER",
        "WaterRich": "AtmosphereType.WATER_RICH",

        "SulphurDioxide": "AtmosphereType.SULPHUR_DIOXIDE",
        "SulphurDioxideRich": "AtmosphereType.SULPHUR_DIOXIDE_RICH",

        "Helium": "AtmosphereType.HELIUM",

        "Ammonia": "AtmosphereType.AMMONIA",
        "AmmoniaRich": "AtmosphereType.AMMONIA_RICH",
    }
    if a not in mapping:
        raise KeyError(f"Unknown atmosphere mapping: {a}")
    return mapping[a]


def volcanism_req(volc_value) -> str:
    """Map the 'volcanism' field to VolcanismRequirement.*."""
    if not volc_value:
        return "VolcanismRequirement.ANY"

    if isinstance(volc_value, (list, tuple)):
        vals = [v for v in volc_value if v is not None]
    else:
        vals = [volc_value]

    norm = {str(v) for v in vals}
    if norm == {"None"}:
        return "VolcanismRequirement.NO_VOLCANISM"
    if "None" not in norm:
        return "VolcanismRequirement.VOLCANIC_ONLY"
    return "VolcanismRequirement.ANY"


def java_str(s):
    if s is None:
        return "null"
    s = str(s)
    # escape backslashes first, then quotes
    s = s.replace("\\", "\\\\").replace('"', '\\"')
    return '"' + s + '"'


def java_opt_num(v) -> str:
    """Return a Java numeric literal or 'null' for None."""
    return "null" if v is None else str(float(v))


def load_all_species():
    """
    Load species and their rulesets from all rulesets/*.py.

    Returns dict:
      (genus, species) -> {
          "value": int,
          "rules": [rule_dict, ...]
      }
    """
    species = {}
    for path in sorted(RULESETS_DIR.glob("*.py")):
        text = path.read_text(encoding="utf-8")
        catalog = load_catalog(text)
        if not catalog:
            continue

        for _genus_key, species_dict in catalog.items():
            for _spec_key, spec in species_dict.items():
                name = spec["name"]  # e.g. "Tubus Conifer"
                value = spec["value"]
                genus, *rest = name.split()
                species_name = " ".join(rest)
                key = (genus, species_name)
                species.setdefault(key, {"value": value, "rules": []})
                species[key]["rules"].extend(spec.get("rulesets", []))

    return species


def normalize_star_list(star_field):
    """
    Normalize the 'star' field into a list of strings for Java.
    It can be:
      - ["O"]
      - [("B","IV"), ("B","V")]
      - mix of tuples/lists/strings
    We convert:
      ("B","IV") -> "B IV"
    """
    if not star_field:
        return []
    out = []
    for entry in star_field:
        if isinstance(entry, (list, tuple)):
            if len(entry) == 2:
                out.append(f"{entry[0]} {entry[1]}")
            else:
                out.append(" ".join(str(x) for x in entry))
        else:
            out.append(str(entry))
    return out


def java_list_of_strings(strings):
    """Return a Java expression for List<String> given a Python list."""
    if not strings:
        return "Collections.<String>emptyList()"
    inner = ", ".join(java_str(s) for s in strings)
    return f"Arrays.asList({inner})"


def java_atmosphere_components(ac_dict):
    """
    Convert 'atmosphere_component' dict -> Map<String, Double> expression.
    Rulesets use at most 1 entry, but this handles >1 just in case.
    """
    if not ac_dict:
        return "Collections.<String, Double>emptyMap()"

    items = list(ac_dict.items())
    if len(items) == 1:
        k, v = items[0]
        return f"Collections.singletonMap({java_str(str(k))}, {float(v)})"

    parts = [
        "new LinkedHashMap<String, Double>() {{"
    ]
    for k, v in items:
        parts.append(f"    put({java_str(str(k))}, {float(v)});")
    parts.append("}}")
    return "\n".join(parts)


def main():
    species = load_all_species()

    # Header: full class with package + imports
    print("package org.dce.ed.exobiology;")
    print()
    print("import java.util.*;")
    print()
    print("import org.dce.ed.exobiology.ExobiologyData.AtmosphereType;")
    print("import org.dce.ed.exobiology.ExobiologyData.PlanetType;")
    print("import org.dce.ed.exobiology.ExobiologyData.SpeciesConstraint;")
    print("import org.dce.ed.exobiology.ExobiologyData.SpeciesRule.SpeciesRuleBuilder;")
    print("import org.dce.ed.exobiology.ExobiologyData.VolcanismRequirement;")
    print()
    print("public final class ExobiologyDataConstraints {")
    print()
    print("    private ExobiologyDataConstraints() {")
    print("    }")
    print()
    print("    /**")
    print("     * Generated from rulesets/*.py by generate_exobio_java_snippet.py")
    print("     */")
    print("    public static void initConstraints(Map<String, SpeciesConstraint> CONSTRAINTS) {")
    print("        SpeciesConstraint sc;")
    print()

    for (genus, species_name), data in sorted(species.items()):
        value = data["value"]
        rules = data["rules"]

        print(
            "        sc = new SpeciesConstraint(%s, %s, %d, new ArrayList<>());"
            % (java_str(genus), java_str(species_name), int(value))
        )
        print("        sc.getRules().addAll(Arrays.asList(")

        rule_blocks = []
        for r in rules:
            # --- fail-fast on unsupported keys ---
            supported_keys = {
                "min_gravity", "max_gravity",
                "min_temperature", "max_temperature",
                "min_pressure", "max_pressure",
                "atmosphere",
                "body_type",
                "atmosphere_component",
                "bodies",
                "max_orbital_period",
                "distance",
                "guardian",
                "nebula",
                "parent_star",
                "regions", "region",
                "star",
                "tuber",
                "volcanism",
            }
            extra = set(r.keys()) - supported_keys
            if extra:
                raise ValueError(
                    f"Unsupported rule keys {extra} in species {genus} {species_name}: {r!r}"
                )

            # Scalars with defaults when missing
            min_g = r.get("min_gravity")
            max_g = r.get("max_gravity")
            min_t = r.get("min_temperature")
            max_t = r.get("max_temperature")
            min_p = r.get("min_pressure")
            max_p = r.get("max_pressure")

            # Atmosphere -> AtmosphereType...
            atmo_raw = r.get("atmosphere", []) or []
            if isinstance(atmo_raw, str):
                atmo_list = [atmo_raw]
            else:
                atmo_list = list(atmo_raw)

            require_atmo_any = any(a == "Any" for a in atmo_list)

            atmo_enum_vals = []
            for a in atmo_list:
                mapped = map_atmo(a)
                if mapped is None:
                    continue
                atmo_enum_vals.append(mapped)

            # body_type -> PlanetType...
            body_list = r.get("body_type", []) or []
            body_enum_vals = [map_body_type(b) for b in body_list]

            # atmosphere_component -> Map<String, Double>
            ac_dict = r.get("atmosphere_component") or {}
            ac_expr = java_atmosphere_components(ac_dict)

            # bodies (host bodies names, if any)
            bodies = [str(x) for x in (r.get("bodies") or [])]
            bodies_expr = java_list_of_strings(bodies)

            # max_orbital_period, distance
            max_orbital = r.get("max_orbital_period")
            distance = r.get("distance")

            # guardian (bool)
            guardian = r.get("guardian")

            # nebula (string like 'all')
            nebula = r.get("nebula")

            # parent_star -> List<String>
            parent_star = r.get("parent_star") or []
            parent_star_list = [str(x) for x in parent_star]
            parent_star_expr = java_list_of_strings(parent_star_list)

            # regions (list of strings) + optional single 'region'
            regions = r.get("regions") or []
            single_region = r.get("region")
            if single_region is not None and single_region not in regions:
                regions = list(regions) + [single_region]
            regions_list = [str(x) for x in regions]
            regions_expr = java_list_of_strings(regions_list)

            # star (host star types)
            star_field = r.get("star") or []
            star_list = normalize_star_list(star_field)
            star_expr = java_list_of_strings(star_list)

            # tuber (anchor regions/targets)
            tuber_field = r.get("tuber") or []
            tuber_list = [str(x) for x in tuber_field]
            tuber_expr = java_list_of_strings(tuber_list)

            volc_raw = r.get("volcanism")
            volc_value = None
            volc_list = None
            if isinstance(volc_raw, (list, tuple)):
                volc_list = [str(v) for v in volc_raw if v is not None]
            elif volc_raw is not None and volc_raw != "":
                volc_value = str(volc_raw)

            builder_lines = [
                "            SpeciesRuleBuilder.create()",
            ]

            # Gravity (emit only if constrained)
            if min_g is not None or max_g is not None:
                builder_lines.append(f"                .gravity({java_opt_num(min_g)}, {java_opt_num(max_g)})")

            # Temperature (emit only if constrained)
            if min_t is not None or max_t is not None:
                builder_lines.append(f"                .temperature({java_opt_num(min_t)}, {java_opt_num(max_t)})")

            # Pressure (emit only if constrained)
            if min_p is not None or max_p is not None:
                builder_lines.append(f"                .pressure({java_opt_num(min_p)}, {java_opt_num(max_p)})")

            # Only add atmosphere and planet-type constraints if they actually restrict something.
            if require_atmo_any:
                builder_lines.append("                .requireAtmosphere()")
            if atmo_enum_vals:
                atmo_args = ", ".join(atmo_enum_vals)
                builder_lines.append(f"                .atmospheres({atmo_args})")
            if body_enum_vals:
                body_args = ", ".join(body_enum_vals)
                builder_lines.append(f"                .planetTypes({body_args})")

            # Only add optional fields if present in the rule.
            if ac_dict:
                builder_lines.append("                .atmosphereComponents(%s)" % ac_expr)
            if bodies:
                builder_lines.append("                .bodies(%s)" % bodies_expr)
            if max_orbital is not None:
                builder_lines.append("                .maxOrbitalPeriod(%s)" % float(max_orbital))
            if distance is not None:
                builder_lines.append("                .distance(%s)" % float(distance))
            if guardian is not None:
                guardian_expr = "Boolean.TRUE" if guardian else "Boolean.FALSE"
                builder_lines.append("                .guardian(%s)" % guardian_expr)
            if nebula is not None:
                builder_lines.append("                .nebula(%s)" % java_str(str(nebula)))
            if parent_star_list:
                builder_lines.append("                .parentStars(%s)" % parent_star_expr)
            if regions_list:
                builder_lines.append("                .regions(%s)" % regions_expr)
            if star_list:
                builder_lines.append("                .stars(%s)" % star_expr)
            if tuber_list:
                builder_lines.append("                .tubers(%s)" % tuber_expr)

                        # Volcanism (BioScan semantics)
            if volc_list:
                volc_args = ", ".join(java_str(v) for v in volc_list)
                builder_lines.append(f"                .volcanismAnyOf({volc_args})")
            elif volc_value:
                builder_lines.append(f"                .volcanism({java_str(volc_value)})")

            builder_lines.append("                .build()")
            rule_blocks.append("\n".join(builder_lines))

        print(",\n".join(rule_blocks))
        print("        ));")
        print("        CONSTRAINTS.put(sc.key(), sc);")
        print()

    print("    }")
    print("}")


if __name__ == "__main__":
    main()
