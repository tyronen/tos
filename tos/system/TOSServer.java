package tos.system;

import java.rmi.*;
import java.util.*;
import tos.api.*;

/** This interface contains identifiers and functions that are
 * inherited and can be accessed remotely by all servers.
 */

interface TOSServer extends Remote
{
	/** Label for sync.	 */
	static String SYNC = "Sync";

	/** Label for pipe.	 */
	static String PIPE = "Pipe";

	/** Label for filename.	 */
	static String FN = "FileName";
	
	/** Returns the name of the server's host.
	 * @return the server's hostname
	 * @exception RemoteException if an RMI problem occurs.
	 */
	String getHost() throws RemoteException;
	
	/** Returns the server's location.
	 * @return the server's location.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	String getLocation() throws RemoteException;
	
	/** Returns the server's object table.
	 * The object table is a hashtable mapping the key object the 
	 * server deals with to the remote stub of the server that holds it.
	 * This table should be identical between all servers of that type.
	 * @return the object table.
	 */
	Hashtable getObjectTable() throws RemoteException;
	
	/** Add a new entry to the object table.
	 * @param key New object to add.
	 * @param Remote stub of server that holds it.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	void addToObjectTable(Object key, TOSServer stub) throws RemoteException;
	
	/** Remove an entry from the object table.
	 * @param Object being removed.
	 * @param Remote stub being removed.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	void deleteFromObjectTable(Object key, TOSServer stub) throws RemoteException;
	
	/** Returns the server table.
	 * Every server maintains a list of all other servers of its class.
	 * @return Server table.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	Vector getServerTable() throws RemoteException;
	
	/** Adds a new server to the server table.
	 * When a new server starts, it calls <code>newServer</code> on 
	 * one server, which in turn calls <code>addServer</code> on
	 * all other servers.
	 * @param stub Remote stub of the new server.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	void addServer(TOSServer stub) throws RemoteException;
	
	/** Called by a newly started server.
	 * @param stub Remote stub of new server.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	void newServer(TOSServer stub) throws RemoteException;

	/** Remove a server from the server table.
	 * @param stub Remote stub of the server to remove.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	void removeServer(TOSServer stub) throws RemoteException;
	
	/** Terminates a server running at the given location.
	 * Unlike <code>removeServer,</code> this function is 
	 * implemented by calling a launcher to carry out the 
	 * destruction itself.
	 * @return TOSServer A new server the calling launcher can connect to.
	 * @param location Location of the server to die.
	 * @exception RemoteException if an RMI problem occurs.
	 * @exception NotFoundException if no server or launcher could be found.
	 */
	TOSServer destroyServer(String location) throws RemoteException, NotFoundException;
	
	/** Terminates this server.
	 * @exception RemoteException if an RMI problem occurs.
	 * @exception NotFoundException if the launcher at this location cannot be found.
	 */
	void terminate() throws RemoteException, NotFoundException;
}
