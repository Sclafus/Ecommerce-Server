/**
 * Class {@code People} is a class that should never be instantiated.
 * This class is used as a container for Class {@code Customer} and
 * its subclass {@code Employee}.
 * @see Customer
 * @see Employee
 */
public abstract class People {
	
	protected String name;
	protected String surname;
	protected String email;
	protected int permission;

	/**
	 * {@code People} Class constructor. This constructor should NEVER
	 * be called.
	 */
	protected People() {
		this.name = "";
		this.surname = "";
		this.email = "";
		this.permission = 1;
	}

	/**
	 * {@code People} Class constructor. 
	 * This constructor should NEVER be called.
	 * @param name name of the new user [String]
	 * @param sur surname(lastname) of the new user [String]
	 * @param email email of the new user [String]
	 */
	protected People(final String name, final String sur, final String email, final int perm) {
		this.name = name;
		this.surname = sur;
		this.email = email;
		this.permission = perm;
	}

}