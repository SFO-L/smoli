// AI生成
const WebSocket = require('ws');
const { db, stmts } = require('./db.js');

const PORT = process.env.PORT || 8080;
const wss = new WebSocket.Server({ port: PORT });

// 内存中的房间管理: roomCode -> { hostWs, members: Map(userId -> {ws, info}) }
const rooms = new Map();
// ws -> { userId, roomCode }
const wsMap = new Map();

function now() { return Date.now(); }

function send(ws, obj) {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(obj));
  }
}

function broadcastRoom(roomCode, obj) {
  const room = rooms.get(roomCode);
  if (!room) return;
  for (const [, m] of room.members) {
    send(m.ws, obj);
  }
}

function buildMemberList(roomCode) {
  const room = rooms.get(roomCode);
  if (!room) return [];
  const list = [];
  for (const [uid, m] of room.members) {
    list.push({
      userId: uid,
      nickname: m.info.nickname,
      avatar: m.info.avatar,
      isHost: uid === room.hostId,
      hasLeft: m.hasLeft
    });
  }
  return list;
}

function broadcastMemberList(roomCode) {
  broadcastRoom(roomCode, {
    type: 'MEMBER_LIST',
    members: buildMemberList(roomCode)
  });
}

function generateRoomCode() {
  let code;
  do {
    code = String(Math.floor(1000 + Math.random() * 9000));
  } while (rooms.has(code));
  return code;
}

