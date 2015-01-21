package tos.api;

/** Thrown if a disk is full.
 */
public class DiskFullException extends Exception
{
	public DiskFullException() { super(); }
	public DiskFullException(String message) { super(message); }
}
