import { describe, expect, it } from 'vitest';
import { createInitialMarbles } from '../../src/game/board';
import { DeckManager } from '../../src/game/deck';
import { GameBoardState, JackarooRuleValidator } from '../../src/game/rules';
import { Card } from '../../src/game/types';

describe('Jackaroo Deck and Dealing', () => {
  it('should initialize and deal 4-4-5 card distribution correctly', () => {
    const deck = new DeckManager();
    expect(deck.remainingCount).toBe(52);

    const round1 = deck.deal(4);
    expect(round1.length).toBe(4);
    expect(round1[0].length).toBe(4);
    expect(deck.remainingCount).toBe(36);

    const round2 = deck.deal(4);
    expect(round2[0].length).toBe(4);
    expect(deck.remainingCount).toBe(20);

    const round3 = deck.deal(5);
    expect(round3[0].length).toBe(5);
    expect(deck.remainingCount).toBe(0);
  });
});

describe('Jackaroo Move Rules', () => {
  function getFreshBoard(): GameBoardState {
    return {
      marbles: [
        createInitialMarbles(0),
        createInitialMarbles(1),
        createInitialMarbles(2),
        createInitialMarbles(3),
      ],
    };
  }

  it('allows Ace or King to exit Base to Track', () => {
    const board = getFreshBoard();
    const aceCard: Card = { id: 'HEARTS_A', suit: 'HEARTS', rank: 'A' };

    const result = JackarooRuleValidator.validateAndApplyMove(
      board,
      0,
      aceCard,
      { player: 0, cardId: 'HEARTS_A', marbleIndex: 0 },
      false
    );

    expect(result.valid).toBe(true);
    expect(board.marbles[0][0].zone).toBe('TRACK');
    expect(board.marbles[0][0].position).toBe(0); // Exit for Player 0 is 0
  });

  it('allows 4 to move backward 4 steps on Track', () => {
    const board = getFreshBoard();
    board.marbles[0][0].zone = 'TRACK';
    board.marbles[0][0].position = 2; // Starts at 2

    const fourCard: Card = { id: 'SPADES_4', suit: 'SPADES', rank: '4' };

    const result = JackarooRuleValidator.validateAndApplyMove(
      board,
      0,
      fourCard,
      { player: 0, cardId: 'SPADES_4', marbleIndex: 0 },
      false
    );

    expect(result.valid).toBe(true);
    // (2 - 4 + 64) % 64 = 62
    expect(board.marbles[0][0].position).toBe(62);
  });

  it('allows Jack to swap positions between two marbles on the track', () => {
    const board = getFreshBoard();
    board.marbles[0][0].zone = 'TRACK';
    board.marbles[0][0].position = 10;

    board.marbles[1][0].zone = 'TRACK';
    board.marbles[1][0].position = 45;

    const jackCard: Card = { id: 'CLUBS_J', suit: 'CLUBS', rank: 'J' };

    const result = JackarooRuleValidator.validateAndApplyMove(
      board,
      0,
      jackCard,
      {
        player: 0,
        cardId: 'CLUBS_J',
        jackSwap: {
          myMarbleIndex: 0,
          targetPlayer: 1,
          targetMarbleIndex: 0,
        },
      },
      false
    );

    expect(result.valid).toBe(true);
    expect(board.marbles[0][0].position).toBe(45);
    expect(board.marbles[1][0].position).toBe(10);
  });

  it('captures opponent marble when landing on their spot', () => {
    const board = getFreshBoard();
    board.marbles[0][0].zone = 'TRACK';
    board.marbles[0][0].position = 10;

    board.marbles[1][0].zone = 'TRACK';
    board.marbles[1][0].position = 15;

    const fiveCard: Card = { id: 'DIAMONDS_5', suit: 'DIAMONDS', rank: '5' };

    const result = JackarooRuleValidator.validateAndApplyMove(
      board,
      0,
      fiveCard,
      { player: 0, cardId: 'DIAMONDS_5', marbleIndex: 0 },
      false
    );

    expect(result.valid).toBe(true);
    expect(board.marbles[0][0].position).toBe(15);
    // Player 1 marble was captured and sent back to BASE
    expect(board.marbles[1][0].zone).toBe('BASE');
    expect(result.capturedMarbles?.length).toBe(1);
  });
});
