#!/usr/bin/env python3
import argparse
import json
import re
import sys
from pathlib import Path


def load_text(paths):
    parts = []
    for path in paths:
        if not path:
            continue
        p = Path(path)
        if not p.exists():
            raise SystemExit(f"missing input: {p}")
        parts.append(p.read_text(encoding="utf-8", errors="replace"))
    return "\n".join(parts)


def pattern_found(pattern, text):
    return re.search(pattern, text, flags=re.IGNORECASE | re.MULTILINE) is not None


def evaluate_signal(signal, text):
    missing_groups = []
    for group in signal["groups"]:
        if not any(pattern_found(pattern, text) for pattern in group):
            missing_groups.append(group)
    return missing_groups


def check_oracle_separation(repo):
    if not repo:
        return []
    repo_path = Path(repo).resolve()
    issues = []
    forbidden = [
        repo_path / "docs" / "threat-model-expectations.md",
        repo_path / "oracle",
        repo_path / "expected-signals.json",
    ]
    for path in forbidden:
        if path.exists():
            issues.append(f"oracle material is inside scan repo: {path}")
    return issues


def main():
    parser = argparse.ArgumentParser(description="Verify a generated threat model against the hidden fixture oracle.")
    parser.add_argument("--repo", help="Scanned repository path; used to verify the oracle is not inside the scan target.")
    parser.add_argument("--report", required=True, help="Path to generated threat-model.md")
    parser.add_argument("--yaml", help="Optional path to generated threat-model.yaml")
    parser.add_argument("--manifest", default=str(Path(__file__).with_name("expected-signals.json")))
    parser.add_argument("--json", action="store_true", help="Emit machine-readable JSON.")
    args = parser.parse_args()

    manifest = json.loads(Path(args.manifest).read_text(encoding="utf-8"))
    report_text = load_text([args.report, args.yaml])
    separation_issues = check_oracle_separation(args.repo)

    results = []
    for signal in manifest["signals"]:
        missing_groups = evaluate_signal(signal, report_text)
        results.append({
            "id": signal["id"],
            "title": signal["title"],
            "category": signal.get("category", "finding"),
            "priority": signal["priority"],
            "passed": not missing_groups,
            "missing_groups": missing_groups,
        })

    passed = [item for item in results if item["passed"]]
    failed = [item for item in results if not item["passed"]]
    status = {
        "fixture": manifest["fixture"],
        "passed": len(passed),
        "failed": len(failed),
        "total": len(results),
        "oracle_separation_issues": separation_issues,
        "results": results,
    }

    if args.json:
        print(json.dumps(status, indent=2))
    else:
        print(f"Fixture: {status['fixture']}")
        print(f"Signals: {len(passed)}/{len(results)} passed")
        if separation_issues:
            print("\nOracle separation issues:")
            for issue in separation_issues:
                print(f"  - {issue}")
        if failed:
            print("\nMissing or weakly evidenced signals:")
            for item in failed:
                print(f"  - {item['id']} {item['title']} [{item['priority']}, {item['category']}]")
                for group in item["missing_groups"]:
                    print(f"    missing one of: {', '.join(group)}")

    if failed or separation_issues:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
