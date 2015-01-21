package tos.system;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import tos.api.*;

/** Server that maps file names from the global namespace to their actual physical location.
 * <p>Every TOS instantiation has at least one filename server.  
 * When the Administrator mounts a disk to a path name, that mapping
 * is stored in the hashtables of the filename server.
 * <p>When a client (either a console or TOS user process) needs to
 * access a file, it uses the filename server to tell it which disk
 * the file is located on and what its path is on the disk.  The client
 * can then deal directly with the disk, which is thus spared the 
 * tedium of converting location-independent names to location-dependent names.
 * <p>The filename server does not deal with objects, as do the other classes
 * derived from <code>TOSServer</code>.  That class' <code>objectTable</code> is 
 * here used only as an amalgam of the two table that the filename server
 * actually uses, <code>MountTable</code> and <code>NameTable</code>.
 */

class FileNameServer extends Server implements TOSFileNameServer
{
	/** Table of mounting information.  The mount path names are the 
	 * keys and the name of the disk are the values. */
	protected Hashtable MountTable = new Hashtable();
	
	/** Table of disk locations.  Disk names are the key and remote
	 * stubs to the disks are the values. */
	protected Hashtable NameTable = new Hashtable();

	/** The standard Java entry function.  It simply calls the constructor.
	 */
	public static final void main(String args[])
	{
		Debug.setLookAndFeel();
		try {
			FileNameServer serv = new FileNameServer();
		} catch (Exception e) {
			Debug.DisplayException("Couldn't create file name server",e);
		}
	}

	/** The constructor calls <code>Server.init</code>and obtains 
	 * the name and mount tables from its parent, if there is one.
	 * @see tos.system.Server#init
	 */
	public FileNameServer() throws RemoteException
	{
		try {
			init(FN);
			// for FN server, objectTable is a dummy containing 
			// MountTable and NameTable
			if (!parent.getLocation().equals(getLocation()))
				setTables();
		} catch (Exception e) {
			Debug.DisplayException("FileNameServer",e);
		}
	}

	/** Returns the pathname within the disk corresponding to a name in the global namespace.
	 * <p>This function is the workhorse of this class, and over the course
	 * of most TOS instantiations it will be called more than any other.
	 * <p>When a name is passed in, each successively shorter prefix is searched for in the 
	 * mounting table.  So if <code>name</code> is <i>/one/two/three/filename</i>, the function
	 * will first search for a disk mounted to <i>/one/two/three</i>, then
	 * <i>/one/two</i>, finally <i>/one</i>.  If none can be found, an exception
	 * is thrown.
	 * @param name Location-independent file name.
	 * @return location-dependent file name.
	 * @exception RemoteException if there is a Java RMI problem.
	 * @exception NoDiskException if there is no disk with a mountpoint in the file's path.
	 */
	public String resolveFileName(String name) throws RemoteException, NoDiskException
	{
		// Algorithm....try succeedingly large namees until filesize
		// is exhausted
		
		int seplength = TOSFile.separator.length();
		String prefix = name;
		if (!prefix.substring(prefix.length()-seplength).equals(TOSFile.separator))
			prefix = prefix + TOSFile.separator;
		int pos;
		do
		{
			pos = prefix.lastIndexOf(TOSFile.separator);
			if (pos>0)
				prefix = prefix.substring(0,pos);
			else
				prefix = TOSFile.separator;
			synchronized (MountTable) {
				if (MountTable.containsKey(prefix))
				{
					String retval =  MountTable.get(prefix) + TOSFile.servermark + name.substring(pos);
					if (retval.indexOf(TOSFile.separator)==-1)
						retval = retval + TOSFile.separator;
					return retval;
				}
			}
		} while (!prefix.equals(TOSFile.separator));
		throw new NoDiskException();
	}

	/** Mounts a disk to the given mount point.
	 * The mount point is added to the mount table.  If the disk is already
	 * mounted, the old mounting is replaced by the new.  The disk is also
	 * contacted directly and informed of its new mount location.
	 * @param diskname Name of disk.
	 * @param mountpt Mount point.
	 * @exception RemoteException if there is an RMI problem.
	 */
	public void mount(String diskname, String mountpt) throws RemoteException
	{
		synchronized (MountTable) {
			if (MountTable.contains(diskname))
			{
				Enumeration keyTable = MountTable.keys();
				while (keyTable.hasMoreElements())
				{
					String oldpt = (String)keyTable.nextElement();
					String oldname = (String)MountTable.get(oldpt);
					if (oldname.equals(diskname))
					{
						MountTable.remove(oldpt);
						break;
					}
				}
			}
			if (!mountpt.equals(""))
				MountTable.put(mountpt,diskname);
		}
		// must synchronize IMMEDIATELY with local host
		
		// update disk's mountpoint
		TOSDisk disk = getDisk(diskname);
		try {
			disk.mount(mountpt);
		} catch (IOException e) {
			Debug.DisplayException("Mount",e);
		}

	}
	
