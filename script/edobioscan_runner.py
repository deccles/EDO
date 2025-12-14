#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import re
import sys
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Set


def _normalize_windows_path(p: str) -> str:
    # Git Bash often passes Windows paths; abspath() will break 'C:\\...' into '/cwd/C:\\...'.
    p = (p or "").strip().strip('"').strip("'")
    if not p:
        return p

    # If it looks like a Windows drive path, do NOT abspath it.
    # Examples: C:\\Users\\... or C:/Users/...
    if re.match(r"^[A-Za-z]:[\\/]", p):
        return p

    return os.path.abspath(p)


# ---- Minimal PlanetData / StarData stand-ins (used by the runner payload) ----

@dataclass
class SimpleStarData:
    name: str
    star_type: str = ""
    luminosity: str = ""
    distance_ls: float = 0.0

    def get_name(self) -> str:
        return self.name

    def get_type(self) -> str:
        return self.star_type

    def get_luminosity(self) -> str:
        return self.luminosity

    def get_distance(self) -> float:
        return self.distance_ls



@dataclass
class SimplePlanetData:
    name: str
    body_type: str = ""
    landable: bool = False
    distance_ls: Optional[float] = None
    gravity: Optional[float] = None           # m/s^2
    temp_k: Optional[float] = None            # Kelvin
    pressure_pa: Optional[float] = None       # Pascals
    atmosphere: str = ""
    atmosphere_comp: Dict[str, float] = field(default_factory=dict)  # gas -> percent
    volcanism: str = ""
    materials: Set[str] = field(default_factory=set)  # element names
    parent_stars: List[str] = field(default_factory=list)
    orbital_period: Optional[float] = None

    def get_name(self) -> str:
        return self.name

    def get_type(self) -> str:
        return self.body_type

    def is_landable(self) -> bool:
        return bool(self.landable)

    def get_distance(self) -> Optional[float]:
        return self.distance_ls

    def get_gravity(self) -> float:
        return float(self.gravity or 0.0)

    def get_temp(self) -> Optional[float]:
        return self.temp_k

    def get_pressure(self) -> Optional[float]:
        return self.pressure_pa

    def get_atmosphere(self) -> str:
        return self.atmosphere or ""

    def get_gas(self, gas: str) -> float:
        return float(self.atmosphere_comp.get(gas, 0.0))

    def get_volcanism(self) -> str:
        return self.volcanism or ""

    def get_materials(self) -> Set[str]:
        return set(self.materials)

    def get_parent_stars(self) -> List[str]:
        return list(self.parent_stars)

    def get_orbital_period(self) -> Optional[float]:
        return self.orbital_period


@dataclass
class SimpleSystem:
    name: str
    x: Optional[float] = None
    y: Optional[float] = None
    z: Optional[float] = None
    region: Optional[int] = None


@dataclass
class SimpleCommander:
    id: int = 0


class _This:
    """
    Mimics the plugin's global `this` object used throughout load.py.
    We only populate the fields BioScan rules commonly read.
    """
    def __init__(self) -> None:
        self.system: Optional[SimpleSystem] = None
        self.stars: Dict[str, SimpleStarData] = {}
        self.planets: Dict[str, SimplePlanetData] = {}
        self.planet_cache: Dict[str, Dict[str, Any]] = {}
        self.main_star_type: str = ""
        self.main_star_luminosity: str = ""
        self.commander: SimpleCommander = SimpleCommander(0)
        self.sql_session = None


# ---- Helpers ----

_CODEX_SYMBOLS = re.compile(r"[\U0001F30C\U0001F4DD]")  # 🌌 📝


def _strip_codex_symbols(text: str) -> str:
    return _CODEX_SYMBOLS.sub("", text).strip()


def _require(obj: Dict[str, Any], key: str, where: str) -> Any:
    if key not in obj:
        raise ValueError(f"Missing required key '{key}' in {where}")
    return obj[key]


def _to_set(value: Any) -> Set[str]:
    if value is None:
        return set()
    if isinstance(value, list):
        return set(str(x) for x in value)
    if isinstance(value, set):
        return set(value)
    if isinstance(value, str):
        return set(x.strip() for x in value.split(",") if x.strip())
    raise ValueError(f"Unsupported materials type: {type(value)}")


