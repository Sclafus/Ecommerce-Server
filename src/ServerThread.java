import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;

public class ServerThread extends Thread {
	private Socket socket;

	ServerThread(Socket socket) {
		this.socket = socket;
	}

	public void run() {

		try {
			InputStream inputStream = this.socket.getInputStream();
			ObjectInputStream in = new ObjectInputStream(inputStream);
			try {
				String[] message = (String[]) in.readObject();
				OutputStream outputStream = this.socket.getOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(outputStream);
				// stuff happens here!
				switch (message[0]) {
					case "login":
					Boolean login_result = login(message[1], message[2]);
					out.writeObject(login_result);
						break;

					default:
						break;
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Connection getConnection() {

		try {
			// username and password are in clear, who cares tbh, just for testing purposes
			String driver = "com.mysql.cj.jdbc.Driver";
			String url = "jdbc:mysql://localhost:3306/assignment3";
			String username = "root";
			String password = "admin";
			Class.forName(driver);

			Connection connection = DriverManager.getConnection(url, username, password);
			return connection;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Boolean login(String username, String password) {
		Connection connection = getConnection();
		String query = String.format("SELECT email, password FROM user WHERE email='%s'", username);
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			if(!query_result.next()){
				return false;
			} else {
				String pwd = query_result.getString("password");
				
				if (password.equals(pwd)){
					return true;
				} else {
					return false;
				}
			}
		} catch (SQLException e) {
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

			ArrayList<Object[]> result = new ArrayList<Object[]>();

			if(fields.length > 0){
				ArrayList<Object> tmp = new ArrayList<Object>();
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
			} else {

			}

		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
