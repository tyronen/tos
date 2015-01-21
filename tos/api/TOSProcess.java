//
//
// TOSProcess
//
//
package tos.api;

import java.io.*;
import java.rmi.*;
import java.net.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import tos.system.*;

/** This is the required base class for all TOS applications.
 * 
 * This class provides a considerable number of services and operations
 * that are carried out behind the scenes.  Their presence ensure that
 * application programmers do not have to worry about handling the details
 * of connecting to launchers and servers.  
 * 
 * Black box functions are provided that return objects such as sync objects and pipes
 * for applications' use.  In addition, the application inheriting this class
 * need only pass in an extra command-line argument to indicate the 
 * launcher to connect to and this class will take care of the rest of 
 * the required TOS operations - connecting to a launcher, registering, etc.
 * 
 * Most of the data members of this class are declared as <code>private</code>,
 * to prevent subclasses from misusing them.
 * 
 * It is the responsibility of the subclass to declare a static 
 * <code>main</code> function.  This function must call the constructor with
 * a single parameter, an array of type <code>Object</code>.  The last
 * argument in this object must be a <code>String</code> containing the 
 * location of the launcher to connect to.
 * 
 * The constructor contains a call to an abstract
 * function called <code>init()</code> that subclasses are intended to 
 * override in order to implement their initialization facilities.
 */

public abstract class TOSProcess
{

	/** Remote stub of launcher */
	private TOSLauncher launcher;
	
	/** Remote stub of listener	 */
	private TOSListener listStub;
	
	/** Listener thread	 */
	private Listener listener;
		
	/** TOS identifier of this process  */
	private int procid;
	
	/** Number of running threads  */
	private int threads = 0;
			
	/** Dummy default constructor.
	 * The default constructor is declared as <code>private</code>
	 * to prevent it from being used. */
	private TOSProcess()
	{
	
	}
	
	/** Constructor.
	 * This constructor should <b>not</b> be overridden by a subclass.  
	 * It contains code both before and after a call to <code>init()</code>,
	 * which subclasses should override instead.
	 * The constructor calls the <code>startup()</code> function as the program
	 * starts and the <code>checkThreads()</code> function as it ends.
	 * */

	public TOSProcess(Object[] parameterList)
	{
		System.runFinalizersOnExit(true);
		startup(getLauncherName(parameterList));
		int len = parameterList.length-1;
		Object[] initList = new Object[len];
		System.arraycopy(parameterList,0,initList,0,len);
		init(initList);
		checkThreads();
	}
	
	/** Obtains the name of the launcher from the parameter list.
	 * The launcher name is assumed to be the last item in the parameterList
	 * array.  If it is not found, the program will terminate.
	 * @return Name of launcher.
	 */
	private String getLauncherName(Object parameterList[])
	{
		String launcherName = "";
		try {
			launcherName = (String)parameterList[parameterList.length-1];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Launcher name not specified properly.");
			System.exit(1);
		} catch (ClassCastException e) {
			System.out.println("Launcher name not specified properly.");
			System.exit(1);
		}			  
		return launcherName;
	}
	
	/** Performs basic startup functions.
	 * Looks up the launcher using its URL, then starts a listener thread and
	 * exports it to an anonymous port.  
	 * @param launcherName Name of launcher.
	 */
	private void startup(String launcherName)
	{
		try {
			launcher = (TOSLauncher)Naming.lookup("rmi://"+launcherName+"/Launcher");
			listener = new Listener();
			listStub = (TOSListener)UnicastRemoteObject.exportObject(listener);
			procid = launcher.registerProcess(listStub,getClass().getName(),InetAddress.getLocalHost().getHostName());
			listener.setId(procid);
			String listname = "Listener" + String.valueOf(procid);
			Naming.rebind(listname,listStub);
			//System.out.println("Launcher id = " + procid);
		} catch (Exception e) {
			System.out.println("Error in initialization: could not connect to launcher.");
			System.exit(0);
		}
		Thread[] tarray = getThreads();
		for (int k=0; k<tarray.length; k++)
		{
			if (!tarray[k].isDaemon())
				threads++;
		}
	}
	
