import { DurableObject } from 'cloudflare:workers';
import { createInitialMarbles } from './game/board';
import { JackarooBot } from './game/bot';
import { DeckManager } from './game/deck';
import { ClientMessage, PublicPlayerInfo, ServerMessage } from './game/protocol';
import { GameBoardState, JackarooRuleValidator } from './game/rules';
import { Card, GamePhase, MoveAction, PlayerSeat, PlayerState, RoundDealCount, Team } from './game/types';

export const TURN_DURATION_MS = 15000;
export const BOT_THINK_DELAY_MS = 1200; // 1.2s delay for natural bot moves

interface SessionData {
  seat: PlayerSeat;
  userId: string;
}

export class GameRoom extends DurableObject {
  private roomId: string = '';
  private phase: GamePhase = 'WAITING_FOR_PLAYERS';
  private deckManager = new DeckManager();
  private currentTurn: PlayerSeat = 0;
  private turnDeadline: number | null = null;
  private dealRoundIndex: 0 | 1 | 2 = 0;
  private winningTeam: Team | null = null;
  private lastDiscardedCard: Card | null = null;

  private players: (PlayerState | null)[] = [null, null, null, null];
  private boardState: GameBoardState = {
    marbles: [
      createInitialMarbles(0),
      createInitialMarbles(1),
      createInitialMarbles(2),
      createInitialMarbles(3),
    ],
  };

  private sessions = new Map<WebSocket, SessionData>();

  constructor(ctx: DurableObjectState, env: any) {
    super(ctx, env);
  }

  async fetch(request: Request): Promise<Response> {
    if (request.headers.get('Upgrade') !== 'websocket') {
      return new Response('Expected WebSocket upgrade', { status: 426 });
    }

    const pair = new WebSocketPair();
    const [client, server] = Object.values(pair);

    this.ctx.acceptWebSocket(server);

    return new Response(null, {
      status: 101,
      webSocket: client,
    });
  }

  async webSocketMessage(ws: WebSocket, message: string | ArrayBuffer) {
    if (typeof message !== 'string') return;

    let parsed: any;
    try {
      parsed = JSON.parse(message);
    } catch {
      this.sendError(ws, 'Invalid JSON payload');
      return;
    }

    if (parsed.type === 'PING') {
      this.send(ws, { type: 'PONG' });
      return;
    }

    if (parsed.type === 'JOIN') {
      this.handleJoin(ws, parsed.userId, parsed.name, parsed.preferredSeat);
      return;
    }

    if (parsed.type === 'ADD_BOTS') {
      this.fillWithBots();
      return;
    }

    const session = this.sessions.get(ws);
    if (!session) {
      this.sendError(ws, 'Must send JOIN before other actions');
      return;
    }

    const seat = session.seat;

    switch (parsed.type) {
      case 'READY':
        this.handleReady(seat, parsed.isReady);
        break;
      case 'SWAP_CARD':
        this.handleCardSwap(seat, parsed.cardId);
        break;
      case 'PLAY_CARD':
        this.handlePlayCard(seat, parsed.action);
        break;
      case 'VOICE_MUTED':
        this.handleVoiceMuted(seat, parsed.isMuted);
        break;
      case 'VOICE_SPEAKING':
        this.handleVoiceSpeaking(seat, parsed.isSpeaking);
        break;
    }
  }

  async webSocketClose(ws: WebSocket) {
    const session = this.sessions.get(ws);
    if (!session) return;

    this.sessions.delete(ws);
    const player = this.players[session.seat];
    if (player && !player.userId.startsWith('bot_')) {
      player.isConnected = false;
      this.broadcast({ type: 'PLAYER_LEFT', seat: session.seat });
    }
  }

  async alarm() {
    if (this.phase !== 'PLAYING' || this.turnDeadline === null) return;

    const activePlayer = this.players[this.currentTurn];
    if (!activePlayer) return;

    // Check if current player is a Bot or timed-out Human
    if (activePlayer.userId.startsWith('bot_')) {
      this.executeBotMove(this.currentTurn);
      return;
    }

    // Human player timed out: auto-discard
    const discarded = activePlayer.hand.shift();
    this.lastDiscardedCard = discarded || null;

    const nextTurn = this.getNextTurnSeat(this.currentTurn);
    this.currentTurn = nextTurn;
    this.turnDeadline = Date.now() + TURN_DURATION_MS;
    await this.ctx.storage.setAlarm(this.turnDeadline);

    this.broadcast({
      type: 'TURN_TIMEOUT',
      player: activePlayer.seat,
      discardedCard: discarded,
      nextTurn: this.currentTurn,
      turnDeadline: this.turnDeadline,
    });

    this.triggerBotTurnIfNeeded();
  }

