package dipole;

import java.util.LinkedList;

public class Player {
	// ---------COSTANTI e VARIABILI SEARCHING------------------
	private final static byte MAX_DEPTH = 15;
	private int timeout;
	private int max_depth; // livello massimo raggiunto nell'iterative deepening
	private int last_depth; // livello raggiunto nell'ultima visita dell'iterative deepening
	private int profondita; // variabile usata per calcolare last_depth e salvare ttentry
	private int level; // variabile usata per calcolare profondita
	// -------WEIGHT EURISTICA-----------
	private final static int[] PESO_PEDINE_RIMANENTI_NOSTRE = { 0, 33554432, 67108864, 100663296, 134217728, 167772160,
			201326592, 234881024, 268435456, 301989888, 335544320, 369098752, 402653184 }; // 33554432*(index)
	private final static int[] PESO_PEDINE_RIMANENTI_AVVERSARIO = { 0, -33554432, -67108864, -100663296, -134217728,
			-167772160, -201326592, -234881024, -268435456, -301989888, -335544320, -369098752, -402653184 }; // -33554432*(index)
	private final static int[] PESO_PEDINE_DA_SPOSTARE_AVANTI = { 0, 10240, 163840, 81920, -4096, -8192, -16384, -32768,
			-65536, -131072, -262144, -524288, -1048576 };
	private final static int[] PESO_PEDINE_DA_SPOSTARE_INDIETRO = { 0, 16384, 32768, 65536, 131072, 262144, 524288,
			1048576, 2097152, 4194304, 8388608, 16777216, 33554432 };
	private final static int[] PESO_DISTANZA_BORDO = { -524288, -282144, -151072, -85536, 32768, 65536, 65536, 65536 };
	private static final int[] PESO_RAPPORTO_CAMPO = { 0, -65536, -32768, -16384, -8192, 32768, 65536, 131072, 262144,
			524288, 1048576, 2097152, 4194304 };
	private final static int PESO_CENTRO = 1310720;
	private final static int PESO_ESTERNO = -1310720; // Negativo
	private final static int CAPTURE_AVANTI = 33554432;
	private final static int PEDINE_SALVE = 16777216; // pedineSalve 33554432/2
	private final static int CAPTURE_INDIETRO = 36909875; // 33554432 * 1.1 TRONCATO
	private final static int MEAN_CAPTURE = -20777216; // usata nel caso siamo sottoattacco //possibile detect
														// dell'attacco più conveniente
	private final static int UNICA_PEDINA_RIMANENTE = -4194304;
	private final static int PESO_EXIT = -33554432;// Negativo
	private final static int BONUS_CELLE_COPERTE = 16384;
	private final static int BONUS_CELLE_ATTACCABILI = 65536;
	private static final int COEFF_SPOSTAMENTO_MINIMO = 18; // Così spostare una pedina è piu conveniente di spostarne
															// due
	// -------OBJECT-----------
	private static Board board;// Configurazione Corrente
	private boolean player; // White=True, Balck=False
	private boolean elapsed;

	public Player(boolean white) {
		board = new Board();
		player = white;
		elapsed = false;
		timeout = 920;
		max_depth = -1;
		last_depth = 0;
		profondita = 0;
		level = 0;
	}
	/*
	 * Elabora la mossa da eseguire, ritorna una mossa vuota se non vi è alcuna
	 * mossa possibile per il giocatore.
	 */

	public Move elaborateMove() throws InterruptedException {
		// Iterative Deepening
		// Se il gioco è finito il metodo non viene richiamato
		long start = System.currentTimeMillis();
		LinkedList<Move> moves = (player) ? board.mosseBianco() : board.mosseNero(); // figli del nodo corrente
		if (moves.size() == 0) {
			int indexPar = board.getFirstStack(player);
			return new Move(indexPar, (byte) 0, "N", player, Move.SHIFT); // Mossa nulla-Passa turno
		}
		Move best = moves.peek();
		for (int cuttingLevel = 3; cuttingLevel <= MAX_DEPTH; cuttingLevel += 1) {
			// A livello 0 c'è la radice che non è altro che la configurazione corrente
			if (elapsed)
				break; // Ritorniamo mossa migliore corrente
			Move move = nmSearch(start, cuttingLevel, moves);
			if (move != null && last_depth > max_depth) {
				/*
				 * Conviene prendere anche la profondità dell'albero su cui abbiamo calcolato la
				 * mossa migliore. Se best viene fissata valutando 6 livelli e all'ultima
				 * ricerca otteniamo un valore fino al terzo livello sovrascriviamo una mossa
				 * migliore con una peggiore. (Più si scende in ampiezza e migliore è
				 * l'euristica)
				 */
				best = move;
				max_depth = last_depth;
			}
		}
		this.max_depth = 0;// azzeriamo max_depth per la prossima iterazione
		elapsed = false; // azzeriamo elapsed per la prossima iterazione
		return best; // Deve essere diverso da null
	}

