#!/usr/bin/env node
'use strict';
/* Connected Minecraft protocol clients on a DISPOSABLE CI world only. */
const fs=require('node:fs'), path=require('node:path');
const deps=process.env.TURNBOUND_PROBE_NODE_MODULES;
const mc=require(path.join(deps,'minecraft-protocol'));
const {Rcon}=require(path.join(deps,'rcon-client'));
const output=process.argv[2]; if(!output)throw Error('Output path required');
const proof={status:'INCOMPLETE',minecraft:'1.20.1',
  disposable_flat_world:true,official_drehmal_loaded:false,
  fixture_shipped_in_production:false,authored_game_enemy_created:false,
  human_playtested:false,real_gameplay_multiplayer_tested:false,
  two_clients_connected:false,visible_warning_delay_not_bypassed:false,
  physical_contact_claimed_battle:false,second_player_not_automatically_claimed:false,
  field_battle_defeat_return_verified:false,defeat_item_preserved:false,
  other_player_unaffected_by_defeat:false,
  observations:[]};
const names=['TBAliceProbe','TBBobProbe'];
const start=[10.5,70,10.5], nearEnemy=[16.5,70,10.5],contact=[20.2,70,10.5];
let rcon,clients=[];
const delay=ms=>new Promise(res=>setTimeout(res,ms));
function insist(x,msg){if(!x)throw Error(msg)}
async function poll(fn,label,ms=35000){
  const end=Date.now()+ms;
  while(Date.now()<end){const v=await fn();if(v)return v;await delay(350);}
  throw Error('Timeout: '+label);
}
async function send(command){return rcon.send(command);}
async function status(name) {const v=await send('tb_ci_field_status '+name);
  return v.includes('CI_FIELD_BATTLE')?'BATTLE':v.includes('CI_FIELD_FREE')?'FREE':'UNKNOWN';}
async function isOnline(name){return (await send('list')).includes(name);}
async function pos(name){
  const s=await send('data get entity '+name+' Pos');
  const match=s.match(/\[\s*(-?\d+(?:\.\d+)?)[dDfF]?\s*,\s*(-?\d+(?:\.\d+)?)[dDfF]?\s*,\s*(-?\d+(?:\.\d+)?)[dDfF]?\s*\]/);
  if(!match)throw Error('Unable to read server-authoritative player coordinates');
  return match.slice(1).map(Number);
}
function close(a,b){return a.every((v,i)=>Math.abs(v-b[i])<0.25);}
async function at(name,target,label){
  const v=await poll(async()=>{const p=await pos(name);return close(p,target)?p:false;},label);
  proof.observations.push({label,position:v});
}
async function join(name){
  const c=mc.createClient({host:'127.0.0.1',port:25565,username:name,
    version:'1.20.1',auth:'offline',keepAlive:true});
  clients.push(c);
  c.on('position',p=>{try {if(p.teleportId!==undefined)c.write('teleport_confirm',{teleportId:p.teleportId});}catch(e){c._error=e.message;}});
  c.on('error',e=>c._error=e.message);
  c.on('kick_disconnect',()=>c._error='kicked');
  await poll(async()=>{if(c._error)throw Error(name+': '+c._error);return isOnline(name);},
    name+' connect',60000);
  return c;
}
async function run(){
  rcon=await poll(async()=>{
    try{
      const c=await Rcon.connect({host:'127.0.0.1',port:25575,
        password:'turnbound_ci_synthetic_only',timeout:3000});
      if((await c.send('list')).includes('players online'))return c;
      c.end();
    }catch{}
    return false;
  },'RCON boot',150000);
  for(const command of [
    'forceload add 10 10','forceload add 20 10',
    'fill 8 69 8 12 69 12 minecraft:stone',
    'fill 8 70 8 12 73 12 minecraft:air',
    'fill 14 69 8 23 69 12 minecraft:stone',
    'fill 14 70 8 23 73 12 minecraft:air'
  ])await send(command);
  await delay(1000);
  await join(names[0]);await join(names[1]);
  await at(names[0],start,'Alice entry');await at(names[1],start,'Bob entry');
  insist(await isOnline(names[0])&&await isOnline(names[1]),'Two clients absent');
  proof.two_clients_connected=true;
  insist((await status(names[0]))==='FREE','Unexpected initial battle');
  insist((await status(names[1]))==='FREE','Unexpected Bob battle');

  const summon=await send('summon minecraft:zombie 20.5 70 10.5 {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b}');
  insist(/Summoned new/i.test(summon),'Synthetic fixture actor spawn failed: '+summon);
  const registered=await send('tb_ci_field_register '+names[0]);
  insist(registered.includes('CI_FIELD_REGISTERED'),'Server-side field actor registration failed: '+registered);
  await delay(500);
  insist((await status(names[0]))==='FREE','Battle started outside notice radius');
  await send('tp '+names[0]+' 16.5 70 10.5');
  await at(names[0],nearEnemy,'Alice in visible notice radius');
  await delay(350);
  insist((await status(names[0]))==='FREE',
    'Immediate combat violated authored warning phase');
  proof.visible_warning_delay_not_bypassed=true;
  // Enter contact range after the required visible warning duration.
  await delay(1100);
  await send('tp '+names[0]+' 20.2 70 10.5');
  await at(names[0],contact,'Alice physically contacted authorized field representative');
  await poll(async()=>(await status(names[0]))==='BATTLE','Server field contact battle session',25000);
  proof.physical_contact_claimed_battle=true;
  insist((await status(names[1]))==='FREE','Distant Bob joined Alice fight without consent');
  await send('tp '+names[1]+' 20.5 70 11.4');
  await delay(900);
  insist((await status(names[1]))==='FREE','Second player claimed occupied representative');
  proof.second_player_not_automatically_claimed=true;
  await send('give '+names[0]+' minecraft:diamond 1');
  await poll(async()=>(await send('data get entity '+names[0]+' Inventory'))
    .includes('minecraft:diamond'),'diamond before synthetic battle defeat');
  const forced=await send('tb_ci_field_defeat '+names[0]);
  insist(forced.includes('CI_FIELD_DEFEAT_APPLIED'),
    'Server-owned terminal defeat was not authored');
  await at(names[0],start,'Field battle defeat returned Alice to first checkpoint');
  proof.field_battle_defeat_return_verified=true;
  insist((await send('data get entity '+names[0]+' Inventory')).includes('minecraft:diamond'),
    'Defeat removed synthetic inventory item');
  proof.defeat_item_preserved=true;
  const bobPos=await pos(names[1]);
  insist(close(bobPos,[20.5,70,11.4]),'Other player moved due to Alice defeat');
  insist((await status(names[1]))==='FREE','Bob falsely joined Alice defeated session');
  proof.other_player_unaffected_by_defeat=true;
}
async function main(){
  try{await run();proof.status='SYNTHETIC_FIELD_CONTACT_OPENED_SERVER_BATTLE';}
  catch(e){proof.status='FAILED';proof.error=String(e?.message||e).slice(0,500);process.exitCode=1;}
  finally{
    for(const c of clients){try{c.end();}catch{}}
    if(rcon){try{await rcon.send('stop');}catch{}try{rcon.end();}catch{}}
    fs.mkdirSync(path.dirname(output),{recursive:true});
    fs.writeFileSync(output,JSON.stringify(proof,null,2)+'\n');
    console.log(JSON.stringify({status:proof.status,observations:proof.observations.length,error:proof.error||null}));
  }
}
main();
