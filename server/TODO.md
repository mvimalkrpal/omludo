# Server Implementation Roadmap: 100% Cloudflare Architecture

**Target:** Cloudflare Workers + Durable Objects + Cloudflare Calls (WebRTC) + Cloudflare KV/D1  
**Role:** Authoritative Game Server, Real-time WebSocket Hub, and WebRTC Voice Signaling

---

## Phase 1: Cloudflare Project Initialization & Infrastructure

- [x] **1.1 Project Scaffolding with Wrangler**
  - **Status:** Done (`package.json`, `tsconfig.json`, `wrangler.jsonc`, Vitest).
- [x] **1.2 Durable Object Configuration (`GameRoom`)**
  - **Status:** Done (`src/room.ts` stateful class with alarms & WebSockets).
- [x] **1.3 Room Index & Matchmaking Store (Cloudflare KV / D1)**
  - **Status:** Done (`POST /api/rooms/create` with 6-digit codes & TTL, `GET /api/rooms/:roomCode`).

---

## Phase 2: Jackaroo Server-Authoritative Game Logic

- [x] **2.1 Card Engine & Deck Shuffling**
  - **Status:** Done (`src/game/deck.ts` with cryptographically secure shuffle & 4-4-5 deal progression).
- [x] **2.2 Board State & Player Seat Management**
  - **Status:** Done (`src/game/board.ts` with 64 track steps, base & safe home zones).
- [x] **2.3 Special Card Rule Validator**
  - **Status:** Done (`src/game/rules.ts` with Ace/King exit, 4 backward, 7 split, Jack swap, captures).
- [x] **2.4 Turn Timer Management via Durable Object Alarms**
  - **Status:** Done (15-second alarms via `this.ctx.storage.setAlarm()` with auto-discard on timeout).
- [x] **2.5 Partner Assist & Victory Conditions**
  - **Status:** Done (partner trade, assist handover, team victory detection).

---

## Phase 3: WebSocket Protocol & State Synchronization

- [x] **3.1 Real-Time Event Message Schema**
  - **Status:** Done (`src/game/protocol.ts` typed client/server messages with hidden card masking).
- [x] **3.2 Disconnect & Reconnect State Hydration**
  - **Status:** Done (reconnecting users automatically receive full board snapshot).

---

## Phase 4: Cloudflare Calls (WebRTC Voice Signaling)

- [x] **4.1 Cloudflare Calls API Integration**
  - **Status:** Done (`src/voice.ts` with `POST /api/voice/session/new` & `POST /api/voice/tracks/new`, local simulation fallback).
- [x] **4.2 Voice State Sync over WebSocket**
  - **Status:** Done (`VOICE_MUTED`, `VOICE_SPEAKING`, and `VOICE_STATE_CHANGED` events).

---

## Phase 5: Security, Performance & Deployment

- [x] **5.1 Type Safety & Input Validation**
  - **Status:** Done (strict TypeScript compilation and JSON parse guards).
- [x] **5.2 Automated Vitest Test Suite**
  - **Status:** Done (`npm test` passes 100% across all game rules & voice sessions).
