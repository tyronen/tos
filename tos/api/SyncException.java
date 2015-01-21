//SyncException.java
package tos.api;

/** Thrown when a problem occurs when handling a sync object.
 */

public class SyncException extends Exception
{
	public SyncException()
	{
		super();
	}
	
	public SyncException(String message)
	{
		super(message);
	}
}
