# Project Roadmap & Implementation Status: Jackaroo with Voice Chat

**Architecture:** Android (Native Kotlin) + Cloudflare Workers + Durable Objects + Cloudflare Calls RTC  
**Live Backend URL:** `https://omludo.mvimalkrpal.workers.dev`  
**WebSocket Gateway:** `wss://omludo.mvimalkrpal.workers.dev/ws/room/:roomId`

---

## Phase 1: Architecture & Server Infrastructure (100% COMPLETE)
- [x] Cloudflare Worker & Durable Objects Setup (`server/src/room.ts`, `server/src/index.ts`)
- [x] Cloudflare KV Namespace integration for 6-digit room code lookup
- [x] Deployed and verified on live Cloudflare production (`/health` and `/api/rooms/create`)

## Phase 2: Jackaroo Game Engine & Rules (100% COMPLETE)
- [x] 52-card deck with Fisher-Yates shuffle & 4-4-5 round dealing
- [x] Full card mechanics: Ace/King exits, 4 backward, 7 split, Jack swap, captures
- [x] 15-second turn timeouts via Durable Object Alarms (`this.ctx.storage.setAlarm`)
- [x] Partner card trading phase and assist handover
- [x] 100% test pass on Vitest test suite

## Phase 3: Android UI & Networking (IN PROGRESS)
- [x] Kotlin data models for Game State, Cards, Marbles, and Events (`GameModels.kt`)
- [x] Real-time OkHttp WebSocket client (`GameSocketClient.kt`)
- [x] Custom Jackaroo Canvas Board Renderer (`JackarooBoardView.kt`)
- [x] Main Game Activity with Hand Cards & Turn HUD (`MainActivity.kt`, `activity_main.xml`)
- [ ] In-game Join/Lobby modal for entering 6-digit codes
- [ ] Audio controls (mic mute / speaking wave indicators)

## Phase 4: Voice Chat (Cloudflare Calls RTC)
- [x] Backend session negotiation (`server/src/voice.ts`)
- [ ] Android WebRTC audio stream bridge
