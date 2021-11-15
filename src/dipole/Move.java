package dipole;

public class Move implements Comparable<Move> {
	static final byte SHIFT = 0;
	static final byte MERGE = 1;
	static final byte CAPTURE = 2;
	static final byte EXIT = 3;
	private static final char[] LETTERS = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H' };
	private final int index;
	private final byte numPedine;
	private final String direzione;
	private final byte moveType;
	private final boolean white;// giocatore che fa la mossa
	private int indexDest;
	private byte pedineMangiate; // serve in reverse
	private int weight;

	public Move(int indexPar, byte numPedine, String direzione, boolean white, byte moveType, int indexDest) {
		this.index = indexPar;
		this.numPedine = numPedine;
		this.direzione = direzione;
		this.white = white;
		this.moveType = moveType;
		this.indexDest = indexDest;
	}

	public Move(int indexPar, byte numPedine, String direzione, boolean white, byte moveType) {
		this.index = indexPar;
		this.numPedine = numPedine;
		this.direzione = direzione;
		this.white = white;
		this.moveType = moveType;
	}

	public int getIndex() {
		return index;
	}

	public byte getNumPedine() {
		return numPedine;
	}

	public String getDirezione() {
		return direzione;
	}

	public int getIndexDest() {
		return indexDest;
	}

	public void setIndexDest(int indexDest) {
		this.indexDest = indexDest;
	}

	public byte getPedineMangiate() {
		return pedineMangiate;
	}

	public void setPedineMangiate(byte pedineMangiate) {
		this.pedineMangiate = pedineMangiate;
	}

	public boolean isWhite() {
		return white;
	}

	public byte getMoveType() {
		return moveType;
	}

	public String getMoveTypeString() {
		if (moveType == 0)
			return "SHIFT";
		if (moveType == 1)
			return "MERGE";
		if (moveType == 2)
			return "CAPTURE";
		return "EXIT";
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		int row = Board.getRow(index);
		String start = String.format("%s%d", LETTERS[row], Board.getColumn(index), row);
		return String.format("MOVE %s,%s,%d", start, direzione, numPedine);
	}

	@Override
	public int compareTo(Move m) {
		// Ordinamento inverso collection ordina dal piu piccolo al piu grande.
		if (this.weight < m.weight)
			return 1;
		else if (this.weight > m.weight)
			return -1;
		else if (this.numPedine < m.numPedine)
			return -1;
		else if (this.numPedine > m.numPedine)
			return 1;
		return 0;
	}
}