	/** Unmounts a disk.
	 * This function simply mounts a disk to an empty string.
	 * @param diskname Disk to be unmounted.
	 * @exception RemoteException if there is an RMI problem.
	 */
	public void unmount(String diskname) throws RemoteException
	{
		mount(diskname,"");
	}

	/** Adds a disk and its stub to the name table.
	 * @param diskname Name of the disk.
	 * @param stub Remote stub of the disk.
 	 * @exception RemoteException if there is an RMI problem.
	 */
	public void addDisk(String diskname, TOSDisk stub) throws RemoteException
	{
		synchronized (NameTable) {																	
			NameTable.put(diskname,stub);
		}
	}

	/** Retrieves a disk's remote stub.
	 * @param diskname Name of disk.
	 * @return Remote stub of disk.
	 * @exception RemoteException if there is an RMI problem.
	 */
	public TOSDisk getDisk(String diskname) throws RemoteException
	{
		return (TOSDisk)NameTable.get(diskname);
	}
	
	/** Returns the names of all disks mounted to points within a given directory.
	 * This function is used by the console's <i>Dir</i> command.  It will
	 * return a <code>String</code> listing all disks mounted within 
	 * the directory.
	 * <p>For example, if <code>dirname</code> is <i>/one/two</i>, and 
	 * disk <i>jack</i> is mounted to <i>/one/two/jack</i> and disk
	 * <i>jill</i> is mounted to <i>/one/two/jill</i>, the function 
	 * will return "joe\njill".
	 * @param dirname Directory to search.
	 * @return String Newline-delimited list of disks.
 	 * @exception RemoteException if there is an RMI problem.
	 */
	public String getDiskName(String dirname) throws RemoteException
	{
		String output = "";
		String mapdir;
		String temp;
		synchronized (MountTable) {
			Enumeration keyTable = MountTable.keys();
			while (keyTable.hasMoreElements())
			{
				mapdir = (String)keyTable.nextElement();
				if (mapdir.startsWith(dirname))
				{
					temp = mapdir.substring(dirname.length());
					if (temp.indexOf(TOSFile.separator)==-1 && temp.length()>0)
						output = output + (String)MountTable.get(mapdir) + "\n";
				}
			}
		}
		return output;
	}

	/** Returns the mount table.
	 * @return Mount table.
 	 * @exception RemoteException if there is an RMI problem.
	 */
	public Hashtable getMountTable() throws RemoteException
	{
		return MountTable;
	}	
	
	/** Returns a dummy hashtable that contains the mount table and 
	 * name table as values to the keys "Mount" and "Name", respectively.
	 * @return the compound hashtable.
 	 * @exception RemoteException if there is an RMI problem.
	 */
	public Hashtable getObjectTable() throws RemoteException
	{
		Hashtable dummy = new Hashtable();
		dummy.put("Mount",MountTable);
		dummy.put("Name",NameTable);
		return dummy;
	}	
	
	/** Used to set the mount and name tables from a parent.
	 * <p> This function extracts the mount table and name table
	 * from the <code>objectTable</code> passed from its parent
	 * when the server started.
	 */
	void setTables()
	{
		MountTable = (Hashtable)objectTable.get("Mount");
		NameTable = (Hashtable)objectTable.get("Name");
	}


/*	public Vector getTable() throws RemoteException
	{
		objectTable.removeAllElements();
		objectTable.addElement(MountTable);
		objectTable.addElement(NameTable);
		return super.getTable();
	}
	
	public void setTable(Vector newTable) throws RemoteException
	{
		super.setTable(newTable);
		synchronized (MountTable) {
			MountTable = (Hashtable)objectTable.elementAt(0);
		}
		synchronized (NameTable) {
			NameTable = (Hashtable)objectTable.elementAt(1);
		}
	}
*/	
	/*
	void commit()
	{
		try {
			synchronized (datafile) {
				File file = new File(datafile);
				FileOutputStream ofstream = new FileOutputStream(file);
				ObjectOutputStream ostream = new ObjectOutputStream(ofstream);
				synchronized (MountTable) {
					ostream.writeObject(MountTable);
				}
				ostream.close();
				ofstream.close();
			}
		} catch (IOException e) {
		}
	}

	void retrieve()
	{
		try {
			synchronized (datafile) {
				File file = new File(datafile);
				FileInputStream ifstream = new FileInputStream(file);
				ObjectInputStream istream = new ObjectInputStream(ifstream);
				synchronized (MountTable) {
					MountTable = (Hashtable)istream.readObject();
				}
				istream.close();
				ifstream.close();
			}
		} catch (Exception e) {
			Debug.DisplayException("FileNameServer: retrieve",e);
		}
	} */
}

