package tos.api;

/**
 * Indicates an incorrect password.
 * 
 * This exception is thrown when an attempt is made
 * to open a protected file with the wrong password,
 * or without supplying a password when permissions 
 * indicate one should be provided.
 * 
 */
public class InvalidPasswordException extends Exception
{
	public InvalidPasswordException() { super(); }
	public InvalidPasswordException(String message) { super(message); }
}
