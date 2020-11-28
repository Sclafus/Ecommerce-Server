import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.sql.Connection;
import java.sql.DriverManager;
public class ServerThread extends Thread {
	private Socket socket;

	ServerThread(Socket socket) {
		this.socket = socket;
	}

	public void run() {

		try {
			String message = null;
			BufferedReader bufferReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			Connection db = getConnection();

			while ((message = bufferReader.readLine()) != null) {
				PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
				System.out.println(message);
				printWriter.println("server received: " + message);
			}
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static Connection getConnection(){
		
		try{
			//username and password are in clear, who cares tbh
			String driver = "com.mysql.cj.jdbc.Driver";
			String url = "jdbc:mysql://localhost:3306/assignment3";
			String username = "root";
			String password = "admin";
			Class.forName(driver);
		
			Connection connection = DriverManager.getConnection(url, username, password);
			System.out.println("database connected!");
			return connection;
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}	

}
