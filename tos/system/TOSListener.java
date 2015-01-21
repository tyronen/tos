//TOSListener.java
package tos.system;

import java.rmi.*;
import tos.api.*;

/** This remote interface supplies the remote functionality
 * of the <code>Listener</code> thread all clients run.
 */

public interface TOSListener extends Remote
{
	/** Suspends a user thread.
	 * @param threadName Thread to suspend.
	 * @exception RemoteException if there has been an RMI error.
	 */
	void suspend(String threadName) throws RemoteException;
	
	/** Resumes a suspended thread.
	 * @param Concatenated TOS process identifier and thread name.
	 * @exception RemoteException if there has been an RMI error.
	 * @exception SyncException if an error occurs with the sync object.
	 */
	void resume(String name) throws RemoteException, SyncException;
	
	/** Kills a process.
	 * The Administrator uses this function to order a process to 
	 * commit suicide.
	 * @exception RemoteException if there has been an RMI error.
	 */
	void killProcess() throws RemoteException;
}
                                                               