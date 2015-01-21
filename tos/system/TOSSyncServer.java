package tos.system;

import java.rmi.*;
import tos.api.*;

/** This remote interface is used to deal with pipe servers.
 */

public interface TOSSyncServer extends TOSServer
{
	/** Shorthand for mutexes */
	static int MUTEX = 1;

	/** Shorthand for semaphores */
	static int SEMAPHORE = 2;

	/** Shorthand for signals */
	static int SIGNAL = 3;

	/** Creates a new sync object.
	 * @param name Name of sync object.
	 * @param type Type of sunc object.
	 * @param count Count of the new object.  
	 * @return if the creation was successful.
	 * @exception RemoteException if there has been an RMI error.
	 */
	boolean createObject(String name,int type,int count) throws RemoteException;
	
	/** Calls <code>Wait</code> on a sync object.
	 * @param name Name of sync object.
	 * @param type Type of sunc object.
	 * @return if
	 * @exception RemoteException if there has been an RMI error.
	 */
	boolean Wait(String name, int type, TOSListener stub, String threadName, int procid) throws RemoteException, SyncException;
	
	/** Calls <code>Release</code> on a sync object.
	 * @param name Name of sync object.
	 * @param type Type of sunc object.
	 * @return <code>true</code> if the caller should halt, otherwise <code>false</code>.
	 * @exception RemoteException if there has been an RMI error.
	 */
	void Release(String name, int type) throws RemoteException;

	/** Increments a sync object's reference count.
	 * @param name Name of sync object.
	 * @param type Type of sunc object.
	 * @exception RemoteException if there has been an RMI error.
	 */
	void addRef(String name, int type) throws RemoteException;
	
	/** Decrements a sync object's reference count.
	 * @param name Name of sync object.
	 * @param type Type of sunc object.
	 * @exception RemoteException if there has been an RMI error.
	 */
	void removeRef(String name, int type) throws RemoteException;
	
	/** Destroys a sync object.
	 * @param rec SyncRecord of the object to destroy.
	 * @exception RemoteException if there has been an RMI error.
	 */
	void destroyObject(SyncRecord rec) throws RemoteException;

}
