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

					case "register_user":
						int register_user_result = register(msg[1], msg[2], msg[3], msg[4], 1);
						out.writeObject(register_user_result);
						break;

					case "register_employee":
						int register_employee_result = register(msg[1], msg[2], msg[3], msg[4], 2);
						out.writeObject(register_employee_result);
						break;
					
					case "add_wine":
						int add_wine_result = add_wine(msg[1], Integer.parseInt(msg[2]), msg[3], msg[4], msg[5]);
						out.writeObject(add_wine_result);
						break;

					case "restock_wine":
						break;
					
					case "get_employees":
						ArrayList<User> employees = getEmployees();
						out.writeObject(employees);
						break;
						
					case "":
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
	

	public static int add_wine(String name, int year, String producer, String grapes, String notes){

		Connection connection = getConnection();
		String query = String.format("SELECT name, year, producer FROM wine WHERE name='%s', year='%d', producer='%s'",
		 name, year, producer);
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			if(!query_result.next()){
				//wine has not been added yet.
				return -1;
			} else{
				String query1 = String.format("INSERT INTO wine(name, year, producer, grapeWines, notes) VALUES ('%s', '%d', '%s', '%s', '%s')", 
				name, year, producer, grapes, notes);

				try{
					PreparedStatement statement1 = connection.prepareStatement(query1);
					statement1.executeUpdate();
				} catch (SQLException e){
					e.printStackTrace();
				} finally{
					System.out.format("Wine %s %s has been added\n", name, year);
				}
				return 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}


	public static int register(String name, String surname, String mail, String password, int permission){
				
		Connection connection = getConnection();
		String query = String.format("SELECT mail FROM user WHERE mail='%s'", mail);
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			if(!query_result.next()){
				//user has not been registered yet.
				return -1;
			} else{
				String query1 = String.format("INSERT INTO user(name, surname, email, password, permission) VALUES ('%s', '%s', '%s', '%s', '%d')", 
				name,surname, mail, password, permission);

				try{
					PreparedStatement statement1 = connection.prepareStatement(query1);
					statement1.executeUpdate();
				} catch (SQLException e){
					e.printStackTrace();
				} finally{
					System.out.format("User %s has been added\n", mail);
				}
				return 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	
	public static ArrayList<User> getEmployees(){
		Connection connection = getConnection();
		String query = "SELECT name,surname,email FROM user WHERE permission='2'";
		ArrayList<User> employees_list = new ArrayList<User>();
		
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			while(query_result.next()){
				String name = query_result.getString("name");
				String surname = query_result.getString("surname");
				String email = query_result.getString("email");
				User tmp = new User(name, surname, email, "", 2);
				employees_list.add(tmp);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return employees_list;

	}
	// public static int restock()
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
