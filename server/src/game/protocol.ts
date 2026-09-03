import { Card, GamePhase, MarblePosition, MoveAction, PlayerSeat, Team } from './types';

export type ClientMessage =
  | { type: 'JOIN'; userId: string; name: string; preferredSeat?: PlayerSeat }
  | { type: 'READY'; isReady: boolean }
  | { type: 'SWAP_CARD'; cardId: string }
  | { type: 'PLAY_CARD'; action: MoveAction }
  | { type: 'VOICE_MUTED'; isMuted: boolean }
  | { type: 'VOICE_SPEAKING'; isSpeaking: boolean }
  | { type: 'PING' };

export interface PublicPlayerInfo {
  seat: PlayerSeat;
  userId: string;
  name: string;
  isReady: boolean;
  isConnected: boolean;
  isMuted: boolean;
  isSpeaking: boolean;
  cardCount: number;
  hasSwappedCard: boolean;
  hasFinishedAllMarbles: boolean;
  voiceSessionId?: string;
}

export type ServerMessage =
  | {
      type: 'ROOM_STATE';
      roomId: string;
      phase: GamePhase;
      mySeat: PlayerSeat;
      currentTurn: PlayerSeat;
      turnDeadline: number | null; // Timestamp ms
      players: (PublicPlayerInfo | null)[];
      myHand: Card[];
      marbles: [
        [MarblePosition, MarblePosition, MarblePosition, MarblePosition],
        [MarblePosition, MarblePosition, MarblePosition, MarblePosition],
        [MarblePosition, MarblePosition, MarblePosition, MarblePosition],
        [MarblePosition, MarblePosition, MarblePosition, MarblePosition]
      ];
      lastDiscardedCard?: Card | null;
      winningTeam?: Team | null;
    }
  | {
      type: 'PLAYER_JOINED';
      player: PublicPlayerInfo;
    }
  | {
      type: 'PLAYER_LEFT';
      seat: PlayerSeat;
    }
  | {
      type: 'PHASE_CHANGED';
      phase: GamePhase;
      currentTurn?: PlayerSeat;
      turnDeadline?: number | null;
    }
  | {
      type: 'CARDS_DEALT';
      myHand: Card[];
      cardCountPerPlayer: number;
    }
  | {
      type: 'CARD_SWAPPED_RECEIVED';
      receivedCard: Card;
    }
  | {
      type: 'MOVE_PLAYED';
      player: PlayerSeat;
      card: Card;
      marbles: [
        [MarblePosition, MarblePosition, MarblePosition, MarblePosition],
        [MarblePosition, MarblePosition, MarblePosition, MarblePosition],
        [MarblePosition, MarblePosition, MarblePosition, MarblePosition],
        [MarblePosition, MarblePosition, MarblePosition, MarblePosition]
      ];
      capturedMarbles?: MarblePosition[];
      nextTurn: PlayerSeat;
      turnDeadline: number;
    }
  | {
      type: 'TURN_TIMEOUT';
      player: PlayerSeat;
      discardedCard?: Card;
      nextTurn: PlayerSeat;
      turnDeadline: number;
    }
  | {
      type: 'VOICE_STATE_CHANGED';
      seat: PlayerSeat;
      isMuted: boolean;
      isSpeaking: boolean;
    }
  | {
      type: 'GAME_OVER';
      winningTeam: Team;
    }
  | {
      type: 'ERROR';
      message: string;
    }
  | {
      type: 'PONG';
    };
