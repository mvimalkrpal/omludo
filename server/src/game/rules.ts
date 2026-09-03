import { Card, MarblePosition, MoveAction, MoveResult, PlayerSeat, Team } from './types';
import {
  getBaseExitTrackIndex,
  getHomeEntranceTrackIndex,
  HOME_LENGTH,
  isTeamWinner,
  TRACK_LENGTH,
} from './board';

export interface GameBoardState {
  marbles: [
    [MarblePosition, MarblePosition, MarblePosition, MarblePosition],
    [MarblePosition, MarblePosition, MarblePosition, MarblePosition],
    [MarblePosition, MarblePosition, MarblePosition, MarblePosition],
    [MarblePosition, MarblePosition, MarblePosition, MarblePosition]
  ];
}

export class JackarooRuleValidator {
  /**
   * Validates and applies a move action on the board state.
   */
  public static validateAndApplyMove(
    state: GameBoardState,
    player: PlayerSeat,
    card: Card,
    action: MoveAction,
    partnerFinished: boolean
  ): MoveResult {
    // 1. Discard action
    if (action.isDiscard) {
      return { valid: true };
    }

    const rank = card.rank;

    // 2. Ace / King Exit or Movement
    if (rank === 'A' || rank === 'K') {
      if (action.marbleIndex !== undefined) {
        const marble = state.marbles[player][action.marbleIndex];
        
        // Exiting Base
        if (marble.zone === 'BASE') {
          const exitSpot = getBaseExitTrackIndex(player);
          const captured = this.findMarbleOnTrack(state, exitSpot);

          marble.zone = 'TRACK';
          marble.position = exitSpot;

          const capturedPositions: MarblePosition[] = [];
          if (captured && (captured.player !== player || captured.marbleIndex !== marble.marbleIndex)) {
            captured.zone = 'BASE';
            captured.position = captured.marbleIndex;
            capturedPositions.push(captured);
          }

          return {
            valid: true,
            capturedMarbles: capturedPositions,
            ...this.checkWinner(state),
          };
        }
      }
    }

    // 3. Jack Swap Rule
    if (rank === 'J') {
      if (!action.jackSwap) {
        return { valid: false, reason: 'Jack requires myMarbleIndex and targetPlayer/targetMarbleIndex' };
      }
      const { myMarbleIndex, targetPlayer, targetMarbleIndex } = action.jackSwap;
      const myMarble = state.marbles[player][myMarbleIndex];
      const targetMarble = state.marbles[targetPlayer][targetMarbleIndex];

      if (myMarble.zone !== 'TRACK' || targetMarble.zone !== 'TRACK') {
        return { valid: false, reason: 'Jack can only swap marbles that are on the open track' };
      }

      // Perform swap
      const tempPos = myMarble.position;
      myMarble.position = targetMarble.position;
      targetMarble.position = tempPos;

      return {
        valid: true,
        ...this.checkWinner(state),
      };
    }

    // 4. Four (4) Move Backward 4 spots
    if (rank === '4') {
      if (action.marbleIndex === undefined) {
        return { valid: false, reason: 'Must specify marbleIndex to move' };
      }
      const marble = state.marbles[player][action.marbleIndex];
      if (marble.zone !== 'TRACK') {
        return { valid: false, reason: 'Can only move backward 4 from open track' };
      }

      const newPos = (marble.position - 4 + TRACK_LENGTH) % TRACK_LENGTH;
      const captured = this.findMarbleOnTrack(state, newPos);

      marble.position = newPos;

      const capturedPositions: MarblePosition[] = [];
      if (captured && (captured.player !== player || captured.marbleIndex !== marble.marbleIndex)) {
        captured.zone = 'BASE';
        captured.position = captured.marbleIndex;
        capturedPositions.push(captured);
      }

      return {
        valid: true,
        capturedMarbles: capturedPositions,
        ...this.checkWinner(state),
      };
    }

    // 5. Seven (7) Split Steps
    if (rank === '7') {
      if (!action.splitMoves || action.splitMoves.length === 0) {
        return { valid: false, reason: 'Card 7 requires splitMoves definition' };
      }

      const totalSteps = action.splitMoves.reduce((acc, curr) => acc + curr.steps, 0);
      if (totalSteps !== 7) {
        return { valid: false, reason: `Total split steps must equal 7 (received ${totalSteps})` };
      }

      const capturedPositions: MarblePosition[] = [];
      for (const split of action.splitMoves) {
        const marble = state.marbles[split.player][split.marbleIndex];
        const res = this.advanceMarbleForward(state, split.player, marble, split.steps);
        if (!res.valid) {
          return res;
        }
        if (res.capturedMarbles) {
          capturedPositions.push(...res.capturedMarbles);
        }
      }

      return {
        valid: true,
        capturedMarbles: capturedPositions,
        ...this.checkWinner(state),
      };
    }

    // 6. Standard Forward Moves (A=1 or 11, 2, 3, 5, 6, 8, 9, 10, Q=12, K=13)
    if (action.marbleIndex === undefined) {
      return { valid: false, reason: 'Must specify marbleIndex to move' };
    }

    let steps = 0;
    switch (rank) {
      case 'A':
        steps = action.aceChoice === 11 ? 11 : 1;
        break;
      case '2': steps = 2; break;
      case '3': steps = 3; break;
      case '5': steps = 5; break;
      case '6': steps = 6; break;
      case '8': steps = 8; break;
      case '9': steps = 9; break;
      case '10': steps = 10; break;
      case 'Q': steps = 12; break;
      case 'K': steps = 13; break;
      default:
        return { valid: false, reason: `Unsupported card rank: ${rank}` };
    }

    const marble = state.marbles[player][action.marbleIndex];
    const res = this.advanceMarbleForward(state, player, marble, steps);
    if (!res.valid) return res;

    return {
      valid: true,
      capturedMarbles: res.capturedMarbles,
      ...this.checkWinner(state),
    };
  }

