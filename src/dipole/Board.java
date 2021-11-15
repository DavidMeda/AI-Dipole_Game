package dipole;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class Board {
	private static final byte INITIAL_WHITE_POSITION = 33;
	private static final byte INITIAL_BLACK_POSITION = 01;
	private static final byte INITIAL_PAWNS_VALUE = 12;
	private static final byte INITIAL_PAWS_BLACKS = -12;
	private static final byte EMPTY_VALUE = 0;
	private static final byte INVALID_CELL_VALUE = 127;
	private static final byte INVALID_CELL_0 = 8;
	private static final byte INVALID_CELL_1 = 17;
	private static final byte INVALID_CELL_2 = 26;
	private byte mosseBiancheFatte = 0;
	private byte mosseNereFatte = 0;
	private byte pedineBianche = 12;
	private byte pedineNere = 12;
	private byte[] board;
	private static final byte N_VALUE = -9;
	private static final byte S_VALUE = +9;
	private static final byte E_VALUE = +1;
	private static final byte W_VALUE = -1;
	private static final byte SE_VALUE = +5;
	private static final byte SW_VALUE = +4;
	private static final byte NW_VALUE = -5;
	private static final byte NE_VALUE = -4;

	public Board() {
		this.board = new byte[35];
		Arrays.fill(this.board, EMPTY_VALUE);
		this.board[INVALID_CELL_0] = INVALID_CELL_VALUE;
		this.board[INVALID_CELL_1] = INVALID_CELL_VALUE;
		this.board[INVALID_CELL_2] = INVALID_CELL_VALUE;
		this.board[INITIAL_WHITE_POSITION] = INITIAL_PAWNS_VALUE;
		this.board[INITIAL_BLACK_POSITION] = INITIAL_PAWS_BLACKS;
	}

	/*
	 * Ritorna il valore della casella Index, NON ritorna in valore assoluto
	 */
	public byte getIndexValue(int index) {
		if (index >= 0 && index < board.length)
			return board[index];
		else
			return INVALID_CELL_VALUE;
	}

	/*
	 * Ritorna dato l'indice la riga corrispondente nella scacchiera Ordinaria
	 */
	public static int getRow(int index) {
		if (index < 0)
			return -1;
		return (index - (index / 8)) / 4;
	}

	/*
	 * Ritorna dato l'indice la colonna corrispondente nella scacchiera Ordinaria, /
	 * calcola intrinsecamente la Riga.
	 */
	public static int getColumn(int index) {
		int row = getRow(index);
		if (row == -1)
			return -1;
		return ((index - (index / 9)) % 4) * 2 + ((row + 1) % 2) + 1;
	}

	/*
	 * Ritorna dato l'indice e la riga, la colonna corrispondente nella scacchiera
	 * Ordinaria
	 */
	public static int getColumn(int index, int row) {
		if (row == -1)
			return -1;
		return ((index - (index / 9)) % 4) * 2 + ((row + 1) % 2) + 1;
	}
	/*
	 * Ritorna per la cella index tutte le mosse fattibili a partire dalla
	 * configurazione corrente. Delle mosse che comportano l'uscita dalla scacchiera
	 * di alcune pedine vengono selezionate per ogni direzione solo le mosse che il
	 * numero di pedine minimo. (Per prendere la minima in generale si dovrebbe
	 * introdurre una variabile che salvi il numero di pedine eliminate. Meglio 3
	 * bit che 8) Viene lanciato dall'euristica di volta in volta. Si ordinano le
	 * mosse e si scartano per ogni index quelle di basso valore.
	 */

	public LinkedList<Move> mosseRaggiungibili(int index) {
		if (board[index] > 0)
			return mosseRaggiungibiliBianco(index);
		else
			return mosseRaggiungibiliNero(index);
	}

	public LinkedList<Move> mosseRaggiungibiliNero(int index) {
		byte numeroPedine = board[index];
		LinkedList<Move> result = new LinkedList<>();
		if (numeroPedine == 0)
			return result;
		byte absNumPed = (byte) (-numeroPedine);
		int column = getColumn(index);
		boolean exitN = false;
		boolean exitNE = false;
		boolean exitNW = false;
		// move black
		for (byte i = 1; i <= absNumPed; i++) {
			// White +i; Black -i
			if (i % 2 == 0) {
				int j = i / 2;
				byte moveN = (byte) (index + (j) * S_VALUE);
				byte moveS = (byte) (index + (j) * N_VALUE);
				byte moveE = (byte) (index + (j) * W_VALUE);
				byte moveW = (byte) (index + (j) * E_VALUE);
				// Possiamo andare N e in caso sia possibile mangiare a E, O e S
				if (!exitN && moveN >= board.length) {
					// moveN >= board.length usciamo dalla scacchiera
					Move m = new Move(index, i, "S", false, Move.EXIT, moveN); // Il N del nero coincide con il S della
																				// scacchiera
					euristica(m);
					result.add(m);
					exitN = true;
				} else if (moveN < board.length && board[moveN] - i <= 0) {
					Move m;
					if (board[moveN] > 0) {
						m = new Move(index, i, "S", false, Move.CAPTURE, moveN);
						m.setPedineMangiate(board[moveN]);
					} else if (board[moveN] < 0)
						m = new Move(index, i, "S", false, Move.MERGE, moveN);
					else
						m = new Move(index, i, "S", false, Move.SHIFT, moveN); // Il N del nero coincide con il S della
																				// scacchiera
					euristica(m);
					result.add(m);
				}
				if (moveS >= 0 && board[moveS] > 0 && board[moveS] - i <= 0) {
					// Non si puo uscire da S
					Move m = new Move(index, i, "N", false, Move.CAPTURE, moveS); // Il S del nero coincide con il N
																					// della scacchiera
					m.setPedineMangiate(board[moveS]);
					euristica(m);
					result.add(m);
				}
				int colW = getColumn(moveW);
				int colE = getColumn(moveE);
				if (i <= 6 && colW > column && colW <= 8 && moveW < board.length && board[moveW] - i <= 0
						&& board[moveW] > 0) {
					// colW>c && colW <= 8 è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					Move m = new Move(index, i, "E", false, Move.CAPTURE, moveW);
					m.setPedineMangiate(board[moveW]);
					euristica(m);
					result.add(m); // W del nero coincide con il E della scacchiera
				}
				if (i <= 6 && colE >= 1 && colE < column && moveE >= 0 && board[moveE] - i <= 0 && board[moveE] > 0) {
					// colE>=1 && colE <c è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					Move m = new Move(index, i, "W", false, Move.CAPTURE, moveE);
					m.setPedineMangiate(board[moveE]);
					euristica(m);
					result.add(m); // E del nero coincide con il W della scacchiera
				}
			}
			if (i < 9) {
				// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
				// anche per spostamenti in diagonale non validi. L'uscita minima nel caso
				// peggiore e con 8 pedine
				// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
				byte moveNE = (byte) (index + i * SW_VALUE);
				byte moveNW = (byte) (index + i * SE_VALUE);
				byte moveSE = (byte) (index + i * NW_VALUE);
				byte moveSW = (byte) (index + i * NE_VALUE);
				int colNE = getColumn(moveNE);
				int colNW = getColumn(moveNW);
				int colSE = getColumn(moveSE);
				int colSW = getColumn(moveSW);
				if (!exitNE && (moveNE >= board.length || board[moveNE] == INVALID_CELL_VALUE || column <= colNE)) {
					Move m = new Move(index, i, "SW", false, Move.EXIT, moveNE);
					euristica(m);
					result.add(m);
					exitNE = true;
				} else if (moveNE < board.length && column > colNE && board[moveNE] - i <= 0) {
					// Se board[moveNE] == INVALID_CELL_VALUE sicuramente board[moveNE] - i>0
					Move m;
					if (board[moveNE] > 0) {
						m = new Move(index, i, "SW", false, Move.CAPTURE, moveNE);
						m.setPedineMangiate(board[moveNE]);
					} else if (board[moveNE] < 0)
						m = new Move(index, i, "SW", false, Move.MERGE, moveNE);
					else
						m = new Move(index, i, "SW", false, Move.SHIFT, moveNE);
					euristica(m);
					result.add(m);
				}
				if (!exitNW && (moveNW >= board.length || column >= colNW)) {
					// se board[moveNW] == INVALID_CELL_VALUE allora getColumn(index) >=
					// getColumn(moveNW)
					Move m = new Move(index, i, "SE", false, Move.EXIT, moveNW);
					euristica(m);
					result.add(m);
					exitNW = true;
				} else if (moveNW < board.length && column < colNW && board[moveNW] - i <= 0) {
					// board[moveNW] == INVALID_CELL_VALUE || getColumn(index) >= getColumn(moveNE)
					Move m;
					if (board[moveNW] > 0) {
						m = new Move(index, i, "SE", false, Move.CAPTURE, moveNW);
						m.setPedineMangiate(board[moveNW]);
					} else if (board[moveNW] < 0)
						m = new Move(index, i, "SE", false, Move.MERGE, moveNW);
					else
						m = new Move(index, i, "SE", false, Move.SHIFT, moveNW);
					euristica(m);
					result.add(m);
				}
				if (moveSE > 0 && column > colSE && board[moveSE] > 0 && board[moveSE] - i <= 0) {
					// Se board[moveSE] == INVALID_CELL_VALUE sicuramente board[moveSE] - i>0
					// a SE non si puo uscire dalla scacchiera
					Move m = new Move(index, i, "NW", false, Move.CAPTURE, moveSE);
					m.setPedineMangiate(board[moveSE]);
					euristica(m);
					result.add(m);
				}
				if (moveSW > 0 && column < colSW && board[moveSW] > 0 && board[moveSW] - i <= 0) {
					// a SW non si puo uscire dalla scacchiera
					Move m = new Move(index, i, "NE", false, Move.CAPTURE, moveSW);
					m.setPedineMangiate(board[moveSW]);
					euristica(m);
					result.add(m);
				}
			}
		}
		return result;
	}

	public LinkedList<Move> mosseRaggiungibiliBianco(int index) {
		byte numeroPedine = board[index];
		LinkedList<Move> result = new LinkedList<>();
		if (numeroPedine == 0) {
			return result;
		}
		int column = getColumn(index);
		boolean exitN = false;
		boolean exitNE = false;
		boolean exitNW = false;
		// move white
		for (byte i = 1; i <= numeroPedine; i++) {
			// White +i; Black-i
			if (i % 2 == 0) {
				int j = i / 2;
				byte moveN = (byte) (index + (j) * N_VALUE);
				byte moveS = (byte) (index + (j) * S_VALUE);
				byte moveE = (byte) (index + (j) * E_VALUE);
				byte moveW = (byte) (index + (j) * W_VALUE);
				// Possiamo andare N e in caso sia possibile mangiare a E, O e S
				if (!exitN && moveN < 0) {
					// moveN < 0 usciamo dalla schacchiera
					Move m = new Move(index, i, "N", true, Move.EXIT, moveN);
					euristica(m);
					result.add(m);
					exitN = true; // uscita minima a N
				} else if (moveN >= 0 && board[moveN] + i >= 0) {
					Move m;
					if (board[moveN] < 0) {
						m = new Move(index, i, "N", true, Move.CAPTURE, moveN);
						m.setPedineMangiate((byte) -board[moveN]);
					} else if (board[moveN] > 0)
						m = new Move(index, i, "N", true, Move.MERGE, moveN);
					else
						m = new Move(index, i, "N", true, Move.SHIFT, moveN);
					euristica(m);
					result.add(m);
				}
				if (moveS < board.length && board[moveS] < 0 && board[moveS] + i >= 0) {
					// Non si puo uscire da S
					Move m = new Move(index, i, "S", true, Move.CAPTURE, moveS);
					m.setPedineMangiate((byte) -board[moveS]); // valore assoluto
					euristica(m);
					result.add(m);
				}
				int colW = getColumn(moveW);
				int colE = getColumn(moveE);
				if (i <= 6 && colW >= 1 && colW < column && moveW >= 0 && board[moveW] + i >= 0 && board[moveW] < 0) {
					// colW >= 1 && colW<c è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					// Non si puo uscire da W
					Move m = new Move(index, i, "W", true, Move.CAPTURE, moveW);
					m.setPedineMangiate((byte) -board[moveW]);
					euristica(m);
					result.add(m);
				}
				if (i <= 6 && colE > column && colE <= 8 && moveE < board.length && board[moveE] + i >= 0
						&& board[moveE] < 0) {
					// colE>c && colE <= 8 è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					// Non si puo uscire da E
					Move m = new Move(index, i, "E", true, Move.CAPTURE, moveE);
					m.setPedineMangiate((byte) -board[moveE]);
					euristica(m);
					result.add(m);
				}
			}
			if (i < 9) {
				// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
				// anche per spostamenti in
				// diagonale non validi
				// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
				byte moveNE = (byte) (index + i * NE_VALUE);
				byte moveNW = (byte) (index + i * NW_VALUE);
				byte moveSE = (byte) (index + i * SE_VALUE);
				byte moveSW = (byte) (index + i * SW_VALUE);
				int colNE = getColumn(moveNE);
				int colNW = getColumn(moveNW);
				int colSE = getColumn(moveSE);
				int colSW = getColumn(moveSW);
				if (!exitNE && (moveNE < 0 || column >= colNE)) {
					// Si puo evitare di controllare board[moveNE] == INVALID_CELL_VALUE dato che in
					// tal caso vale c>=colNE fuori scacchiera
					Move m = new Move(index, i, "NE", true, Move.EXIT, moveNE);
					euristica(m);
					result.add(m);
					exitNE = true;
				} else if (moveNE >= 0 && column < colNE && board[moveNE] + i >= 0) {
					Move m;
					if (board[moveNE] < 0) {
						m = new Move(index, i, "NE", true, Move.CAPTURE, moveNE);
						m.setPedineMangiate((byte) -board[moveNE]);
					} else if (board[moveNE] > 0)
						m = new Move(index, i, "NE", true, Move.MERGE, moveNE);
					else
						m = new Move(index, i, "NE", true, Move.SHIFT, moveNE);
					euristica(m);
					result.add(m);
				}
				if (!exitNW && (moveNW < 0 || board[moveNW] == INVALID_CELL_VALUE || column <= colNW)) {
					// Non si puo evitare di controllare board[moveNW] == INVALID_CELL_VALUE dato
					// che in tal caso non vale c<=colNW
					// Le celle non valide sono poste su colonna 1
					// fuori scacchiera
					Move m = new Move(index, i, "NW", true, Move.EXIT, moveNW);
					euristica(m);
					result.add(m);
					exitNW = true;
				} else if (moveNW >= 0 && board[moveNW] != INVALID_CELL_VALUE && column > colNW
						&& board[moveNW] + i >= 0) {
					Move m;
					if (board[moveNW] < 0) {
						m = new Move(index, i, "NW", true, Move.CAPTURE, moveNW);
						m.setPedineMangiate((byte) -board[moveNW]);
					} else if (board[moveNW] > 0)
						m = new Move(index, i, "NW", true, Move.MERGE, moveNW);
					else
						m = new Move(index, i, "NW", true, Move.SHIFT, moveNW);
					euristica(m);
					result.add(m);
				}
				if (moveSE < board.length && column < colSE && board[moveSE] < 0 && board[moveSE] + i >= 0) {
					// a SE non si puo uscire dalla scacchiera
					Move m = new Move(index, i, "SE", true, Move.CAPTURE, moveSE);
					m.setPedineMangiate((byte) -board[moveSE]);
					euristica(m);
					result.add(m);
				}
				if (moveSW < board.length && column > colSW && board[moveSW] < 0 && board[moveSW] + i >= 0) {
					// a SW non si puo uscire dalla scacchiera
					Move m = new Move(index, i, "SW", true, Move.CAPTURE, moveSW);
					m.setPedineMangiate((byte) -board[moveSW]);
					euristica(m);
					result.add(m);
				}
			}
		}
		return result;
	}
	/*
	 * Ritorna numero di celle che da una data posizione puoi raggiungere ma non
	 * attaccare con numeroPedine Il metodo con il solo Index ritorna le celle
	 * raggiungibili al momento corrente da una data pedina, il metodo che riceve il
	 * numero di pedine, calcolo le celle coperte dall'indice Index con un numero di
	 * pedine dato.
	 */

	public int getNumeroCelleCoperteNero(int index) {
		byte numeroPedine = board[index];
		int result = 0;
		if (numeroPedine == 0) {
			return result;
		}
		byte absNumPed = (byte) (-numeroPedine);
		int column = getColumn(index);
		boolean exitN = false;
		boolean exitNE = false;
		boolean exitNW = false;
		for (byte i = 1; i <= absNumPed; i++) {
			// White +i; Black-i -> cambia solo condizione per mangiare
			if (i % 2 == 0) {
				int j = i / 2;
				byte moveN = (byte) (index + (j) * S_VALUE);
				byte moveS = (byte) (index + (j) * N_VALUE);
				byte moveE = (byte) (index + (j) * W_VALUE);
				byte moveW = (byte) (index + (j) * E_VALUE);
				if (!exitN && moveN >= board.length) {// moveN >= board.length usciamo dalla scacchiera
					exitN = true;
				} else if (moveN < board.length && board[moveN] == 0) {
					result++;
				}
				if (moveS >= 0 && board[moveS] == 0) {
					// Non si puo uscire da S
					result++;
				}
				int colW = getColumn(moveW);
				int colE = getColumn(moveE);
				if (i <= 6 && colW > column && colW <= 8 && moveW < board.length && board[moveW] == 0) {
					// colW>c && colW <= 8 è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					result++;
				}
				if (i <= 6 && colE >= 1 && colE < column && moveE >= 0 && board[moveE] == 0) {
					// colE>=1 && colE <c è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					result++;
				}
			}
			if (i < 9) {
				// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
				// anche per spostamenti in diagonale non validi. L'uscita minima nel caso
				// peggiore e con 8 pedine
				byte moveNE = (byte) (index + i * SW_VALUE);
				byte moveNW = (byte) (index + i * SE_VALUE);
				byte moveSE = (byte) (index + i * NW_VALUE);
				byte moveSW = (byte) (index + i * NE_VALUE);
				int colNE = getColumn(moveNE);
				int colNW = getColumn(moveNW);
				int colSE = getColumn(moveSE);
				int colSW = getColumn(moveSW);
				if (!exitNE && (moveNE >= board.length || board[moveNE] == INVALID_CELL_VALUE || column <= colNE)) {
					exitNE = true;
				} else if (moveNE < board.length && column > colNE && board[moveNE] == 0) {
					// Se board[moveNE] == INVALID_CELL_VALUE sicuramente board[moveNE] - i>0
					result++;
				}
				if (!exitNW && (moveNW >= board.length || column >= colNW)) {
					exitNW = true;
				} else if (moveNW < board.length && column < colNW && board[moveNW] == 0) {
					result++;
				}
				if (moveSE > 0 && column > colSE && board[moveSE] == 0) {
					// a SE non si puo uscire dalla scacchiera
					result++;
				}
				if (moveSW > 0 && column < colSW && board[moveSW] == 0) {
					// a SW non si puo uscire dalla scacchiera
					result++;
				}
			}
		}
		return result;
	}

	public int getNumeroCelleCoperteNero(int index, int numeroPedine) {
		int result = 0;
		if (numeroPedine == 0) {
			return result;
		}
		int column = getColumn(index);
		boolean exitN = false;
		boolean exitNE = false;
		boolean exitNW = false;
		for (byte i = 1; i <= numeroPedine; i++) {
			if (i % 2 == 0) {
				int j = i / 2;
				byte moveN = (byte) (index + (j) * S_VALUE);
				byte moveS = (byte) (index + (j) * N_VALUE);
				byte moveE = (byte) (index + (j) * W_VALUE);
				byte moveW = (byte) (index + (j) * E_VALUE);
				if (!exitN && moveN >= board.length) {// moveN >= board.length usciamo dalla scacchiera
					exitN = true;
				} else if (moveN < board.length && board[moveN] == 0) {
					result++;// Il N del nero coincide con il S della scacchiera
				}
				if (moveS >= 0 && board[moveS] == 0) {
					// Non si puo uscire da S
					result++;
				}
				int colW = getColumn(moveW);
				int colE = getColumn(moveE);
				if (i <= 6 && colW > column && colW <= 8 && moveW < board.length && board[moveW] == 0) {
					// colW>c && colW <= 8 è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					result++;
				}
				if (i <= 6 && colE >= 1 && colE < column && moveE >= 0 && board[moveE] == 0) {
					// colE>=1 && colE <c è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					result++;
				}
			}
			if (i < 9) {
				// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
				// anche per spostamenti in diagonale non validi. L'uscita minima nel caso
				// peggiore e con 8 pedine
				// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
				byte moveNE = (byte) (index + i * SW_VALUE);
				byte moveNW = (byte) (index + i * SE_VALUE);
				byte moveSE = (byte) (index + i * NW_VALUE);
				byte moveSW = (byte) (index + i * NE_VALUE);
				int colNE = getColumn(moveNE);
				int colNW = getColumn(moveNW);
				int colSE = getColumn(moveSE);
				int colSW = getColumn(moveSW);
				if (!exitNE && (moveNE >= board.length || board[moveNE] == INVALID_CELL_VALUE || column <= colNE)) {
					exitNE = true;
				} else if (moveNE < board.length && column > colNE && board[moveNE] == 0) {
					// Se board[moveNE] == INVALID_CELL_VALUE sicuramente board[moveNE] - i>0
					result++;
				}
				if (!exitNW && (moveNW >= board.length || column >= colNW)) {
					exitNW = true;
				} else if (moveNW < board.length && column < colNW && board[moveNW] == 0) {
					result++;
				}
				if (moveSE > 0 && column > colSE && board[moveSE] == 0) {
					// a SE non si puo uscire dalla scacchiera
					result++;
				}
				if (moveSW > 0 && column < colSW && board[moveSW] == 0) {
					// a SW non si puo uscire dalla scacchiera
					result++;
				}
			}
		}
		return result;
	}

	public int getNumeroCelleCoperteBianco(int index) {
		byte numeroPedine = board[index];
		int result = 0;
		if (numeroPedine == 0) {
			return result;
		}
		int column = getColumn(index);
		boolean exitN = false;
		boolean exitNE = false;
		boolean exitNW = false;
		for (byte i = 1; i <= numeroPedine; i++) {
			if (i % 2 == 0) {
				int j = i / 2;
				byte moveN = (byte) (index + (j) * N_VALUE);
				byte moveS = (byte) (index + (j) * S_VALUE);
				byte moveE = (byte) (index + (j) * E_VALUE);
				byte moveW = (byte) (index + (j) * W_VALUE);
				// Possiamo andare N e in caso sia possibile mangiare a E, O e S
				if (!exitN && moveN < 0) {
					exitN = true;
				} else if (moveN >= 0 && board[moveN] == 0) {
					result++;
				}
				if (moveS < board.length && board[moveS] == 0) {
					result++;
				}
				int colW = getColumn(moveW);
				int colE = getColumn(moveE);
				if (i <= 6 && colW >= 1 && colW < column && moveW >= 0 && board[moveW] == 0) {
					// colW >= 1 && colW<c è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					// Non si puo uscire da W
					result++;
				}
				if (i <= 6 && colE > column && colE <= 8 && moveE < board.length && board[moveE] == 0) {
					// colE>c && colE <= 8 è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					// Non si puo uscire da E
					result++;
				}
			}
			if (i < 9) {
				// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
				// anche per spostamenti in diagonale non validi
				// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
				byte moveNE = (byte) (index + i * NE_VALUE);
				byte moveNW = (byte) (index + i * NW_VALUE);
				byte moveSE = (byte) (index + i * SE_VALUE);
				byte moveSW = (byte) (index + i * SW_VALUE);
				int colNE = getColumn(moveNE);
				int colNW = getColumn(moveNW);
				int colSE = getColumn(moveSE);
				int colSW = getColumn(moveSW);
				if (!exitNE && (moveNE < 0 || column >= colNE)) {
					// Si puo evitare di controllare board[moveNE] == INVALID_CELL_VALUE dato che in
					// tal caso vale c>=colNE
					// fuori scacchiera
					exitNE = true;
				} else if (moveNE >= 0 && column < colNE && board[moveNE] == 0) {
					result++;
				}
				if (!exitNW && (moveNW < 0 || board[moveNW] == INVALID_CELL_VALUE || column <= colNW)) {
					exitNW = true;
				} else if (moveNW >= 0 && board[moveNW] != INVALID_CELL_VALUE && column > colNW && board[moveNW] == 0) {
					result++;
				}
				if (moveSE < board.length && column < colSE && board[moveSE] == 0) {
					result++;
				}
				if (moveSW < board.length && column > colSW && board[moveSW] == 0) {
					// a SW non si puo uscire dalla scacchiera
					result++;
				}
			}
		}
		return result;
	}

	public int getNumeroCelleCoperteBianco(int index, int numeroPedine) {
		int result = 0;
		if (numeroPedine == 0) {
			return result;
		}
		int column = getColumn(index);
		boolean exitN = false;
		boolean exitNE = false;
		boolean exitNW = false;
		for (byte i = 1; i <= numeroPedine; i++) {
			if (i % 2 == 0) {
				int j = i / 2;
				byte moveN = (byte) (index + (j) * N_VALUE);
				byte moveS = (byte) (index + (j) * S_VALUE);
				byte moveE = (byte) (index + (j) * E_VALUE);
				byte moveW = (byte) (index + (j) * W_VALUE);
				// Possiamo andare N e in caso sia possibile mangiare a E, O e S
				if (!exitN && moveN < 0) {
					exitN = true;
				} else if (moveN >= 0 && board[moveN] == 0) {
					result++;
				}
				if (moveS < board.length && board[moveS] == 0) {
					result++;
				}
				int colW = getColumn(moveW);
				int colE = getColumn(moveE);
				if (i <= 6 && colW >= 1 && colW < column && moveW >= 0 && board[moveW] == 0) {
					// colW >= 1 && colW<c è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					// Non si puo uscire da W
					result++;
				}
				if (i <= 6 && colE > column && colE <= 8 && moveE < board.length && board[moveE] == 0) {
					// colE>c && colE <= 8 è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					// Non si puo uscire da E
					result++;
				}
			}
			if (i < 9) {
				// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
				// anche per spostamenti in diagonale non validi
				// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
				byte moveNE = (byte) (index + i * NE_VALUE);
				byte moveNW = (byte) (index + i * NW_VALUE);
				byte moveSE = (byte) (index + i * SE_VALUE);
				byte moveSW = (byte) (index + i * SW_VALUE);
				int colNE = getColumn(moveNE);
				int colNW = getColumn(moveNW);
				int colSE = getColumn(moveSE);
				int colSW = getColumn(moveSW);
				if (!exitNE && (moveNE < 0 || column >= colNE)) {
					// Si puo evitare di controllare board[moveNE] == INVALID_CELL_VALUE dato che in
					// tal caso vale c>=colNE
					// fuori scacchiera
					exitNE = true;
				} else if (moveNE >= 0 && column < colNE && board[moveNE] == 0) {
					result++;
				}
				if (!exitNW && (moveNW < 0 || board[moveNW] == INVALID_CELL_VALUE || column <= colNW)) {
					exitNW = true;
				} else if (moveNW >= 0 && board[moveNW] != INVALID_CELL_VALUE && column > colNW && board[moveNW] == 0) {
					result++;
				}
				if (moveSE < board.length && column < colSE && board[moveSE] == 0) {
					result++;
				}
				if (moveSW < board.length && column > colSW && board[moveSW] == 0) {
					// a SW non si puo uscire dalla scacchiera
					result++;
				}
			}
		}
		return result;
	}

	/*
	 * Ritorna numero di celle che da una data posizione puoi attaccare con
	 * numeroPedine Il metodo con il solo Index ritorna le celle attaccabili al
	 * momento corrente da una data pedina, il metodo che riceve il numero di
	 * pedine, calcolo le celle attaccabili dall'indice Index con un numero di
	 * pedine dato.
	 */
	public int getNumeroCelleAttaccabiliNero(int index) {
		byte numeroPedine = board[index];
		int result = 0;
		if (numeroPedine == 0) {
			return result;
		}
		byte absNumPed = (byte) (-numeroPedine);
		int column = getColumn(index);
		boolean exitN = false;
		boolean exitNE = false;
		boolean exitNW = false;
		for (byte i = 1; i <= absNumPed; i++) {
			if (i % 2 == 0) {
				int j = i / 2;
				byte moveN = (byte) (index + (j) * S_VALUE);
				byte moveS = (byte) (index + (j) * N_VALUE);
				byte moveE = (byte) (index + (j) * W_VALUE);
				byte moveW = (byte) (index + (j) * E_VALUE);
				// Possiamo andare N e in caso sia possibile mangiare a E, O e S
				if (!exitN && moveN >= board.length) {// moveN >= board.length usciamo dalla scacchiera
					exitN = true;
				} else if (moveN < board.length && board[moveN] > 0 && board[moveN] - i <= 0) {
					result++;
				}
				if (moveS >= 0 && board[moveS] - i <= 0 && board[moveS] > 0) {
					// Non si puo uscire da S
					result++;
				}
				int colW = getColumn(moveW);
				int colE = getColumn(moveE);
				if (i <= 6 && colW > column && colW <= 8 && moveW < board.length && board[moveW] - i <= 0
						&& board[moveW] > 0) {
					// colW>c && colW <= 8 è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					result++;
				}
				if (i <= 6 && colE >= 1 && colE < column && moveE >= 0 && board[moveE] - i <= 0 && board[moveE] > 0) {
					// colE>=1 && colE <c è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					result++;
				}
			}
			if (i < 9) {
				// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
				// anche per spostamenti in diagonale non validi. L'uscita minima nel caso
				// peggiore e con 8 pedine
				// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
				byte moveNE = (byte) (index + i * SW_VALUE);
				byte moveNW = (byte) (index + i * SE_VALUE);
				byte moveSE = (byte) (index + i * NW_VALUE);
				byte moveSW = (byte) (index + i * NE_VALUE);
				int colNE = getColumn(moveNE);
				int colNW = getColumn(moveNW);
				int colSE = getColumn(moveSE);
				int colSW = getColumn(moveSW);
				if (!exitNE && (moveNE >= board.length || board[moveNE] == INVALID_CELL_VALUE || column <= colNE)) {
					exitNE = true;
				} else if (moveNE < board.length && column > colNE && board[moveNE] - i <= 0 && board[moveNE] > 0) {
					// Se board[moveNE] == INVALID_CELL_VALUE sicuramente board[moveNE] - i>0
					result++;
				}
				if (!exitNW && (moveNW >= board.length || column >= colNW)) {
					exitNW = true;
				} else if (moveNW < board.length && column < colNW && board[moveNW] - i <= 0 && board[moveNW] > 0) {
					result++;
				}
				if (moveSE > 0 && column > colSE && board[moveSE] - i <= 0 && board[moveSE] > 0) {
					// a SE non si puo uscire dalla scacchiera
					result++;
				}
				if (moveSW > 0 && column < colSW && board[moveSW] - i <= 0 && board[moveSW] > 0) {
					// a SW non si puo uscire dalla scacchiera
					result++;
				}
			}
		}
		return result;
	}

	public int getNumeroCelleAttaccabiliNero(int index, int numeroPedine) {
		int result = 0;
		if (numeroPedine == 0) {
			return result;
		}
		int column = getColumn(index);
		boolean exitN = false;
		boolean exitNE = false;
		boolean exitNW = false;
		for (byte i = 1; i <= numeroPedine; i++) {
			if (i % 2 == 0) {
				int j = i / 2;
				byte moveN = (byte) (index + (j) * S_VALUE);
				byte moveS = (byte) (index + (j) * N_VALUE);
				byte moveE = (byte) (index + (j) * W_VALUE);
				byte moveW = (byte) (index + (j) * E_VALUE);
				// Possiamo andare N e in caso sia possibile mangiare a E, O e S
				if (!exitN && moveN >= board.length) {// moveN >= board.length usciamo dalla scacchiera
					exitN = true;
				} else if (moveN < board.length && board[moveN] > 0 && board[moveN] - i <= 0) {
					result++;
				}
				if (moveS >= 0 && board[moveS] - i <= 0 && board[moveS] > 0) {
					// Non si puo uscire da S
					result++;
				}
				int colW = getColumn(moveW);
				int colE = getColumn(moveE);
				if (i <= 6 && colW > column && colW <= 8 && moveW < board.length && board[moveW] - i <= 0
						&& board[moveW] > 0) {
					// colW>c && colW <= 8 è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					result++;
				}
				if (i <= 6 && colE >= 1 && colE < column && moveE >= 0 && board[moveE] - i <= 0 && board[moveE] > 0) {
					// colE>=1 && colE <c è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					result++;
				}
			}
			if (i < 9) {
				// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
				// anche per spostamenti in diagonale non validi. L'uscita minima nel caso
				// peggiore e con 8 pedine
				// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
				byte moveNE = (byte) (index + i * SW_VALUE);
				byte moveNW = (byte) (index + i * SE_VALUE);
				byte moveSE = (byte) (index + i * NW_VALUE);
				byte moveSW = (byte) (index + i * NE_VALUE);
				int colNE = getColumn(moveNE);
				int colNW = getColumn(moveNW);
				int colSE = getColumn(moveSE);
				int colSW = getColumn(moveSW);
				if (!exitNE && (moveNE >= board.length || board[moveNE] == INVALID_CELL_VALUE || column <= colNE)) {
					exitNE = true;
				} else if (moveNE < board.length && column > colNE && board[moveNE] - i <= 0 && board[moveNE] > 0) {
					// Se board[moveNE] == INVALID_CELL_VALUE sicuramente board[moveNE] - i>0
					result++;
				}
				if (!exitNW && (moveNW >= board.length || column >= colNW)) {
					exitNW = true;
				} else if (moveNW < board.length && column < colNW && board[moveNW] - i <= 0 && board[moveNW] > 0) {
					result++;
				}
				if (moveSE > 0 && column > colSE && board[moveSE] - i <= 0 && board[moveSE] > 0) {
					// a SE non si puo uscire dalla scacchiera
					result++;
				}
				if (moveSW > 0 && column < colSW && board[moveSW] - i <= 0 && board[moveSW] > 0) {
					// a SW non si puo uscire dalla scacchiera
					result++;
				}
			}
		}
		return result;
	}

	public int getNumeroCelleAttaccabiliBianco(int index) {
		byte numeroPedine = board[index];
		int result = 0;
		if (numeroPedine == 0) {
			return result;
		}
		int column = getColumn(index);
		boolean exitN = false;
		boolean exitNE = false;
		boolean exitNW = false;
		for (byte i = 1; i <= numeroPedine; i++) {
			if (i % 2 == 0) {
				int j = i / 2;
				byte moveN = (byte) (index + (j) * N_VALUE);
				byte moveS = (byte) (index + (j) * S_VALUE);
				byte moveE = (byte) (index + (j) * E_VALUE);
				byte moveW = (byte) (index + (j) * W_VALUE);
				// Possiamo andare N e in caso sia possibile mangiare a E, O e S
				if (!exitN && moveN < 0) {
					exitN = true; // uscita a N
				} else if (moveN >= 0 && board[moveN] + i >= 0 && board[moveN] < 0) {
					result++;
				}
				if (moveS < board.length && board[moveS] + i >= 0 && board[moveS] < 0) {
					result++;
				}
				int colW = getColumn(moveW);
				int colE = getColumn(moveE);
				if (i <= 6 && colW >= 1 && colW < column && moveW >= 0 && board[moveW] + i >= 0 && board[moveW] < 0) {
					// colW >= 1 && colW<c è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					// Non si puo uscire da W
					result++;
				}
				if (i <= 6 && colE > column && colE <= 8 && moveE < board.length && board[moveE] + i >= 0
						&& board[moveE] < 0) {
					// colE>c && colE <= 8 è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					// Non si puo uscire da E
					result++;
				}
			}
			if (i < 9) {
				// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
				// anche per spostamenti in diagonale non validi
				// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
				byte moveNE = (byte) (index + i * NE_VALUE);
				byte moveNW = (byte) (index + i * NW_VALUE);
				byte moveSE = (byte) (index + i * SE_VALUE);
				byte moveSW = (byte) (index + i * SW_VALUE);
				int colNE = getColumn(moveNE);
				int colNW = getColumn(moveNW);
				int colSE = getColumn(moveSE);
				int colSW = getColumn(moveSW);
				if (!exitNE && (moveNE < 0 || column >= colNE)) {
					// Si puo evitare di controllare board[moveNE] == INVALID_CELL_VALUE dato che in
					// tal caso vale c>=colNE
					// fuori scacchiera
					exitNE = true;
				} else if (moveNE >= 0 && column < colNE && board[moveNE] + i >= 0 && board[moveNE] < 0) {
					result++;
				}
				if (!exitNW && (moveNW < 0 || board[moveNW] == INVALID_CELL_VALUE || column <= colNW)) {
					exitNW = true;
				} else if (moveNW >= 0 && board[moveNW] != INVALID_CELL_VALUE && column > colNW
						&& board[moveNW] + i >= 0 && board[moveNW] < 0) {
					result++;
				}
				if (moveSE < board.length && column < colSE && board[moveSE] + i >= 0 && board[moveSE] < 0) {
					result++;
				}
				if (moveSW < board.length && column > colSW && board[moveSW] + i >= 0 && board[moveSW] < 0) {
					// a SW non si puo uscire dalla scacchiera
					result++;
				}
			}
		}
		return result;
	}

	public int getNumeroCelleAttaccabiliBianco(int index, int numeroPedine) {
		int result = 0;
		if (numeroPedine == 0) {
			return result;
		}
		int column = getColumn(index);
		boolean exitN = false;
		boolean exitNE = false;
		boolean exitNW = false;
		for (byte i = 1; i <= numeroPedine; i++) {
			if (i % 2 == 0) {
				int j = i / 2;
				byte moveN = (byte) (index + (j) * N_VALUE);
				byte moveS = (byte) (index + (j) * S_VALUE);
				byte moveE = (byte) (index + (j) * E_VALUE);
				byte moveW = (byte) (index + (j) * W_VALUE);
				// Possiamo andare N e in caso sia possibile mangiare a E, O e S
				if (!exitN && moveN < 0) {
					exitN = true; // uscita a N
				} else if (moveN >= 0 && board[moveN] + i >= 0 && board[moveN] < 0) {
					result++;
				}
				if (moveS < board.length && board[moveS] + i >= 0 && board[moveS] < 0) {
					result++;
				}
				int colW = getColumn(moveW);
				int colE = getColumn(moveE);
				if (i <= 6 && colW >= 1 && colW < column && moveW >= 0 && board[moveW] + i >= 0 && board[moveW] < 0) {
					// colW >= 1 && colW<c è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					// Non si puo uscire da W
					result++;
				}
				if (i <= 6 && colE > column && colE <= 8 && moveE < board.length && board[moveE] + i >= 0
						&& board[moveE] < 0) {
					// colE>c && colE <= 8 è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					// Non si puo uscire da E
					result++;
				}
			}
			if (i < 9) {
				// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
				// anche per spostamenti in diagonale non validi
				// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
				byte moveNE = (byte) (index + i * NE_VALUE);
				byte moveNW = (byte) (index + i * NW_VALUE);
				byte moveSE = (byte) (index + i * SE_VALUE);
				byte moveSW = (byte) (index + i * SW_VALUE);
				int colNE = getColumn(moveNE);
				int colNW = getColumn(moveNW);
				int colSE = getColumn(moveSE);
				int colSW = getColumn(moveSW);
				if (!exitNE && (moveNE < 0 || column >= colNE)) {
					// Si puo evitare di controllare board[moveNE] == INVALID_CELL_VALUE dato che in
					// tal caso vale c>=colNE
					// fuori scacchiera
					exitNE = true;
				} else if (moveNE >= 0 && column < colNE && board[moveNE] + i >= 0 && board[moveNE] < 0) {
					result++;
				}
				if (!exitNW && (moveNW < 0 || board[moveNW] == INVALID_CELL_VALUE || column <= colNW)) {
					exitNW = true;
				} else if (moveNW >= 0 && board[moveNW] != INVALID_CELL_VALUE && column > colNW
						&& board[moveNW] + i >= 0 && board[moveNW] < 0) {
					result++;
				}
				if (moveSE < board.length && column < colSE && board[moveSE] + i >= 0 && board[moveSE] < 0) {
					result++;
				}
				if (moveSW < board.length && column > colSW && board[moveSW] + i >= 0 && board[moveSW] < 0) {
					// a SW non si puo uscire dalla scacchiera
					result++;
				}
			}
		}
		return result;
	}

	/*
	 * Restituisce true se in un intorno (default=4) vi sia uno stack con un numero
	 * di pedine elevato (numeroPedine/2)
	 */
	public boolean stackVicinoBianco(int indexDest, int numeroPedineDest) {
		if (numeroPedineDest < 4) {
			return false; // si verifica per stack maggiori o uguali a 6
		}
		int column = getColumn(indexDest);
		boolean exitN = false;
		boolean exitNE = false;
		boolean exitNW = false;
		int intorno = 2; // intorno settato a 2 che copre tutti i movimenti
		int limite = numeroPedineDest / 2 + 1;
		for (byte i = 1; i <= intorno; i++) {
			if (i % 2 == 0) {
				int j = i / 2;
				byte moveN = (byte) (indexDest + (j) * N_VALUE);
				byte moveS = (byte) (indexDest + (j) * S_VALUE);
				byte moveE = (byte) (indexDest + (j) * E_VALUE);
				byte moveW = (byte) (indexDest + (j) * W_VALUE);
				// Possiamo andare N e in caso sia possibile mangiare a E, O e S
				if (!exitN && moveN < 0) {
					exitN = true; // uscita a N
				} else if (moveN >= 0 && board[moveN] + i >= 0 && board[moveN] > limite) {
					return true;
				}
				if (moveS < board.length && board[moveS] + i >= 0 && board[moveS] > limite) {
					return true;
				}
				int colW = getColumn(moveW);
				int colE = getColumn(moveE);
				if (i <= 6 && colW >= 1 && colW < column && moveW >= 0 && board[moveW] > limite) {
					// colW >= 1 && colW<c è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					// Non si puo uscire da W
					return true;
				}
				if (i <= 6 && colE > column && colE <= 8 && moveE < board.length && board[moveE] > limite) {
					// board[moveE] < 0
					// colE>c && colE <= 8 è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					// Non si puo uscire da E
					return true;
				}
			}
			if (i < 9) {
				// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
				// anche per spostamenti in
				// diagonale non validi
				// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
				byte moveNE = (byte) (indexDest + i * NE_VALUE);
				byte moveNW = (byte) (indexDest + i * NW_VALUE);
				byte moveSE = (byte) (indexDest + i * SE_VALUE);
				byte moveSW = (byte) (indexDest + i * SW_VALUE);
				int colNE = getColumn(moveNE);
				int colNW = getColumn(moveNW);
				int colSE = getColumn(moveSE);
				int colSW = getColumn(moveSW);
				if (!exitNE && (moveNE < 0 || column >= colNE)) {
					// Si puo evitare di controllare board[moveNE] == INVALID_CELL_VALUE dato che in
					// tal caso vale c>=colNE
					// fuori scacchiera
					exitNE = true;
				} else if (moveNE >= 0 && column < colNE && board[moveNE] > limite) {
					return true;
				}
				if (!exitNW && (moveNW < 0 || board[moveNW] == INVALID_CELL_VALUE || column <= colNW)) {
					exitNW = true;
				} else if (moveNW >= 0 && board[moveNW] != INVALID_CELL_VALUE && column > colNW
						&& board[moveNW] > limite) {
					return true;
				}
				if (moveSE < board.length && column < colSE && board[moveSE] > limite) {
					return true;
				}
				if (moveSW < board.length && column > colSW && board[moveSW] > limite) {
					// a SW non si puo uscire dalla scacchiera
					return true;
				}
			}
		}
		return false;
	}

	public boolean stackVicinoNero(int index, int numeroPedine) {
		if (numeroPedine < 4) {
			return false; // si verifica per stack maggiore o uguale a 6
		}
		int column = getColumn(index);
		boolean exitN = false;
		boolean exitNE = false;
		boolean exitNW = false;
		int intorno = 2; // intorno settato a 2 che copre tutti i movimenti
		int limite = numeroPedine / 2 + 1;
		for (byte i = 1; i <= intorno; i++) {
			// White +i; Black-i -> cambia solo condizione per mangiare
			if (i % 2 == 0) {
				int j = i / 2;
				byte moveN = (byte) (index + (j) * S_VALUE);
				byte moveS = (byte) (index + (j) * N_VALUE);
				byte moveE = (byte) (index + (j) * W_VALUE);
				byte moveW = (byte) (index + (j) * E_VALUE);
				// Possiamo andare N e in caso sia possibile mangiare a E, O e S
				if (!exitN && moveN >= board.length) {
					// moveN >= board.length usciamo dalla scacchiera
					exitN = true;

				} else if (moveN < board.length && -board[moveN] > limite && board[moveN] < 0) {
					return true;// Il N del nero coincide con il S della scacchiera
				}

				if (moveS >= 0 && board[moveS] - i <= 0 && -board[moveS] > limite && board[moveS] < 0) {
					// Non si puo uscire da S. Merge e Capture
					return true;
				}
				int colW = getColumn(moveW);
				int colE = getColumn(moveE);
				if (i <= 6 && colW > column && colW <= 8 && moveW < board.length && -board[moveW] > limite
						&& board[moveW] < 0) {
					// colW>c && colW <= 8 è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					return true; // W del nero coincide con il E della scacchiera
				}
				if (i <= 6 && colE >= 1 && colE < column && moveE >= 0 && -board[moveE] > limite && board[moveE] < 0) {
					// colE>=1 && colE <c è un vincolo valido se i<=8 ma in orizzontale si possono
					// spostare al più 6 pedine
					return true; // E del nero coincide con il W della scacchiera
				}
			}
			if (i < 9) {
				// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
				// anche per spostamenti in diagonale non validi. L'uscita minima nel caso
				// peggiore e con 8 pedine
				// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
				byte moveNE = (byte) (index + i * SW_VALUE);
				byte moveNW = (byte) (index + i * SE_VALUE);
				byte moveSE = (byte) (index + i * NW_VALUE);
				byte moveSW = (byte) (index + i * NE_VALUE);
				int colNE = getColumn(moveNE);
				int colNW = getColumn(moveNW);
				int colSE = getColumn(moveSE);
				int colSW = getColumn(moveSW);
				if (!exitNE && (moveNE >= board.length || board[moveNE] == INVALID_CELL_VALUE || column <= colNE)) {
					exitNE = true;

				} else if (moveNE < board.length && column > colNE && board[moveNE] - i <= 0 && -board[moveNE] > limite
						&& board[moveNE] < 0) {

					// Se board[moveNE] == INVALID_CELL_VALUE sicuramente board[moveNE] - i>0
					return true;
				}
				if (!exitNW && (moveNW >= board.length || column >= colNW)) {
					exitNW = true;
				} else if (moveNW < board.length && column < colNW && -board[moveNW] > limite && board[moveNW] < 0) {
					return true;
				}
				if (moveSE > 0 && column > colSE && -board[moveSE] > limite && board[moveSE] < 0) { // && board[moveSE]
																									// > 0

					// a SE non si puo uscire dalla scacchiera
					return true;
				}
				if (moveSW > 0 && column < colSW && -board[moveSW] > limite && board[moveSW] < 0) { // && board[moveSW]
																									// > 0
					// a SW non si puo uscire dalla scacchiera
					return true;
				}
			}
		}
		return false;
	}
	/*
	 * Verifica se le pedine spostate possono essere attaccate nella configurazione
	 * corrente.
	 */

	public boolean sottoAttacco(Move todo, boolean flag) {
		// Post destinazione <--> flag=true || Post partenza <-->flag=false
		// ritorna true se potresti essere mangiato
		if (flag) {
			if (todo.getMoveType() == Move.EXIT)
				return true; // Non dobbiamo sommare peso di difesa
			if (todo.isWhite()) {
				int indexDest = todo.getIndexDest();
				int pedineDest;
				if (todo.getMoveType() == Move.MERGE)
					pedineDest = todo.getNumPedine() + board[indexDest];
				else
					pedineDest = todo.getNumPedine();
				return captureMoveBlack(indexDest, pedineDest);
			} else {
				int indexDest = todo.getIndexDest();
				int pedineDest;
				if (todo.getMoveType() == Move.MERGE)
					pedineDest = todo.getNumPedine() - board[indexDest];
				else
					pedineDest = todo.getNumPedine();
				return captureMoveWhite(indexDest, pedineDest);
			}
		} else {
			if (todo.isWhite()) {
				int indexPar = todo.getIndex();
				int pedineRim = board[indexPar] - todo.getNumPedine();
				if (pedineRim > 0) {
					return captureMoveBlack(indexPar, pedineRim);
				} else
					return false;
			} else {
				int indexPar = todo.getIndex();
				int pedineRim = board[indexPar] + todo.getNumPedine();
				if (pedineRim < 0) {
					return captureMoveWhite(indexPar, -pedineRim);
				} else
					return false;
			}
		} // flag=false
	}

	/*
	 * Verifica se le pedine rimanenti nell'indice di partenza possono essere
	 * attaccate nella configurazione corrente.
	 */
	public boolean sottoAttacco(int index) {
		// Pre destinazione || Pre partenza
		// Potresti essere mangiato
		int pedine = board[index];
		if (pedine == 0) {
			return false;
		} else if (pedine > 0) {
			return captureMoveBlack(index, pedine);
		} else {
			return captureMoveWhite(index, pedine * -1);
		}
	}

	/*
	 * Veirifica se uno stack di pedine avversarie che può mangiare nell'indice
	 * indexDest il numero di pedine pedineDest
	 */
	private boolean captureMoveWhite(int indexDest, int pedineDest) {
		// pedineDest in valore assoluto
		if (pedineBianche == 0)
			return false;
		byte count = 0;
		// move white
		for (int index = board.length - 1; index >= 0; index--) {
			byte numPedine = board[index];
			if (numPedine >= pedineDest && numPedine != INVALID_CELL_VALUE) {
				int column = getColumn(index);
				for (int i = pedineDest; i <= numPedine; i++) {
					if (i % 2 == 0) {
						int j = i / 2;
						byte moveN = (byte) (index + (j) * N_VALUE);
						byte moveS = (byte) (index + (j) * S_VALUE);
						byte moveE = (byte) (index + (j) * E_VALUE);
						byte moveW = (byte) (index + (j) * W_VALUE);
						if (moveN == indexDest || moveS == indexDest)
							return true;
						int colW = getColumn(moveW);
						int colE = getColumn(moveE);
						if ((i <= 6 && colW >= 1 && colW < column && moveW == indexDest)
								|| (i <= 6 && colE > column && colE <= 8 && moveE == indexDest))
							return true;
					}
					if (i < 9) {
						// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
						// anche per spostamenti in diagonale non validi
						// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
						byte moveNE = (byte) (index + i * NE_VALUE);
						byte moveNW = (byte) (index + i * NW_VALUE);
						byte moveSE = (byte) (index + i * SE_VALUE);
						byte moveSW = (byte) (index + i * SW_VALUE);
						int colNE = getColumn(moveNE);
						int colNW = getColumn(moveNW);
						int colSE = getColumn(moveSE);
						int colSW = getColumn(moveSW);
						if ((moveNE == indexDest && column < colNE) || (moveNW == indexDest && column > colNW)
								|| (moveSE == indexDest && column < colSE) || (moveSW == indexDest && column > colSW))
							return true;
					}
				}
			}
			count += numPedine;
			if (count == pedineBianche)
				break;
		}
		return false;
	}

	private boolean captureMoveBlack(int indexDest, int pedineDest) {
		if (pedineNere == 0)
			return false;
		byte count = 0;
		for (int index = 0; index < board.length; index++) {
			int numPedine = -board[index];
			if (numPedine >= pedineDest) {
				int column = getColumn(index);
				for (int i = pedineDest; i <= numPedine; i++) {
					// White +i; Black-i
					if (i % 2 == 0) {
						int j = i / 2;
						byte moveN = (byte) (index + (j) * S_VALUE);
						byte moveS = (byte) (index + (j) * N_VALUE);
						byte moveE = (byte) (index + (j) * W_VALUE);
						byte moveW = (byte) (index + (j) * E_VALUE);
						if (moveN == indexDest || moveS == indexDest)
							return true;
						int colW = getColumn(moveW);
						int colE = getColumn(moveE);
						if ((i <= 6 && colE >= 1 && colE < column && moveE == indexDest)
								|| (i <= 6 && colW > column && colW <= 8 && moveW == indexDest))
							return true;
					}
					if (i < 9) {
						// Spostando un numero di pedine>=9 il vincolo sulle colonne risulta valido
						// anche per spostamenti in diagonale non validi
						// Possiamo andare NE, NO e in caso sia possibile mangiare a SE e SO
						byte moveNE = (byte) (index + i * SW_VALUE);
						byte moveNW = (byte) (index + i * SE_VALUE);
						byte moveSE = (byte) (index + i * NW_VALUE);
						byte moveSW = (byte) (index + i * NE_VALUE);
						int colNE = getColumn(moveNE);
						int colNW = getColumn(moveNW);
						int colSE = getColumn(moveSE);
						int colSW = getColumn(moveSW);
						if ((moveSW == indexDest && column < colSW) || (moveSE == indexDest && column > colSE)
								|| (moveNW == indexDest && column < colNW) || (moveNE == indexDest && column > colNE))
							return true;
					}
				}
			}
			count += numPedine;
			if (count == pedineNere)
				break;
		}
		return false;
	}

	private void euristica(Move m) {
		if (primaFase(m.isWhite())) {
			Player.euristicaFase1(m);
		} else if (faseFinale()) {
			Player.euristicaFase3(m);
		} else {
			Player.euristicaFase2(m);
		}
	}

	/*
	 * Nero/Bianco hanno le loro pedine oltre le prime due righe
	 */
	private boolean primaFase(boolean white) {
		if (white) {
			byte contBianche = 0;
			for (int i = 0; i < INVALID_CELL_2; i += 2) {
				if (i != INVALID_CELL_0 && i != INVALID_CELL_1) {
					if (board[i] > 0)
						contBianche += board[i];
				}
				if (i + 1 != INVALID_CELL_0 && i + 1 != INVALID_CELL_1) {
					if (board[i + 1] > 0)
						contBianche += board[i + 1];
				}
			}
			return contBianche != pedineBianche;
		} else {
			byte contNero = 0;
			for (int i = 9; i < board.length; i += 2) {
				if (i != INVALID_CELL_1 && i != INVALID_CELL_2) {
					if (board[i] < 0)
						contNero -= board[i];
				}
				if (i + 1 != INVALID_CELL_1 && i + 1 != INVALID_CELL_2) {
					if (board[i + 1] < 0)
						contNero -= board[i + 1];
				}
			}
			return contNero != pedineNere;
		}
	}

	/*
	 * Il nero sta nella meta campo bianca e il bianco sta nella meta campo nera
	 */
	private boolean faseFinale() {
		byte contBianche = 0;
		byte contNero = 0;
		for (int i = 0; i < board.length; i++) {
			if (i <= 16 && i != INVALID_CELL_0) {
				contBianche += board[i]; // Se c'è una pedina nera sicuramente l'and successivo sarà false
			} else if (i >= 18 && i != INVALID_CELL_2) {
				contNero -= board[i];
			}
		}
		if (contBianche == pedineBianche && contNero == pedineNere)
			return true;
		return false;
	}

	/*
	 * Le mosse ricevute e inviate sono lecite quindi si può evitare controllo su
	 * numPed<=board[index] (white) oppure per verificare se si puo andare a sud
	 */
	public void make(Move m) {
		int index = m.getIndex(); // indice partenza
		byte numeroPedine = m.getNumPedine();
		byte typeMossa = m.getMoveType();
		if (board[index] < 0) {
			mosseNereFatte++;
			board[index] += numeroPedine;
			// Se esce dalla board sottraiamo solo le pedine
			if (typeMossa == Move.EXIT) {
				pedineNere -= numeroPedine;
				return;
			}
			int i = m.getIndexDest();
			if (typeMossa == Move.CAPTURE) {
				pedineBianche -= board[i];
				board[i] = (byte) -numeroPedine;
			} else {
				board[i] -= numeroPedine;
			}
		} else if (board[index] > 0) {
			board[index] -= numeroPedine;
			mosseBiancheFatte++;
			// Se esce dalla board sottraiamo solo le pedine
			if (typeMossa == Move.EXIT) {
				pedineBianche -= numeroPedine;
				return;
			}
			int i = m.getIndexDest();
			if (typeMossa == Move.CAPTURE) {
				pedineNere += board[i]; // board[i] negative
				board[i] = numeroPedine;
			} else {
				board[i] += numeroPedine;
			}
		}
	}

	/*
	 * Non possiamo mai passare in diagonale da un lato ad un altro uscendo con il
	 * minimo numero di pedine. Consideriamo che l'avversario elimini al più il
	 * minimo numero di pedine. Ma dato che usiamo update per aggiornare la board
	 * sulla base dei messaggi dal server possiamo evitare di considerare mosse
	 * sconcluse dell'avversario.
	 */
	public void reverse(Move m) {
		int index = m.getIndex();
		boolean white = m.isWhite();
		byte numeroPedine = m.getNumPedine();
		byte typeMossa = m.getMoveType();
		if (typeMossa == Move.EXIT) {
			// index=indice partenza mossa da annullare
			if (white) {
				board[index] += numeroPedine;
				pedineBianche += numeroPedine;
				mosseBiancheFatte--;
			} else {
				board[index] -= numeroPedine;
				pedineNere += numeroPedine;
				mosseNereFatte--;
			}
			return;
		}
		// Non si può uscire fuori scacchiera nell'invertire una mossa
		int indDest = m.getIndexDest();
		if (!white) {
			mosseNereFatte--;
			if (typeMossa == Move.CAPTURE) {
				byte pedMang = m.getPedineMangiate();
				pedineBianche += pedMang;
				board[indDest] = pedMang;// aggiorniamo le pedine nell'indice di arrivo
				board[index] -= numeroPedine;// aggiungiamo le pedine dall'indice di partenza
			} else {
				board[indDest] += numeroPedine;// eliminiamo le pedine dall'indice di arrivo
				board[index] -= numeroPedine;// aggiungiamo le pedine dall'indice di partenza
			}
		} else {
			mosseBiancheFatte--;
			if (typeMossa == Move.CAPTURE) {
				byte pedMang = m.getPedineMangiate();
				pedineNere += pedMang; // Pedine in valore assoluto
				board[indDest] = ((byte) -pedMang); // aggiorniamo le pedine nell'indice di arrivo
				board[index] += numeroPedine;// aggiungiamo le pedine dall'indice di partenza
			} else {
				board[indDest] -= numeroPedine;// eliminiamo le pedine dall'indice di arrivo
				board[index] += numeroPedine;// aggiungiamo le pedine dall'indice di partenza
			}
		}
	}

	/*
	 * Le mosse ricevute e inviate sono lecite quindi si può evitare controllo su
	 * numPed<=board[index] (white) oppure per verificare se si puo andare a sud
	 */
	public void update(int index, String direction, byte numPed) {
		byte directionNum = getDirection(direction); // Non Serve moltiplicare per -1 dato che il nero si muove secondo
		// l'orientamento del bianco
		int column = getColumn(index);
		if (board[index] < 0) {
			mosseNereFatte++;
			board[index] += numPed;
			// Se esce dalla board sottraiamo solo le pedine
			int i = (numPed % 2 == 0
					&& (directionNum != -4 && directionNum != -5 && directionNum != 4 && directionNum != 5))
							? (index + (numPed / 2) * directionNum)
							: (index + numPed * directionNum);
			int col = getColumn(i);
			if (i >= 0 && i < board.length && board[i] != INVALID_CELL_VALUE
					&& (!(directionNum == 5 && column >= col) && !(directionNum == 4 && column <= col))) {
				if ((board[i] > 0)) {
					pedineBianche -= board[i];
					board[i] = (byte) -numPed;
				} else {
					board[i] -= numPed;
				}
			} else {
				pedineNere -= numPed;
			}
		} else if (board[index] > 0) {
			board[index] -= numPed;
			mosseBiancheFatte++;
			// Se esce dalla board sottraiamo solo le pedine
			int i = (numPed % 2 == 0
					&& (directionNum != -4 && directionNum != -5 && directionNum != 4 && directionNum != 5))
							? (index + numPed / 2 * directionNum)
							: (index + numPed * directionNum);
			int col = getColumn(i);
			if (i >= 0 && i < board.length && board[i] != INVALID_CELL_VALUE
					&& (!(directionNum == -5 && column <= col) && !(directionNum == -4 && column >= col))) {
				if (board[i] < 0) {
					pedineNere += board[i]; // board[i] negative
					board[i] = numPed;
				} else {
					board[i] += numPed;
				}
			} else {
				pedineBianche -= numPed;
			}
		}
	}

	public byte getDirection(String dir) {
		switch (dir) {
		case "N":
			return N_VALUE;
		case "NW":
			return NW_VALUE;
		case "NE":
			return NE_VALUE;
		case "S":
			return S_VALUE;
		case "E":
			return E_VALUE;
		case "W":
			return W_VALUE;
		case "SE":
			return SE_VALUE;
		case "SW":
			return SW_VALUE;
		default:
			// throw new IllegalArgumentException(); // only-test
			return 0;
		}
	}

	/*
	 * Calcola la distanza dal confine Opposto.
	 */
	public int distanzaConfineOpposto(int index, boolean white) {
		if (index == INVALID_CELL_0 || index == INVALID_CELL_1 || index == INVALID_CELL_2) {
			return -1;
		} else if (white) {
			return getRow(index);// contiamo come confine la riga 1
		} else {
			return (7 - getRow(index));// contiamo come confine la riga 7
		}
	}

	/*
	 * Ritorna numero pedine che sono nella meta campo avversario piu quelle nella
	 * notra trequarti
	 */
	public int numeroPedineCampoInteresseBianco(int par, int dest, int pedineSpostate) {
		int cont = 0;
		for (int i = 9; i < board.length; i += 2) {
			if (board[i] != INVALID_CELL_VALUE && board[i] > 0)
				cont += board[i];
			if (board[i + 1] != INVALID_CELL_VALUE && board[i + 1] > 0)
				cont += board[i + 1];
		}
		if (par >= 9 && par < board.length && dest < INVALID_CELL_0)
			cont -= pedineSpostate;// pedineSpostate in valore assoluto
		return cont;
	}

	public int numeroPedineCampoInteresseNero(int par, int dest, int pedineSpostate) {
		int cont = 0;
		for (int i = 0; i < INVALID_CELL_2; i += 2) {
			if (board[i] != INVALID_CELL_VALUE && board[i] < 0)
				cont -= board[i];
			if (board[i + 1] != INVALID_CELL_VALUE && board[i + 1] < 0)
				cont -= board[i + 1];
		}
		if (par >= 0 && par < INVALID_CELL_2 && dest > INVALID_CELL_2)
			cont -= pedineSpostate;// pedineSpostate in valore assoluto
		return cont;
	}

	/*
	 * Ritorna il numero di pedine rimanenti
	 */
	public byte getNumeroPedineBianco() {
		if (pedineBianche < 0)
			throw new RuntimeException("PEDINE BIANCHE NEGATIVE");
		return pedineBianche;
	}

	public byte getNumeroPedineNero() {
		if (pedineNere < 0)
			throw new RuntimeException("PEDINE NERE NEGATIVE");
		return pedineNere;
	}

	/*
	 * Ritorna il numero di mosse fatte per un giocatore
	 */
	public byte getMosseFatteBianco() {
		return mosseBiancheFatte;
	}

	public byte getMosseFatteNero() {
		return mosseNereFatte;
	}

	/*
	 * Ritorna il primo gruppo di pedine disponibile per un giocatore. Usato quando
	 * si ritorna la mossa nulla.
	 */
	public int getFirstStack(boolean white) {
		if (white) {
			for (int i = board.length - 1; i >= 0; i--) {
				if (board[i] > 0 && board[i] != INVALID_CELL_VALUE) {
					// INVALID_CELL_VALUE deve essere preservato e non modificato ne da update ne da
					// make/reverse
					return i;
				}
			}
		} else {
			for (int i = 0; i < board.length; i++) {
				if (board[i] < 0 && board[i] != INVALID_CELL_VALUE) {
					return i;
				}
			}
		}
		return -1; // Non ci sono pedine- non può accadere perchè la partita sarebbe terminata
	}

	/*
	 * Calcola tutte le mosse possibili per il giocatore bianco, e le ritorna
	 * ordinate per euristica. Utilizza dei Thread che scandiscono 2 righe ciascuono
	 * Partendo dal basso.
	 */
	public LinkedList<Move> mosseBianco() throws InterruptedException {
		// Priority queue controlla solo primo elemento, sortedset sovrascrive valori
		// uguali
		LinkedList<Move> result = new LinkedList<Move>();
		if (pedineBianche == 0)
			return result;
		AtomicInteger count = new AtomicInteger(0);
		ThreadMosseBianco t1 = new ThreadMosseBianco(27, 35, count);
		t1.run();
		ThreadMosseBianco t2 = new ThreadMosseBianco(18, 26, count);
		t2.run();
		ThreadMosseBianco t3 = new ThreadMosseBianco(9, 17, count);
		t3.run();
		ThreadMosseBianco t4 = new ThreadMosseBianco(0, 8, count);
		t4.run();
		t4.join();
		t3.join();
		t2.join();
		t1.join();
		result.addAll(t1.getResult());
		result.addAll(t2.getResult());
		result.addAll(t3.getResult());
		result.addAll(t4.getResult());
		Collections.sort(result);
		return result;
	}

	/*
	 * Calcola tutte le mosse possibili per il giocatore nero, e le ritorna ordinate
	 * per euristica. Utilizza dei Thread che scandiscono 2 righe ciascuono Partendo
	 * dall'alto.
	 */
	public LinkedList<Move> mosseNero() throws InterruptedException {
		LinkedList<Move> result = new LinkedList<Move>();
		if (pedineBianche == 0)
			return result;
		AtomicInteger count = new AtomicInteger(0);
		ThreadMosseNero t1 = new ThreadMosseNero(0, 8, count);
		t1.run();
		ThreadMosseNero t2 = new ThreadMosseNero(9, 17, count);
		t2.run();
		ThreadMosseNero t3 = new ThreadMosseNero(18, 26, count);
		t3.run();
		ThreadMosseNero t4 = new ThreadMosseNero(27, 35, count);
		t4.run();
		t4.join();
		t3.join();
		t2.join();
		t1.join();
		result.addAll(t1.getResult());
		result.addAll(t2.getResult());
		result.addAll(t3.getResult());
		result.addAll(t4.getResult());
		Collections.sort(result);
		return result;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(70);
		sb.append("[");
		int realIndex = 0;
		for (int i = 0; i < 8; i++) {
			if (i % 2 == 0) {
				for (int j = 0; j < 8; j++) {
					if (j % 2 == 0) {
						sb.append("B\t");
					} else {
						if (realIndex != 8 && realIndex != 17 && realIndex != 26) {
							sb.append(board[realIndex] + " " + realIndex + "\t");
							realIndex++;
						} else {
							realIndex++;
							j--;
						}
					}
				}
				sb.append("]\n[");
			} else {
				for (int j = 0; j < 8; j++) {
					if (j % 2 != 0) {
						sb.append("B\t");
					} else {
						if (realIndex != 8 && realIndex != 17 && realIndex != 26) {
							sb.append(board[realIndex] + " " + realIndex + "\t");
							realIndex++;
						} else {
							realIndex++;
							j--;
						}
					}
				}
				if (i != 7) {
					sb.append("]\n[");
				} else {
					sb.append("]\n");
				}
			}
		}
		return sb.toString();
	}
	// ----------------------------------------------------------------------

	/*
	 * Oggetto Thread che ricerca in un insieme di celle contigue, le mosse possiili
	 * per il giocatore bianco
	 */
	private class ThreadMosseBianco extends Thread {
		// Conviene eliminare AtomicInteger per evitare gestione con semafori e
		// preferire visitare tutte le 8 celle
		private int inizio;
		private int fine;
		private AtomicInteger count;
		private LinkedList<Move> result;

		public ThreadMosseBianco(int inizio, int fine, AtomicInteger count) {
			this.inizio = inizio;
			this.fine = fine;
			this.count = count;
			this.result = new LinkedList<Move>();
		}

		public void run() {
			for (int i = inizio; i < fine; i++) {
				if (board[i] > 0) {
					count.addAndGet(board[i]);
					result.addAll(mosseRaggiungibiliBianco(i));
				}
				if (count.get() == pedineBianche) {
					return;
				}
			}
		}

		public LinkedList<Move> getResult() {
			return result;
		}
	}

	/*
	 * Oggetto Thread che ricerca in un insieme di celle contigue, le mosse possiili
	 * per il giocatore mero
	 */
	private class ThreadMosseNero extends Thread {
		private int inizio;
		private int fine;
		private AtomicInteger count;
		private LinkedList<Move> result;

		public ThreadMosseNero(int inizio, int fine, AtomicInteger count) {
			this.inizio = inizio;
			this.fine = fine;
			this.count = count;
			this.result = new LinkedList<Move>();
		}

		public void run() {
			for (int i = inizio; i < fine; i++) {
				if (board[i] < 0) {
					count.addAndGet(-board[i]);
					result.addAll(mosseRaggiungibiliNero(i));
				}
				if (count.get() == pedineNere) {
					return;
				}
			}
		}

		public LinkedList<Move> getResult() {
			return result;
		}
	}
}
