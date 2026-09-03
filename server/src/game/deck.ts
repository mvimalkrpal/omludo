import { Card, Rank, Suit } from './types';

export const SUITS: Suit[] = ['HEARTS', 'DIAMONDS', 'CLUBS', 'SPADES'];
export const RANKS: Rank[] = ['A', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K'];

export function createDeck(): Card[] {
  const deck: Card[] = [];
  for (const suit of SUITS) {
    for (const rank of RANKS) {
      deck.push({
        id: `${suit}_${rank}`,
        suit,
        rank,
      });
    }
  }
  return deck;
}

/**
 * Cryptographically secure Fisher-Yates shuffle
 */
export function shuffleDeck(deck: Card[]): Card[] {
  const shuffled = [...deck];
  const randomBuffer = new Uint32Array(shuffled.length);
  crypto.getRandomValues(randomBuffer);

  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = randomBuffer[i] % (i + 1);
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }
  return shuffled;
}

export class DeckManager {
  private cards: Card[] = [];

  constructor() {
    this.reset();
  }

  public reset(): void {
    this.cards = shuffleDeck(createDeck());
  }

  public get remainingCount(): number {
    return this.cards.length;
  }

  /**
   * Deals cards to 4 players for a given round deal count (4, 4, or 5)
   */
  public deal(countPerPlayer: 4 | 5): [Card[], Card[], Card[], Card[]] {
    if (this.cards.length < countPerPlayer * 4) {
      this.reset();
    }

    const hands: [Card[], Card[], Card[], Card[]] = [[], [], [], []];
    for (let c = 0; c < countPerPlayer; c++) {
      for (let p = 0; p < 4; p++) {
        const card = this.cards.pop();
        if (!card) throw new Error('Unexpected empty deck during deal');
        hands[p].push(card);
      }
    }
    return hands;
  }
}
