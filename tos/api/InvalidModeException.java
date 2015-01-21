package tos.api;

/**
 * This exception indicates that an attempt has been made
 * to open a file with a non-existent mode.
 * 
 * The only accepted modes are "r" (read), and "w" (write).
 */

public class InvalidModeException extends Exception
{
	public InvalidModeException() { super(); }
	public InvalidModeException(String message) { super(message); }
}