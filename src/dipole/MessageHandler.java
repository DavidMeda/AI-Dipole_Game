package dipole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MessageHandler {
	// Gestisce la comunicazione con il server
	private Board observer;
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	public MessageHandler(String serverAddress, int port) throws UnknownHostException, IOException {
		socket = new Socket(serverAddress, port);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
	}

	public void notify(int index, byte numPed, String direction) {
		observer.update(index, direction, numPed);
	}

	public void send(String msg) {
		out.println(msg);
	}

	public String readLine() throws IOException {
		return in.readLine();
	}

	public void close() throws IOException {
		socket.close();
	}
}
