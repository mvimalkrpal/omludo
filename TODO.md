# Project Roadmap & Implementation Tasks: Jackaroo with Real-Time Voice Chat (Cloudflare Stack)

**Project Target:** Android (Native Kotlin) + Cloudflare Edge Backend + Cloudflare Calls RTC  
**Objective:** Deliver a clean, responsive, 4-player online Jackaroo board game (2v2 partnership & 4-player FFA) with low-latency in-game voice chat using a 100% Cloudflare serverless architecture.

---

## Architecture Overview (100% Cloudflare Stack)

| Layer | Service | Purpose |
| :--- | :--- | :--- |
| **Game Room & Real-time State** | **Cloudflare Workers + Durable Objects** | Persistent in-memory game state, WebSocket connections, turn timers, move validation. |
| **Voice Chat Engine** | **Cloudflare Calls (WebRTC)** | Serverless real-time SFU voice channels (Echo cancellation, low latency, mute/talk states). |
| **Auth & Profiles (Optional)** | **Cloudflare D1 (SQL) / KV** | Player usernames, stats, and session management. |
| **Mobile Client** | **Android Native (Kotlin / Jetpack)** | Canvas/Compose UI, WebSocket Client, WebRTC Audio Engine. |

---

## Phase 1: Cloudflare Backend & Architecture Setup

- [ ] **1.1 Cloudflare Worker & Durable Objects Setup**
  - **Task:** Initialize `server/` with Wrangler CLI, TypeScript, and Durable Object bindings for `GameRoom`.
  - **Acceptance Criteria:** Worker deploys to Cloudflare edge; clients can establish persistent WebSocket connections to distinct room IDs.

- [ ] **1.2 Cloudflare Calls (WebRTC Voice) Integration**
  - **Task:** Configure Cloudflare Calls API credentials, implement session creation endpoints (`POST /api/voice/session`), and exchange WebRTC SDP tracks.
  - **Acceptance Criteria:** Worker issues WebRTC peer connection offers/answers for 4-player audio mesh/SFU room.

- [ ] **1.3 Android App Architecture & Network Client**
  - **Task:** Configure Android project dependencies (OkHttp / Ktor WebSockets, Google WebRTC / Cloudflare Calls SDK, Coroutines, StateFlow).
  - **Acceptance Criteria:** Android client connects to Cloudflare Worker WebSocket and handles reconnection handshakes cleanly.

---

## Phase 2: Jackaroo Core Game Engine (Durable Object Logic)

> *Rule Note:* Jackaroo is played with a standard 52-card deck (no Jokers), 4 players (2 teams of 2), 4 marbles/pegs per player, 64-step circular track + 4 safety/home slots each.

- [ ] **2.1 Card System & Deck Management**
  - **Task:** Implement card model, deterministic deck shuffling, dealing rounds (4, 4, 5 cards per hand), and partner card swap phase.
  - **Acceptance Criteria:** In-memory deck state held securely inside Durable Object; players only receive their own hand over WebSocket.

- [ ] **2.2 Special Card Rules & Move Validator**
  - **Task:** Implement strict move verification:
    - **Ace / King:** Exit base or advance (Ace: 1 or 11; King: 13 & capture).
    - **Four (4):** Move backward 4 steps.
    - **Seven (7):** Split 7 steps across up to 2 marbles.
    - **Jack (J):** Swap any player marble with any other marble on open track.
    - **Queen / Ten / Number Cards:** Standard forward movement.
    - **Capture / Kill:** Send occupied marble back to opponent base.
  - **Acceptance Criteria:** 100% unit test coverage for card actions, invalid move rejections, and base protection rules.

- [ ] **2.3 Durable Object Alarm Turn Timers & Partner Assist**
  - **Task:** Use Cloudflare Durable Object Alarms (`this.ctx.storage.setAlarm()`) for accurate 15s turn timeouts, auto-discarding unplayable hands, and partner marble control once a player finishes.
  - **Acceptance Criteria:** Server automatically advances turns on timeout without needing external cron jobs or persistent servers.

- [ ] **2.4 Victory Conditions & Game Over**
  - **Task:** Detect when a team safely places all 8 marbles into their home slots and broadcast game summary payload.
  - **Acceptance Criteria:** Match concludes, updates player records, and smoothly closes room state.