	/** Waits for other non-daemon threads to finish, then exits.
	 * Normally, the Java virtual machine will exit when all non-daemon threads
	 * have terminated.  Because this class has an object that acts as an RMI server,
	 * (in order to receive calls from TOS servers), the RMI libraries start an
	 * additional thread, called KeepAlive, which will keep the program running
	 * artificially.  To forestall this, this function will check if all 
	 * other non-daemon threads have terminated.  The check is repeated every 30
	 * seconds; once the others have gone, the function forces an application shutdown.
	 */
	private void checkThreads()
	{
		Thread livethread = Thread.currentThread();
		Thread[] tarray = getThreads();
		for (int i=0; i<tarray.length; i++)
			if (!tarray[i].getName().equals("KeepAlive"))
				livethread = tarray[i];
		while(true)
		{
			Thread[] harray = getThreads();
			int tot = 0; // main and KeepAlive
			for (int j=0; j<harray.length; j++)
			{
				String name = harray[j].getName();
				boolean isDaemon = harray[j].isDaemon();
		//		System.out.println("thread " + name + " isDaemon: " + isDaemon);
				if (!isDaemon)
					tot++;
			}
			if (tot==threads)
			{
				try {
					launcher.unregisterProcess(procid);
				} catch (RemoteException e) {
					// doesn't matter
				}
				System.exit(0);
			}
			else //do this check every 30 seconds
			{
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					// just go on
				}
			}
		}
	}

	/** Returns a list of all threads running in the program.
	 * The list is obtained by going to the master thread group
	 * and enumerating its active threads.
	 * @return An array of <code>Thread</code> objects.
	 */
	private Thread[] getThreads()
	{
		Thread lthread = Thread.currentThread();
		ThreadGroup tgroup = lthread.getThreadGroup();
		while (tgroup.getParent()!=null)
			tgroup = tgroup.getParent();
		Thread[] tarray = new Thread[tgroup.activeCount()];
		int count = tgroup.enumerate(tarray,true);
		return tarray;
	}
	
	/** Abstract function subclasses may override to perform initialization.
	 * @param parameterList An array of the parameters, its content to be dealt with by the subclass.
	 */
	public abstract void init(Object[] parameterList);

	/* TOS service functions */
	
	/** Returns a remote stub of the disk running at the given location.
	 * @parameter servername Location of the disk.
	 * @return Remote stub of the disk.
	 */
	public TOSDisk getDisk(String servername)
	{
		String hostname;
		TOSDisk fs;
		try	{
			hostname = launcher.getDiskHost(servername);
			fs = (TOSDisk)Naming.lookup(hostname+"/FS"+servername);
		} catch (Exception e) {
			System.out.println(e); return null;
		}
		return fs;

	}	
		
	/** Open a mutex object.
	 * The mutex object may or may not exist already.
	 * @return Mutex The mutex.
	 * @param name Name of the mutex.
	 * @exception SyncException if an error occurs during the mutex opening.
	 */
	public final Mutex openMutex(String name) throws SyncException
	{
		return new Mutex(name,listStub,procid,launcher);
	}

	/** Open a signal object.
	 * The signal object may or may not exist already.
	 * @return Signal The mutex.
	 * @param name Name of the mutex.
	 * @exception SyncException if an error occurs during the signal opening.
	 */
	public final Signal openSignal(String name) throws SyncException
	{
		return new Signal(name,listStub,procid,launcher);
	}
	
	/** Create a new semaphore object.
	 * The semaphore must not alredy exist.
	 * @return Semaphore The new semaphore.
	 * @param name Name of the semaphore.
	 * @exception SyncException if an error occurs during semaphore creation.
	 */
	public final Semaphore createSemaphore(String name, int count) throws SyncException
	{
		return new Semaphore(name,count,listStub,procid,launcher);
	}
	
	/** Open an existing semaphore object.
	 * The semaphore must exist already.
	 * @return Semaphore The semaphore.
	 * @param name Name of the semaphore.
	 * @exception SyncException if an error occurs during the open operation.
	 * @see Semaphore
	 */
	public final Semaphore openSemaphore(String name) throws SyncException
	{
		return new Semaphore(name,listStub,procid,launcher);
	}
	
	/** Create a new pipe object.
	 * To use the new pipe, the application must call its <code>create()</code>
	 * or <code>connect()</code> methods of Pipe
	 * @return Pipe The new pipe.
	 * @param name Name of the pipe.
	 * @exception PipeException if an error occurs during pipe creation.
	 * @exception RemoteException if an RMI problem occurs.
	 * @see Pipe
	 */
	public final Pipe newPipe(String name) throws PipeException, RemoteException
	{
		Pipe pipe = new Pipe(name,launcher);
		return pipe;
	}

}

