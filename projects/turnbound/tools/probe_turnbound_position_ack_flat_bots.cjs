#!/usr/bin/env node
'use strict';
/** Synthetic flat-world control only: compare bare protocol client vs
 *  vanilla-like post-teleport movement acknowledgement on identical server.
 *  Never included in a TURNBOUND production JAR or official Drehmal map.
 */
const path=require('node:path');
const mc=require(path.join(process.env.TURNBOUND_PROBE_NODE_MODULES,'minecraft-protocol'));
const clients=[];
let closing=false;
for(const [name,sendMovement] of [['TBBareProbe',false],['TBMoveAckProbe',true]]) {
  const c=mc.createClient({host:'127.0.0.1',port:25580,username:name,version:'1.20.1',
                           auth:'offline',keepAlive:true});
  clients.push(c);
  let last=null;
  c.on('position',p=>{
    if(p.teleportId !== undefined) {
      c.write('teleport_confirm',{teleportId:p.teleportId});
    }
    if(sendMovement){
      last={x:p.x,y:p.y,z:p.z,yaw:p.yaw||0,pitch:p.pitch||0,onGround:false};
      c.write('position_look',last);
    }
  });
  if(sendMovement){
    const ticker=setInterval(()=>{
      if(closing || !last)return;
      try{c.write('position_look',last);}catch(err){
        process.stderr.write('MOVEMENT_PACKET_FAILED: '+err.message+'\n');
      }
    },250);
    ticker.unref();
  }
  c.on('error',e=>{if(!closing)process.stderr.write(name+': '+e.message.slice(0,180)+'\n');});
  c.on('kick_disconnect',e=>{if(!closing)process.stderr.write(name+' kicked\n');});
}
function close(){
  if(closing)return;
  closing=true;
  for(const c of clients)try{c.end();}catch{}
  setTimeout(()=>process.exit(0),300).unref();
}
process.on('SIGINT',close);
process.on('SIGTERM',close);
process.stdin.resume();
