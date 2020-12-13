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
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * This class when instantiated will make a new thread, accept the incoming
 * message from the socket, route it with the different options and responds
 * with appropriate output for the different methods.
 */
public class ServerThread extends Thread {
	private Socket socket;

	/**
	 * ServerThread class constructor. This constructor is called automatically by
	 * {@code Server} to invoke a new thread.
	 * 
	 * @param socket the socket passed by the {@code Server}
	 * @see Server
	 */
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
						User loginResult = login(msg[1], msg[2]);
						out.writeObject(loginResult);
						break;

					case "guest":
						// returns a null user, it's used to login as a guest
						out.writeObject(new User());
						break;

					case "register_user":
						User registerUserResult = register(msg[1], msg[2], msg[3], msg[4], 1);
						out.writeObject(registerUserResult);
						break;

					case "register_employee":
						User registerEmployeeResult = register(msg[1], msg[2], msg[3], msg[4], 2);
						out.writeObject(registerEmployeeResult);
						break;

					case "add_wine":
						Wine addWineResult = addWine(msg[1], Integer.parseInt(msg[2]), msg[3], msg[4], msg[5]);
						out.writeObject(addWineResult);
						break;

					case "restock_wine":
						Boolean restockWineResult = restock(Integer.parseInt(msg[1]), Integer.parseInt(msg[2]));
						out.writeObject(restockWineResult);
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
						ArrayList<Wine> searchResult = search(msg[1], msg[2]);
						out.writeObject(searchResult);
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
						Boolean addToCartResult = addToCart(msg[1], Integer.parseInt(msg[2]), Integer.parseInt(msg[3]));
						out.writeObject(addToCartResult);
						break;

					case "remove_from_cart":
						Boolean removeFromCartResult = removeFromCart(msg[1], Integer.parseInt(msg[2]));
						out.writeObject(removeFromCartResult);
						break;

					case "new_order":
						Order buyResult = addOrder(msg[1]);
						out.writeObject(buyResult);
						break;

					case "ship_order":
						Boolean shipped = shipOrder(Integer.parseInt(msg[1]));
						out.writeObject(shipped);
						break;

					case "get_notifications":
						ArrayList<Wine> notificationWines = getNotifications(msg[1]);
						out.writeObject(notificationWines);
						break;

					case "display_cart":
						ArrayList<Wine> displayResult = displayCart(msg[1]);
						out.writeObject(displayResult);
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
			// this is ok since we are not in production
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
			ResultSet queryResult = statement.executeQuery();

