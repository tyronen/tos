package tos.system;

/** Thrown if a disk cannot be found.
 */

public class NoDiskException extends Exception
{
	public NoDiskException() { super(); }
	public NoDiskException(String message) { super(message); }
}