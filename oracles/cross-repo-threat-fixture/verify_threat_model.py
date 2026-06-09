#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

import yaml


def _load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def _names_from_related(path: Path) -> set[str]:
    if not path.is_file():
        return set()
    data = _load_json(path)
    return {str(entry.get("name")) for entry in data.get("related", []) if entry.get("name")}


def _register_entries(path: Path) -> dict[str, dict[str, Any]]:
    if not path.is_file():
        return {}
    data = _load_json(path)
    return {
        str(entry.get("name")): entry
        for entry in data.get("entries", [])
        if isinstance(entry, dict) and entry.get("name")
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    parser.add_argument("--yaml", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    oracle_dir = Path(__file__).resolve().parent
    expected = _load_json(oracle_dir / "expected-signals.json")
    failures: list[str] = []

    if not (args.repo / "docs" / "related-repos.yaml").is_file():
        failures.append("consumer repo is missing docs/related-repos.yaml")

    report_text = args.report.read_text(encoding="utf-8") if args.report.is_file() else ""
    for term in expected.get("required_report_terms", []):
        if term not in report_text:
            failures.append(f"report missing term: {term}")

    if not args.yaml.is_file():
        failures.append(f"missing yaml report: {args.yaml}")
    else:
        loaded_yaml = yaml.safe_load(args.yaml.read_text(encoding="utf-8"))
        if not isinstance(loaded_yaml, dict):
            failures.append("threat-model.yaml is not a mapping")

    related_names = _names_from_related(args.output / ".related-repos-loaded.json")
    if not related_names:
        related_names = _names_from_related(args.output / ".related-repos-preflight.json")
    for name in expected.get("required_related_entries", []):
        if name not in related_names:
            failures.append(f"related-repos loader missing entry: {name}")

    register = _register_entries(args.output / ".cross-repo-register.json")
    for name in expected.get("required_register_entries", []):
        if name not in register:
            failures.append(f"cross-repo register missing entry: {name}")

    for name, expected_interface in expected.get("required_register_interfaces", {}).items():
        actual = str(register.get(name, {}).get("interface") or "")
        if expected_interface not in actual:
            failures.append(f"cross-repo register entry {name} missing interface: {expected_interface}")

    payload = {"ok": not failures, "failures": failures}
    if args.json:
        print(json.dumps(payload, indent=2))
    elif failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
    else:
        print("PASS: expected cross-repo signals present")
    return 0 if not failures else 1


if __name__ == "__main__":
    raise SystemExit(main())
