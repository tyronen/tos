package tos.api;

/**
 * Indicates that a searched-for object or file cannot be found
 * within the system.
 */

public class NotFoundException extends Exception
{
	public NotFoundException() { super(); }
	public NotFoundException(String message) { super(message); }
}
