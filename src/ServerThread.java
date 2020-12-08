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

	/**
	 * Disambiguation function. Gets the input from the socket (it has to be a
	 * String array), calls the right function, returns result.
	 */
	public void run() {

		try {
			InputStream inputStream = this.socket.getInputStream();
			ObjectInputStream in = new ObjectInputStream(inputStream);

			try {
				// input from the client
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
						User register_user_result = register(msg[1], msg[2], msg[3], msg[4], 1);
						out.writeObject(register_user_result);
						break;

					case "register_employee":
						User register_employee_result = register(msg[1], msg[2], msg[3], msg[4], 2);
						out.writeObject(register_employee_result);
						break;

					case "add_wine":
						Wine add_wine_result = addWine(msg[1], Integer.parseInt(msg[2]), msg[3], msg[4], msg[5]);
						out.writeObject(add_wine_result);
						break;

					case "restock_wine":
						Boolean restock_wine_result = restock(Integer.parseInt(msg[1]), Integer.parseInt(msg[2]));
						out.writeObject(restock_wine_result);
						break;

					case "get_employees":
						ArrayList<User> employees = getUsers(2);
						out.writeObject(employees);
						break;

					case "get_users":
						ArrayList<User> users = getUsers(1);
						out.writeObject(users);
						break;

					case "search":
						ArrayList<Wine> search_result = search(msg[1], msg[2]);
						out.writeObject(search_result);
						break;

					case "get_orders":
						ArrayList<Order> orders = getOrders();
						out.writeObject(orders);
						break;

					default:
						break;
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			System.out.println("Client disconnected.");
		}
	}

	/**
	 * Connects to MySQL with jdbc driver.
	 * 
	 * @return object connected to MySQL. [Connection]
	 * @see Connection
	 */
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

	// TODO Finish this javadoc
	/**
	 * 
	 * @param name     of the {@code Wine}. [String]
	 * @param year     of production of the {@code Wine}. [int]
	 * @param producer of the {@code Wine}. [int]
	 * @param grapes   used for the {@code Wine}. [String]
	 * @param notes    notes for the {@code Wine}. [String]
	 * @return {@code true} if the operation is successfull, else {@code false}.
	 *         [Boolean]
	 * @see Wine
	 */
	public static Wine addWine(String name, int year, String producer, String grapes, String notes) {
		Wine nullWine = new Wine();

		Connection connection = getConnection();
		String select_query = String.format(
				"SELECT name, year, producer FROM wine WHERE name='%s' AND year=%d AND producer='%s'", name, year,
				producer);
		try {
			PreparedStatement statement = connection.prepareStatement(select_query);
			ResultSet query_result = statement.executeQuery();

			if (!query_result.next()) {
				String insert_query = String.format(
						"INSERT INTO wine(name, year, producer, grapeWines, notes) VALUES ('%s', %d, '%s', '%s', '%s')",
						name, year, producer, grapes, notes);

				PreparedStatement insert_statement = connection.prepareStatement(insert_query);
				insert_statement.executeUpdate();
				System.out.format("Wine %s %s has been added\n", name, year);

				// building Wine object
				String query_id = String.format(
						"SELECT product_id FROM wine WHERE name='%s' AND year=%d AND producer='%s' AND grapeWines='%s' AND notes='%s'",
						name, year, producer, grapes, notes);
				PreparedStatement statement_id = connection.prepareStatement(query_id);
				ResultSet query_id_result = statement_id.executeQuery();
				int id = query_id_result.getInt("product_id");
				Wine new_wine = new Wine(id, name, producer, year, notes, 0, grapes);
				return new_wine;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return nullWine;
	}

	/**
	 * Checks if the user with the selected email is present in the database. Then,
	 * if not it registers the person with the selected permission.
	 * 
	 * @param name       the name of the {@code User}. [String]
	 * @param surname    the surname of the {@code User}. [String]
	 * @param email      the email of the {@code User}. [String]
	 * @param password   the password of the {@code User}. [String]
	 * @param permission the permission of the {@code User}. [String]
	 * @return {@code User} object. {@code nullUser} if the account is already
	 *         registered, else the correct {@code User}.
	 * @see User
	 */
	public static User register(String name, String surname, String mail, String password, int permission) {

		Connection connection = getConnection();
		String query = String.format("SELECT email FROM user WHERE email='%s'", mail);
		User nullUser = new User();

		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			if (query_result.next()) {
				// user has already been registered.
				return nullUser;
			} else {
				String query1 = String.format(
						"INSERT INTO user(name, surname, email, password, permission) VALUES ('%s', '%s', '%s', '%s', %d)",
						name, surname, mail, password, permission);

				PreparedStatement statement1 = connection.prepareStatement(query1);
				statement1.executeUpdate();
				User newUser = new User(name, surname, mail, password, permission);
				System.out.format("User %s has been added\n", mail);
				return newUser;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return nullUser;
	}

	/**
	 * Gets all the user with the selected permission.
	 * 
	 * @param permission the selected permission. [int]
	 * @return ArrayList with all the Users. [ArrayList<User>]
	 * @see User
	 */
	public static ArrayList<User> getUsers(int permission) {
		Connection connection = getConnection();
		String query = String.format("SELECT name,surname,email FROM user WHERE permission=%d", permission);
		ArrayList<User> user_list = new ArrayList<User>();

		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			while (query_result.next()) {
				String name = query_result.getString("name");
				String surname = query_result.getString("surname");
				String email = query_result.getString("email");
				User user = new User(name, surname, email, "", permission);
				user_list.add(user);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return user_list;

	}

	/**
	 * Returns the list of all the orders that have been placed.
	 * 
	 * @return ArrayList with all the Orders. [ArrayList<Order>]
	 * @see Order
	 */
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

	/**
	 * 
	 * @param id           of the {@code Wine}. [int]
	 * @param new_quantity the quantity that we want to restock. [int]
	 * @return {@code true} if the wine has been restocked, {@code false} if the
	 *         wine has not been restocked for whatever reason. [Boolean]
	 */
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

	// ? regex?
	//TODO javadoc
	public static ArrayList<Wine> search(String name, String year_string) {
		ArrayList<Wine> search_result_list = new ArrayList<Wine>();
		Connection connection = getConnection();
		String query = "";
		int year = 0;

		try {
			year = Integer.parseInt(year_string);

		} catch (NumberFormatException e) {
			// year is null
			if (!name.equals("")) {
				// case 1: name not null, year null
				query = String.format("SELECT * FROM wine WHERE name='%s'", name);
			} else {
				// case 2:everything is null, return, there's nothing to search
				return search_result_list;
			}

		} finally {
			if (name.equals("")) {
				// case 3: name null, year not null
				query = String.format("SELECT * FROM wine WHERE year=%d", year);
			} else if (year != 0) {
				// case 4: nothing is null
				query = String.format("SELECT * FROM wine WHERE year=%d AND name='%s'", year, name);
			}
		}
		System.out.println(query);

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

}