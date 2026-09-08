#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


class ProgressionValidationError(RuntimeError):
    pass


@dataclass(frozen=True)
class Node:
    node_id: str
    dimension: str
    alternatives: tuple[tuple[str, ...], ...]
    optional: bool


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise ProgressionValidationError(message)


def load_graph(path: Path) -> tuple[dict[str, Node], tuple[str, ...], frozenset[str]]:
    raw = json.loads(path.read_text(encoding="utf-8"))
    _require(raw.get("schema") == 1, "unsupported progression schema; expected schema=1")

    forbidden_raw = raw.get("forbidden_required_dimensions")
    _require(isinstance(forbidden_raw, list) and forbidden_raw, "forbidden_required_dimensions must be a non-empty list")
    forbidden = frozenset(_non_blank_strings(forbidden_raw, "forbidden_required_dimensions"))

    milestones_raw = raw.get("main_milestones")
    _require(isinstance(milestones_raw, list) and milestones_raw, "main_milestones must be a non-empty list")
    milestones = tuple(_non_blank_strings(milestones_raw, "main_milestones"))
    _require(len(set(milestones)) == len(milestones), "main_milestones contains duplicates")

    nodes_raw = raw.get("nodes")
    _require(isinstance(nodes_raw, list) and nodes_raw, "nodes must be a non-empty list")

    nodes: dict[str, Node] = {}
    for index, entry in enumerate(nodes_raw):
        _require(isinstance(entry, dict), f"nodes[{index}] must be an object")
        node_id = entry.get("id")
        dimension = entry.get("dimension")
        _require(isinstance(node_id, str) and node_id.strip(), f"nodes[{index}].id must be non-blank")
        _require(node_id not in nodes, f"duplicate progression node id: {node_id}")
        _require(isinstance(dimension, str) and dimension.strip(), f"{node_id}.dimension must be non-blank")
        requires_any = entry.get("requires_any")
        _require(isinstance(requires_any, list) and requires_any, f"{node_id}.requires_any must contain at least one alternative")

        alternatives: list[tuple[str, ...]] = []
        for alt_index, alternative in enumerate(requires_any):
            _require(isinstance(alternative, list), f"{node_id}.requires_any[{alt_index}] must be a list")
            refs = tuple(_non_blank_strings(alternative, f"{node_id}.requires_any[{alt_index}]"))
            _require(len(set(refs)) == len(refs), f"{node_id}.requires_any[{alt_index}] contains duplicate dependencies")
            alternatives.append(refs)

        optional = entry.get("optional", False)
        _require(isinstance(optional, bool), f"{node_id}.optional must be boolean")
        nodes[node_id] = Node(node_id, dimension, tuple(alternatives), optional)

    for milestone in milestones:
        _require(milestone in nodes, f"main milestone references unknown node: {milestone}")
        _require(not nodes[milestone].optional, f"main milestone cannot be optional: {milestone}")

    for node in nodes.values():
        for alternative in node.alternatives:
            for dependency in alternative:
                _require(dependency in nodes, f"{node.node_id} references unknown dependency: {dependency}")

    _validate_acyclic(nodes)
    return nodes, milestones, forbidden


def _non_blank_strings(values: Iterable[object], field: str) -> list[str]:
    result: list[str] = []
    for index, value in enumerate(values):
        _require(isinstance(value, str) and value.strip(), f"{field}[{index}] must be a non-blank string")
        result.append(value)
    return result


def _validate_acyclic(nodes: dict[str, Node]) -> None:
    visiting: list[str] = []
    visiting_set: set[str] = set()
    visited: set[str] = set()

    def dfs(node_id: str) -> None:
        if node_id in visited:
            return
        if node_id in visiting_set:
            start = visiting.index(node_id)
            cycle = visiting[start:] + [node_id]
            raise ProgressionValidationError("progression dependency cycle: " + " -> ".join(cycle))
        visiting.append(node_id)
        visiting_set.add(node_id)
        for alternative in nodes[node_id].alternatives:
            for dependency in alternative:
                dfs(dependency)
        visiting.pop()
        visiting_set.remove(node_id)
        visited.add(node_id)

    for node_id in nodes:
        dfs(node_id)


def find_clean_route(
    node_id: str,
    nodes: dict[str, Node],
    forbidden: frozenset[str],
    memo: dict[str, tuple[bool, tuple[str, ...]]],
) -> tuple[bool, tuple[str, ...]]:
    if node_id in memo:
        return memo[node_id]

    node = nodes[node_id]
    if node.dimension in forbidden:
        result = (False, (f"{node_id}@{node.dimension}",))
        memo[node_id] = result
        return result

    failed_traces: list[tuple[str, ...]] = []
    for alternative in node.alternatives:
        alternative_ok = True
        alternative_trace: tuple[str, ...] = ()
        for dependency in alternative:
            clean, trace = find_clean_route(dependency, nodes, forbidden, memo)
            if not clean:
                alternative_ok = False
                alternative_trace = (node_id,) + trace
                break
        if alternative_ok:
            result = (True, (node_id,))
            memo[node_id] = result
            return result
        failed_traces.append(alternative_trace)

    best_trace = min(failed_traces, key=len) if failed_traces else (node_id,)
    result = (False, best_trace)
    memo[node_id] = result
    return result


def validate_main_path(path: Path) -> None:
    nodes, milestones, forbidden = load_graph(path)
    failures: list[str] = []
    memo: dict[str, tuple[bool, tuple[str, ...]]] = {}

    for milestone in milestones:
        clean, trace = find_clean_route(milestone, nodes, forbidden, memo)
        if not clean:
            failures.append(f"{milestone}: " + " -> ".join(trace))

    if failures:
        joined = "\n  - ".join(failures)
        raise ProgressionValidationError(
            "main progression has no Nether/End-independent route for:\n  - " + joined
        )

    print(
        "PROGRESSION VALIDATION OK: "
        f"{len(milestones)} main milestones retain a route independent of "
        + ", ".join(sorted(forbidden))
    )


def main() -> None:
    project = Path(__file__).resolve().parents[1]
    graph = Path(sys.argv[1]) if len(sys.argv) > 1 else project / "src/main/resources/data/earth_to_stars/progression/main_path.json"
    try:
        validate_main_path(graph)
    except (OSError, json.JSONDecodeError, ProgressionValidationError) as exc:
        raise SystemExit(f"PROGRESSION VALIDATION FAILED: {exc}") from exc


if __name__ == "__main__":
    main()
