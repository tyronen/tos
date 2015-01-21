//SyncServer.java
package tos.system;

import tos.api.NotFoundException;
import tos.api.SyncException;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Vector;

/** The sync server is responsible for managing responses to operations 
 * carried out on TOS sync objects.
 * <p>Unlike the pipe server, the sync server uses its object type, 
 * {@link SyncRecord}, purely for storage.  It is the server class' job 
 * to make the decisions on which threads are to wait and which are to be 
 * released.  The server receives identification data from each calling thread
 * and notifies the listeners of their process when the time comes for a 
 * suspended thread to be resumed.
 */

public class SyncServer extends Server implements TOSSyncServer
{
	/** Master table of created synchronization objects. */
	protected Vector syncTable = new Vector();
	
	/** Table mapping threadnames to Listeners.	 */
	protected Hashtable threadTable = new Hashtable();
	
	/** Translates the integer representation of the sync object type
	 * to its string representation.
	 * @param type Integer representation.
	 * @return String representation.
	 */
	static String numToStr(int type)
	{
		if (type==MUTEX)
			return "Mutex";
		else if (type==SEMAPHORE)
			return "Semaphore";
		else if (type==SIGNAL)
			return "Signal";
		else
			return "Unknown";
	}
	
	/** Translates the string representation of the sync object type
	 * to its integer representation.
	 * @param str String representation.
	 * @return Integer representation.
	 */
	static int strToNum(String str)
	{
		if (str.equals("Mutex"))
			return MUTEX;
		else if (str.equals("Semaphore"))
			return SEMAPHORE;
		else if (str.equals("Signal"))
			return SIGNAL;
		else
			return 0;
	}
		
	/** The standard Java entry function.  
	 * <p>The command-line arguments are ignored, as the superclass obtains
	 * them from the initialization file.
	 */
	public static final void main(String args[])
	{
		Debug.setLookAndFeel();
		try {
			SyncServer serv = new SyncServer();
		} catch (Exception e) {
			Debug.ErrorMessage("Couldn't create sync server","Exception" + e);
		}
	}

	/** Constructor.
	 * <p>Most of the functionality comes from a call to the superclass'
	 * <code>init()</code> function.
	 * @exception RemoteException if an RMI error occurs.
	 * @see tos.system.Server#init
	 */
	public SyncServer() throws RemoteException
	{
		try {
			init(SYNC);
		} catch (Exception e) {
			Debug.ErrorMessage("SyncServer initialization error","Exception" + e);
		}
	}

	/** Creates a new sync object.
	 * <p>This creates a new object of the given type and returns <code>true</code>
	 * if it is created and <code>false</code> if it already exists.
	 * @param name Name of object.
	 * @param type Type of object.
	 * @param max Maximum number of threads.  Ignored unless type==SEMAPHORE.
	 * @return success of the creation.
	 * @exception RemoteException if an RMI error occurs.
	 */
	public boolean createObject(String name,int type,int max) throws RemoteException
	{
		try {
			int countval;
			SyncRecord rec;
			try {
				rec = findObject(name,type);
			} catch (NotFoundException e) 
		
			{
				String location = getLocation();
				if (type==SIGNAL)
				{
					rec = new SyncRecord(name,type,0,location);
					countval = 0;
				}
				else if (type==SEMAPHORE)
				{
					rec = new SyncRecord(name,type,max,location);
				}
				else if (type==MUTEX)
				{
					rec = new SyncRecord(name,type,1,location);
				}
				else
					return false;
				synchronized (syncTable) { syncTable.addElement(rec); }
				addToObjectTable(rec,this);
				return true;
			}
			// this indicates the client has created a new reference
			// to the object
			rec.count++;
			return false;
		} catch (Exception e) {
			Debug.DisplayException("createObject",e);
			return false;
		}
	}