  // --- Bot Filling & Execution ---

  public fillWithBots() {
    const botNames = ['Sufyan (Bot)', 'Tariq (Bot)', 'Fahad (Bot)', 'Zaid (Bot)'];

    for (let seat = 0; seat < 4; seat++) {
      if (this.players[seat] === null) {
        const pSeat = seat as PlayerSeat;
        this.players[pSeat] = {
          seat: pSeat,
          userId: `bot_${pSeat}`,
          name: botNames[pSeat],
          isReady: true,
          isConnected: true,
          isMuted: true,
          isSpeaking: false,
          hand: [],
          swappedCard: null,
          marbles: this.boardState.marbles[pSeat],
          hasFinishedAllMarbles: false,
        };
      }
    }

    this.broadcastRoomState();

    // Check if everyone is ready to start
    const allReady = this.players.every((p) => p !== null && p.isReady);
    if (allReady && this.phase === 'WAITING_FOR_PLAYERS') {
      this.startNewRound();
    }
  }

  private executeBotMove(seat: PlayerSeat) {
    const player = this.players[seat];
    if (!player || player.hand.length === 0) {
      this.advanceTurn();
      return;
    }

    const partnerSeat = ((seat + 2) % 4) as PlayerSeat;
    const partnerFinished = this.players[partnerSeat]?.hasFinishedAllMarbles ?? false;

    const decision = JackarooBot.selectBestMove(seat, player.hand, this.boardState, partnerFinished);
    if (decision) {
      this.handlePlayCard(seat, decision.action);
    } else {
      this.advanceTurn();
    }
  }

  private triggerBotTurnIfNeeded() {
    const activePlayer = this.players[this.currentTurn];
    if (activePlayer && activePlayer.userId.startsWith('bot_')) {
      // Schedule bot move after short realistic think delay
      this.ctx.storage.setAlarm(Date.now() + BOT_THINK_DELAY_MS);
    }
  }

  // --- Game Handlers ---

  private handleJoin(ws: WebSocket, userId: string, name: string, preferredSeat?: PlayerSeat) {
    let seat: number = -1;

    const existingIndex = this.players.findIndex((p) => p && p.userId === userId);
    if (existingIndex !== -1) {
      seat = existingIndex;
      this.players[seat as PlayerSeat]!.isConnected = true;
      this.players[seat as PlayerSeat]!.name = name;
    } else {
      if (preferredSeat !== undefined && this.players[preferredSeat] === null) {
        seat = preferredSeat;
      } else {
        seat = this.players.findIndex((p) => p === null);
      }
    }

    if (seat === -1 || seat < 0 || seat > 3) {
      this.sendError(ws, 'Room is full');
      return;
    }

    const playerSeat = seat as PlayerSeat;

    if (!this.players[playerSeat]) {
      this.players[playerSeat] = {
        seat: playerSeat,
        userId,
        name,
        isReady: false,
        isConnected: true,
        isMuted: false,
        isSpeaking: false,
        hand: [],
        swappedCard: null,
        marbles: this.boardState.marbles[playerSeat],
        hasFinishedAllMarbles: false,
      };
    }

    this.sessions.set(ws, { seat: playerSeat, userId });
    this.send(ws, this.buildRoomStateMessage(playerSeat));
    this.broadcast({
      type: 'PLAYER_JOINED',
      player: this.getPublicPlayerInfo(this.players[playerSeat]!),
    });
  }

  private handleReady(seat: PlayerSeat, isReady: boolean) {
    const player = this.players[seat];
    if (!player || this.phase !== 'WAITING_FOR_PLAYERS') return;

    player.isReady = isReady;

    // Automatically fill remaining empty seats with Bots so the game can start immediately!
    this.fillWithBots();

    this.broadcastRoomState();

    const allReady = this.players.every((p) => p !== null && p.isReady);
    if (allReady) {
      this.startNewRound();
    }
  }