  private static advanceMarbleForward(
    state: GameBoardState,
    owner: PlayerSeat,
    marble: MarblePosition,
    steps: number
  ): MoveResult {
    if (marble.zone === 'BASE') {
      return { valid: false, reason: 'Cannot advance marble forward directly from BASE' };
    }

    if (marble.zone === 'HOME') {
      const newHomePos = marble.position + steps;
      if (newHomePos >= HOME_LENGTH) {
        return { valid: false, reason: 'Overshot HOME zone steps' };
      }
      // Check if slot in HOME is already occupied
      const occupiedInHome = state.marbles[owner].some(
        (m) => m.zone === 'HOME' && m.position === newHomePos && m.marbleIndex !== marble.marbleIndex
      );
      if (occupiedInHome) {
        return { valid: false, reason: 'Target HOME slot already occupied' };
      }

      marble.position = newHomePos;
      return { valid: true };
    }

    // Marble is on TRACK:
    const entrance = getHomeEntranceTrackIndex(owner);
    const currentTrackPos = marble.position;

    // Calculate distance to entrance
    const distToEntrance = (entrance - currentTrackPos + TRACK_LENGTH) % TRACK_LENGTH;

    if (steps > distToEntrance) {
      // Entering HOME
      const homeSteps = steps - distToEntrance - 1;
      if (homeSteps >= HOME_LENGTH) {
        return { valid: false, reason: 'Overshot HOME zone steps' };
      }

      const occupiedInHome = state.marbles[owner].some(
        (m) => m.zone === 'HOME' && m.position === homeSteps
      );
      if (occupiedInHome) {
        return { valid: false, reason: 'Target HOME slot already occupied' };
      }

      marble.zone = 'HOME';
      marble.position = homeSteps;
      return { valid: true };
    }

    // Regular track advancement
    const newTrackPos = (currentTrackPos + steps) % TRACK_LENGTH;
    const captured = this.findMarbleOnTrack(state, newTrackPos);

    marble.position = newTrackPos;

    const capturedPositions: MarblePosition[] = [];
    if (captured && (captured.player !== owner || captured.marbleIndex !== marble.marbleIndex)) {
      captured.zone = 'BASE';
      captured.position = captured.marbleIndex;
      capturedPositions.push(captured);
    }

    return {
      valid: true,
      capturedMarbles: capturedPositions,
    };
  }

  private static findMarbleOnTrack(state: GameBoardState, trackPos: number): MarblePosition | null {
    for (let p = 0; p < 4; p++) {
      for (const m of state.marbles[p as PlayerSeat]) {
        if (m.zone === 'TRACK' && m.position === trackPos) {
          return m;
        }
      }
    }
    return null;
  }

  private static checkWinner(state: GameBoardState): { isWinningMove?: boolean; winningTeam?: Team } {
    if (isTeamWinner(state.marbles, 0)) {
      return { isWinningMove: true, winningTeam: 0 };
    }
    if (isTeamWinner(state.marbles, 1)) {
      return { isWinningMove: true, winningTeam: 1 };
    }
    return {};
  }
}