			if (queryResult.next()) {
				// account exists
				String name = queryResult.getString("name");
				String surname = queryResult.getString("surname");
				String pwd = queryResult.getString("password");
				int permission = queryResult.getInt("permission");
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

	/**
	 * Allows to add a wine to the wine table contained in the database. It checks
	 * first if the wine the employee is trying to add is already present or not. If
	 * so, it returns a {@code nullWine} (operation not successful), otherwise it
	 * adds the new wine to the database and then creates the object {@code Wine} of
	 * the wine just added.
	 * 
	 * @param name     of the {@code Wine}. [String]
	 * @param year     of production of the {@code Wine}. [int]
	 * @param producer of the {@code Wine}. [int]
	 * @param grapes   used for the {@code Wine}. [String]
	 * @param notes    notes for the {@code Wine}. [String]
	 * @return the object {@code Wine} of the wine inserted if the insertion has
	 *         been successful or a {@code nullWine} if not. [Wine]
	 * @see Wine
	 */
	public static Wine addWine(String name, int year, String producer, String grapes, String notes) {
		Wine nullWine = new Wine();
		Connection connection = getConnection();
		String selectQuery = String.format(
				"SELECT name, year, producer FROM wine WHERE name='%s' AND year=%d AND producer='%s'", name, year,
				producer);

		try {
			PreparedStatement statement = connection.prepareStatement(selectQuery);
			ResultSet queryResult = statement.executeQuery();

			if (!queryResult.next()) {
				// no wine with such name, year and producer was found so it is inserted
				String insertQuery = String.format(
						"INSERT INTO wine(name, year, producer, grapeWines, notes, quantity) VALUES ('%s', %d, '%s', '%s', '%s', 0)",
						name, year, producer, grapes, notes);
				PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
				insertStatement.executeUpdate();
				// building Wine object
				String queryId = String.format(
						"SELECT product_id FROM wine WHERE name='%s' AND year=%d AND producer='%s' AND grapeWines='%s' AND notes='%s'",
						name, year, producer, grapes, notes);
				PreparedStatement statementId = connection.prepareStatement(queryId);
				ResultSet queryIdResult = statementId.executeQuery();

				if (queryIdResult.next()) {
					// Wine object is created and returned
					int id = queryIdResult.getInt("product_id");
					System.out.format("Wine %s %s has been added\n", name, year);
					Wine newWine = new Wine(id, name, producer, year, notes, 0, grapes);
					return newWine;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// operation failed and nullWine is returned
		return nullWine;
	}

	/**
	 * Checks if the user with the selected email is present in the database. Then,
	 * if not, it registers the person with the selected permission.
	 * 
	 * @param name       the name of the {@code User}. [String]
	 * @param surname    the surname of the {@code User}. [String]
	 * @param mail       the email of the {@code User}. [String]
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
			ResultSet queryResult = statement.executeQuery();

			if (queryResult.next()) {
				// user has already been registered.
				return nullUser;
			} else {
				// user never registered
				String insertQuery = String.format(
						"INSERT INTO user(name, surname, email, password, permission) VALUES ('%s', '%s', '%s', '%s', %d)",
						name, surname, mail, password, permission);
				PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
				insertStatement.executeUpdate();
				User newUser = new User(name, surname, mail, password, permission);
				// registration successful, the User object is returned
				return newUser;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// registration not successful, the nullUser object is returned
		return nullUser;
	}

	/**
	 * Gets all the users with the selected permission.
	 * 
	 * @param permission the selected permission. [int]
	 * @return ArrayList with all the Users. [ArrayList of User]
	 * @see User
	 */
	public static ArrayList<User> getUsers(int permission) {
		Connection connection = getConnection();
		String query = String.format("SELECT name,surname,email FROM user WHERE permission=%d", permission);
		ArrayList<User> userList = new ArrayList<User>();

		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet queryResult = statement.executeQuery();

			while (queryResult.next()) {
				// gets the user's informations
				String name = queryResult.getString("name");
				String surname = queryResult.getString("surname");
				String email = queryResult.getString("email");
				User user = new User(name, surname, email, "", permission);
				userList.add(user);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return userList;
	}

	/**
	 * This method can be used in two ways:
	 * <ul>
	 * <li>no parameters: returns all the {@code Order}s</li>
	 * <li>two parameters: if the second parameter is 1, it will return all the
	 * {@code Order}s made by the specified user in the first parameter. Else, it
	 * will return all the unshipped {@code Order}s.</li>
	 * </ul>
	 * 
	 * @param user [optional] Argument 1: email of the user. [String] Argument 2:
	 *             permission. [String]
	 * @return ArrayList with all the Orders. [ArrayList of Order]
	 * @see User
	 * @see Order
	 */
	public static ArrayList<Order> getOrders(String... user) {
		Connection connection = getConnection();
		ArrayList<Order> ordersList = new ArrayList<Order>();
		// no optional parameter passed
		// it returns the list of all the orders -> only the admins can access
		String queryId = "";
		if (user.length == 0) {
			queryId = "SELECT order_id FROM assignment3.order";
		}
		// optional parameters are passed
		if (user.length == 2) {
			// checks the parameter user[1]
			// it represents the permission of the User calling the method
			if (user[1] == "1") {
				// permission = 1 -> the basic User is calling the method, we select all of his
				// orders.
				queryId = String.format("SELECT order_id FROM assignment3.order WHERE email='%s'", user[0]);
			} else {
				// permission != 1 -> the employee is calling the method, only unshipped orders
				// can be shown
				queryId = "SELECT order_id FROM assignment3.order WHERE shipped=false";
			}
		}

		try {
			PreparedStatement statementId = connection.prepareStatement(queryId);
			ResultSet queryResultId = statementId.executeQuery();
			ArrayList<Integer> orderIdDuplicates = new ArrayList<Integer>();
			// gets all the orders from the order table.
			while (queryResultId.next())
				orderIdDuplicates.add(queryResultId.getInt("order_id"));

			// we have multiple entries for a determined orderId,
			// we have to delete the duplicates
			ArrayList<Integer> orderIds = (ArrayList<Integer>) orderIdDuplicates.stream().distinct()
					.collect(Collectors.toList());
			Collections.sort(orderIds);

			for (int orderId : orderIds) {
				// select a determined order and all of his entries in the DBMS
				String query = String.format("SELECT * FROM assignment3.order WHERE order_id=%d", orderId);
				PreparedStatement statement = connection.prepareStatement(query);
				ResultSet queryResult = statement.executeQuery();
				ArrayList<Wine> products = new ArrayList<Wine>();
				String email = "";
				Boolean shipped = false;

				while (queryResult.next()) {
					// gets the wine of the selected entry
					email = queryResult.getString("email");
					shipped = queryResult.getBoolean("shipped");
					int productId = queryResult.getInt("product_id");
					int quantity = queryResult.getInt("quantity");
					// gets the wine from the DBMS
					String queryWine = String.format("SELECT * FROM assignment3.wine WHERE product_id=%d", productId);
					PreparedStatement statementWine = connection.prepareStatement(queryWine);
					ResultSet queryResultWine = statementWine.executeQuery();

					while (queryResultWine.next()) {
						// instantiate the wine object
						String name = queryResultWine.getString("name");
						String producer = queryResultWine.getString("producer");
						int year = queryResultWine.getInt("year");
						String notes = queryResultWine.getString("notes");
						String grapes = queryResultWine.getString("grapeWines");
						Wine tmp = new Wine(productId, name, producer, year, notes, quantity, grapes);
						// adds the wine to the arraylist of wines for the present order
						products.add(tmp);
					}
				}
				// instantiate the order object
				Order newOrder = new Order(orderId, shipped, email, products);
				ordersList.add(newOrder);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return ordersList;
	}

	/**
	 * Allows to restock a wine by adding to the existing quantity a new quantity
	 * specified by the employee. It returns the {@code true} if the operation is
	 * successful, otherwise it returns the {@code false}.
	 * 
	 * @param id          of the {@code Wine}. [int]
	 * @param newQuantity the quantity that we want to restock. [int]
	 * @return {@code true} if the wine has been restocked, {@code false} if the
	 *         wine has not been restocked for whatever reason. [Boolean]
	 * @see Wine
	 */
	public static Boolean restock(int id, int newQuantity) {
		Connection connection = getConnection();
		String query = String.format("SELECT quantity FROM wine WHERE product_id = %d", id);

		try {
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet queryResult = statement.executeQuery();

			if (queryResult.next()) {
				// the Wine to restock is found
				int oldQuantity = queryResult.getInt("quantity");
				// the quantity is updated
				String queryRestock = String.format("UPDATE wine SET quantity = %d WHERE product_id = %d",
						newQuantity + oldQuantity, id);
				PreparedStatement statementRestock = connection.prepareStatement(queryRestock);
				statementRestock.executeUpdate();
				// notifications
				if (newQuantity > 0) {
					String notificationsQuery = String.format("UPDATE notification SET send=true WHERE product_id=%d",
							id);
					PreparedStatement notificationStatement = connection.prepareStatement(notificationsQuery);
					notificationStatement.executeUpdate();
				}
				// successful restock
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// unsuccessful restock
		return false;
	}

	/**
	 * Responds with a list with all the wines corriponding to the given search
	 * constraints. The research can be done either by year, by name or both of the
	 * {@code Wine}. Once the wines are found, their corrisponding objects are added
	 * to a list which is then returned.
	 * 
	 * @param name       of the wine to search. [String]
	 * @param yearString of the wine to search. [String]
	 * @return ArrayList with all the Wines found. [ArrayList of Wine]
	 * @see Wine
	 */
	public static ArrayList<Wine> search(String name, String yearString) {
		ArrayList<Wine> searchResultList = new ArrayList<Wine>();
		Connection connection = getConnection();
		String query = "";
		int year = 0;

		try {
			year = Integer.parseInt(yearString);
		} catch (NumberFormatException e) {
			// year is null
			if (!name.equals("")) {
				// case 1: name not null, year null
				query = String.format("SELECT * FROM wine WHERE name='%s'", name);
			} else {
				// case 2:everything is null, there's nothing to search,
				// gives all the wines
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
				// Wines withe the given costraints are found
				int wineProductId = results.getInt("product_id");
				String wineName = results.getString("name");
				int wineYear = results.getInt("year");
				String wineProducer = results.getString("producer");
				String wineGrapes = results.getString("grapeWines");
				int wineQuantity = results.getInt("quantity");
				String wineNotes = results.getString("notes");
				// Wine object is created
				Wine wine = new Wine(wineProductId, wineName, wineProducer, wineYear, wineNotes, wineQuantity,
						wineGrapes);
				// Wine object is added to the list of the search results
				searchResultList.add(wine);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// list of search results is returned
		return searchResultList;
	}

	/**
	 * Allows the {@code User} to add a quantity of a {@code Wine} to the cart. It
	 * returns the {@code true} if the operation is successful, else the
	 * {@code false}.
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
			return false;
		}
	}

	/**
	 * Gets all the notifications from the notifications' table where the email's
	 * field in the table corrisponds with the given {@code User}'s email. It
	 * returns a ArrayList of Wine containing all the wines the {@code User} need to
	 * be notificated for.
	 * 
	 * @param email of the {@code User}. [String]
	 * @return the ArrayList with the {@code Wine} the {@code User} needs to be
	 *         notificated for. [ArrayList of Wine]
	 * @see User
	 * @see Wine
	 */
	public static ArrayList<Wine> getNotifications(String email) {
		Connection connection = getConnection();
		ArrayList<Wine> notificationList = new ArrayList<Wine>();
		// select all the notifications to send
		String querySelectNotification = String
				.format("SELECT product_id, send FROM assignment3.notification WHERE email='%s' AND send=true", email);

		try {
			PreparedStatement selectNotificationStatement = connection.prepareStatement(querySelectNotification);
			ResultSet resultSelectNotification = selectNotificationStatement.executeQuery();

			while (resultSelectNotification.next()) {
				int productId = resultSelectNotification.getInt("product_id");
				String querySelectWine = String.format("SELECT * FROM assignment3.wine WHERE product_id=%d", productId);
				PreparedStatement selectWineStatement = connection.prepareStatement(querySelectWine);
				ResultSet resultSelectWine = selectWineStatement.executeQuery();

				if (resultSelectWine.next()) {
					String name = resultSelectWine.getString("name");
					int year = resultSelectWine.getInt("year");
					String producer = resultSelectWine.getString("producer");
					String grapeWines = resultSelectWine.getString("grapeWines");
					int quantity = resultSelectWine.getInt("quantity");
					String notes = resultSelectWine.getString("notes");
					Wine wine = new Wine(productId, name, producer, year, notes, quantity, grapeWines);
					notificationList.add(wine);
				}
			}
			String deleteNotificationQuery = String
					.format("DELETE FROM assignment3.notification WHERE email='%s' AND send=true", email);
			PreparedStatement deleteQueryStatement = connection.prepareStatement(deleteNotificationQuery);
			deleteQueryStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return notificationList;
	}

	/**
	 * Adds a new notification to the notifications' table.
	 * 
	 * @param email     of the {@code User} who will receive the notification.
	 *                  [String]
	 * @param productId of the {@code wine} that the {@code User} will be
	 *                  notificated about. [int]
	 * @return {@code true} if the operation is successful, otherwise the
	 *         {@code false}. [Boolean]
	 * @see User
	 * @see Wine
	 */
	public static Boolean addNotification(String email, int productId) {
		Connection connection = getConnection();
		String insertNotificationQuery = String.format("INSERT INTO notification(email, product_id) VALUES ('%s', %d)",
				email, productId);

		try {
			PreparedStatement insertNotificationStatement = connection.prepareStatement(insertNotificationQuery);
			insertNotificationStatement.executeUpdate();
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * Allows the {@code User} to place a {@code Order}. It returns an object
	 * {@code Order} if the operation was successful, else a {@code nullOrder}.
	 * 
	 * @param email of the {@code User} placing the {@code Order}. [String]
	 * @return {@code Order} if the operation is successful, otherwise a
	 *         {@code nullOrder}. [Order]
	 * @see User
	 * @see Wine
	 */
	public static Order addOrder(String email) {
		ArrayList<Wine> winesOrder = new ArrayList<Wine>();
		ArrayList<Integer> ids = new ArrayList<Integer>();
		ArrayList<Integer> idPostOrder = new ArrayList<Integer>();
		Order nullOrder = new Order();
		int maxId = 0;
		int maxIdPost;
		Boolean shipped = false;
		Connection connection = getConnection();

		// selects all the order ids from the order table and adds them to a list
		String getIdQuery = String.format("SELECT order_id FROM assignment3.order");

		try {
			PreparedStatement getIdStatement = connection.prepareStatement(getIdQuery);
			ResultSet getIdResult = getIdStatement.executeQuery();

			while (getIdResult.next()) {
				int orderId = getIdResult.getInt("order_id");
				ids.add(orderId);
			}
			// gets the max id from the list, it refers to the last order placed
			if (!(ids.size() == 0)){
				maxId = Collections.max(ids);
			}
			int orderId = maxId + 1;
			// selects all the items the user wants to buy from the cart table
			String selectQuery = String.format("SELECT * FROM cart WHERE email = '%s'", email);

			PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
			ResultSet selectQueryResult = selectStatement.executeQuery();

			while (selectQueryResult.next()) {
				int wineProductId = selectQueryResult.getInt("product_id");
				int wineQuantity = selectQueryResult.getInt("quantity");
				// selects the wines the user wants to buy from the wine table
				String queryWine = String.format("SELECT * FROM wine WHERE product_id = %d", wineProductId);
				PreparedStatement wineStatement = connection.prepareStatement(queryWine);
				ResultSet wineQueryResult = wineStatement.executeQuery();

				if (wineQueryResult.next()) {
					int stockQuantity = wineQueryResult.getInt("quantity");

					// checks if the quantity the user wants of a certain wine is in stock
					if (stockQuantity >= wineQuantity && stockQuantity > 0) {
						String wineName = wineQueryResult.getString("name");
						String wineProducer = wineQueryResult.getString("producer");
						String wineGrapeWines = wineQueryResult.getString("grapeWines");
						String wineNotes = wineQueryResult.getString("notes");
						int wineYear = wineQueryResult.getInt("year");

						// creates the object newWine and adds it to the list of all the wines the user
						// wants to buy
						Wine newWine = new Wine(wineProductId, wineName, wineProducer, wineYear, wineNotes,
								wineQuantity, wineGrapeWines);
						winesOrder.add(newWine);
						// uses the restock method with a negative quantity to subtract from the
						// quantity in stock of a certain wine the quantity the user is buying
						restock(wineProductId, -wineQuantity);
						// inserts the new order to the orders table
						String query = String.format(
								"INSERT INTO assignment3.order(order_id, product_id, quantity, email, shipped) VALUES (%d, %d, %d, '%s', %b)",
								orderId, wineProductId, wineQuantity, email, false);
						PreparedStatement statement = connection.prepareStatement(query);
						statement.executeUpdate();

						// deletes from the cart table the cart of the user whose order has just been
						// placed
						String deleteCartQuery = String.format("DELETE FROM cart WHERE email='%s' AND product_id='%d'",
								email, wineProductId);
						PreparedStatement deleteCartStatement = connection.prepareStatement(deleteCartQuery);
						deleteCartStatement.executeUpdate();
					} else {
						addNotification(email, wineProductId);
					}
				}
			}
			// checks if the order was placed or not
			String postOrderQuery = String.format("SELECT order_id FROM assignment3.order");
			PreparedStatement postOrderStatement = connection.prepareStatement(postOrderQuery);
			ResultSet postOrderResult = postOrderStatement.executeQuery();

			while (postOrderResult.next()) {
				int orderIds = postOrderResult.getInt("order_id");
				idPostOrder.add(orderIds);
			}
			maxIdPost = Collections.max(idPostOrder);
			// the order was added to the order table
			if (maxIdPost == orderId) {
				Order newOrder = new Order(orderId, shipped, email, winesOrder);
				// returns the object of the new order once the order is completed
				return newOrder;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NoSuchElementException e) {
			e.printStackTrace();
		}
		// returns a nullOrder object if the placing of the order fails
		return nullOrder;
	}

	/**
	 * Allows the {@code User} to view his cart to review it.
	 * 
	 * @param email of the {@code User}. [String]
	 * @return the ArrayList with all the {@code Wine} present in the user's cart.
	 *         [ArrayList of Wine]
	 * @see Wine
	 * @see User
	 */
	public static ArrayList<Wine> displayCart(String email) {
		ArrayList<Wine> displayCartList = new ArrayList<Wine>();
		Connection connection = getConnection();
		String getCartQuery = String.format("SELECT * FROM cart WHERE email = '%s'", email);

		try {
			PreparedStatement cartStatement = connection.prepareStatement(getCartQuery);
			ResultSet cartQueryResult = cartStatement.executeQuery();

			while (cartQueryResult.next()) {
				// the cart was found
				int productId = cartQueryResult.getInt("product_id");
				int quantity = cartQueryResult.getInt("quantity");
				// selecting each item of the cart from the wine table
				String getWineQuery = String.format("SELECT * FROM wine WHERE product_id = %d", productId);
				PreparedStatement wineStatement = connection.prepareStatement(getWineQuery);
				ResultSet wineQueryResult = wineStatement.executeQuery();

				if (wineQueryResult.next()) {
					// creating the object Wine
					String wineName = wineQueryResult.getString("name");
					String wineProducer = wineQueryResult.getString("producer");
					String wineGrapeWines = wineQueryResult.getString("grapeWines");
					String wineNotes = wineQueryResult.getString("notes");
					int wineYear = wineQueryResult.getInt("year");
					Wine newWine = new Wine(productId, wineName, wineProducer, wineYear, wineNotes, quantity,
							wineGrapeWines);
					// object wine is added to the list of wines to display
					displayCartList.add(newWine);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return displayCartList;
	}

	/**
	 * Allows the {@code User} to remove from the cart any of the wine added to it.
	 * 
	 * @param email of the {@code User}. [String]
	 * @param id    of te {@code Wine} that the {@code User} wants to remove from
	 *              the cart. [int]
	 * @return {@code true} if the operation is successful, otherwise the
	 *         {@code false}. [Boolean]
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

	/**
	 * Allows the {@code User} with permission = 2 (the employee) to ship a selected
	 * {@code Order}.
	 * 
	 * @param orderId of the {@code Order} to ship. [int]
	 * @return {@code true} if the operation is successful, otherwise the
	 *         {@code false}. [Boolean]
	 * @see Order
	 */
	public static Boolean shipOrder(int orderId) {
		Connection connection = getConnection();
		String querySelectOrder = String.format("UPDATE assignment3.order SET shipped=true WHERE order_id=%d", orderId);

		try {
			PreparedStatement statementSelectOrder = connection.prepareStatement(querySelectOrder);
			statementSelectOrder.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
}