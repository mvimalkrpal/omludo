export type Suit = 'HEARTS' | 'DIAMONDS' | 'CLUBS' | 'SPADES';

export type Rank = 
  | 'A' 
  | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '10' 
  | 'J' | 'Q' | 'K';

export interface Card {
  id: string; // e.g. "HEARTS_A", "SPADES_7"
  suit: Suit;
  rank: Rank;
}

export type PlayerSeat = 0 | 1 | 2 | 3;
export type Team = 0 | 1; // Team 0 = Seats (0, 2), Team 1 = Seats (1, 3)

export type MarbleZone = 'BASE' | 'TRACK' | 'HOME';

export interface MarblePosition {
  player: PlayerSeat;
  marbleIndex: 0 | 1 | 2 | 3;
  zone: MarbleZone;
  /**
   * - BASE: 0..3 (slot in base)
   * - TRACK: 0..63 (shared circular board index)
   * - HOME: 0..3 (safety zone steps leading to goal)
   */
  position: number;
}

export type RoundDealCount = 4 | 4 | 5; // 4, then 4, then 5 (total 13 per player = 52 card deck)

export type GamePhase = 
  | 'WAITING_FOR_PLAYERS'
  | 'PARTNER_SWAP'
  | 'PLAYING'
  | 'ROUND_ENDED'
  | 'GAME_OVER';

export interface PlayerState {
  seat: PlayerSeat;
  userId: string;
  name: string;
  isReady: boolean;
  isConnected: boolean;
  isMuted: boolean;
  isSpeaking: boolean;
  voiceSessionId?: string;
  hand: Card[];
  swappedCard: Card | null; // Card selected to pass to partner
  marbles: [MarblePosition, MarblePosition, MarblePosition, MarblePosition];
  hasFinishedAllMarbles: boolean;
}

export interface MoveAction {
  player: PlayerSeat;
  cardId: string;
  marbleIndex?: 0 | 1 | 2 | 3;
  aceChoice?: 1 | 11;
  splitMoves?: Array<{
    marbleIndex: 0 | 1 | 2 | 3;
    player: PlayerSeat;
    steps: number;
  }>;
  jackSwap?: {
    myMarbleIndex: 0 | 1 | 2 | 3;
    targetPlayer: PlayerSeat;
    targetMarbleIndex: 0 | 1 | 2 | 3;
  };
  isDiscard?: boolean;
}

export interface MoveResult {
  valid: boolean;
  reason?: string;
  capturedMarbles?: MarblePosition[];
  isWinningMove?: boolean;
  winningTeam?: Team;
}
