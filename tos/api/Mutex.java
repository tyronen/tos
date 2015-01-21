package tos.api;

import java.rmi.*;
import java.util.*;
import tos.system.*;

/** 
 * Provides the functionality of a mutex object.
 * 
 * The Mutex class is used to guard critical sections of code. 
 * It can be considered the same as a semaphore with the <i>count</i> set
 * to 1 by default.
 * 
 * TOS applications cannot call the Mutex constructor directly.  The TOSProcess
 * class provides a function, openMutex, which subclasses use to obtain a 
 * Mutex object.
 * 
 * Mutexes are based on the Microsoft Windows 95/NT mutex objects.  When a 
 * thread calls the Wait() function, a message is sent to a synchronization
 * server to see if any other thread in any TOS process has already taken
 * possession of the mutex.  If none have, the thread takes possession of 
 * the mutex and holds it until it calls the Release() function.
 * 
 * Before the first thread calls Release(), any other thread that calls Wait()
 * on a mutex of this name will be suspended.  
 * 
 * Deadlocks can be resolved manually from the TOS Administrator program, which 
 * enables a superuser to call Release on the thread owning the mutex or
 * to remove it from the system completely.
 * 
 * Mutexes can be passed freely to any object. Applications should make sure that every thread
 * that calls Wait() also calls Release().
 * 
 * The mutex server will remove the mutex from the system once its reference count
 * has gone to zero.
 * 
 * @see TOSProcess#openMutex
 */

public class Mutex extends SyncObject
{
	/** Creates the mutex on a sync server.
	 * @param name Name of mutex.
	 * @param liststub Remote stub of listener thread.
	 * @param procid TOS identifier of process.
	 * @param launcher RemoteStub of process' launcher.
	 * @exception SyncException if the server cannot be reached.
	 */
	protected Mutex(String name, TOSListener liststub, int procid,TOSLauncher launcher) throws SyncException
	{
		super(name,liststub,procid, launcher);
		try {
			server.createObject(name,TOSSyncServer.MUTEX,0);
		} catch (RemoteException e) {
			throw new SyncException("Unable to connect to sync server.");
		}
		type = SyncServer.MUTEX;
		addRef();
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

