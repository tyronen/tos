package tos.api;

/** Thrown if a process attempts to use a TOS file without 
 * opening it first.
 * 
 * @see TOSFile@open
 */

public class TOSFileNotOpenException extends Exception
{
	public TOSFileNotOpenException() { super(); }
	public TOSFileNotOpenException(String message) { super(message); }
}
