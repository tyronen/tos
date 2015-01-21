package tos.system;

import tos.api.*;
import java.rmi.*;
import java.util.*;

/** This remote interface is used to deal with filename servers.
 */


public interface TOSFileNameServer extends TOSServer
{
	/** Returns the name of a disk where the specified TOS file is located.
	 * @param Name of TOS file.
	 * @return Name of disk.
	 * @exception RemoteException if there has been an RMI error.
	 * @exception NoDiskException if there is no disk with this file.
	 */
	String resolveFileName(String name) throws RemoteException, NoDiskException;

	/** Obtain the remote stub of the given disk.
	 * @param servername Name of disk.
	 * @return Remote stub of the disk.
	 * @exception RemoteException if there has been an RMI error.
	 */
	TOSDisk getDisk(String servername) throws RemoteException;

	/** Mounts a disk.
	 * @param servername Name of disk.
	 * @param mountpt Absolute path name of mount point.
	 * @exception RemoteException if there has been an RMI error.
	 * @exception NotFoundException
	 */
	void mount(String servername, String mountpt) throws RemoteException;

	/** Unmounts a disk.
	 * @param servername Name of disk.
	 * @exception RemoteException if there has been an RMI error.
	 */
	void unmount(String servername) throws RemoteException;
	
	/** Returns the disk on which a directory is situated.
	 * @param dirname Name of TOS directory file.
	 * @exception RemoteException if there has been an RMI error.
	 */
	String getDiskName(String dirname) throws RemoteException;

	/** Returns a copy of the mount table.
	 * This table has mount points as keys and server names as values.
	 * @return the mount table.
	 * @exception RemoteException if there has been an RMI error.
	 */
	Hashtable getMountTable() throws RemoteException;

	
	/** Adds a new disk to the server
	 * @param servername Name of disk.
	 * @param stub Remote stub of disk.
	 * @exception RemoteException if there has been an RMI error.
	 */
	void addDisk(String servername, TOSDisk stub) throws RemoteException;


}
