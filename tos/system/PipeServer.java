package tos.system;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import tos.api.*;

/** The pipe server is the class responsible for managing pipes, whereas
 * most of the code handling the actual interprocess communication lies in 
 * the <code>InternalPipe</code> class.  
 */

class PipeServer extends Server implements TOSPipeServer
{
	/** Table of the pipes.	 */
	protected Vector pipeTable = new Vector();
	
	/** Standard Java entry function.  Arguments are ignored. */
	public static final void main(String args[])
	{
		Debug.setLookAndFeel();
		PipeServer server;
		try {
			server = new PipeServer();
		} catch (Exception e) {
			Debug.DisplayException("Couldn't create pipe server",e);
		}

	}
	
	/** Constructor.
	 * <p>Most of the functionality comes from a call to the superclass'
	 * <code>init()</code> function.
	 * @exception RemoteException if an RMI error occurs.
	 * @see tos.system.Server#init
	 */	
	PipeServer() throws IOException
	{
		try {
			init(PIPE);
		} catch (Exception e) {
			Debug.DisplayException("PipeServer",e);
		}
	}
		
	/** Obtains the record of a pipe from the table.
	 * <p>Every other function in this class calls this one.
	 * @param name Name of pipe.
	 * @return <code>InternalPipe</code> object of the pipe.
	 * @exception NoSuchPipeException if there is no such pipe.
	 */
	InternalPipe getPipe(String name) throws NoSuchPipeException
	{
		synchronized (pipeTable) {
			for (int i=0; i<pipeTable.size(); i++)
			{
				InternalPipe pipe = (InternalPipe)pipeTable.elementAt(i);
				if (pipe.name.equals(name))
					return pipe;
			}
		}
		throw new NoSuchPipeException();
	
	}

	/** Creates a new pipe.
	 * @param name Name of pipe.
	 * @return handle representing moniker of the new pipe.
	 * @exception RemoteException if there has been an RMI error.
	 */
	public int create(String name) throws PipeExistsException, RemoteException, IOException
	{
		try {
			getPipe(name);
			throw new PipeExistsException();
		} catch (NoSuchPipeException e) {
			// this is the desired result
		}
		InternalPipe pipe = new InternalPipe(name,getLocation());
		synchronized (pipeTable) {
			pipeTable.addElement(pipe);
		}
		addToObjectTable(pipe,this);
		return pipe.open();
	}
	
	/** Connects to a pipe.
	 * @param name Name of pipe.
	 * @return handle representing moniker of the pipe.
	 * @exception PipeExistsException if the pipe already exists.
	 * @exception RemoteException if there has been an RMI error.
	 * @exception IOException if a runtime I/O exception occurs.
	 */
	public int connect(String name) throws NoSuchPipeException, RemoteException
	{
		InternalPipe pipe = getPipe(name);
		return pipe.open();
	}
	
	/** Reads from a pipe.
	 * @param name Name of pipe.
	 * @exception NoSuchPipeException if there is no pip of that name.
	 * @exception RemoteException if there has been an RMI error.
	 */
	public byte[] read(String name, int handle, int count) throws RemoteException, IOException, NoSuchPipeException
	{
		InternalPipe pipe = getPipe(name);
		return pipe.read(handle,count);
	}
																 
	/** Writes to a pipe.
	 * @param name Name of pipe.
	 * @exception RemoteException if there has been an RMI error.
	 * @exception IOException if a runtime I/O exception occurs.
	 * @exception NoSuchPipeException if there is no pip of that name.
   	 */
	public void write(String name, int handle, byte[] buffer, int count) throws RemoteException, IOException, NoSuchPipeException
	{
		InternalPipe pipe = getPipe(name);
		pipe.write(handle,buffer,count);
	}
	
	/** Closes a pipe.
	 * @param name Name of pipe.
	 * @exception RemoteException if there has been an RMI error.
	 * @exception NoSuchPipeException if there is no pip of that name.
	 * @exception IOException if a runtime I/O exception occurs.
	 */
	public void close(String name, int handle) throws RemoteException, NoSuchPipeException
	{
		InternalPipe pipe = getPipe(name);
		pipe.close(handle);
		destroy(pipe);
	
	}
	
	/** Destroys a pipe.
	 * @param name Name of pipe.
	 * @exception RemoteException if there has been an RMI error.
	 */
	public void destroy(InternalPipe pipe) throws RemoteException
	{
		synchronized (pipeTable) {
			if (pipe.unused())
				pipeTable.removeElement(pipe);
		}
		
		Enumeration enumeration = objectTable.keys();
		InternalPipe mypipe = pipe;
		while (enumeration.hasMoreElements())
		{
			mypipe = (InternalPipe)enumeration.nextElement();
			if (mypipe.name.equals(pipe.name))
				break;
		}
		deleteFromObjectTable(mypipe,this);
	}
	

}
