package tos.api;

import java.rmi.*;
import tos.system.*;

/** This class provides the functionality specific to Semaphore objects.
 * 
 * Modelled on Microsoft Windows semaphores, this object type can be utilized
 * to limit the number of threads permitted to run within a given section(s) of
 * code.  
 * 
 * The semaphore's creator indicates, in addition to the semaphore's name, 
 * a count.  This indicates the maximum number of threads that can call
 * <code>Wait()</code> without being halted.  After this, any more threads
 * that call <code>Wait()</code> will be suspended until another thread 
 * calls <code>Release()</code>.
 * 
 * Once a semaphore object has been created, using the <code>createSemaphore
 * </code> function of the <code>TOSProcess</code> no other process can create
 * another semaphore of the same name.  Nor can the count be changed once
 * the semaphore has been created.  Other processes can access the semaphore
 * by passing its name to the <code>openSemaphore</code> function of that class.
 * 
 * @see TOSProcess#createSemaphore
 * @see TOSProcess#openSemaphore
 */

public class Semaphore extends SyncObject
{
	/** Constructor to connect to a semaphore that already exists.
	 * Called by the <code>TOSProcess.openSemaphore()</code> function.
	 * @param name Name of semaphore.
	 * @param liststub Remote stub of listener thread.
	 * @param procid TOS identifier of process.
	 * @param launcher RemoteStub of process' launcher.
	 */
	protected Semaphore(String name, TOSListener liststub, int procid, TOSLauncher launcher) throws SyncException
	{
		super(name,liststub,procid,launcher);
		type = SyncServer.SEMAPHORE;
		addRef();

	}
	
	/** Constructor to create a new semaphore.
	 * Called by the <code>TOSProcess.createSemaphore()</code> function.
	 * @param name Name of semaphore.
	 * @param count Semaphore count.
	 * @param liststub Remote stub of listener thread.
	 * @param procid TOS identifier of process.
	 * @param launcher RemoteStub of process' launcher.
	 */
	protected Semaphore(String name, int count, TOSListener liststub, int procid, TOSLauncher launcher) throws SyncException
	{
		super(name,liststub,procid,launcher);
		create(count);
		type = SyncServer.SEMAPHORE;
		addRef();
	}

	/** Calls the server to create the new semaphore.
	 * It is here that the call to the server is actually made.
	 * @param count Semaphore's count.
	 * @exception SyncException if server cannot be contacted or a semaphore already exists with that name.
	 */
	protected void create(int count) throws SyncException
	{
		boolean retval;
		try {
			synchronized (server) {
				retval = server.createObject(name,SyncServer.SEMAPHORE,count);
			}
		} catch (RemoteException e) {
			throw new SyncException("Unable to connect to sync server.");
		}
		if (retval==false)
			throw new SyncException("Semaphore already exists with that name.");
	}
	
	/** Releases the semaphore before the object goes out of scope.
	 * This feature is added for extra safety, but application programmers
	 * should not count on its use.  All semaphores should be explicitly
	 * released.
	 * @exception Throwable if the superclass' <code>finalize()</code> function throws an error or exception.
	 */
	public void finalize() throws Throwable
	{
		if (CanRelease)
			Release();
		super.finalize();
	}


}