	private Move nmSearch(long start, int depth, LinkedList<Move> moves) throws InterruptedException {
		// va in ampiezza
		int alfa = -Integer.MAX_VALUE;
		int beta = Integer.MAX_VALUE;
		Move best = null;
		this.last_depth = 0;// azzeriamo last_depth su una nuova ricerca
		for (Move m : moves) {
			// visitiamo un fratello e azzeriamo profondita e poniamo level al livello di
			// profondita
			// cui vorremmo arrivare
			this.profondita = 0;
			level = depth;
			board.make(m); // abbiamo giocato noi.
			int score = 0;
			if (endGame()) {
				byte blackP = board.getNumeroPedineNero();
				byte whiteP = board.getNumeroPedineBianco();
				if (player) {
					if (blackP > whiteP)
						score = -Integer.MAX_VALUE;
					else if (whiteP > blackP)
						score = Integer.MAX_VALUE;
					else
						score = 0;
				} else {
					if (blackP < whiteP)
						score = -Integer.MAX_VALUE;
					else if (whiteP < blackP)
						score = Integer.MAX_VALUE;
					else
						score = 0;
				}
			} else
				score = -negaMax(start, depth - 1, -beta, -alfa, !player); // gioca l'avversario
			if (profondita > last_depth)
				last_depth = profondita;
			if (elapsed) {
				// non ha alcun effetto ritornare -1
				board.reverse(m);
				return best;
			}
			if (score > alfa) {
				alfa = score;
				best = m;
			}
			board.reverse(m);
		}
		return best;
	}

	private int negaMax(long start, int depth, int alfa, int beta, boolean player) throws InterruptedException {
		// va in profondità fino al cutting level fissato
		if (System.currentTimeMillis() - start > timeout) {
			// Il timeout risulta scaduto e blocchiamo la ricerca
			elapsed = true;
			return -1;
		}
		if (depth < this.level) {
			// Se raggiungiamo un nuovo livello di profondita
			this.profondita++;
			this.level = depth;
		}
		LinkedList<Move> moves = (player) ? board.mosseBianco() : board.mosseNero(); // ordinate per peso euristico
		if (depth == -1 && !moves.isEmpty()) {
			// Conviene usare depth==-1 per visitare in ampiezza l'ultimo livello e evitare
			// errori
			// nelle ultime tre mosse
			// Abbiamo raggiunto l'ultimo livello ma non un nodo foglia.
			int score = moves.peek().getWeight();
			if (score > alfa)
				alfa = score;
			return alfa;
		}
		for (Move m : moves) {
			board.make(m);
			int score = 0;
			if (endGame()) {
				byte blackP = board.getNumeroPedineNero();
				byte whiteP = board.getNumeroPedineBianco();
				if (player) {
					if (blackP > whiteP)
						score = -Integer.MAX_VALUE;
					else if (whiteP > blackP)
						score = Integer.MAX_VALUE;
					else
						score = 0;
				} else {
					if (blackP < whiteP)
						score = -Integer.MAX_VALUE;
					else if (whiteP < blackP)
						score = Integer.MAX_VALUE;
					else
						score = 0;
				}
			} else
				score = -negaMax(start, depth - 1, -beta, -alfa, !player); // Peso mossa avversario per -1
			board.reverse(m);
			if (elapsed) {
				return -1;
			}
			if (score >= beta) {
				// fail-hard beta-cutoff
				return beta;
			}
			if (score > alfa) {
				alfa = score;
			}
		}
		return alfa;
	}
	/*
	 * Verifica se il gioco è terminato se 1) Un giocatore termina le pedine 2) Si è
	 * compiuto il limite massimo di mosse.
	 */

	public static boolean endGame() {
		return board.getNumeroPedineBianco() == 0 || board.getNumeroPedineNero() == 0
				|| (board.getMosseFatteBianco() == 60 && board.getMosseFatteNero() == 60);
	}

