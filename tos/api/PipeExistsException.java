package tos.api;

/** Thrown when an attempt is made to create a pipe
 * that already exists
 * 
 * @see Pipe#create
 */

public class PipeExistsException extends Exception
{
	public PipeExistsException() { super(); }
	public PipeExistsException(String message) { super(message); }
}
