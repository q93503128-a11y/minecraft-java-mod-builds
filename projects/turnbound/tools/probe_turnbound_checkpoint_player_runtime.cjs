#!/usr/bin/env node
'use strict';
/*
 * Connects two non-human Minecraft Java 1.20.1 protocol clients to one
 * disposable local CI server. Never uses a Drehmal map or actual players.
 * The 10.5/20.5 coordinates are SYNTHETIC CI-ONLY, NOT a game spawn approval.
 */
const fs = require('node:fs');
const path = require('node:path');
const mc = require(path.join(process.env.TURNBOUND_PROBE_NODE_MODULES, 'minecraft-protocol'));
const { Rcon } = require(path.join(process.env.TURNBOUND_PROBE_NODE_MODULES, 'rcon-client'));

const phase = process.argv[2];
const output = process.argv[3];
if (!['first', 'second'].includes(phase) || !output) {
  throw Error('Usage: node script.cjs first|second output.json');
}
const names = ['TBAliceProbe', 'TBBobProbe'];
const position = Object.freeze({ entry: [10.5, 70, 10.5], away: [20.5, 70, 10.5] });
const proof = {
  schema: 1,
  phase,
  status: 'UNVERIFIED',
  synthetic_ci_world_only: true,
  official_drehmal_used: false,
  fake_player_names_only: names,
  entry_coordinates_approved_for_game: false,
  entry: position.entry,
  same_server_two_players_connected: false,
  first_join_teleport_verified: false,
  other_player_independent: false,
  rejoin_preserves_last_position: false,
  restarted_server_preserves_player_position: false,
  uuid_state_persistence_verified: false,
  respawn_tested: false,
  synthetic_force_kill_path_tested: false,
  synthetic_forced_death_return_verified: false,
  synthetic_force_kill_handling: 'NOT_TESTED',
  synthetic_inventory_preserved_after_forced_death: false,
  synthetic_respawn_request_sent: false,
  structures_tested: false,
  original_world_modified: false,
  real_human_playtested: false,
  multiplayer_gameplay_tested: false,
  observations: [],
};
let rcon;
const active = [];
const ms = n => new Promise(resolve => setTimeout(resolve, n));
function check(condition, description) {
  if (!condition) throw Error(description);
}
async function until(fn, label, timeoutMs=25000) {
  const end = Date.now() + timeoutMs;
  let last;
  while (Date.now() < end) {
    try {
      const value = await fn();
      if (value) return value;
    } catch (e) {
      last = e;
    }
    await ms(500);
  }
  throw Error('Timed out: ' + label + (last ? ' ('+last.message+')' : ''));
}
async function connectRcon() {
  return until(async () => {
    const connection = await Rcon.connect({
      host: '127.0.0.1', port: 25575, password: 'turnbound_ci_synthetic_only',
      timeout: 3000
    });
    const response = await connection.send('list');
    if (!response.includes('players online')) {
      connection.end();
      return null;
    }
    return connection;
  }, 'RCON server start', 90000);
}
async function command(line) {
  return rcon.send(line);
}
async function online(name) {
  const list = await command('list');
  return list.includes(name);
}
function parsePosition(response) {
  const match = response.match(/\[\s*(-?\d+(?:\.\d+)?)(?:[dDfF])?\s*,\s*(-?\d+(?:\.\d+)?)(?:[dDfF])?\s*,\s*(-?\d+(?:\.\d+)?)(?:[dDfF])?\s*\]/);
  if (!match) throw Error('Cannot parse a server-authoritative entity position');
  return match.slice(1).map(Number);
}
async function readPos(name) {
  const result = await command('data get entity '+name+' Pos');
  return parsePosition(result);
}
function near(actual, expected, tolerance=0.2) {
  return actual.length===3 && actual.every((v,i) => Math.abs(v-expected[i]) <= tolerance);
}
async function expectPosition(name, expected, label) {
  const found = await until(async () => {
    if (!await online(name)) return false;
    const actual = await readPos(name);
    return near(actual, expected) ? actual : false;
  }, label, 20000);
  proof.observations.push({label, position: found});
}
function join(name) {
  const client = mc.createClient({
    host: '127.0.0.1', port: 25565, version: '1.20.1',
    username: name, auth: 'offline', keepAlive: true
  });
  active.push(client);
  client.on('position', packet => {
    if (packet.teleportId === undefined) return;
    try { client.write('teleport_confirm', {teleportId: packet.teleportId}); }
    catch (err) { client._turnboundError = err.message; }
  });
  client.on('error', err => {client._turnboundError = err.message;});
  client.on('kick_disconnect', packet => { client._turnboundError='kicked by server'; });
  // A /kill may bypass the Fabric death cancellation hook. In that case
  // emulate the ordinary 1.20.1 client pressing Respawn, once per death.
  client.on('update_health', packet => {
    if (packet.health <= 0 && !client._turnboundRespawnPending) {
      client._turnboundRespawnPending = true;
      setTimeout(() => {
        try {
          client.write('client_command', {actionId: 0});
          proof.synthetic_respawn_request_sent = true;
        } catch (err) {
          client._turnboundError = err.message;
        }
      }, 600);
    } else if (packet.health > 0) {
      client._turnboundRespawnPending = false;
    }
  });
  return client;
}
async function joined(name) {
  const client = join(name);
  await until(async () => {
    if (client._turnboundError) throw Error('Protocol client '+name+': '+client._turnboundError);
    return online(name);
  }, 'Minecraft login '+name, 45000);
  await ms(1500);
  return client;
}
async function leave(name, client) {
  client.end('test disconnect');
  await until(async () => !(await online(name)), name+' disconnect');
}
async function first() {
  // Force-load the two chunks before any player JOIN. This synthetic map
  // intentionally has no real Drehmal structures; the test may make terrain.
  for (const line of [
    'forceload add 10 10', 'forceload add 20 10',
    'fill 8 69 8 12 69 12 minecraft:stone',
    'fill 8 70 8 12 73 12 minecraft:air',
    'fill 18 69 8 22 69 12 minecraft:stone',
    'fill 18 70 8 22 73 12 minecraft:air'
  ]) await command(line);
  await ms(2000);
  const alice = await joined(names[0]);
  await expectPosition(names[0], position.entry, 'first join uses explicit approved-for-CI entry');
  proof.first_join_teleport_verified = true;
  const bob = await joined(names[1]);
  await expectPosition(names[1], position.entry, 'independent second join uses same entry');
  proof.same_server_two_players_connected = await online(names[0]) && await online(names[1]);
  check(proof.same_server_two_players_connected, 'Two simultaneous protocol players not connected');
  await command('tp '+names[0]+' 20.5 70 10.5');
  await expectPosition(names[0], position.away, 'first player moved away by server console');
  await expectPosition(names[1], position.entry, 'second player unaffected by first player move');
  proof.other_player_independent = true;
  await leave(names[0], alice);
  const rejoined = await joined(names[0]);
  await expectPosition(names[0], position.away, 'same-UUID reconnect does not reset position');
  proof.rejoin_preserves_last_position = true;
  await expectPosition(names[1], position.entry, 'second player position still independent after reconnect');
  await leave(names[0], rejoined);
  await leave(names[1], bob);
}
async function second() {
  const alice = await joined(names[0]);
  await expectPosition(names[0], position.away, 'saved player position survived dedicated server restart');
  proof.restarted_server_preserves_player_position = true;
  const bob = await joined(names[1]);
  await expectPosition(names[1], position.entry, 'second saved player position survived restart');
  proof.other_player_independent = true;
  proof.same_server_two_players_connected = await online(names[0]) && await online(names[1]);
  proof.uuid_state_persistence_verified = proof.same_server_two_players_connected;
  // Synthetic-world only. Observe whether /kill is cancelled before vanilla
  // death or whether a vanilla respawn still preserves the player inventory.
  const give = await command('give '+names[0]+' minecraft:diamond 1');
  await until(async () =>
    (await command('data get entity '+names[0]+' Inventory')).includes('minecraft:diamond'),
    'test diamond present before force-kill');
  const kill = await command('kill '+names[0]);
  proof.observations.push({label:'synthetic forced kill issued', server_response_kind:
    /Killed|killed/i.test(kill) ? 'kill_acknowledged' : 'other_response'});
  // A real protocol client must explicitly send a respawn request if the
  // server reached the death screen. If the server cancelled death first,
  // the same packet is ignored and cannot trigger a second death.
  await ms(500);
  const stillListed = await online(names[0]);
  const healthRead = await command('data get entity '+names[0]+' Health');
  const healthMatch = healthRead.match(/(-?\d+(?:\.\d+)?)[fFdD]?\s*$/);
  check(healthMatch !== null, 'Could not read server authoritative Health NBT after /kill');
  let currentHealth = Number(healthMatch[1]);
  let postKillPosition = await readPos(names[0]);
  proof.observations.push({
    label:'server post-kill entity status before possible respawn',
    same_player_still_listed:stillListed,
    health_positive:currentHealth>0,
    server_position:postKillPosition
  });
  if (!stillListed || currentHealth<=0) {
    // Only a genuinely dead player is asked to respawn. Unlike the old
    // probe, an already living player is never sent a pointless request.
    alice.write('client_command', {actionId:0});
    proof.synthetic_respawn_request_sent = true;
    await expectPosition(names[0], position.entry,
                         'vanilla respawn returned to registered checkpoint');
    currentHealth = Number((await command('data get entity '+names[0]+' Health'))
      .match(/(-?\d+(?:\.\d+)?)[fFdD]?\s*$/)[1]);
    postKillPosition = await readPos(names[0]);
    proof.synthetic_force_kill_handling='VANILLA_RESPAWN_WITH_CHECKPOINT';
  } else if (near(postKillPosition, position.entry)) {
    proof.synthetic_force_kill_handling='DEATH_CANCELLED_AND_RETURNED_TO_CHECKPOINT';
  } else if (near(postKillPosition, position.away)) {
    proof.synthetic_force_kill_handling='VANILLA_DEATH_DENIED_WITHOUT_TELEPORT';
  } else {
    throw Error('Unexpected position after /kill; no trusted checkpoint semantics');
  }
  check(currentHealth>0,'Forced kill left player with zero or negative health');
  proof.synthetic_forced_death_return_verified = near(postKillPosition, position.entry);
  const inventory = await command('data get entity '+names[0]+' Inventory');
  check(inventory.includes('minecraft:diamond'),
        'Force kill removed the test diamond; lossless death policy failed');
  proof.synthetic_inventory_preserved_after_forced_death = true;
  proof.synthetic_force_kill_path_tested = true;
  proof.observations.push({
    label:'post-force-kill outcome',
    handling:proof.synthetic_force_kill_handling,
    inventory_preserved:true,
    positive_health:true
  });
  await expectPosition(names[1], position.entry,
                       'Bob retained entry position after Alice force-kill');
  await leave(names[0], alice);
  await leave(names[1], bob);
}
async function main() {
  try {
    rcon = await connectRcon();
    if (phase === 'first') await first();
    else await second();
    proof.status = 'SYNTHETIC_PROTOCOL_PLAYER_JOIN_RECONNECT_PASS';
  } catch (e) {
    proof.status = 'FAILED';
    proof.error = String(e && e.message || e).slice(0,400);
    process.exitCode = 1;
  } finally {
    for (const client of active) {
      try {client.end();} catch {}
    }
    if (rcon) {
      try {await rcon.send('stop');} catch {}
      try {rcon.end();} catch {}
    }
    fs.mkdirSync(path.dirname(output),{recursive:true});
    fs.writeFileSync(output, JSON.stringify(proof,null,2)+'\n');
    console.log(JSON.stringify({phase,status:proof.status,
      observations:proof.observations.length,error:proof.error||null}));
  }
}
main();