	private static void euristicaFase1Bianco(Move m) {
		int partenza = m.getIndex();
		int dest = m.getIndexDest();
		int numPed = m.getNumPedine();
		int row = Board.getRow(dest);
		int col = Board.getColumn(dest, row);
		int type = m.getMoveType();
		int weight = (PESO_PEDINE_RIMANENTI_NOSTRE[board.getNumeroPedineBianco()]
				+ PESO_PEDINE_RIMANENTI_AVVERSARIO[board.getNumeroPedineNero()]);
		int distanza = board.distanzaConfineOpposto(dest, true);
		if (distanza >= 0)
			weight += PESO_DISTANZA_BORDO[distanza];
		if (col < 2 || col > 7)
			weight += PESO_ESTERNO;
		else if (row >= 3 && row <= 5)
			weight += PESO_CENTRO;
		else
			weight += PESO_ESTERNO;
		if (type == Move.EXIT) {
			weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
			weight += PESO_EXIT * numPed;
		} else {
			if (type == Move.CAPTURE) {
				if (row == Board.getRow(partenza)) {
					weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
					weight += CAPTURE_INDIETRO * m.getPedineMangiate();
				} else if (partenza < dest) {
					weight += PESO_PEDINE_DA_SPOSTARE_INDIETRO[numPed];
					weight += CAPTURE_INDIETRO * m.getPedineMangiate();
				} else {
					weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
					weight += CAPTURE_AVANTI * m.getPedineMangiate();
				}
			} else {
				weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
				if (board.getIndexValue(partenza) - numPed == 1) {
					weight += UNICA_PEDINA_RIMANENTE / numPed;
				}
			}
			int pedineDest = numPed;
			if (type == Move.SHIFT && !board.stackVicinoBianco(dest, pedineDest)) {
				int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliBianco(dest, pedineDest);
				int attaccabiliPre = board.getNumeroCelleAttaccabiliBianco(partenza);
				int attaccabiliDiff = bonusCelleAttaccabili - attaccabiliPre;
				if (attaccabiliDiff > 0)
					weight += (attaccabiliDiff) * BONUS_CELLE_ATTACCABILI;
				int bonusCelleCoperte = board.getNumeroCelleCoperteBianco(dest, pedineDest);
				int copertePre = board.getNumeroCelleCoperteBianco(partenza);
				int coperteDiff = bonusCelleCoperte - copertePre;
				if (coperteDiff > 0)
					weight += (coperteDiff) * BONUS_CELLE_COPERTE;
			} else if (type == Move.MERGE) {
				pedineDest += board.getIndexValue(dest);
				int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliBianco(dest, pedineDest);
				int attaccabiliPre = board.getNumeroCelleAttaccabiliBianco(dest); // Per vedere se un merge rafforzativo
				int attaccabiliDiff = bonusCelleAttaccabili - attaccabiliPre;
				if (attaccabiliDiff > 0)
					weight += (attaccabiliDiff) * BONUS_CELLE_ATTACCABILI;
				int bonusCelleCoperte = board.getNumeroCelleCoperteBianco(dest, pedineDest);
				int copertePre = board.getNumeroCelleCoperteBianco(dest);
				int coperteDiff = bonusCelleCoperte - copertePre;
				if (coperteDiff > 0)
					weight += (coperteDiff) * BONUS_CELLE_COPERTE;
			}
			if (board.sottoAttacco(m, true)) {
				// post destinazione
				weight += MEAN_CAPTURE * pedineDest;
			} else {
				if (type == Move.CAPTURE) {
					int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliBianco(dest, pedineDest);
					weight += bonusCelleAttaccabili * BONUS_CELLE_ATTACCABILI;
					int bonusCelleCoperte = board.getNumeroCelleCoperteBianco(dest, pedineDest);
					weight += (bonusCelleCoperte) * BONUS_CELLE_COPERTE;
				} else if (board.sottoAttacco(dest)) {
					// pre destinazione
					weight += (type != Move.MERGE) ? PEDINE_SALVE * board.getIndexValue(dest)
							: (PEDINE_SALVE / 2) * board.getIndexValue(dest); // Per evitare stallo
				}
			}
			if (board.sottoAttacco(partenza)) {
				// pre partenza
				weight += PEDINE_SALVE * numPed;
			}
		}
		if (board.sottoAttacco(m, false)) {
			// post partenza
			int pedineRim = board.getIndexValue(partenza) - numPed;
			weight += MEAN_CAPTURE * pedineRim;
		}
		m.setWeight(weight);
	}

