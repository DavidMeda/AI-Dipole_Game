import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import dipole.MessageHandler;
import dipole.Move;
import dipole.Player;

public class F2D {
	public static void main(String[] args) throws InterruptedException, UnknownHostException, IOException {
		mainServer(args[0].trim(), Integer.parseInt(args[1].trim()));	
	}
	
	/*
	 * WELCOME <colour> Il server comunica al client il colore che gli è stato assegnato.
	 * MESSAGE <message> Il server comunica al client qualcosa. OPPONENT_MOVE <move> Il server
	 * comunica al client l'ultima mossa dell'avversario. YOUR_TURN Il server comunica al
	 * client che è il suo turno. VALID_MOVE Il server conferma al client che la sua ultima
	 * mossa è valida. ILLEGAL_MOVE Il server informa il client che la sua ultima mossa non è
	 * valida (l'avversario vince). TIMEOUT Il server informa il client che il tempo per
	 * comunicare la mossa è scaduto. VICTORY Il server informa il client che ha vinto la
	 * partita. TIE Il server informa il client che la partita è finita in parità. DEFEAT Il
	 * server informa il client che ha perso la partita
	 */

	private static void mainServer(String serverIp, int serverPort) throws UnknownHostException, IOException, InterruptedException {
		new Converter();
		MessageHandler socket = null;
		try{
			socket = new MessageHandler(serverIp, serverPort);
			Player p = null;
			String mess = "";
			do{
				mess = socket.readLine();
				if(mess.startsWith("WELCOME")){
					System.out.println(mess.substring(8));
					if(mess.substring(8).trim().toLowerCase().equals("white")){
						p = new Player(true);
						System.out.println("GIOCHI COME BIANCO");
					} else if(mess.substring(8).trim().toLowerCase().equals("black")){
						p = new Player(false);
						System.out.println("GIOCHI COME NERO");
					}
				} else{
					System.out.println(socket.readLine());
				}
			} while (mess.startsWith("WELCOME"));
			while (true){
				mess = socket.readLine();
				if(mess.startsWith("YOUR_TURN")){
					Move m = p.elaborateMove();
					socket.send(m.toString());
					Player.getBoard().make(m);
					p.setTimeout(950);
				} else if(mess.startsWith("VALID_MOVE")){
					System.out.println("Mossa valida");
				} else if(mess.startsWith("ILLEGAL_MOVE")){
					System.out.println("Hai effettuato una mossa non consentita");
					break;
				} else if(mess.startsWith("OPPONENT_MOVE")){
					String[] move = mess.substring(13).trim().split(",");
					String dir = move[1].toUpperCase();
					int numPed = Integer.parseInt(move[2]);
					int index = Converter.get(move[0]);
					Player.getBoard().update(index, dir, (byte) numPed);
				} else if(mess.startsWith("VICTORY")){
					System.out.println("HAI VINTO!");
					break;
				} else if(mess.startsWith("DEFEAT")){
					System.out.println("HAI PERSO");
					break;
				} else if(mess.startsWith("TIE")){
					System.out.println("HAI PAREGGIATO");
					break;
				} else if(mess.startsWith("MESSAGE")) System.out.println(mess.substring(8));
				else if(mess.startsWith("TIMEOUT")) System.out.println("TEMPO SCADUTO, HAI PERSO!");
			}
		} finally{
			socket.close();
		}
	}
	
	public static class Converter {
		private static HashMap<String, Integer> cells = new HashMap<String, Integer>();

		private Converter() {
			cells.put("A2", 0);
			cells.put("A4", 1);
			cells.put("A6", 2);
			cells.put("A8", 3);
			cells.put("B1", 4);
			cells.put("B3", 5);
			cells.put("B5", 6);
			cells.put("B7", 7);
			cells.put("C2", 9);
			cells.put("C4", 10);
			cells.put("C6", 11);
			cells.put("C8", 12);
			cells.put("D1", 13);
			cells.put("D3", 14);
			cells.put("D5", 15);
			cells.put("D7", 16);
			cells.put("E2", 18);
			cells.put("E4", 19);
			cells.put("E6", 20);
			cells.put("E8", 21);
			cells.put("F1", 22);
			cells.put("F3", 23);
			cells.put("F5", 24);
			cells.put("F7", 25);
			cells.put("G2", 27);
			cells.put("G4", 28);
			cells.put("G6", 29);
			cells.put("G8", 30);
			cells.put("H1", 31);
			cells.put("H3", 32);
			cells.put("H5", 33);
			cells.put("H7", 34);
		}

		public static int get(String s) {
			return cells.get(s);
		}
	}
}
