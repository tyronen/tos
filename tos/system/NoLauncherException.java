package tos.system;

/** Thrown if an operation is unable to find a specified launcher.
 */
public class NoLauncherException extends Exception
{
	public NoLauncherException() { super(); }
	public NoLauncherException(String message) { super(message); }
}
