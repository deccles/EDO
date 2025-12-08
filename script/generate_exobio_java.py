#!/usr/bin/env python3
import ast
from pathlib import Path

RULESETS_DIR = Path("rulesets")


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


def map_atmo(a: str) -> str:
    """Map ruleset atmosphere keys -> AtmosphereType enum constant."""
    mapping = {
        "CarbonDioxide": "AtmosphereType.CO2",
        "CarbonDioxideRich": "AtmosphereType.CO2",
        "Methane": "AtmosphereType.METHANE",
        "MethaneRich": "AtmosphereType.METHANE",
        "Nitrogen": "AtmosphereType.NITROGEN",
        "Oxygen": "AtmosphereType.OXYGEN",
        "Neon": "AtmosphereType.NEON",
        "NeonRich": "AtmosphereType.NEON",
        "Argon": "AtmosphereType.ARGON",
        "ArgonRich": "AtmosphereType.ARGON",
        "Water": "AtmosphereType.WATER",
        "WaterRich": "AtmosphereType.WATER",
        "SulphurDioxide": "AtmosphereType.SULPHUR_DIOXIDE",
        "Helium": "AtmosphereType.HELIUM",
        "Ammonia": "AtmosphereType.AMMONIA",
        "None": "AtmosphereType.NONE",
        "No atmosphere": "AtmosphereType.NONE",
    }
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


def java_str(s: str) -> str:
    """Quote and escape a Java string literal."""
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


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
    print("import org.dce.ed.exobiology.ExobiologyData.SpeciesRuleBuilder;")
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
            min_g = float(r.get("min_gravity", 0.0))
            max_g = float(r.get("max_gravity", 100.0))
            min_t = float(r.get("min_temperature", 0.0))
            max_t = float(r.get("max_temperature", 1_000_000.0))
            min_p = float(r.get("min_pressure", 0.0))
            max_p = float(r.get("max_pressure", 1_000_000.0))

            # Atmosphere -> AtmosphereType...
            atmo_list = r.get("atmosphere", []) or []
            atmo_enum_vals = [map_atmo(a) for a in atmo_list]

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

            volc_req = volcanism_req(r.get("volcanism"))

            builder_lines = [
                "            SpeciesRuleBuilder.create()",
                "                .gravity(%s, %s)" % (min_g, max_g),
                "                .temperature(%s, %s)" % (min_t, max_t),
                "                .pressure(%s, %s)" % (min_p, max_p),
            ]

            # Only add atmosphere and planet-type constraints if they actually restrict something.
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

            # Volcanism: only emit if not the default ANY
            if volc_req != "VolcanismRequirement.ANY":
                builder_lines.append("                .volcanism(%s)" % volc_req)

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
