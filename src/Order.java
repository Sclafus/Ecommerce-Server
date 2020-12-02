import java.util.ArrayList;
import java.util.Collections;

public class Order {

	private ArrayList<Wine> items = new ArrayList<Wine>();
	private int id;
	private Boolean shipped;

	/**
	 * {@code Order} class constructor.
	 */
	public Order(){
	}

	/**
	 * {@code Order} class constructor.
	 * @param wines the wines the {@code People} wants to buy. [Wine Array]
	 * @see Wine
	 * @see User
	 */
	public Order(final Wine[] wines){
		Collections.addAll(items, wines);
	}

	
	/**
	 * Gets the id of the selected {@code Order}.
	 * @return the id of the {@code Order}. [int]
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Gets the wines from the selected {@code Order}.
	 * @return the wines of the {@code Order}. [Wine Array]
	 * @see Wine
	 */
	public Wine[] getWines(){
		Wine[] wines_arr = new Wine[items.size()]; 
		wines_arr = items.toArray(wines_arr);
		return wines_arr;
	}
}