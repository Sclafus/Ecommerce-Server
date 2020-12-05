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
import java.util.Collections;
import java.util.stream.Collectors;

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
						User login_result = login(msg[1], msg[2]);
						out.writeObject(login_result);
						break;

					case "register_user":
						// TODO: int -> user
						int register_user_result = register(msg[1], msg[2], msg[3], msg[4], 1);
						out.writeObject(register_user_result);
						break;

					case "register_employee":
						// TODO: int->user
						int register_employee_result = register(msg[1], msg[2], msg[3], msg[4], 2);
						out.writeObject(register_employee_result);
						break;

					case "add_wine":
						Boolean add_wine_result = add_wine(msg[1], Integer.parseInt(msg[2]), msg[3], msg[4], msg[5]);
						out.writeObject(add_wine_result);
						break;

					case "restock_wine":
						Boolean restock_wine_result = restock(Integer.parseInt(msg[1]), Integer.parseInt(msg[2]));
						out.writeObject(restock_wine_result);
						break;

					case "get_employees":
						ArrayList<User> employees = getEmployees();
						out.writeObject(employees);
						break;

					case "search":
						ArrayList<Wine> search_result = search(msg[1], msg[2]);
						out.writeObject(search_result);
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
			// username and password are in clear and have all the permissions.
			// this is ok since we are not in production 🤷‍♂️
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

	/**
	 * Checks if the user with the selected email and password is present in the
	 * database.
	 * 
	 * @param email    the email of the {@code User}. [String]
	 * @param password the password of the {@code User}. [String]
	 * @return {@code User} object. {@code nullUser} if the account is not
	 *         registered or the password is wrong, else the correct {@code User}.
	 * @see User
	 */
	public static User login(String email, String password) {

		Connection connection = getConnection();
		String query = String.format("SELECT * FROM user WHERE email='%s'", email);
		User nullUser = new User();

		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			if (query_result.next()) {
				// account exists
				String name = query_result.getString("name");
				String surname = query_result.getString("surname");
				String pwd = query_result.getString("password");
				int permission = query_result.getInt("permission");
				if (password.equals(pwd)) {
					User user = new User(name, surname, email, pwd, permission);
					return user;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// returns nullUser if the password is wrong or account doesn't exists.
		return nullUser;
	}

	// TODO permission check, add User as a parameter
	/**
	 * 
	 * @param name     of the {@code Wine}. [String]
	 * @param year     of production of the {@code Wine}. [int]
	 * @param producer of the {@code Wine}. [int]
	 * @param grapes   used for the {@code Wine}. [String]
	 * @param notes
	 * @return [Boolean]
	 */
	public static Boolean add_wine(String name, int year, String producer, String grapes, String notes) {

		Connection connection = getConnection();
		String query = String.format("SELECT name, year, producer FROM wine WHERE name='%s', year=%d, producer='%s'",
				name, year, producer);
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			if (query_result.next()) {
				String insert_query = String.format(
						"INSERT INTO wine(name, year, producer, grapeWines, notes) VALUES ('%s', %d, '%s', '%s', '%s')",
						name, year, producer, grapes, notes);

				PreparedStatement insert_statement = connection.prepareStatement(insert_query);
				insert_statement.executeUpdate();
				System.out.format("Wine %s %s has been added\n", name, year);
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static int register(String name, String surname, String mail, String password, int permission) {

		Connection connection = getConnection();
		String query = String.format("SELECT mail FROM user WHERE mail='%s'", mail);
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			if (!query_result.next()) {
				// user has not been registered yet.
				return -1;
			} else {
				String query1 = String.format(
						"INSERT INTO user(name, surname, email, password, permission) VALUES ('%s', '%s', '%s', '%s', %d)",
						name, surname, mail, password, permission);

				try {
					PreparedStatement statement1 = connection.prepareStatement(query1);
					statement1.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
				} finally {
					System.out.format("User %s has been added\n", mail);
				}
				return 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static ArrayList<User> getEmployees() {
		Connection connection = getConnection();
		String query = "SELECT name,surname,email FROM user WHERE permission=2";
		ArrayList<User> employees_list = new ArrayList<User>();

		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			while (query_result.next()) {
				String name = query_result.getString("name");
				String surname = query_result.getString("surname");
				String email = query_result.getString("email");
				User employee = new User(name, surname, email, "", 2);
				employees_list.add(employee);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return employees_list;

	}

	public static ArrayList<Order> getOrders() {
		Connection connection = getConnection();
		String query_id = "SELECT order_id FROM order";
		ArrayList<Order> orders_list = new ArrayList<Order>();

		try {
			PreparedStatement statement_id = connection.prepareStatement(query_id);
			ResultSet query_result_id = statement_id.executeQuery();
			ArrayList<Integer> order_id_duplicates = new ArrayList<Integer>();

			while (query_result_id.next()) {
				order_id_duplicates.add(query_result_id.getInt("order_id"));
			}

			ArrayList<Integer> order_ids = (ArrayList<Integer>) order_id_duplicates.stream().distinct()
					.collect(Collectors.toList());

			Collections.sort(order_ids);

			for (int order_id : order_ids) {
				String query = String.format("SELECT * FROM order WHERE id=%d", order_id);
				PreparedStatement statement = connection.prepareStatement(query);
				ResultSet query_result = statement.executeQuery();
				ArrayList<Wine> products = new ArrayList<Wine>();

				String email = query_result.getString("email");
				Boolean shipped = query_result.getBoolean("shipped");

				while (query_result.next()) {
					int product_id = query_result.getInt("product_id");
					int quantity = query_result.getInt("quantity");

					String query_wine = String.format("SELECT * FROM wine WHERE product_id=%d", product_id);
					PreparedStatement statement_wine = connection.prepareStatement(query_wine);
					ResultSet query_result_wine = statement_wine.executeQuery();

					String name = query_result_wine.getString("name");
					String producer = query_result_wine.getString("producer");
					int year = query_result_wine.getInt("year");
					String notes = query_result_wine.getString("notes");
					String grapes = query_result_wine.getString("grapes");

					Wine tmp = new Wine(product_id, name, producer, year, notes, quantity, grapes);
					products.add(tmp);
				}

				Order new_order = new Order(order_id, shipped, email, (Wine[]) products.toArray());
				orders_list.add(new_order);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return orders_list;
	}

	public static Boolean restock(int id, int new_quantity) {
		Connection connection = getConnection();
		String query = String.format("SELECT quantity FROM wine WHERE product_id = %d", id);

		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			while (query_result.next()) {
				int old_quantity = query_result.getInt("quantity");

				String query_restock = String.format("UPDATE wine SET quantity = %d WHERE product_id = %d",
						new_quantity + old_quantity, id);

				PreparedStatement statement_restock = connection.prepareStatement(query_restock);
				statement_restock.executeUpdate();
				System.out.format("Wine %d has been restocked\n", id);
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static ArrayList<Wine> search(String name, String year_string) {
		ArrayList<Wine> search_result_list = new ArrayList<Wine>();
		Connection connection = getConnection();

		String query = "";
		int year = -1;
		try {
			year = Integer.parseInt(year_string);

		} catch (NumberFormatException e) {
			// year is null
			if (!name.equals(null)) {
				// case 1: name not null, year null
				query = String.format("SELECT * FROM wine WHERE name='%s'", name);
			} else {
				// case 2:everything is null, return, there's nothing to search
				return search_result_list;
			}

		} finally {
			if (name.equals(null)) {
				// case 3: name null, year not null
				query = String.format("SELECT * FROM wine WHERE year=%d", year);
			} else {
				// case 4: nothing is null
				query = String.format("SELECT * FROM wine WHERE year=%d AND name='%s'", year, name);
			}
		}

		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet results = statement.executeQuery();

			while (results.next()) {
				int wine_product_id = results.getInt("product_id");
				String wine_name = results.getString("name");
				int wine_year = results.getInt("year");
				String wine_producer = results.getString("producer");
				String wine_grapes = results.getString("grapeWines");
				int wine_quantity = results.getInt("quantity");
				String wine_notes = results.getString("notes");
				Wine wine = new Wine(wine_product_id, wine_name, wine_producer, wine_year, wine_notes, wine_quantity,
						wine_grapes);
				search_result_list.add(wine);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return search_result_list;
	}

	/*
	 * public static ArrayList<Object[]> execQuery(String query, String... fields) {
	 * try { //connects to db, executes the query, gets result Connection connection
	 * = getConnection(); PreparedStatement statement =
	 * connection.prepareStatement(query); ResultSet query_result =
	 * statement.executeQuery();
	 * 
	 * ArrayList<Object[]> result = new ArrayList<Object[]>();
	 * 
	 * if(fields.length > 0){ ArrayList<Object> tmp = new ArrayList<Object>();
	 * 
	 * checks every element of result, inserts it in tmp, tmp gets converted to an
	 * array of strings and then cleared,
	 * 
	 * 
	 * while (query_result.next()){ for (String field : fields){
	 * tmp.add(query_result.getString(field)); } Object[] tuple_array =
	 * tmp.toArray(); result.add(tuple_array); tmp.clear(); } return result; } else
	 * {
	 * 
	 * }
	 * 
	 * } catch (Exception e){ e.printStackTrace(); } return null; }
	 */
}