	private static void euristicaFase1Nero(Move m) {
		int partenza = m.getIndex();
		int dest = m.getIndexDest();
		int numPed = m.getNumPedine();
		int row = Board.getRow(dest);
		int col = Board.getColumn(dest, row);
		int type = m.getMoveType();
		int weight = (PESO_PEDINE_RIMANENTI_NOSTRE[board.getNumeroPedineNero()]
				+ PESO_PEDINE_RIMANENTI_AVVERSARIO[board.getNumeroPedineBianco()]);
		int distanza = board.distanzaConfineOpposto(dest, false);
		if (distanza >= 0)
			weight += PESO_DISTANZA_BORDO[distanza];
		if (col < 2 || col > 7)
			weight += PESO_ESTERNO;
		else if (row >= 2 && row <= 4)
			weight += PESO_CENTRO;
		else
			weight += PESO_ESTERNO;
		if (type == Move.EXIT) {
			weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
			weight += PESO_EXIT * numPed;
		} else {
			if (type == Move.CAPTURE) {
				if (row == Board.getRow(partenza)) {
					weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
					weight += CAPTURE_INDIETRO * m.getPedineMangiate();
				} else if (partenza > dest) {
					weight += PESO_PEDINE_DA_SPOSTARE_INDIETRO[numPed];
					weight += CAPTURE_INDIETRO * m.getPedineMangiate();
				} else {
					weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
					weight += CAPTURE_AVANTI * m.getPedineMangiate();
				}
			} else {
				weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
				if (-board.getIndexValue(partenza) - numPed == 1) {
					// getIndexValue ritorna il valore con segno
					weight += UNICA_PEDINA_RIMANENTE / numPed;
				}
			}
			int pedineDest = numPed;
			if (type == Move.SHIFT && !board.stackVicinoNero(dest, pedineDest)) {
				int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliNero(dest, pedineDest);
				int attaccabiliPre = board.getNumeroCelleAttaccabiliNero(partenza);
				int attaccabiliDiff = bonusCelleAttaccabili - attaccabiliPre;
				if (attaccabiliDiff > 0)
					weight += (attaccabiliDiff) * BONUS_CELLE_ATTACCABILI;
				int bonusCelleCoperte = board.getNumeroCelleCoperteNero(dest, pedineDest);
				int copertePre = board.getNumeroCelleCoperteNero(partenza);
				int coperteDiff = bonusCelleCoperte - copertePre;
				if (coperteDiff > 0)
					weight += (coperteDiff) * BONUS_CELLE_COPERTE;
			} else if (type == Move.MERGE) {
				pedineDest -= board.getIndexValue(dest); // getIndexValue ritorna il valore con segno
				int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliNero(dest, pedineDest);
				int attaccabiliPre = board.getNumeroCelleAttaccabiliNero(dest);
				int attaccabiliDiff = bonusCelleAttaccabili - attaccabiliPre;
				if (attaccabiliDiff > 0)
					weight += (attaccabiliDiff) * BONUS_CELLE_ATTACCABILI;
				int bonusCelleCoperte = board.getNumeroCelleCoperteNero(dest, pedineDest);
				int copertePre = board.getNumeroCelleCoperteNero(dest);
				int coperteDiff = bonusCelleCoperte - copertePre;
				if (coperteDiff > 0)
					weight += (coperteDiff) * BONUS_CELLE_COPERTE;
			}
			if (board.sottoAttacco(m, true)) {
				// post destinazione
				weight += MEAN_CAPTURE * pedineDest;
			} else {
				if (type == Move.CAPTURE) {
					int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliNero(dest, pedineDest);
					weight += (bonusCelleAttaccabili) * BONUS_CELLE_ATTACCABILI;
					int bonusCelleCoperte = board.getNumeroCelleCoperteNero(dest, pedineDest);
					weight += bonusCelleCoperte * BONUS_CELLE_COPERTE;
				} else if (board.sottoAttacco(dest)) {
					// pre destinazione
					weight += PEDINE_SALVE * (-board.getIndexValue(dest));
				}
			}
			if (board.sottoAttacco(partenza)) {
				// pre partenza
				weight += PEDINE_SALVE * numPed;
			}
		}
		if (board.sottoAttacco(m, false)) {
			// post partenza
			int pedineRim = -(board.getIndexValue(partenza) + numPed);
			weight += MEAN_CAPTURE * pedineRim;
		}
		m.setWeight(weight);
	}

	protected static void euristicaFase1(Move m) {
		/*
		 * 1) Spostare poche pedine in avanti e molte indietro. 2) Conviene rimanere
		 * lontano dal bordo per evitare di avanzare con poche pedine in avanti e
		 * lasciare le altre in posizione iniziale 2) Cercare di spostarsi pedine al
		 * centro della scacchiera (controllo indici riga e colonna- due righe e due
		 * colonne a lato) 3) Conviene Mangiare all'indietro piuttosto che in avanti
		 * (Peso della mangiata relativo al numero pedine che mangi) 4) Conviene non
		 * uscire per garantire che Numero Pedine Nostro >= Numero Pedine avversario Se
		 * riusciamo a controllare a partire dalla configurazione corrente 5) Merge per
		 * rafforzare pedine in posizioni strategiche (non farsi mangiare o costringerlo
		 * a spostarsi)
		 */
		if (m.isWhite())
			euristicaFase1Bianco(m);
		else
			euristicaFase1Nero(m);
	}

