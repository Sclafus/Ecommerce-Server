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
				String[] msg = (String[]) in.readObject();
				OutputStream outputStream = this.socket.getOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(outputStream);

				// stuff happens here!
				switch (msg[0]) {

					case "login":
						int login_result = login(msg[1], msg[2]);
						out.writeObject(login_result);
						break;

					case "register":
						int register_result = register(msg[1], msg[2], msg[3], msg[4]);
						out.writeObject(register_result);
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

	public static int login(String username, String password) {
		
		Connection connection = getConnection();
		String query = String.format("SELECT email, password, permission FROM user WHERE email='%s'", username);
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			if(!query_result.next()){
				//account doesn't exist.
				return -1;
			} else {
				//account exists
				String pwd = query_result.getString("password");
				int permission = query_result.getInt("permission");
				if (password.equals(pwd)){
					return permission;
				} else {
					//wrong password
					return 0;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -2;
	}
	
	public static int register(String name, String surname, String mail, String password){
				
		Connection connection = getConnection();
		String query = String.format("INSERT INTO user(name, surname, email, password) VALUES ('%s', '%s', '%s', '%s')", 
		name,surname, mail, password);

		try{
			PreparedStatement statement = connection.prepareStatement(query);
			statement.executeUpdate();
			
		} catch (SQLException e){
			e.printStackTrace();
		}
		finally{
			System.out.format("user %s has been added", mail);
		}
		return 0;
	}
	/*
	public static ArrayList<Object[]> execQuery(String query, String... fields) {
		try {
			//connects to db, executes the query, gets result
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			ArrayList<Object[]> result = new ArrayList<Object[]>();

			if(fields.length > 0){
				ArrayList<Object> tmp = new ArrayList<Object>();
				 
				checks every element of result, inserts it in tmp,
				tmp gets converted to an array of strings and then cleared,
	
				
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
	*/
}
