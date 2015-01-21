//TOSLauncher.java
package tos.system;

import java.io.*;
import java.rmi.*;
import java.util.*;
import tos.api.*;

/** This interface defines functions visible
 * from launchers
 *  to user processes and to
 * servers.
 * 
 */
public interface TOSLauncher extends TOSServer
{
	/** Terminates the launcher.
	 * @exception RemoteException if there is an RMI problem. 
	 */	
	void terminate() throws RemoteException;
	
	/** Registers a new server.
	 * @param type Type of server (filename, pipe, or sync).
	 * @stub Remote stub of server.
	 * @exception RemoteException if there is an RMI problem.
	*/	
	TOSServer registerServer(String type,TOSServer stub) throws RemoteException;
	
	/** Returns the default sync server.
	 * @return <code>TOSSyncServer</code> Default sync server.
	 * @exception RemoteException if there is an RMI problem.
	 */
	TOSSyncServer getSyncServer() throws RemoteException;
	
	/** Returns the default pipe server.
	 * @return <code>TOSPipeServer</code> Default pipe server.
	 * @exception RemoteException if there is an RMI problem.
	 */
	TOSPipeServer getPipeServer() throws RemoteException;
	
	/** Returns the default filename server.
	 * @return <code>TOSFileNameServer</code> Default filename server.
	 * @exception RemoteException if there is an RMI problem.
	 */
	TOSFileNameServer getFileNameServer() throws RemoteException;
	
	/** Registers a new process.
	 * @param process Remote stub of the process' <code>Listener</code> thread
	 * @return TOS identifier of the new process.
	 * @exception RemoteException if there is an RMI problem.
	 */
	int registerProcess(tos.system.TOSListener process, String classname, String hostname) throws RemoteException;

	/** Removes an exiting process from the system tables.
	 * @param process TOS identifier of process.
	 * @exception RemoteException if there is an RMI problem.
	 */
	void unregisterProcess(int process) throws RemoteException;

	/** Identifies the host a given disk is running on.
 	 * @param servername Name of disk.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NotFoundException is there is no disk of that name.
	 */
	String getDiskHost(String servername) throws RemoteException, NotFoundException;
       
	/** Registers a new disk.
	 * @param filename Physical file of the disk.
	 * @param servername Server name.
	 * @param stub Remote stub of disk.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NoDiskException if no disk is expected of that name.
	 */
	void registerDisk(String filename, String servername, TOSDisk stub) throws RemoteException, NoDiskException;
	
	/** Launches a server on this launcher's host.
	 * @param name Name of new server.
	 * @exception RemoteException if there is an RMI problem.
	 */
	void launchServer(String name) throws RemoteException, IOException;
	
	/** Adds a new server to the system tables.
	 * @param type Type of server.
	 * @param location Location of new server.
	 */
	void addToTable(String type,String location) throws RemoteException;

}
