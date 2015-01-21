package tos.system;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import tos.api.*;

/** This abstract class provides the basic functionality of all servers.
 * <p>The TOS server model assumes that multiple servers can exist in 
 * the system.  In that case, each server has a parent, from which it obtains
 * a complete set of data when it starts.  Usually, the parent server is simply 
 * the default server of the parent launcher of the new server's launcher.
 * <p>Each server has a hashtable that maps every object on every server to 
 * the remote stub of its actual location.  Thus, any server can redirect a client
 * to the correct server to deal with an object.  The key of this hashtable would
 * vary with each subclass - <code>InternalPipe</code> objects for pipe
 * servers, <code>SyncRecord</code> objects for sync servers, and so on.
 * <p>Each server also maintains a vector containing the remote stubs of every
 * other server of its class.
 * <p>This class also provides functions with which servers can synchronize their 
 * data with other servers, ensuring consistency throughout the TOS system.
 */

abstract public class Server extends RemoteServer implements TOSServer
{

	/** The map of objects to servers */
	protected Hashtable objectTable = new Hashtable();
	
	/** The table of other servers.	 */
	protected Vector serverTable = new Vector();
	
	/** The launcher to which the server is connected. */
	protected TOSLauncher launcher;	
	
	/** The parent server.	 */
	TOSServer parent;
	
	/** The host where the server is running. */
	protected String hostname;

	/** The TCP/IP port the server is listening on. */
	protected int portnum;
	
	/** Type of the server.	 */
	protected String type;
	
	/** Extension of initialization files.	 */
	static String Extension = ".dat";
	
	/** Constructor.
	 * <p>The constructor calls that of the superclass and instructs the
	 * Java Virtual Machine to run all <code>finalize()</code> functions on 
	 * virtual machine termination.
	 */
	public Server() throws RemoteException
	{
		super();
		System.runFinalizersOnExit(true);
	}
	
	/** Obtains the port number from the initialization file. */
	protected final int getPort(String name)
	{
		int PORTLENGTH = 5;
		boolean isdoneimportant = false;
		char portchar[] = new char[PORTLENGTH];
		int len = 0;
		try 
		{
			File file = new File(name+"Server"+Extension);
			FileReader reader = new FileReader(file);
			len = reader.read(portchar,0,PORTLENGTH);
			isdoneimportant = true;
			reader.close();
			file.delete();
		}
		catch (IOException e) 
		{
			if (!isdoneimportant)
			{
				Debug.ErrorMessage(name,"Unable to start " + name + ".\nError reading initialization file.");
				System.exit(1);
			}
		}
		String portstr = new String(portchar,0,len);
		int launchport = 0;
		try {
			launchport = Integer.parseInt(portstr);
		} catch (NumberFormatException e) {
			Debug.ErrorMessage(name,"Unable to start " + name + ".\nError in initialization file.");
			System.exit(1);
		}
		return launchport;
	}
	
	/** Exports the server as a remote object, registers it with the launcher,
	 * and obtains data from the server's parent, if any.
	 * <p>This function is called from the constructor of each subclass.
	 * @param type Type of server.
	 * @exception Exception if an error occurs.
	 */
	protected void init(String type) throws Exception
	{
		try {
			setStreams(type);
			int launchport = getPort(type);
			TOSServer stub = (TOSServer)UnicastRemoteObject.exportObject(this);
			hostname = InetAddress.getLocalHost().getHostName();
			Registry registry = LocateRegistry.getRegistry(launchport);
			registry.bind(type,this);
			launcher = (TOSLauncher)registry.lookup("Launcher");
			portnum = launchport;
			this.type = type;
			parent = launcher.registerServer(type,stub);
			if (!parent.getLocation().equals(getLocation()))
			{
				serverTable = parent.getServerTable();
				objectTable = parent.getObjectTable();
			}
			else
				addServer(stub);
		} catch (Exception e) {
			Debug.DisplayException("Server.init",e);
		}
	}
	
	/** Returns the name of the server's host.
	 * @return the server's hostname
	 * @exception RemoteException if an RMI problem occurs.
	 */
	public String getHost() throws RemoteException
	{
		return hostname;
	}
		
	/** Returns the server's object table.
	 * The object table is a hashtable mapping the key object the 
	 * server deals with to the remote stub of the server that holds it.
	 * This table should be identical between all servers of that type.
	 * @return the object table.
	 */
	public Hashtable getObjectTable() throws RemoteException
	{
		return objectTable;
	}	
	