wss.on('connection', (ws) => {
  console.log(`[连接] ${now()} 新WebSocket连接`);

  ws.on('message', (data) => {
    let msg;
    try { msg = JSON.parse(data.toString()); } catch (e) { return; }
    const ctx = wsMap.get(ws) || {};
    const type = msg.type;

    switch (type) {
      case 'REGISTER': {
        const { userId, nickname, avatar } = msg;
        stmts.upsertUser.run(userId, nickname, avatar, now());
        wsMap.set(ws, { userId, roomCode: null });
        console.log(`[注册] ${userId} ${nickname}`);
        break;
      }

      case 'CREATE_ROOM': {
        const userId = ctx.userId;
        if (!userId) return;
        const roomCode = generateRoomCode();
        rooms.set(roomCode, {
          hostId: userId,
          hostWs: ws,
          members: new Map([[userId, { ws, info: { nickname: msg.nickname || '', avatar: msg.avatar || '' }, hasLeft: false }]]),
          started: false,
          durationSeconds: 0,
          countdownSeconds: 0
        });
        stmts.createRoom.run(roomCode, userId, now());
        stmts.addMember.run(roomCode, userId);
        wsMap.set(ws, { userId, roomCode });
        send(ws, { type: 'ROOM_CREATED', roomCode });
        broadcastMemberList(roomCode);
        console.log(`[建房] ${userId} -> 房间 ${roomCode}`);
        break;
      }

      case 'JOIN_ROOM': {
        const userId = ctx.userId;
        if (!userId) return;
        const roomCode = msg.roomCode;
        const room = rooms.get(roomCode);
        if (!room) {
          send(ws, { type: 'ERROR', message: '房间不存在' });
          return;
        }
        if (room.started) {
          send(ws, { type: 'ERROR', message: '游戏已开始，无法加入' });
          return;
        }
        const userRow = stmts.getUser.get(userId);
        room.members.set(userId, {
          ws,
          info: { nickname: userRow?.nickname || msg.nickname || '', avatar: userRow?.avatar || msg.avatar || '' },
          hasLeft: false
        });
        stmts.addMember.run(roomCode, userId);
        wsMap.set(ws, { userId, roomCode });
        send(ws, { type: 'ROOM_JOINED', roomCode });
        broadcastMemberList(roomCode);
        console.log(`[加入] ${userId} -> 房间 ${roomCode} (共${room.members.size}人)`);
        break;
      }

      case 'START_GAME': {
        const roomCode = ctx.roomCode;
        const room = rooms.get(roomCode);
        if (!room || room.hostId !== ctx.userId) return;
        room.started = true;
        room.durationSeconds = msg.durationSeconds || 300;
        room.countdownSeconds = msg.countdownSeconds || 10;
        // 服务器选定共同建筑类型（加权随机）
        const buildingTypes = ['COTTAGE','BRICK_HOUSE','TENT','TREEHOUSE','LIGHTHOUSE','WINDMILL','CASTLE','PYRAMID','PALACE','WONDER'];
        const weights = [30, 25, 20, 15, 5, 3, 2, 1, 1, 1];
        const totalW = weights.reduce((a, b) => a + b, 0);
        let r = Math.floor(Math.random() * totalW);
        let buildingType = buildingTypes[0];
        for (let i = 0; i < weights.length; i++) { r -= weights[i]; if (r < 0) { buildingType = buildingTypes[i]; break; } }
        room.sharedBuildingType = buildingType;
        stmts.updateRoomStatus.run('playing', room.durationSeconds, room.countdownSeconds, roomCode);
        broadcastRoom(roomCode, {
          type: 'GAME_STARTED',
          durationSeconds: room.durationSeconds,
          countdownSeconds: room.countdownSeconds,
          buildingType: buildingType
        });
        console.log(`[开始] 房间 ${roomCode} 时长${room.durationSeconds}s 建筑${buildingType}`);
        break;
      }

      case 'LEFT_APP': {
        const roomCode = ctx.roomCode;
        const userId = ctx.userId;
        if (!roomCode || !userId) return;
        const room = rooms.get(roomCode);
        if (!room) return;
        const member = room.members.get(userId);
        if (member) member.hasLeft = true;
        stmts.setMemberLeft.run(1, roomCode, userId);
        broadcastRoom(roomCode, { type: 'MEMBER_LEFT_APP', userId });
        console.log(`[切出] ${userId} 在房间 ${roomCode}`);
        break;
      }

      case 'BACK_APP': {
        const roomCode = ctx.roomCode;
        const userId = ctx.userId;
        if (!roomCode || !userId) return;
        const room = rooms.get(roomCode);
        if (!room) return;
        const member = room.members.get(userId);
        if (member) member.hasLeft = false;
        stmts.setMemberLeft.run(0, roomCode, userId);
        broadcastRoom(roomCode, { type: 'MEMBER_BACK_APP', userId });
        console.log(`[回来] ${userId} 在房间 ${roomCode}`);
        break;
      }

      case 'PENALTY_REPORT': {
        const roomCode = ctx.roomCode;
        if (!roomCode) return;
        broadcastRoom(roomCode, {
          type: 'PENALTY',
          reason: msg.reason || '有人离开太久'
        });
        console.log(`[惩罚] 房间 ${roomCode}: ${msg.reason || ''}`);
        break;
      }

      case 'GAME_END': {
        const userId = ctx.userId;
        if (!userId) return;
        const today = new Date().toISOString().slice(0, 10);
        stmts.addRecord.run(
          userId, ctx.roomCode || null, today,
          msg.success ? 1 : 0, msg.buildingType || null,
          msg.duration || 0, now()
        );
        console.log(`[结束] ${userId} success=${msg.success}`);
        break;
      }

      case 'LEAVE_ROOM': {
        handleLeave(ws);
        break;
      }
    }
  });

  ws.on('close', () => {
    handleLeave(ws);
    wsMap.delete(ws);
    console.log(`[断开] ${now()} WebSocket关闭`);
  });
});

function handleLeave(ws) {
  const ctx = wsMap.get(ws);
  if (!ctx || !ctx.roomCode) return;
  const { userId, roomCode } = ctx;
  const room = rooms.get(roomCode);
  if (!room) return;

  room.members.delete(userId);
  stmts.removeMember.run(roomCode, userId);

  if (userId === room.hostId) {
    // 房主离开 -> 解散房间
    broadcastRoom(roomCode, { type: 'ROOM_DISBANDED' });
    stmts.clearMembers.run(roomCode);
    stmts.deleteRoom.run(roomCode);
    rooms.delete(roomCode);
    console.log(`[解散] 房间 ${roomCode} (房主离开)`);
  } else {
    broadcastMemberList(roomCode);
    console.log(`[离开] ${userId} 离开房间 ${roomCode}`);
  }

  wsMap.set(ws, { userId, roomCode: null });
}

console.log(`SleepTown 服务器已启动，端口 ${PORT}`);
