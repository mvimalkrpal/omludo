import { describe, expect, it } from 'vitest';
import { CloudflareCallsService } from '../../src/voice';

describe('Cloudflare Calls Service', () => {
  it('should generate simulated session in local/mock dev mode', async () => {
    const service = new CloudflareCallsService(); // No tokens provided
    const session = await service.createSession();
    expect(session.sessionId).toContain('simulated_cf_session_');
  });

  it('should simulate track negotiation when no tokens are configured', async () => {
    const service = new CloudflareCallsService();
    const result = await service.newTracks(
      'simulated_session_1',
      [{ location: 'local', trackName: 'audio-in' }],
      { type: 'offer', sdp: 'v=0\r\n' }
    );

    expect(result.sessionDescription?.type).toBe('answer');
    expect(result.tracks?.length).toBe(1);
    expect(result.tracks?.[0].trackName).toBe('audio-in');
  });
});
