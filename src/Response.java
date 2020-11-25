
import java.io.Serializable;

/**
*
* The class {@code Response} provides a simplified model of a response message.
*
**/
public class Response implements Serializable
{
  private static final long serialVersionUID = 1L;

  private final int value;

  /**
   * Class constructor.
   *
   * @param v  the value.
   *
  **/
  public Response(final int v)
  {
    this.value = v;
  }

  /**
   * Gets the value.
   *
   * @return the value.
   *
  **/
  public int getValue()
  {
    return this.value;
  }
}
