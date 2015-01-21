package tos.api;

//Pipe.java
import java.io.*;
import java.rmi.*;
import tos.system.*;

/**
 * This is the class that provides the functionality of a pipe object.
 * 
 * Pipes in TOS are modelled after the named-pipe mechanism of Windows
 * NT.  They enable different processes to quickly and easily pass data 
 * back and forth without having to worry about the fine details of the 
 * underlying network or even of the Java RMI framework.
 * 
 * TOS pipes are full-duplex.  Either process may both read and write
 * information into their pipe without interference with data flowing
 * in the other direction.  Once written, data will remain in the pipe 
 * until it is retrieved on the other side.
 * 
 * Pipes are serializable, so they can be passed to other functions, both
 * local and remote.  If two or more threads share the same Pipe object, 
 * however, it is their responsibility to guard access to it, using the 
 * Java <code> synchronized </code> modifier to prevent distortions in 
 * data being read or written.
 * 
 * Pipes will be removed from the system when the processes on both sides 
 * have closed it.  If one process has closed its object and the other has
 * not, any TOS process can access the pipe.
 * 
 * 
 */

public class Pipe implements Serializable
{
	/** The name of the pipe. */
	protected String name;
	
	/** Remote reference to the pipe server for this pipe. */
	protected TOSPipeServer server;
	
	/** 
	 * Identifier used by the server.
	 * The server uses the handle to determine which 'side' of
	 * the pipe this object lies on.
	 */
	protected int handle;
	
	/** 
	 * Creates a new pipe object
	 * The constructor may only be called by the <code> createPipe </code>
	 * function of the <code> TOSProcess </code> class.  
	 * It obtains a reference to the pipe's server and assigns its
	 * name.
	 * 
	 * @param name Name of the new pipe.
	 * @param launcher Reference to the launcher from which the pipe
	 * server reference will be found.
	 * @throws PipeException if an error occurs.
	 * @see TOSProcess#createPipe
	 */
	protected Pipe(String name, TOSLauncher launcher) throws PipeException
	{
		try {
			server = (TOSPipeServer)launcher.getPipeServer();
		} catch (Exception e) {
			throw new PipeException("Unable to create pipe");
		}
		this.name = name;
	}
	
	/**
	 * Creates the pipe.
	 * Contacts the server and instructs it to allocate a new pipe
	 * with the given name.
	 * Applications may call <code>connect()</code> instead if a
	 * <code>PipeExistsException</code> is thrown.
	 */
	public void create() throws PipeExistsException, RemoteException, IOException
	{
		handle = server.create(name);
	}

	/** 
	 * Connects to an existing pipe.
	 * Contacts the server and establishes the caller as the other side
	 * of an existing pipe.
	 */
	public void connect() throws NoSuchPipeException, RemoteException
	{
		handle = server.connect(name);
	}

	/** Reads from a pipe.	 
	 * @return Array of bytes read.
	 * @param count Maximum number of bytes to read.
	 * @exception PipeException if any error occurs.
	 */
	public byte[] read(int count) throws PipeException
	{
		try {
			return server.read(name,handle,count);
		} catch (Exception e) {
			throw new PipeException("Error reading from pipe");
		}
	}
	
	/** Writes to a pipe.	 
	 * @param buffer Array of bytes to write.
	 * @param count Number of bytes to write.
	 * @exception PipeException if any error occurs.
	 */
	public void write(byte[] buffer, int count) throws PipeException
	{
		try {
			server.write(name,handle,buffer,count);
		} catch (Exception e) {
			throw new PipeException("Error writing to pipe");
		}

	}
	
	/** Closes a pipe.  
	 * The pipe should be closed when it is no longer needed.  
	 * Applications may not use a pipe after closing it.
	 * @exception PipeException if an error occurs.
	 */
	public void close() throws PipeException
	{
		try {
			server.close(name,handle);
		} catch (Exception e) {
			throw new PipeException("Error closing pipe");
		}
	
	}
	
	/** Closes a pipe if it is still open
	 * @exception Throwable any exception thrown by the superclass.
	 */
	public void finalize() throws Throwable
	{
		try {
			close();
		} catch (Exception e) {
			// well at least we tried
		}
		super.finalize();
	}
}
   