	/** Provides basic waiting functionality.
	 * <p>Looks up the sync object.  If the calling thread needs to be
	 * suspended, it is added to the queue and the function returns 
	 * <code>true</code>.  If the caller proceeds, the function returns 
	 * <code>false</code>.
	 * @param name Name of object.
	 * @param type Type of object.
	 * @param stub Remote stub of caller's listener thread.
	 * @param threadName Name of calling thread.
	 * @param procid ID of calling process.
	 * @return <code>true</code> if thread must suspend, 
	 *         otherwise <code>false</code>.
 	 * @exception RemoteException if an RMI error occurs.
	 * @exception SyncException if another error occurs.
	 */
	public boolean Wait(String name, int type, TOSListener stub, 
						String threadName, int procid) 
		throws RemoteException, SyncException
	{
		SyncRecord rec;
		try {
			rec = findObject(name,type);
		} catch (NotFoundException e) {
			throw new SyncException("No such sync object");
		}
		// Increment count for semaphores and mutexes
		try {
			if (rec.max!=0)
			{
				rec.count++;
				if (rec.count<=rec.max)
					return false;
			}
			String id = String.valueOf(procid)+threadName;
			rec.addElement(id);
			synchronized (threadTable) { threadTable.put(id,stub); }
			return true;
		} catch (Exception e) {
			if (e instanceof SyncException)
				throw (SyncException)e;
			Debug.ErrorMessage("Wait",e.toString());
			return false;
		}
	}

	/** Releases thread(s) waiting on a sync object.
	 * @param name Name of object.
	 * @param type Type of object.
 	 * @exception RemoteException if an RMI error occurs.
	 */
	public void Release(String name, int type) throws RemoteException
	{
		SyncRecord rec;
		try {
		try {
			rec = findObject(name,type);
		} catch (NotFoundException e) {
			return;
		}
		// Decrement semaphore count
		if (rec.max!=0)
		{
			rec.count--;
		}
		// for signals, resume all waiting threads,
		// for mutex and semaphores, just the first
		if (type==SIGNAL)
		{
			while (!rec.queue.isEmpty())
				resumeThread(rec);
		}
		else
			resumeThread(rec);
		} catch (Exception e) {
			Debug.DisplayException("Release",e);
		}
	}
	
	/** Resumes threads scheduled for release.
	 * <p>Called by the <code>Release</code> function.
	 * @param rec Record of object to release.
 	 * @exception RemoteException if an RMI error occurs.
	 * @exception SyncException if another error occurs.
	 */
	void resumeThread(SyncRecord rec) throws RemoteException, SyncException
	{
		if (rec.queue.isEmpty())
			return;
		String id = (String)rec.queue.firstElement();
		rec.removeElement(id);
		TOSListener stub;
		synchronized (threadTable) {
			stub = (TOSListener)threadTable.get(id);
			threadTable.remove(id);
		}
		try {
			stub.resume(id);
		} catch (Exception e) {
			// These exception are thrown if a thread or process had terminated
			// without releasing a lock.  We just release
			// the next item in the queue instead.
			if ((e instanceof SyncException) || 
				(e instanceof java.rmi.ConnectException))
			{
				removeRef(rec.name,rec.type);
				resumeThread(rec); 
			}
		}
	}

	/** Increments a sync object's reference count.
	 * @param name Name of sync object.
	 * @param type Type of sunc object.
	 * @exception RemoteException if there has been an RMI error.
	 */
	public void addRef(String name, int type) throws RemoteException
	{
		SyncRecord rec;
		try {
			rec = findObject(name,type);
		} catch (NotFoundException e) {
			return;
		}
		rec.count++;
	}
	
	/** Decrements a sync object's reference count.
	 * @param name Name of sync object.
	 * @param type Type of sunc object.
	 * @exception RemoteException if there has been an RMI error.
	 */
	public void removeRef(String name, int type) throws RemoteException
	{
		SyncRecord rec;
		try {
			rec = findObject(name,type);
		} catch (NotFoundException e) {
			return;
		}
		rec.count--;
		// destroy object if it no longer exists
		if (rec.count<=0)
		{
			destroyObject(rec);
		}
	}
	
	/** Destroys a sync object.
	 * @param rec SyncRecord of the object to destroy.
	 * @exception RemoteException if there has been an RMI error.
	 */
	public void destroyObject(SyncRecord rec) throws RemoteException
	{
		SyncRecord myrec;
		try {
			myrec = findObject(rec.name,rec.type);
		} catch (NotFoundException e) {
			return;
		}
		synchronized (syncTable) { 
			syncTable.removeElement(myrec); 
		}
		deleteFromObjectTable(myrec,this);
	}
	
	/** Obtains the full record of a sync object, given its name and type.
	 * @param name Name of object.
	 * @param type Type of object.
	 * @return Full sync record.
	 * @exception NotFoundException if there is no such object.
	 */
	SyncRecord findObject(String name, int type) throws NotFoundException
	{
		for (int i=0; i<syncTable.size(); i++)
		{
			SyncRecord myrec = (SyncRecord)syncTable.elementAt(i);
			if ((myrec.name).equals(name) && (myrec.type)==type)
				return myrec;
		}
		throw new NotFoundException();
	}
	
}