import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class ServerThread extends Thread {
	private Socket socket;

	ServerThread(Socket socket) {
		this.socket = socket;
	}

	public void run() {

		try {
			String message = null;
			BufferedReader bufferReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			while((message = bufferReader.readLine()) != null) {
				System.out.println("message received: " + message);
				PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
				printWriter.println();

			}
			//connection reset exception when client stops
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
