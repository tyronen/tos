package tos.api;

/** Thrown when an error takes place within a pipe or when 
 * dealing with a pipe.
 */

public class PipeException extends Exception
{
	public PipeException() { super(); }
	public PipeException(String message) { super(message); }
}
