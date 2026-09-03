import { GameRoom } from './room';
import { CloudflareCallsService } from './voice';

export { GameRoom };

export interface Env {
  GAME_ROOM: DurableObjectNamespace<GameRoom>;
  ROOM_KV?: KVNamespace;
  CF_CALLS_APP_ID?: string;
  CF_CALLS_APP_TOKEN?: string;
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);

    // CORS Headers for API calls
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    };

    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    // 1. Health check
    if (url.pathname === '/' || url.pathname === '/health') {
      return new Response(JSON.stringify({ status: 'healthy', time: Date.now() }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }

    // 2. Room Code Creation: POST /api/rooms/create
    if (url.pathname === '/api/rooms/create' && request.method === 'POST') {
      const roomCode = Math.floor(100000 + Math.random() * 900000).toString(); // 6-digit code
      const roomId = env.GAME_ROOM.newUniqueId().toString();

      if (env.ROOM_KV) {
        // Expire room code after 2 hours
        await env.ROOM_KV.put(`room:${roomCode}`, roomId, { expirationTtl: 7200 });
      }

      return new Response(JSON.stringify({ roomCode, roomId }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }

    // 3. Room Code Lookup: GET /api/rooms/:roomCode
    if (url.pathname.startsWith('/api/rooms/') && request.method === 'GET') {
      const roomCode = url.pathname.split('/')[3];
      let roomId = roomCode;

      if (env.ROOM_KV) {
        const mappedId = await env.ROOM_KV.get(`room:${roomCode}`);
        if (!mappedId) {
          return new Response(JSON.stringify({ error: 'Room not found or expired' }), {
            status: 404,
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          });
        }
        roomId = mappedId;
      }

      return new Response(JSON.stringify({ roomCode, roomId }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }

    // 4. Voice Session Creation: POST /api/voice/session/new
    if (url.pathname === '/api/voice/session/new' && request.method === 'POST') {
      const callsService = new CloudflareCallsService(env.CF_CALLS_APP_ID, env.CF_CALLS_APP_TOKEN);
      try {
        const session = await callsService.createSession();
        return new Response(JSON.stringify(session), {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        });
      } catch (err: any) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        });
      }
    }

    // 5. Voice Track Negotiation: POST /api/voice/tracks/new
    if (url.pathname === '/api/voice/tracks/new' && request.method === 'POST') {
      const callsService = new CloudflareCallsService(env.CF_CALLS_APP_ID, env.CF_CALLS_APP_TOKEN);
      try {
        const body: any = await request.json();
        const response = await callsService.newTracks(body.sessionId, body.tracks, body.sessionDescription);
        return new Response(JSON.stringify(response), {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        });
      } catch (err: any) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        });
      }
    }

    // 6. WebSocket Game Room Connection: /ws/room/:roomId
    if (url.pathname.startsWith('/ws/room/')) {
      const roomId = url.pathname.split('/')[3];
      if (!roomId) {
        return new Response('Room ID is required', { status: 400 });
      }

      const id = env.GAME_ROOM.idFromString(roomId.length === 64 ? roomId : env.GAME_ROOM.newUniqueId().toString());
      const roomObject = env.GAME_ROOM.get(id);

      return roomObject.fetch(request);
    }

    return new Response('Not Found', { status: 404, headers: corsHeaders });
  },
};