  private startNewRound() {
    const counts: RoundDealCount[] = [4, 4, 5];
    const dealCount = counts[this.dealRoundIndex];
    const dealtHands = this.deckManager.deal(dealCount);

    for (let i = 0; i < 4; i++) {
      this.players[i]!.hand = dealtHands[i];
      this.players[i]!.swappedCard = null;

      // If Bot, automatically pick first card to swap with partner
      if (this.players[i]!.userId.startsWith('bot_') && this.players[i]!.hand.length > 0) {
        this.players[i]!.swappedCard = this.players[i]!.hand.shift() || null;
      }
    }

    this.phase = 'PARTNER_SWAP';
    this.broadcast({
      type: 'PHASE_CHANGED',
      phase: 'PARTNER_SWAP',
    });

    for (const [ws, session] of this.sessions.entries()) {
      const p = this.players[session.seat];
      if (p) {
        this.send(ws, {
          type: 'CARDS_DEALT',
          myHand: p.hand,
          cardCountPerPlayer: dealCount,
        });
      }
    }

    // Check if all players (including bots) are ready with swaps
    const allSwapped = this.players.every((p) => p && p.swappedCard !== null);
    if (allSwapped) {
      this.executePartnerSwaps();
    }
  }

  private handleCardSwap(seat: PlayerSeat, cardId: string) {
    if (this.phase !== 'PARTNER_SWAP') return;

    const player = this.players[seat];
    if (!player) return;

    const cardIndex = player.hand.findIndex((c) => c.id === cardId);
    if (cardIndex === -1) return;

    player.swappedCard = player.hand.splice(cardIndex, 1)[0];
    this.broadcastRoomState();

    const allSwapped = this.players.every((p) => p && p.swappedCard !== null);
    if (allSwapped) {
      this.executePartnerSwaps();
    }
  }

  private executePartnerSwaps() {
    const partnerPairs: [PlayerSeat, PlayerSeat][] = [
      [0, 2],
      [1, 3],
    ];

    for (const [p1, p2] of partnerPairs) {
      const card1 = this.players[p1]!.swappedCard!;
      const card2 = this.players[p2]!.swappedCard!;

      this.players[p1]!.hand.push(card2);
      this.players[p2]!.hand.push(card1);

      this.players[p1]!.swappedCard = null;
      this.players[p2]!.swappedCard = null;

      this.sendToSeat(p1, { type: 'CARD_SWAPPED_RECEIVED', receivedCard: card2 });
      this.sendToSeat(p2, { type: 'CARD_SWAPPED_RECEIVED', receivedCard: card1 });
    }

    this.phase = 'PLAYING';
    this.turnDeadline = Date.now() + TURN_DURATION_MS;
    this.ctx.storage.setAlarm(this.turnDeadline);

    this.broadcast({
      type: 'PHASE_CHANGED',
      phase: 'PLAYING',
      currentTurn: this.currentTurn,
      turnDeadline: this.turnDeadline,
    });

    this.triggerBotTurnIfNeeded();
  }

  private async handlePlayCard(seat: PlayerSeat, action: MoveAction) {
    if (this.phase !== 'PLAYING' || this.currentTurn !== seat) {
      return;
    }

    const player = this.players[seat];
    if (!player) return;

    const cardIndex = player.hand.findIndex((c) => c.id === action.cardId);
    if (cardIndex === -1) return;

    const card = player.hand[cardIndex];
    const partnerSeat = ((seat + 2) % 4) as PlayerSeat;
    const partnerFinished = this.players[partnerSeat]?.hasFinishedAllMarbles ?? false;

    const result = JackarooRuleValidator.validateAndApplyMove(
      this.boardState,
      seat,
      card,
      action,
      partnerFinished
    );

    if (!result.valid) {
      const ws = this.findSocketForSeat(seat);
      if (ws) this.sendError(ws, result.reason || 'Illegal move');
      return;
    }

    player.hand.splice(cardIndex, 1);
    this.lastDiscardedCard = card;

    if (result.isWinningMove && result.winningTeam !== undefined) {
      this.phase = 'GAME_OVER';
      this.winningTeam = result.winningTeam;
      this.broadcast({
        type: 'GAME_OVER',
        winningTeam: result.winningTeam,
      });
      return;
    }

    const nextTurn = this.getNextTurnSeat(seat);
    this.currentTurn = nextTurn;
    this.turnDeadline = Date.now() + TURN_DURATION_MS;
    await this.ctx.storage.setAlarm(this.turnDeadline);

    this.broadcast({
      type: 'MOVE_PLAYED',
      player: seat,
      card,
      marbles: this.boardState.marbles,
      capturedMarbles: result.capturedMarbles,
      nextTurn: this.currentTurn,
      turnDeadline: this.turnDeadline,
    });

    const handsEmpty = this.players.every((p) => p && p.hand.length === 0);
    if (handsEmpty) {
      this.dealRoundIndex = ((this.dealRoundIndex + 1) % 3) as 0 | 1 | 2;
      this.startNewRound();
    } else {
      this.triggerBotTurnIfNeeded();
    }
  }

