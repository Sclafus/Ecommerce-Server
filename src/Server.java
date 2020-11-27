import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	public static final int PORT = 4316;

	public static void main(String[] args) throws IOException {
		new Server().run();
	}

	public void run() throws IOException {

		ServerSocket serverSocket = new ServerSocket(PORT);
		System.out.println("Server ready for connections!");
		
		while(true){
			Socket socket = serverSocket.accept();
			new ServerThread(socket).start();
		}
		
	}
}