	private static void euristicaFase2Bianco(Move m) {
		int partenza = m.getIndex();
		int dest = m.getIndexDest();
		int numPed = m.getNumPedine();
		int type = m.getMoveType();
		int row = Board.getRow(dest);
		int col = Board.getColumn(dest, row);
		int weight = (PESO_PEDINE_RIMANENTI_NOSTRE[board.getNumeroPedineBianco()]
				+ PESO_PEDINE_RIMANENTI_AVVERSARIO[board.getNumeroPedineNero()]);
		weight += PESO_RAPPORTO_CAMPO[board.numeroPedineCampoInteresseBianco(partenza, dest, numPed)];
		int distanza = board.distanzaConfineOpposto(dest, true);
		if (distanza >= 0)
			weight += PESO_DISTANZA_BORDO[distanza];
		if (col < 2 || col > 7)
			weight += PESO_ESTERNO;
		else if (row >= 3 && row <= 5)
			weight += PESO_CENTRO;
		else
			weight += PESO_ESTERNO;
		if (type == Move.EXIT) {
			weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
			weight += PESO_EXIT * numPed;
		} else {
			if (type == Move.CAPTURE) {
				if (Board.getRow(dest) == Board.getRow(partenza)) {
					weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
					weight += CAPTURE_INDIETRO * m.getPedineMangiate();
				} else if (partenza < dest) {
					weight += PESO_PEDINE_DA_SPOSTARE_INDIETRO[numPed];
					weight += CAPTURE_INDIETRO * m.getPedineMangiate();
				} else {
					weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
					weight += CAPTURE_AVANTI * m.getPedineMangiate();
				}
			} else {
				weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
				if (board.getIndexValue(partenza) - numPed == 1) {
					weight += UNICA_PEDINA_RIMANENTE / numPed;
				}
			}
			int pedineDest = numPed;
			if (type == Move.SHIFT && !board.stackVicinoBianco(dest, pedineDest)) {
				int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliBianco(dest, pedineDest);
				int attaccabiliPre = board.getNumeroCelleAttaccabiliBianco(partenza);
				int attaccabiliDiff = bonusCelleAttaccabili - attaccabiliPre;
				if (attaccabiliDiff > 0)
					weight += (attaccabiliDiff) * BONUS_CELLE_ATTACCABILI;
				int bonusCelleCoperte = board.getNumeroCelleCoperteBianco(dest, pedineDest);
				int copertePre = board.getNumeroCelleCoperteBianco(partenza);
				int coperteDiff = bonusCelleCoperte - copertePre;
				if (coperteDiff > 0)
					weight += (coperteDiff) * BONUS_CELLE_COPERTE;
			} else if (type == Move.MERGE) {
				pedineDest += board.getIndexValue(dest);
				int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliBianco(dest, pedineDest);
				int attaccabiliPre = board.getNumeroCelleAttaccabiliBianco(dest);
				int attaccabiliDiff = bonusCelleAttaccabili - attaccabiliPre;
				if (attaccabiliDiff > 0)
					weight += (attaccabiliDiff) * BONUS_CELLE_ATTACCABILI;
				int bonusCelleCoperte = board.getNumeroCelleCoperteBianco(dest, pedineDest);
				int copertePre = board.getNumeroCelleCoperteBianco(dest);
				int coperteDiff = bonusCelleCoperte - copertePre;
				if (coperteDiff > 0)
					weight += (coperteDiff) * BONUS_CELLE_COPERTE;
			}
			if (board.sottoAttacco(m, true)) {
				// post destinazione
				weight += MEAN_CAPTURE * pedineDest;
			} else {
				if (type == Move.CAPTURE) {
					int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliBianco(dest, pedineDest);
					weight += (bonusCelleAttaccabili) * BONUS_CELLE_ATTACCABILI;
					int bonusCelleCoperte = board.getNumeroCelleCoperteBianco(dest, pedineDest);
					weight += bonusCelleCoperte * BONUS_CELLE_COPERTE;
				} else if (board.sottoAttacco(dest)) {
					// pre destinazione
					weight += (type != Move.MERGE) ? PEDINE_SALVE * board.getIndexValue(dest)
							: (PEDINE_SALVE / 2) * board.getIndexValue(dest); // Per evitare stallo
				}
			}
			if (board.sottoAttacco(partenza)) {
				// pre partenza
				weight += PEDINE_SALVE * numPed;
			}
		}
		if (board.sottoAttacco(m, false)) {
			// post partenza
			int pedineRim = board.getIndexValue(partenza) - numPed;
			weight += MEAN_CAPTURE * pedineRim;
		}
		m.setWeight(weight);
	}

