package tos.api;

import java.rmi.*;
import tos.system.*;

/** This class provides the functionality of the Signal sync object.
 * 
 * This type is modelled after Microsoft Windows' event object, but bears
 * the name <i>signal</i> to avoid confusion with the <code>java.awt.Event</code>.
 * 
 * Signals, unlike mutexes and semaphores, are not aimed at guarding a section
 * of code.  The goal here is to coordinate the actions of processes so that
 * one does not perform a certain action until another process has reached a 
 * desired point.  To prevent this from happening, the first process is blocked
 * until the <code>Signal</code> is used to tell it to proceed.
 * 
 * Any thread that calls <code>Wait</code> will come to a halt.  It will
 * remain suspended until another thread calls <code>Release</code>.  
 * There is no concept of ownership for signal objects; the thread calling
 * <code>Release</code> does not 'own' the object because it cannot have called 
 * <code>Wait</code>, else it too would be suspended.
 * 
 * More than one thread can be waiting on the signal, in which case they
 * will all resume when <code>Release</code> is called, regardless of the
 * order in which they called <code>Wait</code>.  Any threads that come to 
 * <code>Wait</code> after the call to <code>Release</code> is made will remain
 * suspended until the latter is released again.
 * 
 * Programmers should remain aware of potential timing problems.  If, for instance
 * there are several threads intended to be resumed by a single call to <code>Release</code>,
 * the releasing thread should make sure all its targets have actually made the call
 * to <code>Wait</code>, lest they remain stranded.
 * 
 * For those familiar with the Win32 API, the <code>Wait</code> and 
 * <code>Release</code> correspond to the Win32 <i>WaitForSingleObject</i>
 * and <i>PulseEvent</i> functions, respectively.
 */

public class Signal extends SyncObject
{
	/** Creates the signal object.
	 * The constructor cannot be called directly but must be called via
	 * the <code>TOSProcess.openSignal()</code> function.
	 * @param name Name of signal.
	 * @param liststub Remote stub of listener thread.
	 * @param procid TOS identifier of process.
	 * @param launcher RemoteStub of process' launcher.
	 * @exception SyncException if the server cannot be reached.
	 * @see TOSProcess#openSignal
	 */
	protected Signal(String name, TOSListener liststub, int procid, TOSLauncher launcher) throws SyncException
	{
		super(name,liststub,procid, launcher);
		CanRelease = true;
		try {
			server.createObject(name,SyncServer.SIGNAL,0);
		} catch (RemoteException e) {
			throw new SyncException("Unable to connect to sync server.");
		}
		type = SyncServer.SIGNAL;
		addRef();
	}

	/** Releases the signal.
	 * @exception SyncException if the server cannot be reached.
	 */
	public void Release() throws SyncException
	{
		try {
			synchronized (server) {
				server.Release(name,type);
			}
		} catch (RemoteException e) {
			throw new SyncException("Unable to contact sync server");
		}
	}
	
	
}
