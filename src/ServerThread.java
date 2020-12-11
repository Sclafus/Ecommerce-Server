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

//TODO javadoc
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
			InputStream input_stream = this.socket.getInputStream(); 
			ObjectInputStream in = new ObjectInputStream(input_stream);

			try {
				// input from the client
				String[] msg = (String[]) in.readObject();
				OutputStream output_stream = this.socket.getOutputStream(); 
				ObjectOutputStream out = new ObjectOutputStream(output_stream);

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

					case "get_orders_employee":
						ArrayList<Order> orders2 = getOrders(msg[1], "2");
						out.writeObject(orders2);
						break;

					case "get_orders_user":
						ArrayList<Order> orders3 = getOrders(msg[1], "1");
						out.writeObject(orders3);
						break;

					case "get_wines":
						ArrayList<Wine> wines = search("", "");
						out.writeObject(wines);
						break;

					case "add_to_cart":
						Boolean add_to_cart_result = addToCart(msg[1], Integer.parseInt(msg[2]),
								Integer.parseInt(msg[3]));
						out.writeObject(add_to_cart_result);
						break;

					case "remove_from_cart":
						Boolean remove_from_cart_result = removeFromCart(msg[1], Integer.parseInt(msg[2]));
						out.writeObject(remove_from_cart_result);
						break;

					case "new_order":
						Order buy_result = addOrder(msg[1]);
						out.writeObject(buy_result);
						break;

					case "ship_order":
						Boolean shipped = shipOrder(Integer.parseInt(msg[1]));
						out.writeObject(shipped);
						break;

					case "get_notifications":
						ArrayList<Wine> notification_wines = getNotifications(msg[1]);
						out.writeObject(notification_wines);
						break;

					case "display_cart":
						ArrayList<Wine> display_result = displayCart(msg[1]);
						out.writeObject(display_result);
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
			String url = "jdbc:mysql://localhost:3306/assignment3?useLegacyDatetimeCode=false&serverTimezone=UTC";
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
	 * @return {@code User} object. {@code null_user} if the account is not
	 *         registered or the password is wrong, else the correct {@code User}.
	 * @see User
	 */
	public static User login(String email, String password) {

		Connection connection = getConnection();
		String query = String.format("SELECT * FROM user WHERE email='%s'", email);
		User null_user = new User(); 

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
		// returns null_user if the password is wrong or account doesn't exists.
		return null_user; 
	}

	/**
	 * Allows to add a wine to the wine table contained in the database. It checks
	 * first if the wine the employee is trying to add is already present or not. If
	 * so, it returns a {@code null_wine} (operation not successful), otherwise it adds the
	 * new wine to the database and then creates the object {@code Wine} of the wine
	 * just added.
	 * 
	 * @param name     of the {@code Wine}. [String]
	 * @param year     of production of the {@code Wine}. [int]
	 * @param producer of the {@code Wine}. [int]
	 * @param grapes   used for the {@code Wine}. [String]
	 * @param notes    notes for the {@code Wine}. [String]
	 * @return the object {@code Wine} of the wine inserted if the insertion has
	 *         been successful or a {@code null_wine} if not. [Wine]
	 * @see Wine
	 */
	public static Wine addWine(String name, int year, String producer, String grapes, String notes) {
		Wine null_wine = new Wine(); 

		Connection connection = getConnection();
		String select_query = String.format(
				"SELECT name, year, producer FROM wine WHERE name='%s' AND year=%d AND producer='%s'", name, year,
				producer);
		try {
			PreparedStatement statement = connection.prepareStatement(select_query);
			ResultSet query_result = statement.executeQuery();

			if (!query_result.next()) {
				//no wine with such name, year and producer was found so it is inserted
				String insert_query = String.format(
						"INSERT INTO wine(name, year, producer, grapeWines, notes, quantity) VALUES ('%s', %d, '%s', '%s', '%s', 0)",
						name, year, producer, grapes, notes);

				PreparedStatement insert_statement = connection.prepareStatement(insert_query);
				insert_statement.executeUpdate();

				// building Wine object
				String query_id = String.format(
						"SELECT product_id FROM wine WHERE name='%s' AND year=%d AND producer='%s' AND grapeWines='%s' AND notes='%s'",
						name, year, producer, grapes, notes);

				PreparedStatement statement_id = connection.prepareStatement(query_id);
				ResultSet query_id_result = statement_id.executeQuery();
				if (query_id_result.next()) {
					//Wine object is created and returned
					int id = query_id_result.getInt("product_id");
					System.out.format("Wine %s %s has been added\n", name, year);
					Wine new_wine = new Wine(id, name, producer, year, notes, 0, grapes);
					return new_wine;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		//operation failed and null_wine is returned
		return null_wine; 
	}

	/**
	 * Checks if the user with the selected email is present in the database. Then,
	 * if not, it registers the person with the selected permission.
	 * 
	 * @param name       the name of the {@code User}. [String]
	 * @param surname    the surname of the {@code User}. [String]
	 * @param email      the email of the {@code User}. [String]
	 * @param password   the password of the {@code User}. [String]
	 * @param permission the permission of the {@code User}. [String]
	 * @return {@code User} object. {@code null_user} if the account is already
	 *         registered, else the correct {@code User}.
	 * @see User
	 */
	public static User register(String name, String surname, String mail, String password, int permission) {

		Connection connection = getConnection();
		String query = String.format("SELECT email FROM user WHERE email='%s'", mail);
		User null_user = new User(); 

		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			if (query_result.next()) {
				//user has already been registered.
				return null_user;
			} else {
				//user never registered
				String insert_query = String.format(
						"INSERT INTO user(name, surname, email, password, permission) VALUES ('%s', '%s', '%s', '%s', %d)",
						name, surname, mail, password, permission); 

				PreparedStatement insert_statement = connection.prepareStatement(insert_query); 
				insert_statement.executeUpdate(); 
				User new_user = new User(name, surname, mail, password, permission); 
				System.out.format("User %s has been added\n", mail);
				//registration successful, the User object is returned
				return new_user; 
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		//registration not successful, the null_user object is returned
		return null_user; 
	}

	/**
	 * Gets all the users with the selected permission.
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
				//gets the user's informations
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

	// TODO fix javadoc & add comments
	/**
	 * Returns the list of the orders that have been placed if no 
	 * parameters are given. If a parameter is passed it can return 
	 * the list of all the orders made by the given {@code User}
	 * if the permission of the {@code User} is 1, otherwise it returns 
	 * all the {@code Order} made by any {@code User} which have not been
	 * shipped yet (the employee can perform this action).
	 * @return ArrayList with all the Orders. [ArrayList<Order>]
	 * @see Order
	 */
	public static ArrayList<Order> getOrders(String... user) {
		Connection connection = getConnection();
		ArrayList<Order> orders_list = new ArrayList<Order>();
		//no optional parameter passed
		//it returns the list of all the orders -> only the admins can visualize it
		String query_id = "";
		if (user.length == 0) {
			query_id = "SELECT order_id FROM assignment3.order";
		}
		//optional parameters are passed
		if (user.length == 2) {
			//checks the parameter user[1] 
			//it represents the permissione of the User calling the method
			//permission = 1 -> the basic User is calling the method, only his orders can be shown
			if (user[1] == "1") {
				query_id = String.format("SELECT order_id FROM assignment3.order WHERE email='%s'", user[0]);
			} else { //permission != 1 -> the employee is calling the method, only unshipped orders can be shown
				query_id = String.format("SELECT order_id FROM assignment3.order WHERE shipped=false", user[0]);//TODO wwhy the user[0]
			}
		}
//TODO FINISH COMMENTS HERE
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
				String query = String.format("SELECT * FROM assignment3.order WHERE order_id=%d", order_id);
				PreparedStatement statement = connection.prepareStatement(query);
				ResultSet query_result = statement.executeQuery();
				ArrayList<Wine> products = new ArrayList<Wine>();

				String email = "";
				Boolean shipped = false;
				while (query_result.next()) {
					email = query_result.getString("email");
					shipped = query_result.getBoolean("shipped");
					int product_id = query_result.getInt("product_id");
					int quantity = query_result.getInt("quantity");

					String query_wine = String.format("SELECT * FROM assignment3.wine WHERE product_id=%d", product_id);
					PreparedStatement statement_wine = connection.prepareStatement(query_wine);
					ResultSet query_result_wine = statement_wine.executeQuery();

					while (query_result_wine.next()) {
						String name = query_result_wine.getString("name");
						String producer = query_result_wine.getString("producer");
						int year = query_result_wine.getInt("year");
						String notes = query_result_wine.getString("notes");
						String grapes = query_result_wine.getString("grapeWines");
						Wine tmp = new Wine(product_id, name, producer, year, notes, quantity, grapes);
						products.add(tmp);
					}
				}

				Order new_order = new Order(order_id, shipped, email, products);
				orders_list.add(new_order);

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return orders_list;
	}

	/**
	 * Allows to restock a wine by adding to the existing quantity a new quantity
	 * specified by the employee. It returns the {@code true} if the operation is
	 * successful, otherwise it returns the {@code false}.
	 * 
	 * @param id           of the {@code Wine}. [int]
	 * @param new_quantity the quantity that we want to restock. [int]
	 * @return {@code true} if the wine has been restocked, {@code false} if the
	 *         wine has not been restocked for whatever reason. [Boolean]
	 * @see Wine
	 */
	public static Boolean restock(int id, int new_quantity) {
		Connection connection = getConnection();
		String query = String.format("SELECT quantity FROM wine WHERE product_id = %d", id);

		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet query_result = statement.executeQuery();

			if (query_result.next()) {
				//the Wine to restock is found
				int old_quantity = query_result.getInt("quantity");
				//the quantity is updated
				String query_restock = String.format("UPDATE wine SET quantity = %d WHERE product_id = %d",
						new_quantity + old_quantity, id);

				PreparedStatement statement_restock = connection.prepareStatement(query_restock);
				statement_restock.executeUpdate();
				// notifications
				String notifications_query = String.format("UPDATE notification SET send=true WHERE product_id=%d", id);
				PreparedStatement notification_statement = connection.prepareStatement(notifications_query);
				notification_statement.executeUpdate();
				//successful restock
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		//unsuccessful restock
		return false;
	}

	/**
	 * Responds with a list with all the wines corriponding to the given search
	 * constraints. The research can be done either by year, by name or both of the
	 * {@code Wine}. Once the wines are found, their corrisponding objects are added
	 * to a list which is then returned.
	 * 
	 * @param name        of the wine to search. [String]
	 * @param year_string of the wine to search. [String]
	 * @return ArrayList with all the Wines found. [ArrayList<Wine>]
	 * @see Wine
	 */
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
				// case 2:everything is null, return, there's nothing to search //TODO Fix this
				query = "SELECT * FROM wine";
			}

		} finally {
			if (name.equals("") && year != 0) {
				// case 3: name null, year not null
				query = String.format("SELECT * FROM wine WHERE year=%d", year);
			} else if (year != 0) {
				// case 4: nothing is null 
				query = String.format("SELECT * FROM wine WHERE year=%d AND name='%s'", year, name);
			}
		}

		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet results = statement.executeQuery();

			while (results.next()) {
				//Wines withe the given costraints are found
				int wine_product_id = results.getInt("product_id");
				String wine_name = results.getString("name");
				int wine_year = results.getInt("year");
				String wine_producer = results.getString("producer");
				String wine_grapes = results.getString("grapeWines");
				int wine_quantity = results.getInt("quantity");
				String wine_notes = results.getString("notes");
				//Wine object is created
				Wine wine = new Wine(wine_product_id, wine_name, wine_producer, wine_year, wine_notes, wine_quantity,
						wine_grapes);
				//Wine object is added to the list of the search results
				search_result_list.add(wine);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		//list of search results is returned
		return search_result_list;
	}

	/**
	 * Allows the {@code User} to add a quantity of a {@code Wine} to the cart.
	 * It returns the {@code true} if the operation is successful,
	 * else the {@code false}. 
	 * 
	 * @param email    of the {@code User} adding to the cart. [String]
	 * @param id       of the {@code Wine}. [int]
	 * @param quantity the quantity that the {@code User} wants to buy. [int]
	 * @return {@code true} if the wine has been added to the cart, {@code false} if
	 *         the wine has not been added to the cart for whatever reason.
	 *         [Boolean]
	 * @see User
	 * @see Wine
	 */
	public static Boolean addToCart(String email, int id, int quantity) {
		Connection connection = getConnection();
		String query = String.format("INSERT INTO cart(email, product_id, quantity) VALUES ('%s', %d, %d)", email, id,
				quantity);
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			statement.executeUpdate();
			return true;

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	// TODO javadoc and comments
	public static ArrayList<Wine> getNotifications(String email) {
		Connection connection = getConnection();
		ArrayList<Wine> notification_list = new ArrayList<Wine>();
		String query_select_notification = String
				.format("SELECT product_id, send FROM assignment3.notification WHERE email='%s'", email);

		try {
			PreparedStatement select_notification_statement = connection.prepareStatement(query_select_notification);
			ResultSet result_select_notification = select_notification_statement.executeQuery();

			while (result_select_notification.next()) {
				int product_id = result_select_notification.getInt("product_id");
				if (result_select_notification.getBoolean("send")) {
					String query_select_wine = String.format("SELECT * FROM assignment3.wine WHERE product_id=%d",
							product_id);
					PreparedStatement select_wine_statement = connection.prepareStatement(query_select_wine);
					ResultSet result_select_wine = select_wine_statement.executeQuery();

					if (result_select_wine.next()) {
						String name = result_select_wine.getString("name");
						int year = result_select_wine.getInt("year");
						String producer = result_select_wine.getString("producer");
						String grapeWines = result_select_wine.getString("grapeWines");
						int quantity = result_select_wine.getInt("quantity");
						String notes = result_select_wine.getString("notes");
						Wine wine = new Wine(product_id, name, producer, year, notes, quantity, grapeWines);
						notification_list.add(wine);
					}
				}
			}
			String delete_notification_query = String
					.format("DELETE FROM assignment3.notification WHERE email='%s' AND send=true", email);
			PreparedStatement delete_query_statement = connection.prepareStatement(delete_notification_query);
			delete_query_statement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return notification_list;
	}

	// TODO javadoc and comments
	public static Boolean addNotification(String email, int product_id) {
		Connection connection = getConnection();
		String insert_notification_query = String
				.format("INSERT INTO notification(email, product_id) VALUES ('%s', %d)", email, product_id);

		try {
			PreparedStatement insert_notification_statement = connection.prepareStatement(insert_notification_query);
			insert_notification_statement.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Allows the {@code User} to place a {@code Order}. It returns an object
	 * {@code Order} if the operation was successful, else a {@code null_order}.
	 * 
	 * @param email    of the {@code User} placing the {@code Order}. [String]
	 * @return {@code Order} if the operation is successful, otherwise a {@code null_order}.
	 *         [Order]
	 * @see User
	 * @see Wine
	 */
	public static Order addOrder(String email) {

		ArrayList<Wine> wines_order = new ArrayList<Wine>();
		ArrayList<Integer> ids = new ArrayList<Integer>();
		Order null_order = new Order(); 
		Boolean shipped = false;
		Connection connection = getConnection();

		//selects all the order ids from the order table and adds them to a list
		String get_id_query = String.format("SELECT order_id FROM assignment3.order");
		try {
			PreparedStatement get_id_statement = connection.prepareStatement(get_id_query);
			ResultSet get_id_result = get_id_statement.executeQuery();

			while (get_id_result.next()) {
				int order_id = get_id_result.getInt("order_id");
				ids.add(order_id);
			}
			//gets the max id from the list, it refers to the last order placed
			int max_id = Collections.max(ids);
			System.out.println(max_id);
			int order_id = max_id + 1;
			//selects all the items the user wants to buy from the cart table
			String select_query = String.format("SELECT * FROM cart WHERE email = '%s'", email);

			PreparedStatement select_statement = connection.prepareStatement(select_query);
			ResultSet select_query_result = select_statement.executeQuery();

			while (select_query_result.next()) {
				int wine_product_id = select_query_result.getInt("product_id");
				int wine_quantity = select_query_result.getInt("quantity");
				//selects the wines the user wants to buy from the wine table
				String query_wine = String.format("SELECT * FROM wine WHERE product_id = %d", wine_product_id);

				PreparedStatement wine_statement = connection.prepareStatement(query_wine);
				ResultSet wine_query_result = wine_statement.executeQuery();

				if (wine_query_result.next()) {
					int stock_quantity = wine_query_result.getInt("quantity");

					//checks if the quantity the user wants of a certain wine is in stock
					if (stock_quantity >= wine_quantity && stock_quantity > 0) {
						String wine_name = wine_query_result.getString("name");
						String wine_producer = wine_query_result.getString("producer");
						String wine_grapeWines = wine_query_result.getString("grapeWines");
						String wine_notes = wine_query_result.getString("notes");
						int wine_year = wine_query_result.getInt("year");

						//creates the object new_wine and adds it to the list of all the wines the user wants to buy
						Wine new_wine = new Wine(wine_product_id, wine_name, wine_producer, wine_year, wine_notes,
								wine_quantity, wine_grapeWines);
						wines_order.add(new_wine);
						//uses the restock method with a negative quantity to subtract from the quantity in stock of a certain wine the quantity the user is buying
						restock(wine_product_id, -wine_quantity);
						//inserts the new order to the orders table
						String query = String.format(
								"INSERT INTO assignment3.order(order_id, product_id, quantity, email, shipped) VALUES (%d, %d, %d, '%s', %b)",
								order_id, wine_product_id, wine_quantity, email, false);

						PreparedStatement statement = connection.prepareStatement(query);
						statement.executeUpdate();

						//deletes from the cart table the cart of the user whose order has just been placed
						String delete_cart_query = String.format("DELETE FROM cart WHERE email='%s'", email);
						PreparedStatement delete_cart_statement = connection.prepareStatement(delete_cart_query);
						delete_cart_statement.executeUpdate(); //TODO WHY 2 DELETE FROM CART?
					} else {
						addNotification(email, wine_product_id);
					}
				}
			}
			//creates the object of the new order
			Order new_order = new Order(order_id, shipped, email, wines_order);

			String delete_cart_query = String.format("DELETE FROM cart WHERE email='%s'", email);
			PreparedStatement delete_query_statement = connection.prepareStatement(delete_cart_query);
			delete_query_statement.executeUpdate();
			//returns the object of the new order once the order is completed
			return new_order;

		} catch (SQLException e) {
			e.printStackTrace();
		}
		//returns a null_order object if the placing of the order fails
		return null_order;
	}

	/**
	 * Allows the {@code User} to view his cart to review it.
	 * 
	 * @param email    of the {@code User}. [String]
	 * @return the ArrayList with all the {@code Wine} present in the user's cart.
	 *         [ArrayList<Wine>]
	 * @see Wine
	 * @see User
	 */
	public static ArrayList<Wine> displayCart(String email) {
		ArrayList<Wine> display_cart_list = new ArrayList<Wine>();

		Connection connection = getConnection();
		String get_cart_query = String.format("SELECT * FROM cart WHERE email = '%s'", email);

		try {
			PreparedStatement cart_statement = connection.prepareStatement(get_cart_query);
			ResultSet cart_query_result = cart_statement.executeQuery();

			while (cart_query_result.next()) {
				int product_id = cart_query_result.getInt("product_id");
				int quantity = cart_query_result.getInt("quantity");
				String get_wine_query = String.format("SELECT * FROM wine WHERE product_id = %d", product_id);

				PreparedStatement wine_statement = connection.prepareStatement(get_wine_query);
				ResultSet wine_query_result = wine_statement.executeQuery();
				if (wine_query_result.next()) {

					String wine_name = wine_query_result.getString("name");
					String wine_producer = wine_query_result.getString("producer");
					String wine_grapeWines = wine_query_result.getString("grapeWines");
					String wine_notes = wine_query_result.getString("notes");
					int wine_year = wine_query_result.getInt("year");

					Wine new_wine = new Wine(product_id, wine_name, wine_producer, wine_year, wine_notes, quantity,
							wine_grapeWines);
					display_cart_list.add(new_wine);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return display_cart_list;
	}

	/**
	 * Allows the {@code User} to remove from the cart any of the wine added to it.
	 * 
	 * @param email    of the {@code User}. [String]
	 * @param id       of te {@code wine} that the {@code User} wants to remove from the cart. [int]
	 * @return {@code true} if the operation is successful, otherwise the {@code false}.
	 *         [Boolean]
	 * @see User
	 * @see Wine
	 */
	public static Boolean removeFromCart(String email, int id) {
		Connection connection = getConnection();
		String query = String.format("DELETE FROM cart WHERE email='%s' AND product_id=%d", email, id);
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			statement.executeUpdate();
			return true;

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	// TODO javadoc
	public static Boolean shipOrder(int order_id) {
		Connection connection = getConnection();
		String query_select_order = String.format("UPDATE assignment3.order SET shipped=true WHERE order_id=%d",
				order_id);

		try {
			PreparedStatement statement_select_order = connection.prepareStatement(query_select_order);
			statement_select_order.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;

	}
}