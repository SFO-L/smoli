// AI生成
const Database = require('better-sqlite3');
const db = new Database('sleeptown.db');

db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    user_id TEXT PRIMARY KEY,
    nickname TEXT NOT NULL,
    avatar TEXT NOT NULL,
    created_at INTEGER NOT NULL
  );

  CREATE TABLE IF NOT EXISTS rooms (
    room_code TEXT PRIMARY KEY,
    host_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'waiting',
    duration_seconds INTEGER DEFAULT 0,
    countdown_seconds INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (host_id) REFERENCES users(user_id)
  );

  CREATE TABLE IF NOT EXISTS room_members (
    room_code TEXT NOT NULL,
    user_id TEXT NOT NULL,
    has_left INTEGER DEFAULT 0,
    PRIMARY KEY (room_code, user_id),
    FOREIGN KEY (room_code) REFERENCES rooms(room_code),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
  );

  CREATE TABLE IF NOT EXISTS building_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    room_code TEXT,
    date TEXT NOT NULL,
    success INTEGER NOT NULL,
    building_type TEXT,
    duration INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
  );
`);

const stmts = {
  upsertUser: db.prepare(`INSERT OR REPLACE INTO users (user_id, nickname, avatar, created_at) VALUES (?, ?, ?, ?)`),
  getUser: db.prepare(`SELECT * FROM users WHERE user_id = ?`),
  createRoom: db.prepare(`INSERT INTO rooms (room_code, host_id, status, created_at) VALUES (?, ?, 'waiting', ?)`),
  getRoom: db.prepare(`SELECT * FROM rooms WHERE room_code = ?`),
  updateRoomStatus: db.prepare(`UPDATE rooms SET status = ?, duration_seconds = ?, countdown_seconds = ? WHERE room_code = ?`),
  deleteRoom: db.prepare(`DELETE FROM rooms WHERE room_code = ?`),
  addMember: db.prepare(`INSERT OR REPLACE INTO room_members (room_code, user_id, has_left) VALUES (?, ?, 0)`),
  removeMember: db.prepare(`DELETE FROM room_members WHERE room_code = ? AND user_id = ?`),
  getMembers: db.prepare(`SELECT rm.*, u.nickname, u.avatar FROM room_members rm JOIN users u ON rm.user_id = u.user_id WHERE rm.room_code = ?`),
  setMemberLeft: db.prepare(`UPDATE room_members SET has_left = ? WHERE room_code = ? AND user_id = ?`),
  clearMembers: db.prepare(`DELETE FROM room_members WHERE room_code = ?`),
  addRecord: db.prepare(`INSERT INTO building_records (user_id, room_code, date, success, building_type, duration, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)`),
  getRecords: db.prepare(`SELECT * FROM building_records WHERE user_id = ? ORDER BY created_at DESC LIMIT 50`),
};

module.exports = { db, stmts };