	/** Add a new entry to the object table.
	 * <p>If called locally, this function will also update objects
	 * on all other servers of its type.
	 * @param key New object to add.
	 * @param Remote stub of server that holds it.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	public void addToObjectTable(Object key, TOSServer stub) throws RemoteException
	{
		synchronized (objectTable) {
			objectTable.put(key,stub);
		}

		if (stub.getLocation().equals(this.getLocation())) 
		{
			for (int i=0; i<serverTable.size(); i++)
			{
				TOSServer other = (TOSServer)serverTable.elementAt(i);
				if (!other.getLocation().equals(stub.getLocation()))
				{ 					
					other.addToObjectTable(key,stub);
				}
			}
		}		
	}
	
	/** Remove an entry from the object table.
	 * @param Object being removed.
	 * @param Remote stub being removed.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	public void deleteFromObjectTable(Object key,TOSServer stub) throws RemoteException
	{
		synchronized (objectTable) {
			objectTable.remove(key);
		}
		if (stub==this) 
		{
			for (int i=0; i<serverTable.size(); i++)
			{
				TOSServer other = (TOSServer)serverTable.elementAt(i);
				other.deleteFromObjectTable(key,stub);
			}
		}		
	}
		
	/** Adds a new server to the server table.
	 * When a new server starts, it calls <code>newServer</code> on 
	 * one server, which in turn calls <code>addServer</code> on
	 * all other servers.
	 * @param stub Remote stub of the new server.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	public void addServer(TOSServer stub) throws RemoteException
	{
		synchronized (serverTable) {
			serverTable.addElement(stub);
		}
	}

	/** Called by a newly started server.
	 * @param stub Remote stub of new server.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	public void newServer(TOSServer stub) throws RemoteException
	{
		for (int i=0; i<serverTable.size(); i++)
		{
			TOSServer other = (TOSServer)serverTable.elementAt(i);
			other.addServer(stub);
		}
	}
	
	/** Terminates a server running at the given location.
	 * Unlike <code>removeServer,</code> this function is 
	 * implemented by calling a launcher to carry out the 
	 * destruction itself.
	 * @return TOSServer A new server the calling launcher can connect to.
	 * @param location Location of the server to die.
	 * @exception RemoteException if an RMI problem occurs.
	 * @exception NotFoundException if no server or launcher could be found.
	 */
	public TOSServer destroyServer(String location) throws RemoteException, NotFoundException
	{
		int i=0;
		TOSServer stub = (TOSServer)this;
		while (i<serverTable.size())
		{
			stub = (TOSServer)serverTable.elementAt(i);
			if (location.equals(stub.getLocation()))
				break;
			i++;
		}
		if (i==serverTable.size())
			throw new NotFoundException();
		for (i=0; i<serverTable.size(); i++)
		{
			TOSServer other=(TOSServer)serverTable.elementAt(i);
			if (!location.equals(other.getLocation()))
				other.removeServer(stub);
		}
		try {
			stub.terminate();		
		} catch (UnmarshalException e) {
			// expected
		}
		TOSServer retval;
		try {
			retval = parent;
		} catch (NullPointerException e) {
			retval = (TOSServer)serverTable.elementAt(0);
			if (location.equals(retval.getLocation()))
				retval = (TOSServer)serverTable.elementAt(1);
		}
		return retval;
	}
	
	/** Terminates this server.
	 * @exception RemoteException if an RMI problem occurs.
	 * @exception NotFoundException if the launcher at this location cannot be found.
	 */
	public void terminate() throws RemoteException
	{
		System.exit(0);
	}

	/** Remove a server from the server table.
	 * @param stub Remote stub of the server to remove.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	public void removeServer(TOSServer stub) throws RemoteException
	{
		synchronized (serverTable) {
			serverTable.removeElement(stub);
		}
		synchronized (objectTable) {
			Enumeration enumeration = objectTable.keys();
			while (enumeration.hasMoreElements())
			{
				Object key = enumeration.nextElement();
				TOSServer objstub = (TOSServer)objectTable.get(key);
				if (objstub.getLocation().equals(stub.getLocation()))
					objectTable.remove(key);
			}
		}	
	}

	/** Returns the server table.
	 * Every server maintains a list of all other servers of its class.
	 * @return Server table.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	public Vector getServerTable() throws RemoteException
	{
		return serverTable;
	}
	
	/** Returns the server's location.
	 * @return the server's location.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	public String getLocation() throws RemoteException
	{
		return hostname + ":" + String.valueOf(portnum);
	}
	
	/** Unbinds the server from the RMI registry before being discarded.
	 * @exception Throwable if an exception is thrown by the superclass.
	 */
	public void finalize() throws Throwable
	{
		Naming.unbind(type);
		super.finalize();
	}
	
	/** Sets the standard output to point to a log file.
	 * <p>The reason for this function's existence is that using a log file
	 * to record errors is the only method that will work both in the all-GUI
	 * environment of the Macintosh and the non-GUI environment of a UNIX 
	 * telnet connection.
	 * @param filename Name of log file.
	 */
	void setStreams(String filename)
	{
		try {
			FileOutputStream fout = new FileOutputStream(filename+".log",true);
			PrintStream pout = new PrintStream(fout);
			System.setOut(pout);
		} catch (IOException e) {
			System.out.println("Unable to create error log file.");
		}
	}
}
