package tos.system;

import java.io.*;
import java.rmi.*;
import java.util.*;
import tos.api.*;

/** Interface listing functions accessible only from the TOS administrator.
 * <p>There are two remote interfaces exported by the <code>Launcher</code>
 * class; <code>LauncherAdmin</code> and <code>TOSLauncher</code>.  
 * Because the functions in <code>LauncherAdmin</code> are more sensitive,
 * they have been placed in a separate interface.  This way, the 
 * <code>TOSLauncher</code> functions can be used more freely, even in classes
 * running in the client's address space.
 */

interface LauncherAdmin extends Remote
{
	// DO NOT DECLARE THE IMPLEMENTATION 
	// OF ANY OF THESE FUNCTIONS AS 'SYNCHRONIZED' !!!
	// SYNCHRONIZED REMOTE FUNCTIONS CAUSE GUI CLIENTS
	// TO CRASH

	/** Returns the specified system table.
	 * @param name The name of the table to return.
	 * @return the table of that name.
	 * @exception RemoteException if there is an RMI problem.
	 */
	Vector getTable(String name) throws RemoteException;

	/** Receive a registration from a new launcher.
	 * @param TOSLauncher Remote stub of new launcher.
	 * @param hostname Name of host new launcher is on.
	 * @param port New port number.
	 * @exception RemoteException if there is an RMI problem.
	 */
	void registerLauncher(TOSLauncher launcher, String hostname, int portnum) 
		throws RemoteException;

	/** Terminates another launcher.
	 * @param name host:port of the launcher to terminate.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NoLauncherException if there is no such launcher.
	 */
	void terminateLauncher(String fullname) 
		throws RemoteException, NoLauncherException;
	
	/** Sends a launch request to the launcher in question.
	 * @param name Name of server to launch.
	 * @param location Location of new server.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NoLauncherException if there is no launcher at that location.
	 * @exception IOException if a problem occurs on the other launcher.
	 */
	void launchServer(String name, String location) 
		throws RemoteException, IOException, NoLauncherException;
	
	/** Signals server to terminate.
	 * @param name Type of server to destroy.
	 * @param location Location of server to destroy.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NotFoundException if there is no launcher at that location.
	 */
	void terminateServer(String name, String location) 
		throws RemoteException, NotFoundException;
	
	/** Returns a complete set of the system's internal state.
	 * @return the set of tables
	 * @exception RemoteException If there is an RMI problem.
	 */
	Vector getAll() throws RemoteException;
	
	/** Replaces the system tables.
	 * @param systemTables Set of system tables.
	 * @exception RemoteException if there is an RMI problem.
	 */
	void setAll(Vector objectTable) throws RemoteException;
	
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
	
	/** Creates a new disk on this host.
	 * @param name Name of disk.
	 * @param numfiles Maximum number of files disk can hold.
	 * @param blocksize Size of data blocks.
	 * @param numblocks Number of data blocks available.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NoLauncherException if no launcher exists on that host.
	 * @exception IOException if an I/O problem develops during disk creation.
	 */
	void createDisk(String name,int numfiles,int blocksize,int numblocks) 
		throws RemoteException, IOException, NoLauncherException;
	
	/** Creates a new disk on the specified host.
	 * @param name Name of new disk.
	 * @param hostname Host disk will run on.
	 * @param numfiles Maximum number of files.
	 * @param blocksize Size of data blocks.
	 * @param numblocks Number of data blocks.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception IOException if the maximum number of disks has been launched 
	 * <b>or</b> an I/O problem occurs when writing the initialization file or starting
	 * the new process.
	 */
	void createDisk(String name,String hostname,int numfiles,int blocksize,int numblocks) 
		throws RemoteException, NoLauncherException, IOException;
	
	/** Restarts a previously running disk.
	 * @param servername Name of disk.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NotFoundException is there is no disk of that name.
	 * @exception NoLauncherException if there is no launcher at that location.
	 * @exception IOException if an error occurs starting the disk.
	 */
	void startDisk(String name) throws RemoteException, 
									   NotFoundException, 
									   NoLauncherException, 
									   IOException;
	
	/** Launches a disk on the launcher's host.
	 * @param servername Disk to be launched.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NotFoundException is there is no disk of that name.
	 * @exception NoLauncherException if there is no launcher at that location.
	 * @exception IOException if an error occurs starting the disk.
	 */
	void launchDisk(String servername) throws RemoteException;
	
	/** Stops a running disk.
	 * @param servername Name of disk.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NotFoundException is there is no disk of that name.
	 */
	void stopDisk(String name) throws RemoteException, NotFoundException;
	
	/** Removes a disk from the system.
 	 * @param servername Name of disk.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NotFoundException is there is no disk of that name.
	 */
	void removeDisk(String servername) 
		throws RemoteException, NotFoundException;
	
	/** Kills a process immediately.
	 * @param id TOS identifier of process to die.
	 * @exception RemoteException if there is an RMI problem.
	 */
	void killProcess(int id) throws RemoteException;

	/** Returns the launcher's location.
	 * @return the launcher's location.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	String getLocation() throws RemoteException;

	/** Terminates this launcher.
	 * @exception RemoteException if an RMI problem occurs.
	 * @exception NotFoundException if the launcher at this location cannot be found.
	 */
	void terminate() throws RemoteException;

}

