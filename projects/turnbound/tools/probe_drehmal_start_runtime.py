#!/usr/bin/env python3
"""1.20.1 Fabric dedicated-server smoke on disposable, verified Drehmal world copies.

Two isolated runs (six-stage vs seven-stage) capture only derived block-state
metadata. No source map, NBT Command strings, server log or UUID is uploaded.
This is NOT player interaction, client playtesting or mechanism functionality proof.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import secrets
import shutil
import signal
import socket
import struct
import subprocess
import sys
import time
from pathlib import Path

import audit_drehmal_starter_mechanisms as physical
import prepare_drehmal_isolated_copy as six
import verify_drehmal_start_chain_isolation as seven
from scan_drehmal_command_blocks import scan_region

RUN_MARKER = ".turnbound-disposable-ci-runtime"
ROOTS = [(x, 214, -288) for x in seven.TOP] + [(x, 214, -260) for x in seven.BOTTOM]
BOUNDARY_WAIT_SECONDS = 260
PORT = 25581


def chain_positions():
    roles = seven.coordinate_roles()
    positions = set(roles)
    positions.update((x, y, -288) for x in seven.TOP for y in (212, 213, 214))
    positions.update((x, y, -260) for x in seven.BOTTOM for y in (207, 209, 212, 213, 214))
    if len(positions) != seven.CHAIN_COUNT:
        raise ValueError("Full start chain coordinate map drift")
    return positions


def snapshot(world):
    view = physical.World(world)
    coordinates = set()
    for x, y, z in ROOTS:
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                for dy in range(-10, 2):
                    coordinates.add((x + dx, y + dy, z + dz))
    states = {}
    for pos in sorted(coordinates):
        state = view.state(pos)
        states[pos] = (state["id"], state["facing"], state["conditional"])
    return states


def signature(states):
    raw = json.dumps([[*p, *v] for p, v in sorted(states.items())],
                     ensure_ascii=True, separators=(",", ":")).encode()
    return hashlib.sha256(raw).hexdigest()


def choose_trigger_sites(world):
    view = physical.World(world)
    sites = []
    for root in ROOTS:
        if not physical.iscmd(view.state(root)):
            raise ValueError("Expected original command block at root candidate")
        options = []
        for dx, dy, dz in ((1,0,0), (-1,0,0), (0,0,1), (0,0,-1), (0,1,0), (0,-1,0)):
            pos = (root[0]+dx, root[1]+dy, root[2]+dz)
            if view.state(pos)["id"] in ("minecraft:air","minecraft:cave_air","minecraft:void_air"):
                options.append(pos)
        sites.append(options[0] if options else None)
    if len([p for p in sites if p is not None]) != len(set(p for p in sites if p is not None)):
        raise ValueError("Redstone injection would alias a second test root")
    return sites


def count_source_commands(world):
    expected = chain_positions()
    records, warnings = scan_region(Path(world) / seven.REGION)
    found = {}
    for item in records:
        pos = tuple(item.get(k) for k in ("x", "y", "z"))
        if pos in expected:
            if pos in found:
                raise ValueError("Duplicated start command actor")
            found[pos] = item["command"]
    if set(found) != expected:
        raise ValueError("One or more start block entities missing after runtime")
    return sum(value == "" for value in found.values()), len(warnings)


def recv_exact(sock, size):
    chunks = []
    while size:
        chunk = sock.recv(size)
        if not chunk:
            raise ConnectionError("RCON socket unexpectedly closed")
        chunks.append(chunk)
        size -= len(chunk)
    return b"".join(chunks)


class Rcon:
    def __init__(self, password):
        self.sock = socket.create_connection(("127.0.0.1", PORT), timeout=4)
        self.sock.settimeout(12)
        self.send(29, 3, password)
        for _ in range(3):
            req, kind, _ = self.read()
            if req == -1:
                raise ValueError("RCON authentication rejected")
            if req == 29 and kind == 2:
                break
        else:
            raise ValueError("RCON did not acknowledge authentication")

    def send(self, request, kind, payload):
        content = struct.pack("<ii", request, kind) + payload.encode("utf-8") + b"\0\0"
        self.sock.sendall(struct.pack("<i", len(content)) + content)

    def read(self):
        length = struct.unpack("<i", recv_exact(self.sock, 4))[0]
        if length < 10 or length > 16384:
            raise ValueError("Invalid RCON packet size")
        packet = recv_exact(self.sock, length)
        request, kind = struct.unpack("<ii", packet[:8])
        return request, kind, packet[8:-2].decode("utf-8", errors="replace")

    def command(self, command):
        self.send(30, 2, command)
        for _ in range(6):
            req, kind, data = self.read()
            if req == 30 and kind == 0:
                return data
            if req == -1:
                raise ValueError("RCON command rejected")
        raise ValueError("RCON command response missing")

    def close(self):
        self.sock.close()


def prepare_run_directory(project):
    run = project / "run"
    if run.exists():
        if not (run / RUN_MARKER).is_file():
            raise ValueError("Refusing a pre-existing run directory")
        shutil.rmtree(run)
    run.mkdir()
    (run / RUN_MARKER).write_text("disposable CI only\n", encoding="utf-8")
    password = secrets.token_hex(20)
    (run / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    properties = [
        "server-ip=127.0.0.1", "server-port=25580",
        "enable-rcon=true", f"rcon.port={PORT}", f"rcon.password={password}",
        "enable-command-block=true", "online-mode=false",
        "white-list=true", "max-players=0",
        "view-distance=3", "simulation-distance=3",
        "spawn-protection=0", "max-tick-time=-1",
        "motd=TURNBOUND isolated CI only",
    ]
    (run / "server.properties").write_text("\n".join(properties) + "\n", encoding="utf-8")
    return password


def wait_rcon(process, password):
    deadline = time.monotonic() + BOUNDARY_WAIT_SECONDS
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise ValueError("Dedicated Fabric server exited before RCON readiness")
        try:
            return Rcon(password)
        except (OSError, ConnectionError, ValueError):
            time.sleep(2)
    raise TimeoutError("Dedicated Fabric server readiness timed out")


def game_time(response):
    found = re.findall(r"\d+", response)
    if not found:
        raise ValueError("Could not parse Minecraft gametime response")
    return int(found[-1])


def launch_probe(project, world, label, injection_sites):
    password = prepare_run_directory(project)
    log = project / "build" / ("drehmal-runtime-" + label + ".internal.log")
    log.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        "gradle", "runServer", "--no-daemon",
        "--args=--nogui --universe " + str(world.parent) + " --world " + world.name,
    ]
    result = {"dedicated_server_started": False,
              "force_loaded_start_area": False,
              "observed_server_ticks": 0,
              "synthetic_redstone_injections": 0}
    with log.open("w", encoding="utf-8") as out:
        process = subprocess.Popen(cmd, cwd=project, stdout=out,
                                   stderr=subprocess.STDOUT, start_new_session=True)
        rcon = None
        try:
            rcon = wait_rcon(process, password)
            result["dedicated_server_started"] = True
            first = game_time(rcon.command("time query gametime"))
            forced = rcon.command("forceload add 26448 -304 26480 -240")
            if not forced or any(term in forced.lower() for term in ("unknown", "error", "failed")):
                raise ValueError("Server refused original start-area chunk force loading")
            result["force_loaded_start_area"] = True
            # Synthetic activation on disposable copies only. No real player or
            # original-world mechanism-interaction success is inferred.
            for site in injection_sites:
                if site is None:
                    continue
                x, y, z = site
                resp = rcon.command(f"setblock {x} {y} {z} minecraft:redstone_block replace")
                if not resp or "changed" not in resp.lower():
                    raise ValueError("Safe air-neighbor redstone placement failed")
                result["synthetic_redstone_injections"] += 1
            time.sleep(7)
            second = game_time(rcon.command("time query gametime"))
            result["observed_server_ticks"] = max(0, second - first)
            if result["observed_server_ticks"] < 5:
                raise ValueError("Server was alive but did not advance enough ticks")
            saved = rcon.command("save-all flush")
            if not saved or "error" in saved.lower():
                raise ValueError("Server did not flush disposable world")
            rcon.command("stop")
            try:
                process.wait(timeout=60)
            except subprocess.TimeoutExpired as exc:
                raise TimeoutError("Dedicated server did not stop cleanly") from exc
            if process.returncode != 0:
                raise ValueError("Dedicated Fabric server did not exit successfully")
            result["dedicated_server_stopped_cleanly"] = True
            return result
        finally:
            if rcon is not None:
                rcon.close()
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=12)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait(timeout=10)
    # Private server logs are intentionally excluded from published artifacts.


def verify_staged_copy(world, label):
    if (world / "turnbound-world.marker").exists():
        raise ValueError("Refusing an activated world")
    if label == "six":
        result = six.verify(world)
        if result["status"] != "SIX_BOUNDARIES_PRESENT_VERIFIED_OFFLINE_EXPERIMENT":
            raise ValueError("Not a pinned six-boundary copied world")
    else:
        result = seven.verify(world)
        if not result["seven_stage_offline_experiment_verified"]:
            raise ValueError("Not a pinned seven-boundary copied world")


def probe(project, world, label):
    world = world.resolve(strict=True)
    if label not in ("six", "seven"):
        raise ValueError("Invalid comparative test stage")
    verify_staged_copy(world, label)
    before = snapshot(world)
    initial_empty, initial_warnings = count_source_commands(world)
    expected_empty = 24 if label == "six" else 57
    if initial_empty != expected_empty:
        raise ValueError("Original command isolation baseline count drift")
    sites = choose_trigger_sites(world)
    live = launch_probe(project, world, label, sites)
    after = snapshot(world)
    post_empty, post_warnings = count_source_commands(world)
    if post_empty != expected_empty:
        raise ValueError("Original command count changed during server load")
    deltas = [(pos, before[pos], after[pos]) for pos in sorted(before) if before[pos] != after[pos]]
    injections = set(s for s in sites if s is not None)
    unexpected = [(p, a, b) for p, a, b in deltas if p not in injections]
    return {
        "status": "DEDICATED_SERVER_SCENE_SMOKE_PASS",
        "stage": label,
        **live,
        "checked_scene_state_positions": len(before),
        "scene_before_sha256": signature(before),
        "scene_after_sha256": signature(after),
        "start_command_blocks_present": len(chain_positions()),
        "start_command_blocks_empty": post_empty,
        "initial_region_warnings": initial_warnings,
        "post_runtime_region_warnings": post_warnings,
        "scene_changed_positions_including_injections": len(deltas),
        "scene_changes_excluding_injections": len(unexpected),
        "scene_changes_sample": [
            {"coordinates": list(pos), "before": old[0], "after": new[0]}
            for pos, old, new in unexpected[:24]
        ],
        "synthetic_injection_sites": [list(pos) if pos else None for pos in sites],
        "no_human_players_connected": True,
        "redstone_simulation_not_equivalent_to_normal_entry": True,
        "player_join_respawn_tested": False,
        "client_mechanisms_tested": False,
        "multiplayer_tested": False,
        "map_separation_complete": False,
        "original_code_reused": False,
        "original_world_modified": False,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--project", required=True, type=Path)
    parser.add_argument("--world-copy", required=True, type=Path)
    parser.add_argument("--stage", required=True, choices=("six", "seven"))
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    project = args.project.resolve(strict=True)
    world = args.world_copy.resolve(strict=True)
    output = args.output.resolve()
    if world == output or world in output.parents:
        parser.error("Probe result must not be written into the map copy")
    result = probe(project, world, args.stage)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps({
        "status": result["status"], "stage": result["stage"],
        "observed_server_ticks": result["observed_server_ticks"],
        "scene_changes_excluding_injections": result["scene_changes_excluding_injections"],
        "start_command_blocks_empty": result["start_command_blocks_empty"],
        "player_join_respawn_tested": False,
    }))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, TimeoutError, ConnectionError,
            subprocess.SubprocessError) as err:
        print("Isolated Drehmal dedicated-server smoke BLOCKED: " + str(err), file=sys.stderr)
        sys.exit(2)
