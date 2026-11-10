#!/usr/bin/env node
'use strict';
/**
 * CI-only vanilla-like movement-protocol connections into copied official Drehmal 2.2.2f.
 * NOT a graphical player, mechanics use test or a client playtest.
 * No RCON commands, world patching or new game content originate here.
 */
const path = require('node:path');
const dep = process.env.TURNBOUND_PROBE_NODE_MODULES;
if (!dep) throw Error('TURNBOUND_PROBE_NODE_MODULES required');
const mc = require(path.join(dep,'minecraft-protocol'));
const clients=[];
let closing=false;
for(const username of ['DrehmalAliceCI','DrehmalBobCI']){
  const c=mc.createClient({
    host:'127.0.0.1',port:25580,version:'1.20.1',
    username,auth:'offline',keepAlive:true
  });
  clients.push(c);
  let lastPosition=null;
  c.on('position',packet=>{
    if(packet.teleportId !== undefined) {
      c.write('teleport_confirm',{teleportId:packet.teleportId});
    }
    lastPosition={x:packet.x,y:packet.y,z:packet.z,
                  yaw:packet.yaw||0,pitch:packet.pitch||0,onGround:false};
    c.write('position_look',lastPosition);
  });
  const ticker=setInterval(()=>{
    if(!closing && lastPosition){
      try { c.write('position_look',lastPosition); }
      catch(e){process.stderr.write('Protocol motion: '+e.message+'\n');}
    }
  },250);
  ticker.unref();
  c.on('error',err=>{
    if(!closing)process.stderr.write('CI protocol error: '+err.message.slice(0,150)+'\n');
  });
  c.on('kick_disconnect',()=>{
    if(!closing)process.stderr.write('CI synthetic player disconnected unexpectedly\n');
  });
}
function shut(){
  if(closing)return;
  closing=true;
  for(const c of clients)try{c.end();}catch{}
  setTimeout(()=>process.exit(0),350).unref();
}
process.on('SIGINT',shut);
process.on('SIGTERM',shut);
process.stdin.resume();
