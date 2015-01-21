package tos.api;

/** 
 * Thrown to indicate that 
 * a file server has reached its maximum 
 * number of files and cannot create any more.
 * 
 * If you are running into this exception regularly,
 * you may wish to use a Disk with a larger number
 * of files permitted.
 * 
 * @see tos.system.Disk#newInode
 */

public class FilesFullException extends Exception
{
	public FilesFullException()
	{
		super();
	}
	
	public FilesFullException(String message)
	{
		super(message);
	}
}