---

## Phase 3: Android UI / UX & Game Board Rendering

- [ ] **3.1 Jackaroo Board Canvas / Custom View**
  - **Task:** Build custom interactive board renderer for 64 track nodes, 4 home paths, 4 base slots, and 4 player color themes (Red, Blue, Yellow, Green).
  - **Acceptance Criteria:** Scalable vector-drawn board supporting multiple screen densities and tablet aspect ratios.

- [ ] **3.2 Marble Animations & Path Transitions**
  - **Task:** Smooth interpolated marble movement along track paths, backward step animations (4), and swap transitions (Jack).
  - **Acceptance Criteria:** 60 FPS fluid marble movements with capture bounce animations.

- [ ] **3.3 Player Hand & Card Interaction UI**
  - **Task:** Card carousel/hand UI with drag-and-drop or tap selection, highlighting valid target marbles upon card tap.
  - **Acceptance Criteria:** Clear visual highlights for playable moves; haptic vibration on invalid target attempts.

- [ ] **3.4 In-Game HUD & Voice Indicators**
  - **Task:** Player avatars, circular turn timer depletion, team badges, mic mute toggle button, and animated speaker audio-wave rings.
  - **Acceptance Criteria:** Active player and active speaker states are clearly visible at a glance.

---

## Phase 4: Real-Time Networking & Multiplayer Room Flow

- [ ] **4.1 Matchmaking & 6-Digit Private Room Codes**
  - **Task:** Implement room creation, 6-digit code lookup in Cloudflare KV/D1, and seat assignment (Team 1: Seats 0 & 2; Team 2: Seats 1 & 3).
  - **Acceptance Criteria:** Host can share room code, friends join seamlessly, and game launches when all 4 seats are ready.

- [ ] **4.2 WebSocket Message Protocol**
  - **Task:** Define compact JSON/Binary event schemas: `JOIN_ROOM`, `SWAP_CARD`, `PLAY_CARD`, `MARBLE_MOVED`, `TURN_CHANGE`, `PLAYER_SPEAKING`.
  - **Acceptance Criteria:** Sub-100ms message delivery between client and Cloudflare Edge Durable Object.

- [ ] **4.3 Disconnect Grace Period & State Sync**
  - **Task:** If an app drops connection, hold player slot for 30s. When reconnected, deliver snapshot of current board and hand.
  - **Acceptance Criteria:** Client seamlessly resumes match without board desync or app crash.

---

## Phase 5: Voice Chat (Cloudflare Calls WebRTC Integration)

- [ ] **5.1 Cloudflare Calls Android WebRTC Client**
  - **Task:** Integrate WebRTC audio track streaming via Cloudflare Calls SFU API endpoints.
  - **Acceptance Criteria:** 4 players in the same room can talk and hear each other with low latency (<200ms) and acoustic echo cancellation.

- [ ] **5.2 Voice Controls & Runtime Permissions**
  - **Task:** Mic Mute/Unmute toggle, Speaker on/off, individual player volume adjustments, and Android `RECORD_AUDIO` permission flow.
  - **Acceptance Criteria:** Permission request handled gracefully; clear mic status indicator.

- [ ] **5.3 Audio Focus & Interruption Handling**
  - **Task:** Handle Android `AudioFocusRequest` when receiving phone calls, notifications, or switching between Bluetooth / speakerphone.
  - **Acceptance Criteria:** Voice chat properly pauses on phone call and resumes automatically when call ends.

---

## Phase 6: QA, Polish & Release

- [ ] **6.1 Load & Latency Testing**
  - **Task:** Simulate concurrent Durable Object game rooms and test performance under jitter and packet loss.
  - **Acceptance Criteria:** No state corruption under high concurrency; edge latency remains <100ms globally.

- [ ] **6.2 SFX & Haptics Polish**
  - **Task:** Sound effects for card flips, marble hops, capture sounds, timer alert countdown, and victory jingle.
  - **Acceptance Criteria:** Crisp audio cues that mix cleanly without distorting voice chat.

- [ ] **6.3 Release Build & Play Store Readiness**
  - **Task:** ProGuard/R8 optimization, Signed AAB generation, and Google Play microphone usage policy compliance.
  - **Acceptance Criteria:** Production APK/AAB tested and passing Google Play pre-launch health checks.
