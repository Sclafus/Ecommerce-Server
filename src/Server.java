import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	private static final int PORT = 4316;

	public static void main(String[] args) throws IOException {
		new Server().run();
	}
	
	@SuppressWarnings({ "resource" })
	public void run() throws IOException {

		ServerSocket serverSocket = new ServerSocket(PORT);
		System.out.println("Server ready for connections!");
		int i = 0;
		
		while(true){
			Socket socket = serverSocket.accept();
			System.out.format("Connection %d\n", ++i);
			new ServerThread(socket).start();
		}
		
	}
}
