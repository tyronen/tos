//TOSFileException.java

package tos.api;

/** Thrown when an exception occurs with a TOS File that is not
 * covered by one of the other exception classes.
 */

public class TOSFileException extends Exception
{
	public TOSFileException()
	{
		super();
	}
	
	public TOSFileException(String message)
	{
		super(message);
	}
}