def _build_context(payload: Dict[str, Any]) -> _This:
    ctx = _This()

    sys_obj = _require(payload, "system", "root")
    system = SimpleSystem(
        name=_require(sys_obj, "name", "system"),
        x=sys_obj.get("x"),
        y=sys_obj.get("y"),
        z=sys_obj.get("z"),
        region=sys_obj.get("region"),
    )
    ctx.system = system

    main_star = sys_obj.get("mainStar") or {}
    ctx.main_star_type = (main_star.get("type") or "").strip()
    ctx.main_star_luminosity = (main_star.get("luminosity") or "").strip()

    for s in (sys_obj.get("stars") or []):
        star = SimpleStarData(
            name=_require(s, "name", "system.stars[]"),
            star_type=(s.get("type") or "").strip(),
            luminosity=(s.get("luminosity") or "").strip(),
            distance_ls=float(s.get("distanceLs", 0.0) or 0.0),
        )
        ctx.stars[star.name] = star

    bodies = _require(payload, "bodies", "root")
    if not isinstance(bodies, list):
        raise ValueError("root.bodies must be a list")

    for b in bodies:
        body = SimplePlanetData(
            name=_require(b, "name", "bodies[]"),
            body_type=(b.get("type") or "").strip(),
            landable=bool(b.get("landable", False)),
            distance_ls=b.get("distanceLs"),
            gravity=b.get("gravity"),
            temp_k=b.get("tempK"),
            pressure_pa=b.get("pressurePa"),
            atmosphere=(b.get("atmosphere") or "").strip(),
            atmosphere_comp=dict(b.get("atmosphereComposition") or {}),
            volcanism=(b.get("volcanism") or "").strip(),
            materials=_to_set(b.get("materials")),
            parent_stars=list(b.get("parentStars") or []),
            orbital_period=b.get("orbitalPeriod"),
        )
        ctx.planets[body.name] = body
        ctx.planet_cache[body.name] = {}

    return ctx


def _install_context_into_load(load_mod: Any, ctx: _This) -> None:
    # BioScan load.py uses a global `this` object
    load_mod.this = ctx

    # BioScan decorates names with codex markers using DB lookups;
    # for standalone validation, force "already in codex" so it doesn't try DB.
    def _always_true(*_a: Any, **_kw: Any) -> bool:
        return True

    if hasattr(load_mod, "check_codex"):
        load_mod.check_codex = _always_true
    if hasattr(load_mod, "check_codex_from_name"):
        load_mod.check_codex_from_name = _always_true


def _predict(load_mod: Any, ctx: _This) -> List[Dict[str, Any]]:
    results: List[Dict[str, Any]] = []

    for body_name, body in ctx.planets.items():
        try:
            possible = load_mod.get_possible_values(body)
        except Exception as e:
            results.append({
                "body": body_name,
                "error": f"{type(e).__name__}: {e}",
                "bioscan": []
            })
            continue

        # possible = [(display_name, (min,max,set_of_species)), ...]
        names = [_strip_codex_symbols(item[0]) for item in possible if item and item[0]]

        seen: Set[str] = set()
        bioscan: List[str] = []
        for n in names:
            if n and n not in seen:
                seen.add(n)
                bioscan.append(n)

        results.append({
            "body": body_name,
            "bioscan": bioscan
        })

    return results


def main(argv: List[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("input_json", help="Input JSON file (system + bodies)")
    ap.add_argument("--bioscan-root", required=True,
                    help="Path to BioScan plugin checkout src folder (contains load.py and bio_scan/)")
    ap.add_argument("--edmc-root",
                    default=r"C:\Program Files (x86)\EDMarketConnector",
                    help="Path to installed EDMarketConnector folder (must contain library.zip)")
    ap.add_argument("--pretty", action="store_true", help="Pretty-print output JSON")
    args = ap.parse_args(argv)

    bioscan_root = _normalize_windows_path(args.bioscan_root)


    if not os.path.isdir(bioscan_root):
        raise ValueError(f"--bioscan-root is not a directory: {bioscan_root}")

    load_py = os.path.join(bioscan_root, "load.py")
    if not os.path.isfile(load_py):
        raise ValueError(f"Could not find load.py at: {load_py}")

    edmc_root = _normalize_windows_path(args.edmc_root)
    edmc_zip = os.path.join(edmc_root, "library.zip")
    if not os.path.isdir(edmc_root):
        raise ValueError(f"--edmc-root is not a directory: {edmc_root}")
    if not os.path.isfile(edmc_zip):
        raise ValueError(f"Could not find EDMC library.zip at: {edmc_zip}")

    # IMPORTANT: EDMC runtime first (provides monitor/myNotebook/edmc_data/I10n/etc.)
    if edmc_zip not in sys.path:
        sys.path.insert(0, edmc_zip)
    if edmc_root not in sys.path:
        sys.path.insert(0, edmc_root)

    # Then BioScan checkout
    if bioscan_root not in sys.path:
        sys.path.insert(1, bioscan_root)

    # Force I10n + i10n to be EDMC's implementation (prevents site-packages hijack)
    import I10n as _edmc_I10n  # type: ignore
    sys.modules["I10n"] = _edmc_I10n
    sys.modules["i10n"] = _edmc_I10n

    with open(args.input_json, "r", encoding="utf-8") as f:
        payload = json.load(f)

    import load as load_mod  # type: ignore

    ctx = _build_context(payload)
    _install_context_into_load(load_mod, ctx)

    out = {
        "system": ctx.system.name if ctx.system else "",
        "results": _predict(load_mod, ctx),
    }

    if args.pretty:
        print(json.dumps(out, indent=2, ensure_ascii=False))
    else:
        print(json.dumps(out, ensure_ascii=False))

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