  private advanceTurn() {
    this.currentTurn = this.getNextTurnSeat(this.currentTurn);
    this.turnDeadline = Date.now() + TURN_DURATION_MS;
    this.ctx.storage.setAlarm(this.turnDeadline);
    this.triggerBotTurnIfNeeded();
  }

  private getNextTurnSeat(current: PlayerSeat): PlayerSeat {
    return ((current + 1) % 4) as PlayerSeat;
  }

  private handleVoiceMuted(seat: PlayerSeat, isMuted: boolean) {
    const player = this.players[seat];
    if (!player) return;
    player.isMuted = isMuted;
    this.broadcast({
      type: 'VOICE_STATE_CHANGED',
      seat,
      isMuted: player.isMuted,
      isSpeaking: player.isSpeaking,
    });
  }

  private handleVoiceSpeaking(seat: PlayerSeat, isSpeaking: boolean) {
    const player = this.players[seat];
    if (!player) return;
    player.isSpeaking = isSpeaking;
    this.broadcast({
      type: 'VOICE_STATE_CHANGED',
      seat,
      isMuted: player.isMuted,
      isSpeaking: player.isSpeaking,
    });
  }

  // --- Utility & Serialization ---

  private buildRoomStateMessage(forSeat: PlayerSeat): ServerMessage {
    const player = this.players[forSeat];
    return {
      type: 'ROOM_STATE',
      roomId: this.roomId,
      phase: this.phase,
      mySeat: forSeat,
      currentTurn: this.currentTurn,
      turnDeadline: this.turnDeadline,
      players: this.players.map((p) => (p ? this.getPublicPlayerInfo(p) : null)),
      myHand: player ? player.hand : [],
      marbles: this.boardState.marbles,
      lastDiscardedCard: this.lastDiscardedCard,
      winningTeam: this.winningTeam,
    };
  }

  private getPublicPlayerInfo(p: PlayerState): PublicPlayerInfo {
    return {
      seat: p.seat,
      userId: p.userId,
      name: p.name,
      isReady: p.isReady,
      isConnected: p.isConnected,
      isMuted: p.isMuted,
      isSpeaking: p.isSpeaking,
      cardCount: p.hand.length,
      hasSwappedCard: p.swappedCard !== null,
      hasFinishedAllMarbles: p.hasFinishedAllMarbles,
      voiceSessionId: p.voiceSessionId,
    };
  }

  private broadcastRoomState() {
    for (const [ws, session] of this.sessions.entries()) {
      this.send(ws, this.buildRoomStateMessage(session.seat));
    }
  }

  private broadcast(msg: ServerMessage) {
    const payload = JSON.stringify(msg);
    for (const ws of this.sessions.keys()) {
      try {
        ws.send(payload);
      } catch {}
    }
  }

  private sendToSeat(seat: PlayerSeat, msg: ServerMessage) {
    const ws = this.findSocketForSeat(seat);
    if (ws) this.send(ws, msg);
  }

  private send(ws: WebSocket, msg: ServerMessage) {
    try {
      ws.send(JSON.stringify(msg));
    } catch {}
  }

  private sendError(ws: WebSocket, message: string) {
    this.send(ws, { type: 'ERROR', message });
  }

  private findSocketForSeat(seat: PlayerSeat): WebSocket | null {
    for (const [ws, session] of this.sessions.entries()) {
      if (session.seat === seat) return ws;
    }
    return null;
  }
}
