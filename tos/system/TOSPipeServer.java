package tos.system;

import java.rmi.*;
import java.io.*;
import tos.api.*;

/** This remote interface is used to deal with pipe servers.
 */

public interface TOSPipeServer extends TOSServer
{
	
	/** Creates a new pipe.
	 * @param name Name of pipe.
	 * @return handle representing moniker of the new pipe.
	 * @exception RemoteException if there has been an RMI error.
	 */
	int create(String name) throws PipeExistsException, RemoteException, IOException;
	
	/** Connects to a pipe.
	 * @param name Name of pipe.
	 * @return handle representing moniker of the pipe.
	 * @exception PipeExistsException if the pipe already exists.
	 * @exception RemoteException if there has been an RMI error.
	 * @exception IOException if a runtime I/O exception occurs.
	 */
	int connect(String name) throws NoSuchPipeException, RemoteException;
	
	/** Reads from a pipe.
	 * @param name Name of pipe.
	 * @exception NoSuchPipeException if there is no pip of that name.
	 * @exception RemoteException if there has been an RMI error.
	 */
	byte[] read(String name, int handle, int count) throws RemoteException, IOException, NoSuchPipeException;
	
	/** Writes to a pipe.
	 * @param name Name of pipe.
	 * @exception RemoteException if there has been an RMI error.
	 * @exception IOException if a runtime I/O exception occurs.
	 * @exception NoSuchPipeException if there is no pip of that name.
	*/
	void write(String name, int handle, byte[] buffer, int count) throws RemoteException, IOException, NoSuchPipeException;
	
	/** Closes a pipe.
	 * @param name Name of pipe.
	 * @exception RemoteException if there has been an RMI error.
	 * @exception NoSuchPipeException if there is no pip of that name.
	 * @exception IOException if a runtime I/O exception occurs.
	 */
	void close(String name, int handle) throws RemoteException, NoSuchPipeException;

	/** Destroys a pipe.
	 * @param name Name of pipe.
	 * @exception RemoteException if there has been an RMI error.
	 */
	void destroy(InternalPipe pipe) throws RemoteException;
	
}