	private static void euristicaFase2Nero(Move m) {
		int partenza = m.getIndex();
		int dest = m.getIndexDest();
		int numPed = m.getNumPedine();
		int type = m.getMoveType();
		int weight = (PESO_PEDINE_RIMANENTI_NOSTRE[board.getNumeroPedineNero()]
				+ PESO_PEDINE_RIMANENTI_AVVERSARIO[board.getNumeroPedineBianco()]);
		weight += PESO_RAPPORTO_CAMPO[board.numeroPedineCampoInteresseNero(partenza, dest, numPed)];
		int distanza = board.distanzaConfineOpposto(dest, false);
		if (distanza >= 0)
			weight += PESO_DISTANZA_BORDO[distanza];
		if (type == Move.EXIT) {
			weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
			weight += PESO_EXIT * numPed;
		} else {
			if (type == Move.CAPTURE) {
				if (Board.getRow(dest) == Board.getRow(partenza)) {
					weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
					weight += CAPTURE_INDIETRO * m.getPedineMangiate();
				} else if (partenza > dest) {
					weight += PESO_PEDINE_DA_SPOSTARE_INDIETRO[numPed];
					weight += CAPTURE_INDIETRO * m.getPedineMangiate();
				} else {
					weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
					weight += CAPTURE_AVANTI * m.getPedineMangiate();
				}
			} else {
				weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
				if (-board.getIndexValue(partenza) - numPed == 1) {
					// getIndexValue ritorna il valore con segno
					weight += UNICA_PEDINA_RIMANENTE / numPed;
				}
			}
			int pedineDest = numPed;
			if (type == Move.SHIFT && !board.stackVicinoNero(dest, pedineDest)) {
				int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliNero(dest, pedineDest);
				int attaccabiliPre = board.getNumeroCelleAttaccabiliNero(partenza);
				int attaccabiliDiff = bonusCelleAttaccabili - attaccabiliPre;
				if (attaccabiliDiff > 0)
					weight += (attaccabiliDiff) * BONUS_CELLE_ATTACCABILI;
				int bonusCelleCoperte = board.getNumeroCelleCoperteNero(dest, pedineDest);
				int copertePre = board.getNumeroCelleCoperteNero(partenza);
				int coperteDiff = bonusCelleCoperte - copertePre;
				if (coperteDiff > 0)
					weight += (coperteDiff) * BONUS_CELLE_COPERTE;
			} else if (type == Move.MERGE) {
				pedineDest -= board.getIndexValue(dest); // getIndexValue ritorna il valore con segno
				int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliNero(dest, pedineDest);
				int attaccabiliPre = board.getNumeroCelleAttaccabiliNero(dest);
				int attaccabiliDiff = bonusCelleAttaccabili - attaccabiliPre;
				if (attaccabiliDiff > 0)
					weight += (attaccabiliDiff) * BONUS_CELLE_ATTACCABILI;
				int bonusCelleCoperte = board.getNumeroCelleCoperteNero(dest, pedineDest);
				int copertePre = board.getNumeroCelleCoperteNero(dest);
				int coperteDiff = bonusCelleCoperte - copertePre;
				if (coperteDiff > 0)
					weight += (coperteDiff) * BONUS_CELLE_COPERTE;
			}
			if (board.sottoAttacco(m, true)) {
				// post destinazione
				weight += MEAN_CAPTURE * pedineDest;
			} else {
				if (type == Move.CAPTURE) {
					int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliNero(dest, pedineDest);
					weight += (bonusCelleAttaccabili) * BONUS_CELLE_ATTACCABILI;
					int bonusCelleCoperte = board.getNumeroCelleCoperteNero(dest, pedineDest);
					weight += bonusCelleCoperte * BONUS_CELLE_COPERTE;
				} else if (board.sottoAttacco(dest)) {
					// pre destinazione
					weight += PEDINE_SALVE * (-board.getIndexValue(dest));
				}
			}
			if (board.sottoAttacco(partenza)) {
				// pre partenza
				weight += PEDINE_SALVE * numPed;
			}
		}
		if (board.sottoAttacco(m, false)) {
			// post partenza
			int pedineRim = -(board.getIndexValue(partenza) + numPed);
			weight += MEAN_CAPTURE * pedineRim;
		}
		m.setWeight(weight);
	}

	protected static void euristicaFase2(Move m) {
		/*
		 * 1) Spostare poche pedine in avanti e molte indietro. 2) Conviene rimanere
		 * lontano dal bordo. 3) Conviene Mangiare all'indietro piuttosto che in avanti
		 * (Peso della mangiata relativo al numero pedine che mangi) 4) Conviene non
		 * uscire per garantire che Numero Pedine Nostro >= Numero Pedine avversario 5)
		 * Rapporto pedine nella propria meta campo 6) Merge per rafforzare pedine in
		 * posizioni strategiche (non farsi mangiare o costringerlo a spostarsi)
		 */
		if (m.isWhite())
			euristicaFase2Bianco(m);
		else
			euristicaFase2Nero(m);
	}

