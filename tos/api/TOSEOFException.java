package tos.api;

/** Thrown when a TOS File reaches its end.
 */

public class TOSEOFException extends Exception
{
	public TOSEOFException() { super(); }
	public TOSEOFException(String message) { super(message); }
}
