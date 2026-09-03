import { MarblePosition, PlayerSeat } from './types';

export const TRACK_LENGTH = 64; // 16 track slots per player quadrant * 4
export const SLOTS_PER_PLAYER = 16;
export const HOME_LENGTH = 4;

/**
 * Player Base Exit positions on the shared 64-slot track:
 * Player 0 (Red): 0
 * Player 1 (Blue): 16
 * Player 2 (Yellow): 32
 * Player 3 (Green): 48
 */
export function getBaseExitTrackIndex(player: PlayerSeat): number {
  return player * SLOTS_PER_PLAYER;
}

/**
 * Home Entrance on the shared 64-slot track (the step right before wrapping past base exit):
 * Player 0: 63
 * Player 1: 15
 * Player 2: 31
 * Player 3: 47
 */
export function getHomeEntranceTrackIndex(player: PlayerSeat): number {
  return (player * SLOTS_PER_PLAYER - 1 + TRACK_LENGTH) % TRACK_LENGTH;
}

/**
 * Initializes 4 starting marbles in BASE for a player
 */
export function createInitialMarbles(player: PlayerSeat): [MarblePosition, MarblePosition, MarblePosition, MarblePosition] {
  return [
    { player, marbleIndex: 0, zone: 'BASE', position: 0 },
    { player, marbleIndex: 1, zone: 'BASE', position: 1 },
    { player, marbleIndex: 2, zone: 'BASE', position: 2 },
    { player, marbleIndex: 3, zone: 'BASE', position: 3 },
  ];
}

/**
 * Checks if a player has all 4 marbles safely in HOME
 */
export function isPlayerCompleted(marbles: MarblePosition[]): boolean {
  return marbles.every((m) => m.zone === 'HOME');
}

/**
 * Checks if all marbles on a team (e.g. 0 & 2 or 1 & 3) are safely in HOME
 */
export function isTeamWinner(allMarbles: MarblePosition[][], team: 0 | 1): boolean {
  const seats: PlayerSeat[] = team === 0 ? [0, 2] : [1, 3];
  return seats.every((seat) => isPlayerCompleted(allMarbles[seat]));
}
