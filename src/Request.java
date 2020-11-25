import java.io.Serializable;

/**
*
* The class {@code Request} provides a simplified model of a request message.
*
**/
public class Request implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private final String value;

	/**
	 * Class constructor.
	 *
	 * @param v  the value.
	 *
	 **/
	public Request(final String v){
		this.value = v;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value.
	 *
	 **/
	public String getValue(){
    	return this.value;
	}
}
