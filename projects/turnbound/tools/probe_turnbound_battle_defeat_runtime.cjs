#!/usr/bin/env node
'use strict';
/**
 * CI-only Fabric 1.20.1 defeat bridge acceptance on a disposable flat world.
 * Requires a separately injected, server-only trusted fixture command.
 * No actual Drehmal bytes, player secrets or approved game spawn are involved.
 */
const fs = require('node:fs');
const path = require('node:path');
const deps = process.env.TURNBOUND_PROBE_NODE_MODULES;
const mc = require(path.join(deps, 'minecraft-protocol'));
const {Rcon} = require(path.join(deps, 'rcon-client'));
const output = process.argv[2];
if (!output) throw Error('Output JSON path required');

const names = ['TBAliceProbe', 'TBBobProbe'];
const entry = [10.5,70,10.5];
const away = [20.5,70,10.5];
const offsetAway = [20.5,70,11.5];
const proof = {
  schema: 1,
  status: 'INCOMPLETE',
  world: 'SYNTHETIC_FLAT_CI_ONLY',
  original_drehmal_loaded: false,
  production_fixture_shipped: false,
  real_encounter_spawned: false,
  synthetic_server_battle_state: true,
  checkpoint_coordinates_approved_for_game: false,
  two_protocol_clients_connected: false,
  server_terminal_defeat_recovered_alice: false,
  no_duplicate_recovery: false,
  second_player_unchanged_during_alice_defeat: false,
  independent_second_player_defeat_recovered: false,
  item_preservation_verified: false,
  human_playtested: false,
  actual_map_playtested: false,
  multiplayer_gameplay_tested: false,
  observations: []
};
let rcon;
let clients = [];
const sleep = ms => new Promise(res => setTimeout(res, ms));
const assert = (v, message) => { if (!v) throw Error(message); };
async function wait(fn, label, duration=30000) {
  const end=Date.now()+duration;
  while(Date.now()<end) {
    const value=await fn();
    if(value) return value;
    await sleep(350);
  }
  throw Error('Timed out: '+label);
}
async function send(s) { return rcon.send(s); }
async function online(name) {
  const s = await send('list');
  return s.includes(name);
}
async function location(name) {
  const text = await send('data get entity '+name+' Pos');
  const found = text.match(/\[\s*(-?\d+(?:\.\d+)?)[dDfF]?\s*,\s*(-?\d+(?:\.\d+)?)[dDfF]?\s*,\s*(-?\d+(?:\.\d+)?)[dDfF]?\s*\]/);
  if(!found) throw Error('Cannot parse authoritative server position');
  return found.slice(1).map(Number);
}
function near(a,b) {return a.length===3&&a.every((v,i)=>Math.abs(v-b[i])<0.21);}
async function expectPos(name,coords,label) {
  const actual=await wait(async()=>{
    if(!await online(name))return false;
    const pos=await location(name);
    return near(pos,coords)?pos:false;
  },label);
  proof.observations.push({label,position:actual});
}
function connect(name) {
  const client=mc.createClient({
    username:name,auth:'offline',version:'1.20.1',host:'127.0.0.1',port:25565,keepAlive:true
  });
  clients.push(client);
  client.on('position',pkt=>{
    if(pkt.teleportId!==undefined) {
      try{client.write('teleport_confirm',{teleportId:pkt.teleportId});}
      catch(e){client._probeError=e.message;}
    }
  });
  client.on('error',err=>{client._probeError=err.message;});
  client.on('kick_disconnect',()=>{client._probeError='kicked';});
  return client;
}
async function join(name) {
  const client=connect(name);
  await wait(async()=>{
    if(client._probeError)throw Error(name+' '+client._probeError);
    return online(name);
  },name+' login',50000);
  await sleep(800);
  return client;
}
async function inventoryHas(name,item) {
  return (await send('data get entity '+name+' Inventory')).includes(item);
}
async function run() {
  rcon=await wait(async()=>{
    try {
      const conn=await Rcon.connect({
        host:'127.0.0.1',port:25575,password:'turnbound_ci_synthetic_only',timeout:3000
      });
      if((await conn.send('list')).includes('players online')) return conn;
      conn.end();
    } catch {}
    return false;
  },'Fabric RCON boot',140000);

  for (const cmd of [
    'forceload add 10 10',
    'forceload add 20 10',
    'fill 8 69 8 12 69 12 minecraft:stone',
    'fill 8 70 8 12 73 12 minecraft:air',
    'fill 18 69 8 22 69 12 minecraft:stone',
    'fill 18 70 8 22 73 12 minecraft:air'
  ]) await send(cmd);
  await sleep(1200);

  await join(names[0]);
  await join(names[1]);
  proof.two_protocol_clients_connected = await online(names[0])&&await online(names[1]);
  assert(proof.two_protocol_clients_connected,'Two distinct fake players not connected');
  await expectPos(names[0],entry,'Alice first entry');
  await expectPos(names[1],entry,'Bob first entry');

  await send('tp '+names[0]+' 20.5 70 10.5');
  await expectPos(names[0],away,'Alice before synthetic defeat');
  await send('give '+names[0]+' minecraft:diamond 1');
  await wait(async()=>inventoryHas(names[0],'minecraft:diamond'),'item before loss');
  const response=await send('tb_ci_server_defeat '+names[0]);
  assert(response.includes('ENEMY_VICTORY'), 'Only CI server-side synthetic battle may set defeat');
  await expectPos(names[0],entry,'real bridge settlement moved Alice to checkpoint');
  proof.server_terminal_defeat_recovered_alice = true;
  await expectPos(names[1],entry,'Bob unaffected by Alice synthetic battle');
  proof.second_player_unchanged_during_alice_defeat = true;
  assert(await inventoryHas(names[0],'minecraft:diamond'),'Loss incorrectly removed diamond');
  proof.item_preservation_verified=true;

  await send('tp '+names[0]+' 20.5 70 10.5');
  await expectPos(names[0],away,'Alice moved after settled defeat');
  await sleep(1500);
  assert(near(await location(names[0]),away),'Repeated terminal battle ticks teleported settled player');
  proof.no_duplicate_recovery=true;

  await send('tp '+names[1]+' 20.5 70 11.5');
  await expectPos(names[1],offsetAway,'Bob before independent synthetic defeat');
  const second=await send('tb_ci_server_defeat '+names[1]);
  assert(second.includes('ENEMY_VICTORY'),'Bob defeat server fixture not accepted');
  await expectPos(names[1],entry,'real bridge settled Bob in distinct session');
  proof.independent_second_player_defeat_recovered=true;
  await expectPos(names[0],away,'Alice unaffected by Bob synthetic battle');
}
async function main() {
  try {
    await run();
    proof.status='SYNTHETIC_SERVER_OWNED_BATTLE_DEFEAT_CHECKPOINT_PASS';
  } catch(e) {
    proof.status='FAIL';
    proof.error=String(e?.message||e).slice(0,500);
    process.exitCode=1;
  } finally {
    for (const client of clients) {
      try{client.end();}catch{}
    }
    if(rcon) {
      try{await rcon.send('stop');}catch{}
      try{rcon.end();}catch{}
    }
    fs.mkdirSync(path.dirname(output),{recursive:true});
    fs.writeFileSync(output,JSON.stringify(proof,null,2)+'\n');
    console.log(JSON.stringify({status:proof.status,error:proof.error||null,
      observations:proof.observations.length}));
  }
}
main();