	private static void euristacaFase3Bianco(Move m) {
		int partenza = m.getIndex();
		int dest = m.getIndexDest();
		int numPed = m.getNumPedine();
		int distanza = board.distanzaConfineOpposto(dest, true);
		int weight = (PESO_PEDINE_RIMANENTI_NOSTRE[board.getNumeroPedineBianco()]
				+ PESO_PEDINE_RIMANENTI_AVVERSARIO[board.getNumeroPedineNero()]);
		if (distanza >= 0)
			weight += PESO_DISTANZA_BORDO[distanza];
		int row = Board.getRow(dest);
		int col = Board.getColumn(dest, row);
		if (col < 2 || col > 7)
			weight += PESO_CENTRO;
		else if (row >= 3 && row <= 5)
			weight += PESO_ESTERNO;
		else
			weight += PESO_CENTRO;
		int type = m.getMoveType();
		if (type == Move.EXIT) {
			weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
			weight += PESO_EXIT * numPed;
		} else {
			if (type == Move.CAPTURE) {
				if (Board.getRow(dest) == Board.getRow(partenza)) {
					weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
					weight += CAPTURE_INDIETRO * m.getPedineMangiate();
				} else if (partenza < dest) {
					weight += PESO_PEDINE_DA_SPOSTARE_INDIETRO[numPed];
					weight += CAPTURE_INDIETRO * m.getPedineMangiate();
				}
				// non consideriamo le capture in avanti dato che in fase 3 ogni giocatore ha le
				// pedine
				// nel campo avversario
			} else {
				if (partenza <= 7 && numPed == 1) {
					weight += (PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed]) * COEFF_SPOSTAMENTO_MINIMO;
				} else {
					weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
				}
			}
			int pedineDest = numPed;
			if (type == Move.SHIFT && !board.stackVicinoBianco(dest, pedineDest)) {
				int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliBianco(dest, pedineDest);
				int attaccabiliPre = board.getNumeroCelleAttaccabiliBianco(partenza);
				int attaccabiliDiff = bonusCelleAttaccabili - attaccabiliPre;
				if (attaccabiliDiff > 0)
					weight += (attaccabiliDiff) * BONUS_CELLE_ATTACCABILI;
				int bonusCelleCoperte = board.getNumeroCelleCoperteBianco(dest, pedineDest);
				int copertePre = board.getNumeroCelleCoperteBianco(partenza);
				int coperteDiff = bonusCelleCoperte - copertePre;
				if (coperteDiff > 0)
					weight += (coperteDiff) * BONUS_CELLE_COPERTE;
			} else if (type == Move.MERGE) {
				pedineDest += board.getIndexValue(dest);
				int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliBianco(dest, pedineDest);
				int attaccabiliPre = board.getNumeroCelleAttaccabiliBianco(dest);
				int attaccabiliDiff = bonusCelleAttaccabili - attaccabiliPre;
				if (attaccabiliDiff > 0)
					weight += (attaccabiliDiff) * BONUS_CELLE_ATTACCABILI;
				int bonusCelleCoperte = board.getNumeroCelleCoperteBianco(dest, pedineDest);
				int copertePre = board.getNumeroCelleCoperteBianco(dest);
				int coperteDiff = bonusCelleCoperte - copertePre;
				if (coperteDiff > 0)
					weight += (coperteDiff) * BONUS_CELLE_COPERTE;
			}
			if (board.sottoAttacco(m, true)) {
				// post destinazione
				weight += MEAN_CAPTURE * pedineDest;
			} else {
				if (type == Move.CAPTURE) {
					int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliBianco(dest, pedineDest);
					weight += (bonusCelleAttaccabili) * BONUS_CELLE_ATTACCABILI;
					int bonusCelleCoperte = board.getNumeroCelleCoperteBianco(dest, pedineDest);
					weight += bonusCelleCoperte * BONUS_CELLE_COPERTE;
				} else if (board.sottoAttacco(dest)) {
					// pre destinazione
					weight += PEDINE_SALVE * board.getIndexValue(dest); // In fase 3 fare merge risulta piu conveniente
				}
			}
			if (board.sottoAttacco(partenza)) {
				// pre partenza
				weight += PEDINE_SALVE * numPed;
			}
		}
		if (board.sottoAttacco(m, false)) {
			// post partenza
			int pedineRim = board.getIndexValue(partenza) - numPed;
			weight += MEAN_CAPTURE * pedineRim;
		}
		m.setWeight(weight);
	}

	private static void euristacaFase3Nero(Move m) {
		int partenza = m.getIndex();
		int dest = m.getIndexDest();
		int numPed = m.getNumPedine();
		int distanza = board.distanzaConfineOpposto(dest, false);
		int weight = (PESO_PEDINE_RIMANENTI_NOSTRE[board.getNumeroPedineNero()]
				+ PESO_PEDINE_RIMANENTI_AVVERSARIO[board.getNumeroPedineBianco()]);
		if (distanza >= 0)
			weight += PESO_DISTANZA_BORDO[distanza];
		int row = Board.getRow(dest);
		int col = Board.getColumn(dest, row);
		if (col < 2 || col > 7)
			weight += PESO_CENTRO;
		else if (row >= 2 && row <= 4)
			weight += PESO_ESTERNO;
		else
			weight += PESO_CENTRO;
		int type = m.getMoveType();
		if (type == Move.EXIT) {
			weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
			weight += PESO_EXIT * numPed;
		} else {
			if (type == Move.CAPTURE) {
				if (Board.getRow(dest) == Board.getRow(partenza)) {
					weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
					weight += CAPTURE_INDIETRO * m.getPedineMangiate();
				} else if (partenza > dest) {
					weight += PESO_PEDINE_DA_SPOSTARE_INDIETRO[numPed];
					weight += CAPTURE_INDIETRO * m.getPedineMangiate();
				}
				// non consideriamo le capture in avanti dato che in fase 3 ogni giocatore ha le
				// pedine
				// nel campo avversario
			} else {
				if (partenza >= 27 && numPed == 1) {
					weight += (PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed]) * COEFF_SPOSTAMENTO_MINIMO;
				} else {
					weight += PESO_PEDINE_DA_SPOSTARE_AVANTI[numPed];
				}
			}
			int pedineDest = numPed;
			if (type == Move.SHIFT && !board.stackVicinoNero(dest, pedineDest)) {
				int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliNero(dest, pedineDest);
				int attaccabiliPre = board.getNumeroCelleAttaccabiliNero(partenza);
				int attaccabiliDiff = bonusCelleAttaccabili - attaccabiliPre;
				if (attaccabiliDiff > 0)
					weight += (attaccabiliDiff) * BONUS_CELLE_ATTACCABILI;
				int bonusCelleCoperte = board.getNumeroCelleCoperteNero(dest, pedineDest);
				int copertePre = board.getNumeroCelleCoperteNero(partenza);
				int coperteDiff = bonusCelleCoperte - copertePre;
				if (coperteDiff > 0)
					weight += (coperteDiff) * BONUS_CELLE_COPERTE;
			} else if (type == Move.MERGE) {
				pedineDest -= board.getIndexValue(dest); // getIndexValue ritorna il valore con segno
				int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliNero(dest, pedineDest);
				int attaccabiliPre = board.getNumeroCelleAttaccabiliNero(dest);
				int attaccabiliDiff = bonusCelleAttaccabili - attaccabiliPre;
				if (attaccabiliDiff > 0)
					weight += (attaccabiliDiff) * BONUS_CELLE_ATTACCABILI;
				int bonusCelleCoperte = board.getNumeroCelleCoperteNero(dest, pedineDest);
				int copertePre = board.getNumeroCelleCoperteNero(dest);
				int coperteDiff = bonusCelleCoperte - copertePre;
				if (coperteDiff > 0)
					weight += (coperteDiff) * BONUS_CELLE_COPERTE;
			}
			if (board.sottoAttacco(m, true)) {
				// post destinazione
				weight += MEAN_CAPTURE * pedineDest;
			} else {
				if (type == Move.CAPTURE) {
					int bonusCelleAttaccabili = board.getNumeroCelleAttaccabiliNero(dest, pedineDest);
					weight += (bonusCelleAttaccabili) * BONUS_CELLE_ATTACCABILI;
					int bonusCelleCoperte = board.getNumeroCelleCoperteNero(dest, pedineDest);
					weight += bonusCelleCoperte * BONUS_CELLE_COPERTE;
				} else if (board.sottoAttacco(dest)) {
					// pre destinazione
					weight += PEDINE_SALVE * (-board.getIndexValue(dest));
				}
			}
			if (board.sottoAttacco(partenza)) {
				// pre partenza
				weight += PEDINE_SALVE * numPed;
			}
		}
		if (board.sottoAttacco(m, false)) {
			// post partenza
			int pedineRim = -(board.getIndexValue(partenza) + numPed);
			weight += MEAN_CAPTURE * pedineRim;
		}
		m.setWeight(weight);
	}

	protected static void euristicaFase3(Move m) {
		/*
		 * Fase 3 si ha quando le nostre nostre pedine sono da una parte e quelle
		 * dell'avversario dall'altra. Dobbiamo evitare di farci mangiare perché se non
		 * si può mangiare/essere mangiati vince chi ha il numero di mosse maggiore per
		 * unità (spostandoci una mossa per volta) messo in relazione al numero di mosse
		 * effettuare. A parità di mosse vince chi gioca dopo.
		 */
		if (m.isWhite())
			euristacaFase3Bianco(m);
		else
			euristacaFase3Nero(m);
	}

	public static Board getBoard() {
		return board;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}
