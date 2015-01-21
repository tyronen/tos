package tos.api;

/** 
 * Thrown to indicate that a reference to a directory is not valid.
 * 
 * @see tos.api.TOSFile#delete
 * @see tos.api.TOSFile#dir
 */

public class InvalidDirectoryException extends Exception
{
	public InvalidDirectoryException() { super(); }
	public InvalidDirectoryException(String message) { super(message); }
	
}
