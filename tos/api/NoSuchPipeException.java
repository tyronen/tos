package tos.api;

/** 
 * Thrown when an attempt is made to access a pipe which 
 * does not exist.
 * 
 * If previous attempts to open this pipe have succeeded,
 * it may indicate the pipe has been automatically deleted.
 * Pipe servers delete a pipe when all processes that used it have
 * released it.
 * 
 * Less probably, it may indicate that either the pipe or its pipe server
 * have been removed from the system by the administrator.
 * 
 */

public class NoSuchPipeException extends Exception
{
	public NoSuchPipeException() { super(); }
	public NoSuchPipeException(String message) { super(message); }
}
