import { Card, MarblePosition, MoveAction, PlayerSeat } from './types';
import { GameBoardState, JackarooRuleValidator } from './rules';

export class JackarooBot {
  /**
   * Evaluates all playable cards in hand and marbles to select the best legal move.
   */
  public static selectBestMove(
    seat: PlayerSeat,
    hand: Card[],
    board: GameBoardState,
    partnerFinished: boolean
  ): { action: MoveAction; cardId: string } | null {
    if (hand.length === 0) return null;

    const myMarbles = board.marbles[seat];

    // 1. Try to Exit Base with Ace or King first
    const baseMarbleIndex = myMarbles.findIndex((m) => m.zone === 'BASE');
    if (baseMarbleIndex !== -1) {
      const exitCard = hand.find((c) => c.rank === 'A' || c.rank === 'K');
      if (exitCard) {
        const action: MoveAction = {
          player: seat,
          cardId: exitCard.id,
          marbleIndex: baseMarbleIndex as 0 | 1 | 2 | 3,
        };
        const validation = JackarooRuleValidator.validateAndApplyMove(
          this.cloneBoard(board),
          seat,
          exitCard,
          action,
          partnerFinished
        );
        if (validation.valid) {
          return { action, cardId: exitCard.id };
        }
      }
    }

    // 2. Try to Advance marbles on Track or Home
    for (const card of hand) {
      // Try regular move on any marble
      for (let mIdx = 0; mIdx < 4; mIdx++) {
        const marble = myMarbles[mIdx];
        if (marble.zone === 'BASE') continue;

        const action: MoveAction = {
          player: seat,
          cardId: card.id,
          marbleIndex: mIdx as 0 | 1 | 2 | 3,
          aceChoice: 11,
        };

        if (card.rank === 'J') {
          // Find any opponent marble on track to swap
          const opponentSeats = seat % 2 === 0 ? [1, 3] : [0, 2];
          for (const oppSeat of opponentSeats) {
            for (let oppIdx = 0; oppIdx < 4; oppIdx++) {
              if (board.marbles[oppSeat as PlayerSeat][oppIdx].zone === 'TRACK') {
                action.jackSwap = {
                  myMarbleIndex: mIdx as 0 | 1 | 2 | 3,
                  targetPlayer: oppSeat as PlayerSeat,
                  targetMarbleIndex: oppIdx as 0 | 1 | 2 | 3,
                };
                break;
              }
            }
            if (action.jackSwap) break;
          }
        }

        const validation = JackarooRuleValidator.validateAndApplyMove(
          this.cloneBoard(board),
          seat,
          card,
          action,
          partnerFinished
        );

        if (validation.valid) {
          return { action, cardId: card.id };
        }
      }
    }

    // 3. Fallback: Discard the first card
    const discardCard = hand[0];
    return {
      action: {
        player: seat,
        cardId: discardCard.id,
        isDiscard: true,
      },
      cardId: discardCard.id,
    };
  }

  private static cloneBoard(board: GameBoardState): GameBoardState {
    return {
      marbles: [
        board.marbles[0].map((m) => ({ ...m })) as any,
        board.marbles[1].map((m) => ({ ...m })) as any,
        board.marbles[2].map((m) => ({ ...m })) as any,
        board.marbles[3].map((m) => ({ ...m })) as any,
      ],
    };
  }
}
