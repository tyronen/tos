//
//
// Listener
//
//
package tos.system;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import tos.api.*;

/** The purpose of the listener is to enable TOS applications, which on
 * many occasions need to <i>receive</i> RMI calls as well as <i>send</i>
 * them, to act as RMI servers.  Every TOSProcess-derived class will have
 * a single Listener object whose purpose is to receive calls
 * from TOS servers to deal with the user's threads in some way.
 */

public class Listener extends RemoteObject implements TOSListener
{
	/** List of threads in the process.	 */
	protected Vector threadlist = new Vector();
	
	/** TOS identifier of the process. */
	protected int procid;
	
	/** Remote stub of launcher. */
	TOSLauncher launcher;
	
	/** Constructor.
	 * <p>The constructor merely calls that of the superclass.
	 * @exception RemoteException if an RMI error occurs.
	 */
	public Listener() throws RemoteException
	{
		super();
	}
	
	/** Set the process identifier.
	 * <p> This function cannot be called remotely.
	 * @param id identifier.
	 */
	public void setId(int id)
	{
		procid = id;
	}
		
	/** Suspends a user thread.
	 * @param threadName Thread to suspend.
	 * @exception RemoteException if there has been an RMI error.
	 */
	public void suspend(String threadName) throws RemoteException
	{
		try {
			Thread lthread = Thread.currentThread();
			ThreadGroup tgroup = lthread.getThreadGroup();
			while (tgroup.getParent()!=null)
				tgroup = tgroup.getParent();
			Thread[] tarray = new Thread[tgroup.activeCount()];
			int count = tgroup.enumerate(tarray,true);
			for (int i=0; i<count; i++)
			{
				String newname = tarray[i].getName();
				if (threadName.equals(newname))
				{
					threadlist.addElement(tarray[i]);
					tarray[i].suspend();
					break;
				}
			}																  		
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	/** Resumes a suspended thread.
	 * @param Concatenated TOS process identifier and thread name.
	 * @exception RemoteException if there has been an RMI error.
	 * @exception SyncException if an error occurs with the sync object.
	 */
	public void resume(String name) throws RemoteException, SyncException
	{
		if (!name.startsWith(String.valueOf(procid)))
			throw new SyncException("Wrong process");
		String threadName = name.substring(String.valueOf(procid).length());
		Thread curthread;
		int i;
		for (i=0; i<threadlist.size(); i++)
		{
			curthread = (Thread)threadlist.elementAt(i);
			if (threadName.equals(curthread.getName()))
			{
				curthread.resume();
				threadlist.removeElementAt(i);
				return;
			}
		}
		throw (new SyncException("No thread found of given name."));
	}
	
	/** Kills a process.
	 * The Administrator uses this function to order a process to 
	 * commit suicide.
	 * @exception RemoteException if there has been an RMI error.
	 */	
	public void killProcess() throws RemoteException
	{
		System.out.println("The TOS Administrator has ordered this process destroyed.");
		System.exit(0);	
	}


	
}

