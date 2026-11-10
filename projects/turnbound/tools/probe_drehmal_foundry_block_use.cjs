#!/usr/bin/env node
'use strict';
// CI-only genuine 1.20.1 serverbound block-use packets. This does not
// emulate a rendered client or claim gameplay/door/puzzle validation.
const mc = require(require('node:path').join(process.env.TURNBOUND_PROBE_NODE_MODULES,'minecraft-protocol'));
const c = mc.createClient({host:'127.0.0.1',port:25580,version:'1.20.1',
 username:'DrehmalAliceCI',auth:'offline',keepAlive:true});
let pos=null,dead=false,seq=0,buffer='';
c.on('position',p=>{
 if(p.teleportId!==undefined)c.write('teleport_confirm',{teleportId:p.teleportId});
 pos={x:p.x,y:p.y,z:p.z,yaw:p.yaw||0,pitch:p.pitch||0,onGround:false};
 c.write('position_look',pos);
});
const tick=setInterval(()=>{
 if(pos && !dead){try{c.write('position_look',pos)}catch(e){console.error(e.message)}}
},250);
process.stdin.on('data',chunk=>{
 buffer+=chunk.toString();
 let i;
 while((i=buffer.indexOf('\n'))!==-1){
  const line=buffer.slice(0,i);buffer=buffer.slice(i+1);
  if(!line)continue;
  const req=JSON.parse(line);
  if(req.action!=='click'||!Array.isArray(req.position)||req.position.length!==3)
   throw Error('Refusing unknown interaction');
  if(!pos)throw Error('No server position acknowledgement');
  const [x,y,z]=req.position;
  c.write('block_place',{hand:0,location:{x,y,z},direction:1,
    cursorX:0.5,cursorY:0.5,cursorZ:0.5,insideBlock:false,sequence:++seq});
  process.stdout.write('packet_sent\n');
 }
});
c.on('error',e=>{console.error('client error',e.message);process.exitCode=2});
c.on('kick_disconnect',e=>{console.error('client kicked');process.exitCode=2});
function exit(){dead=true;clearInterval(tick);try{c.end()}catch{}}
process.on('SIGTERM',exit);process.on('SIGINT',exit);
