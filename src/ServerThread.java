import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
public class ServerThread extends Thread {
	private Socket socket;

	ServerThread(Socket socket) {
		this.socket = socket;
	}

	public void run() {

		try {
			String message = null;
			BufferedReader bufferReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//reads from client input
			while ((message = bufferReader.readLine()) != null) {
				for(Object[] b : execQuery("SELECT * FROM assignment3.user", "id", "name", "surname")){
					System.out.println("[");
					for(Object c : b){
						System.out.print((String) c);
					}
					System.out.println("]");
				}
				//server -> client
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
			//username and password are in clear, who cares tbh, just for testing purposes
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

	public static ArrayList<Object[]> execQuery(String query, String... fields) {
		try {
			//connects to db, executes the query, gets result
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			ArrayList<Object> tmp = new ArrayList<Object>();
			ArrayList<Object[]> result = new ArrayList<Object[]>();
			/* 
			checks every element of result, inserts it in tmp,
			tmp gets converted to an array of strings and then cleared,

			*/
			while (query_result.next()){
				for (String field : fields){
					tmp.add(query_result.getString(field));
				}
				Object[] tuple_array = tmp.toArray();
				result.add(tuple_array);
				tmp.clear();
			}
			return result;

		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
