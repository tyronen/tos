package tos.api;

import java.io.*;
import java.rmi.*;
import tos.system.*;

/** The superclass of all sync objects.
 * This class provides the common functionality that all sync
 * objects have in common.  They are created by the various 
 * <code>TOSProcess</code> wrapper functions; applications cannot
 * call their constructors directly.  The reason for this is so that
 * the details of interfacing with the launcher and the sync server
 * can be hidden from the application.
 * 
 * All the sync objects have some type of Wait() and Release() primitive;
 * they differ in how these are handled on the server side.  Each object
 * also maintains a reference count inside the server, and informs 
 * the server when an object goes out of scope on the client side.  The
 * server will remove the object from the system tables when no client
 * has a local reference to it.
 */

public class SyncObject implements Serializable
{
	/** Exported remote stub of client's listener.	 */
	protected TOSListener liststub;
	
	/** Remote stub of server. */
	protected TOSSyncServer server;
	
	/** Set to <code>true</code> if this object is owned by the application. */
	protected boolean CanRelease = false;
	
	/** Set to <code>true</code> if this object is in a state where it can be used. */
	protected boolean usable = true;
	
	/** Name of the object.	 */
	String name;
	
	/** TOS identifier of the creating process */
	protected int procid;
	
	/** Type of the object (mutex, semaphore, or signal) */
	protected int type;

	/** Dummy constructor.
	 * This empty constructor is provided merely to override the default constructor.
	 */
	protected SyncObject()
	{
		// do nothing - allows subclasses to define new constructors	
	}

	/** Actual constructor.
	 * The constructor connects to the sync server and assigns the server's
	 * instance variables.
	 * @param name Name of object.
	 * @param liststub Remote stub of client listener.
	 * @param procid TOS identifier of creator.
	 * @param launcher Remote stub to client's launcher.
	 * @exception SyncException if no sync server is obtained from the launcher.
	 */
	protected SyncObject(String name, TOSListener liststub, int procid, TOSLauncher launcher) throws SyncException
	{
		try {
			server = launcher.getSyncServer();
		} catch (Exception e){
			throw (new SyncException("No sync server found"));
		}
		this.name = name;
		this.liststub = liststub;
		this.procid = procid;
	}		

	/** Throws an exception if the object is not in a usable state.
	 * @exception SyncException The object has been discarded and is therefore not usable.
	 */
	void check() throws SyncException
	{
		if (!usable)
			throw new SyncException("This object has been discarded.");
	}
	
	
	/** Calls the Wait() function on the server and halts the calling thread if necessary.
	 * The client calls the server's <code>Wait</code>function, passing to the
	 * server information about the calling thread and the object.  The server
	 * will return <code>true</code> if the caller is to halt.  The halt itself
	 * is carried out by the listener thread.  
	 * 
	 * When the thread is released it resumes execution here, and is marked
	 * as the owner of the object.
	 * 
	 * @exception SyncException if either the server or the listener thread could not be contacted.
	 */
	public void Wait() throws SyncException
	{
		check();
		Thread curthread = Thread.currentThread();
		String threadname = curthread.getName();
		boolean halt;
		try {
			synchronized (server) {
				halt = server.Wait(name,type, liststub, threadname, procid);
			}
		} catch (RemoteException e) {
			synchronized(System.out) {
				 }
			throw new SyncException("Unable to contact sync server");
		}
		try {
			if (halt)	
				liststub.suspend(curthread.getName());
		} catch (RemoteException e) {
			synchronized(System.out) {
				 }
			throw new SyncException("Unable to contact listener.");
		}
		CanRelease = true;
		// this will be executed whether thread had to halt or not

	}
	
	/** Relinquishes ownership of the object.
	 * Calls the server's <code>Release</code> function.  The server
	 * will handle the resumption of any waiting threads.
	 * @exception SyncException if this thread is not the owner's object
	 * or the server could not be reached.
	 */
	public void Release() throws SyncException											
	{
		check();
		if (!CanRelease)
			throw new SyncException("Not current owner of object");
		try {
			synchronized (server) {
				server.Release(name,type);
			}
		} catch (RemoteException e) {
			
			throw new SyncException("Unable to contact sync server");
		}
		CanRelease = false;
	}
	
	/** Informs the server of another local reference.
	 * The server maintains a reference count of every client that has
	 * created a local reference to an object of this name.  This is 
	 * initialized to 1 when the first one is created.  Once all clients
	 * have discarded the object, it can then be removed from the server.
	 * @exception SyncException if the server could not be contacted.
	 */
	public void addRef() throws SyncException
	{
		try {
			server.addRef(name,type);
		} catch (RemoteException e) {
			throw new SyncException("Unable to contact sync server");
		}	
	}
	
	/** Removes a local reference.
	 * Called by the <code>finalize</code> function when the object
	 * goes out of scope.  The server is informed of the client's abandonment
	 * of the object and can decrement the reference count.
	 * @exception SyncException if the call to the server fails.
	 */
	public void discard() throws SyncException
	{
		try {
			server.removeRef(name,type);
		} catch (RemoteException e) {
			throw new SyncException("Unable to delete sync object reference for object: " + name);
		}
		usable = false;
	}
	
	/** Calls <code>discard</code>.
	 * @exception Throwable if any error or exception is thrown by this or by
	 * the superclass' <code>finalize()</code>function.
	 */
	public void finalize() throws Throwable
	{
		if (usable)
			discard();
		super.finalize();
	}


}
