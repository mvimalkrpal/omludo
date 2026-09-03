/**
 * Cloudflare Calls WebRTC Integration Service
 * Cloudflare Calls API documentation: https://developers.cloudflare.com/calls/
 */

export interface CallsSessionResponse {
  sessionId: string;
}

export interface CallsTrackRequest {
  location: 'local' | 'remote';
  mid?: string;
  trackName: string;
  sessionId?: string;
}

export interface CallsNewTracksResponse {
  sessionDescription?: {
    type: 'offer' | 'answer';
    sdp: string;
  };
  tracks?: Array<{
    location: 'local' | 'remote';
    mid: string;
    trackName: string;
    status?: 'active' | 'inactive' | 'waiting';
  }>;
}

export class CloudflareCallsService {
  private appId: string;
  private appToken: string;
  private apiBaseUrl: string;

  constructor(appId?: string, appToken?: string) {
    this.appId = appId || '';
    this.appToken = appToken || '';
    this.apiBaseUrl = `https://rtc.live.cloudflare.com/v1/apps/${this.appId}`;
  }

  /**
   * Creates a new Cloudflare Calls WebRTC Session for an audio participant
   */
  async createSession(): Promise<CallsSessionResponse> {
    if (!this.appId || !this.appToken) {
      // Return a simulated session ID in local dev mode when credentials aren't set
      return { sessionId: `simulated_cf_session_${crypto.randomUUID()}` };
    }

    const response = await fetch(`${this.apiBaseUrl}/sessions/new`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${this.appToken}`,
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const errText = await response.text();
      throw new Error(`Failed to create Cloudflare Calls session: ${response.status} ${errText}`);
    }

    return (await response.json()) as CallsSessionResponse;
  }

  /**
   * Publishes or renegotiates audio tracks for a given session
   */
  async newTracks(
    sessionId: string,
    tracks: CallsTrackRequest[],
    sessionDescription?: { type: 'offer'; sdp: string }
  ): Promise<CallsNewTracksResponse> {
    if (!this.appId || !this.appToken) {
      return {
        sessionDescription: sessionDescription
          ? { type: 'answer', sdp: 'v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=Simulated\r\nt=0 0\r\n' }
          : undefined,
        tracks: tracks.map((t, idx) => ({
          location: t.location,
          mid: t.mid || `mid_${idx}`,
          trackName: t.trackName,
          status: 'active',
        })),
      };
    }

    const response = await fetch(`${this.apiBaseUrl}/sessions/${sessionId}/tracks/new`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${this.appToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        tracks,
        sessionDescription,
      }),
    });

    if (!response.ok) {
      const errText = await response.text();
      throw new Error(`Failed to negotiate Cloudflare Calls tracks: ${response.status} ${errText}`);
    }

    return (await response.json()) as CallsNewTracksResponse;
  }

  /**
   * Closes an audio track or session
   */
  async closeSession(sessionId: string): Promise<void> {
    if (!this.appId || !this.appToken) return;

    await fetch(`${this.apiBaseUrl}/sessions/${sessionId}`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${this.appToken}`,
      },
    });
  }
}
