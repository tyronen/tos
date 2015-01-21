package tos.system;

import com.sun.java.swing.*;
import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import tos.api.*;

/**
 * The class that implements the functionality of the launcher.
 * 
 * 
 */


public class Launcher extends Server implements TOSLauncher, LauncherAdmin
{
	/** The name of the file that Disk information 
	 *  is stored in.
	 */
	public static final String datafile = "tos.rec";
	
	/** The port number, defaulted to 1099. */
	protected int portnum = 1099;
	
	/** The launcher to which this launcher connected when 
	 *  it was started, if there was one.	 */
	protected TOSLauncher parent;
	
	/** The remote stub of this launcher.	 */
	protected TOSLauncher stub;
	
	/** Set to <code>true</code> if this is the original launcher. 	 */
	protected boolean isFounder = false;
	
	/* Set to true if the launcher has disks to launch before a filename server is registered. */
	protected boolean disksWaiting = false;
	
	/** The host of this launcher.
	 * @see tos.system.Host 
	 */
	Host host = new Host();
	
	/** List of all child servers started by this launcher.
	 *  This table contains Process objects of each child server.  It
	 * is used in the <code>finalize()</code> function when the launcher
	 * terminates to stop all of them.
	 *  */
	Vector ChildTable = new Vector();
	
	/** List of all filename servers in the system.
	 *  This table contains <code>String</code> objects 
	 *  representing the host:port location of each
	 *  filename server.
	 */
	Vector FileNameServerTable = new Vector();

	/** List of all sync servers in the system.
	 *  This table contains <code>String</code> objects 
	 *  representing the host:port location of each 
	 *  sync server.
	 */
	Vector SyncServerTable = new Vector();
	
	/** List of all pipe servers in the system.
	 *  This table contains <code>String</code> objects 
	 *  representing the host:port location of each 
	 *  pipe server in the system
	 */
	Vector PipeServerTable = new Vector();

	/** List of all processes in the system.
	 *  This table contains <code>ProcessRecord</code> 
	 *  objects representing every process in the system.
	 *  @see tos.system.ProcessRecord
	 */
	Vector ProcessTable = new Vector();   
	
	/** List of all disks started by this launcher only.
	 *  This table contains <code>DiskRecord</code> 
	 *  objects representing every disk started by this launcher.
	 *  @see tos.system.DiskRecord
	 */
	Vector DiskTable = new Vector();
	
	/** List of all launchers in the system.
	 *  This table contains <code>ServerRecord</code> 
	 *  objects representing every launcher in the system.
	 *  @see tos.system.ServerRecord
	 */
	Vector LauncherTable = new Vector();
	
	/** List of all hosts in the system.
	 * This table contains <code>Host</code> 
	 * objects representing every host in the system.
	 * @see tos.system.Host
	 */
	Vector HostTable = new Vector();
	
	/** Table mapping launcher locations to the remote stubs of each launcher.
	 *  This hashtable has as key a <code>String</code> of the 
	 *  form host:port.  The value is a remote stub of each of 
	 *  the launchers in the system.
	 */
	Hashtable LauncherHostMap = new Hashtable();

	/** Remote stub of the default filename server.	 */
	TOSFileNameServer fnserver;

	/** Remote stub of the default pipe server.	 */
	TOSPipeServer pserver;

	/** Remote stub of the default sync server.	 */
	TOSSyncServer sserver;
		
	/** Retrieves argument list from launcher.dat file.
	 *  This function retrieves the starting arguments from the file
	 *  named 'launcher.dat'.  If the file is not present or produces
	 *  an I/O error, an empty argument list is returned.
	 *  The arguments are expected to be delimited by a single space.
	 * @return the argument list.
	 */
	static final String[] getArgs()
	{
		int LINELENGTH = 80;
		boolean isdoneimportant = false;
		char arglist[] = new char[LINELENGTH];
		int len = 0;
		try 
		{
			File file = new File("Launcher"+Extension);
			FileReader reader = new FileReader(file);
			len = reader.read(arglist,0,LINELENGTH);
			isdoneimportant = true;
			reader.close();
			file.delete();
		}
		catch (IOException e) 
		{
			// Assuming error caused because file absent
			// this would mean no arguments
			return new String[0];
		}
		String argstr = new String(arglist,0,len);
		StringTokenizer strtok = new StringTokenizer(argstr);
		int arglen = strtok.countTokens();
		String[] argarray = new String[arglen];
		for (int i=0; i<arglen; i++)
		{
			argarray[i] = strtok.nextToken();
		}
		return argarray;
	
	}
	
	/** The standard Java entry function.
	 *
	 * Main first uses <code>getArgs()</code> to read in the 
	 * initialization arguments.  If more than two are present, the function 
	 * will terminate with an error message.
	 * If there is at least one argument present, the first is checked 
	 * to see if it is numeric.  If it is, it is assumed to represent the port
	 * number and any second argument to represent the parent launcher to 
	 * connect to.  
	 * If the first argument is non-numeric, it is assumed to represent the 
	 * parent launcher, and any second argument present produces an error.
	 * If either argument is omitted, the port number defaults to 1099 
	 * and there is assumed to be no parent.
	 * <code>Main()</code> concludes by invoking the appropriate constructor.
	 */
	public static final void main(String args[])
	{
		String parentname;
		int port;
		String[] myargs = getArgs();
				
		String errstr = "File launcher.dat should read: [port] [parent]";
		if (myargs.length>2)
		{
			System.out.println(errstr);
			return;
		}
		
		port = 1099;
		parentname = "";
		if (myargs.length>0)
		{
			try {
				port = Integer.parseInt(myargs[0]);
				if (myargs.length==2)
					parentname = myargs[1]; // only executes if myargs[0] is numeric
			} catch (NumberFormatException e) {
				if (myargs.length==1)
					parentname = myargs[0]; // only executes if myargs[0] is non-numeric
				else
				{
					System.out.println(errstr);
					return;
				}
			}
		}
	
		System.setSecurityManager(new RMISecurityManager());
		Launcher launcher;
		try {
			if (parentname.equals(""))
				launcher = new Launcher(port);
			else
				launcher = new Launcher(port,parentname);
		} catch (RemoteException e) {
			
			System.exit(1);
		}
		
	}
	
	/* Constructors */
	
	/** Constructor to start a new system.
	 * This constructor starts the launcher off, initializes 
	 * data structures, starts any disks found in <i>tos.rec</i>,
	 * and starts a filename, pipe, and sync server.  Finally, it 
	 * initializes certain parent-only data structures.
	 * @param port Port number to listen on.
	 * @exception RemoteException If there is an RMI problem
	 */
	public Launcher(int port) throws RemoteException
	{
		super();
		startLauncher(port);
		initialize();
		retrieve();
		parentInit();
		startOtherServers();
		disksWaiting = true;
		System.out.println("Launcher running.");
	}
	
	/** Constructor to join a new system.
	 * This constructor differs from the other in that it does not
	 * start the three servers and does not initialize the parent-only
	 * structures.  Instead, it connects to the launcher at the location
	 * passed in.
	 * @param port Port number to listen on
	 * @param parentname Host:port location of parent
	 * @exception RemoteException If there is an RMI problem
	 */
	public Launcher(int port, String parentname) throws RemoteException
	{
		super();
		startLauncher(port);
		initialize();
		connect(parentname);
		retrieve();
		startDisks();
		System.out.println("Launcher running.");
	}
	
	/* Initialization functions */
	
	/** Starts the launcher.
	 * This function creates an RMI registry at the specified port,
	 * exports remote references to this launcher, and binds it under 
	 * the name "Launcher".  It will terminate the launcher if the port
	 * is already taken.
	 * @param port port number to listen on
	 */
	void startLauncher(int port)
	{
		Registry registry;
		try {
			registry = LocateRegistry.createRegistry(port);
			String hostname = "//"+InetAddress.getLocalHost().getHostName();
			stub = (TOSLauncher)UnicastRemoteObject.exportObject(this);
			try {
				registry.bind("Launcher", this);
			} catch (AlreadyBoundException e) {
				System.out.println("This port is already in use by another launcher.");
				System.exit(1);
			}
			this.portnum = port;
			System.out.println("Launcher is starting...");
        } catch (Exception e) {
           System.out.println("Launcher err: " + e.getMessage());
           
		   System.exit(1);
		}
	}

	/** Initializes data structures.
	 * The only variable initialized here is <code>host</code>.
	 * @see tos.system.Host
	 */
	void initialize()
	{
		host = new Host();
	}

	/** Connects to a parent launcher.
	 * This function uses the RMI Naming system to connect to a parent launcher.
	 * When it finds the parent, it registers itself with the parent and
	 * imports the system tables from the parent.
	 * @param parentname Host:port location of parent launcher.
	 */
	void connect(String parentname) throws RemoteException
	{
		System.out.println("Attempting to connect to " + parentname);
		try {
			parent = (TOSLauncher)Naming.lookup("rmi://" + parentname + "/Launcher");
			LauncherAdmin mylauncher = (LauncherAdmin)parent;			
			mylauncher.registerLauncher(stub,host.hostname,portnum);
			Vector systemTables = mylauncher.getAll();
			setAll(systemTables);
		} catch (Exception e) {
			System.out.println("Unable to connect to system.");
			System.out.println(e);
			
			terminate();
		}
	}

	/** Returns a complete set of the system's internal state.
	 * This function returns a complete set of the system tables
	 * concatenated in one vector.  The tables included are the <code>
	 * FileNameServerTAble, SyncServerTable, PipeServerTable, LauncherHostMap,
	 * ProcessTable, DiskTable, LauncherTable,</code> and <code>HostTable.</code>
	 * Finally, the three stubs to the servers are added.
	 * @return the set of tables
	 * @exception RemoteException If there is an RMI problem.
	 */
	public Vector getAll() throws RemoteException
	{
		Vector systemTables = new Vector();
		systemTables.addElement(FileNameServerTable);
		systemTables.addElement(SyncServerTable);
		systemTables.addElement(PipeServerTable);
		systemTables.addElement(LauncherHostMap);
		systemTables.addElement(ProcessTable);
		systemTables.addElement(DiskTable);
		systemTables.addElement(LauncherTable);
		systemTables.addElement(HostTable);
		systemTables.addElement(fnserver);
		systemTables.addElement(sserver);
		systemTables.addElement(pserver);
		return systemTables;
	}	
	
	/** Replaces the system tables.
	 * This function, called by <code>startLauncher,</code> replaces
	 * every system table with an updated set obtained from 
	 * another launcher.
	 * @param systemTables Set of system tables.
	 * @exception RemoteException if there is an RMI problem.
	 */
	public void setAll(Vector systemTables) throws RemoteException
	{
		int i = 0;
		// Use of the i++'s makes the code
		// easier to modify than using 0,1,2,3...
		FileNameServerTable = (Vector)systemTables.elementAt(i++);
		SyncServerTable = (Vector)systemTables.elementAt(i++);
		PipeServerTable = (Vector)systemTables.elementAt(i++);
		LauncherHostMap = (Hashtable)systemTables.elementAt(i++);
		ProcessTable = (Vector)systemTables.elementAt(i++);
		DiskTable = (Vector)systemTables.elementAt(i++);
		LauncherTable = (Vector)systemTables.elementAt(i++);
		HostTable = (Vector)systemTables.elementAt(i++);
		fnserver = (TOSFileNameServer)systemTables.elementAt(i++);
		sserver = (TOSSyncServer)systemTables.elementAt(i++);
		pserver = (TOSPipeServer)systemTables.elementAt(i++);

	}
	
	/** Initializes parent data structures.
	 * This function adds representations of this launcher to selected
	 * system tables - <code>HostTable, LauncherTable,</code> and 
	 * <code>LauncherHostMap</code>
	 */
	void parentInit()
	{
		isFounder = true;
		HostTable.addElement(host);
		ServerRecord srec = new ServerRecord(stub,"Launcher",host.hostname,portnum);
		LauncherTable.addElement(srec);
		LauncherHostMap.put(host.hostname+":"+String.valueOf(portnum),stub);
	}
			
	/** Starts disks to be restarted and if necessary a filename server.
	 * If a filename server has not already been started, one is.  Then,
	 * every entry in the DiskTable is examined and the corresponding 
	 * disk server started if it belongs with this launcher.  
	 */
	void startDisks()
	{
		/*if (hasFN)
		{
			try {
				launchServer(FN+"Server");
			} catch (RemoteException e) {
				System.out.println("Unable to launch file name server.");
			}
		}*/
		for (int i=0; i<DiskTable.size(); i++)
		{
			DiskRecord fsrec = (DiskRecord)DiskTable.elementAt(i);
			if (fsrec.hostname.equals(host.hostname))
			{
				try {
					startDisk(fsrec.servername);
				} catch (Exception e) { 
					System.out.println("Error launching Disks.");
					
				}
			}
		}
		
	}
	
	/** Starts the sync, pipe, and filename servers.
	 * @exception RemoteException if there is an RMI problem.
	 */
	void startOtherServers() throws RemoteException
	{
		launchServer(FN+"Server");
		launchServer(SYNC+"Server");
		launchServer(PIPE+"Server");
	}

	/* Internal info functions */
	
	/** Returns the <code>LauncherHostMap</code>.
	 * This function overrides the <code>getObjectTable()</code>
	 * function of class <code>Server.</code>
	 * @return the LauncherHostMap.
	 * @exception RemoteException if there is an RMI problem.
	 * @see Server#getObjectTable
	 */
	public Hashtable getObjectTable() throws RemoteException
	{
		return LauncherHostMap;
	}
	
	/** Returns the default filename server.
	 * @return <code>TOSFileNameServer</code> Default filename server.
	 * @exception RemoteException if there is an RMI problem.
	 */
	public TOSFileNameServer getFileNameServer() throws RemoteException
	{
		return fnserver;
	}
	
	/** Returns the default pipe server.
	 * @return <code>TOSPipeServer</code> Default pipe server.
	 * @exception RemoteException if there is an RMI problem.
	 */
	public TOSPipeServer getPipeServer() throws RemoteException
	{
		return pserver;
	}


	/** Returns the default sync server.
	 * @return <code>TOSSyncServer</code> Default sync server.
	 * @exception RemoteException if there is an RMI problem.
	 */
	public TOSSyncServer getSyncServer() throws RemoteException	
	{
		return sserver;
	}
	
	/** Returns a <code>Host</code> object for the given <code>String</code> representation.
	 * This is an internal service function that maps string locations
	 * to <code>Host</code> objects.
	 * @param location String location of host name.
	 * @return <code>Host</code> searched for.
	 * @exception NotFoundException if there is no such host.
	 * @see Host
	 */
	Host getHost(String location) throws NotFoundException
	{
		int i;
		Host myhost;
		String hostname;
		// Strip off references to port number
		int lastcolon = location.indexOf(":");
		if (lastcolon>0)
			hostname = location.substring(0,lastcolon);
		else
			hostname = location;
		synchronized (HostTable) {
			for (i=0; i<HostTable.size(); i++)
			{
				myhost = (Host)HostTable.elementAt(i);
				if (myhost.hostname.equals(hostname))
					return myhost;
				if (myhost.address.getHostAddress().equals(hostname))
					return myhost;
			}	
		}
		throw (new NotFoundException());
	}
	

	/** Returns the launcher's location.
	 * @return the launcher's location.
	 * @exception RemoteException if an RMI problem occurs.
	 */
	public String getLocation() throws RemoteException
	{
		return host.hostname + ":" + String.valueOf(portnum);
	}

	/** Determines if a host exists with this name.
	 * Searches the host table if a host exists with this name.
	 * @return <code>true</code> if the host exists, else <code>false</code>.
	 * @param otherhostname Host to search for.
	 */
	boolean hostExists(String otherhostname)
	{
		for (int i=0; i<HostTable.size(); i++)
		{
			Host myhost = (Host)HostTable.elementAt(i);
			if (host.hostname.equals(otherhostname))
				return true;
		}
		return false;
	}
	
	/** Returns the specified system table.
	 * Taking a string representation of the table, this 
	 * function returns one of the vector-based tables.
	 * @param name The name of the table to return.
	 * @return the table of that name.
	 * @exception RemoteException if there is an RMI problem.
	 */
	public Vector getTable(String name) throws RemoteException
	{		
		try {
		if (name.equals("Disk"))
			return DiskTable;
		else if (name.equals("Launcher"))
			return LauncherTable;
		else if (name.equals("Process"))
			return ProcessTable;	
		else if (name.equals("Host"))
			return HostTable;
		else if (name.equals(TOSServer.SYNC))
			return SyncServerTable;
		else if (name.equals(TOSServer.PIPE))
			return PipeServerTable;
		else if (name.equals(TOSServer.FN))
			return FileNameServerTable;
		else
			throw new RemoteException("Unknown table requested.");
		} catch (Throwable e) {
			
			throw new RemoteException("Unknown table requested.");
		}
	
	}
	
	/* Launcher functions */
	
	/** Receive a registration from a new launcher.
	 * This function is called inside an existing launcher that a new 
	 * launcher is connecting to.  It adds the new launcher's host name to the
	 * HostTable, its <code>ServerRecord</code> to the <code>LauncherTable</code>,
	 * and its name and remote stub entered into the <code>LauncherHostMap</code> hashtable.
	 * @param TOSLauncher Remote stub of new launcher.
	 * @param hostname Name of host new launcher is on.
	 * @param port New port number.
	 * @exception RemoteException if there is an RMI problem.
	 */
	public void registerLauncher(TOSLauncher launcher,String hostname, int port) throws RemoteException
	{
		synchronized (HostTable) {
			if (!hostExists(hostname))
				HostTable.addElement(hostname);
		}
		ServerRecord srec = new ServerRecord(launcher,"Launcher",hostname,port);
		synchronized (LauncherTable) {
			if (!LauncherTable.contains(srec))
				LauncherTable.addElement(srec);
		}
		String fullname = hostname+":"+String.valueOf(port);
		synchronized (LauncherHostMap) {
			LauncherHostMap.put(fullname,launcher);
		}
		System.out.println("Launcher from " + fullname + " has been registered.");
	}
	
	
	/** Terminates another launcher.
	 * The indicated launcher is first removed from the <code>LauncherHostMap</code>
	 * and <code>LauncherTable</code> before having its <code>terminate()</code>
	 * function called.
	 * @param name host:port of the launcher to terminate.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NoLauncherException if there is no such launcher.
	 */
	public void terminateLauncher(String name) throws RemoteException, NoLauncherException
	{
		
		// remove all servers on that location
		if (FileNameServerTable.contains(name))
		{
			try {
				fnserver.destroyServer(name);
			} catch (NotFoundException e) {
				// anomaly; ignore it
			}
			FileNameServerTable.removeElement(name);
		}
		if (PipeServerTable.contains(name))
		{
			try {
				pserver.destroyServer(name);
			} catch (NotFoundException e) {
				// anomaly; ignore it
			}
			PipeServerTable.removeElement(name);
		}
		if (SyncServerTable.contains(name))
		{
			try {
				sserver.destroyServer(name);
			} catch (NotFoundException e) {
				// anomaly; ignore it
			}
			SyncServerTable.removeElement(name);
		}

		// find the launcher running on that host
		// then instruct it to terminate.  
		TOSLauncher launcher;
		synchronized (LauncherHostMap) {
			launcher = (TOSLauncher)LauncherHostMap.get(name);
			LauncherHostMap.remove(name);
		}
		synchronized (LauncherTable) {
			LauncherTable.removeElement(launcher);
		}
		try {
			launcher.terminate();
		} catch (UnmarshalException e) {
			// this is expected
		}
	}

	/** Terminates the launcher.
	 * Unbinds the launcher from the registry, kills all running disks, 
	 * writes the launcher state to disk, and calls <code>System.exit()</code>
	 * @exception RemoteException if there is an RMI problem. 
	 */	
	public void terminate() throws RemoteException 
	{
		try {
			Naming.unbind("Launcher");
		} catch (Exception e) {
			// Ah who cares.....if it was already unbound it doesn't matter does it?
		}
		// kill all running Disks
		int i;
		synchronized (DiskTable) {
			for (i=0; i<DiskTable.size(); i++)
			{
				DiskRecord fsrec = (DiskRecord)DiskTable.elementAt(i);
				if (fsrec.isRunning && fsrec.hostname.equals(host.hostname))
				{
					TOSDisk fs;
						try {
						fs = (TOSDisk)Naming.lookup("rmi://" + host.hostname + "/FS"+fsrec.servername);
						fs.terminate();
					} catch (UnmarshalException e) {
					// do nothing...this is normal	
					} catch (Exception e) {
						System.out.println(e);
						System.out.println("Unable to terminate file server " + fsrec.servername);
					}
				}
			}
		}
		commit();

		// Kill all child processes
		String loc = getLocation();
		try {
			if (loc.equals(fnserver.getLocation()))
			{
				try {
					fnserver.terminate();
				} catch (UnmarshalException e) {
					// ignore - this is normal
				}
			}
			if (loc.equals(sserver.getLocation()))
			{
				try {
					sserver.terminate();
				} catch (UnmarshalException e) {
					// ignore - this is normal
				}
			}
			if (loc.equals(pserver.getLocation()))
			{
				try {
					pserver.terminate();
				} catch (UnmarshalException e) {
					// ignore - this is normal
				}
			}
		} catch (NotFoundException e) {
			System.out.println("Error terminating a server.");
		} catch (NullPointerException e) {
			// this will happen if no server exists - ignore
		}
		System.exit(0);
	}

	/* Server functions */

	/** Sends a launch request to the launcher in question.
	 * Searches the <code>LauncherHostMap</code>for the desired location to
	 * obtain the correct remote stub, then calls that stub's <code>launchServer</code
	 * function.
	 * @param name Name of server to launch.
	 * @param location Location of new server.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NoLauncherException if there is no launcher at that location.
	 * @exception IOException if a problem occurs on the other launcher.
	 */
	public void launchServer(String name, String location) throws RemoteException, NoLauncherException, IOException
	{
		TOSLauncher launcher;
		try {
			launcher = (TOSLauncher)LauncherHostMap.get(location);
		} catch (NullPointerException e) {
			throw new NoLauncherException("Launcher not found on remote host");
		}
		launcher.launchServer(name);
		System.out.println("Launching " + name + " at " + location);
	}
	
	/** Launches a server on this launcher's host.
	 * <b>This function requires a correction for Macintosh compatibility!</b>
	 * Creates an initialization file containing this launcher's port number
	 * and calls <code>Runtime.exec()</code>to start the server in a new
	 * host process.
	 * @param name Name of new server.
	 * @exception RemoteException if there is an RMI problem.
	 */
	public void launchServer(String name) throws RemoteException
	{ 
		try {
			File file = new File(name+Server.Extension);
			FileWriter writer = new FileWriter(file);
			char[] portstr = String.valueOf(portnum).toCharArray();
			writer.write(portstr,0,portstr.length);
			writer.close();
			Process process = Runtime.getRuntime().exec(name);
			ChildTable.addElement(process);
		} catch (IOException e) {
			System.out.println("Error launching "+name);	
			throw new RemoteException();
		}
	}
	
	

	
	/** Adds a new server to the system tables.
	 * The new server can a pipe, sync, or filename server at
	 * any location.  This function is an internal launcher
	 * service function.
	 * @param type Type of server.
	 * @param location Location of new server.
	 */
	public void addToTable(String type,String location) throws RemoteException
	{
		if (type.equals(TOSServer.PIPE))
			PipeServerTable.addElement(location);
		else if (type.equals(TOSServer.SYNC))
			SyncServerTable.addElement(location);
		else if (type.equals(TOSServer.FN))
			FileNameServerTable.addElement(location);
		if (location.equals(getLocation()))
		{
			Enumeration enumeration = LauncherHostMap.keys();
			while (enumeration.hasMoreElements())
			{
				String curloc = (String)enumeration.nextElement();
				TOSLauncher curlaun = (TOSLauncher)LauncherHostMap.get(curloc);
				if (!curloc.equals(location))
					curlaun.addToTable(type,location);
			}
		}
	}

	/** Signals server to terminate.
	 * Calls another function to destroy the server destined to die.  
	 * If the victim is not a launcher, the call is made via the default 
	 * server of that type.
	 * @param name Type of server to destroy.
	 * @param location Location of server to destroy.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NotFoundException if there is no launcher at that location.
	 */
	public void terminateServer(String name, String location) throws RemoteException, NotFoundException
	{
		if (name.equals("Launcher"))
			try {
				terminateLauncher(location);
			} catch (NoLauncherException e) {
				throw new NotFoundException();  // for consistency
			}
		else if (name.equals(FN))
			fnserver = (TOSFileNameServer)fnserver.destroyServer(location);
		else if (name.equals(PIPE))
			pserver = (TOSPipeServer)pserver.destroyServer(location);
		else if (name.equals(SYNC))
			sserver = (TOSSyncServer)sserver.destroyServer(location);
	}
	
	/** Registers a new server.
	 * Filename, pipe, and sync servers all call this function upon 
	 * their successful initialization, passing in their remote stubs
	 * so that the launchers will be able to access them.
	 * The new server will also become the default server for this launcher
	 * of that type.  A caught <code>NullPointerException</code>is used
	 * if there was previously no default server.
	 * @param type Type of server (filename, pipe, or sync).
	 * @stub Remote stub of server.
	 * @exception RemoteException if there is an RMI problem.
	 * @see FileNameServer
	 * @see PipeServer
	 * @see SyncServer
	 */
	public TOSServer registerServer(String type,TOSServer stub) throws RemoteException
	{
		if (type.equals(FN) && disksWaiting==true)
			startDisks();
		TOSServer parent;
		addToTable(type,stub.getLocation());
		try {
			parent = addServer(type,stub);
		} catch (NullPointerException e) {
			// this just means this is the first time
			changeStubs(type,stub);
			parent = addServer(type,stub);
			System.out.println(type + " server has been registered as the parent.");
			return parent;
		}
		changeStubs(type,stub);
		System.out.println(type + " server has been registered.");
		return parent;
	}
	
	/** Sends a new remote stub to the default server of that type.
	 * The default server will, in turn, notify all other servers of the
	 * new server's existence, making it a happy member of the family.
	 * @return Remote stub of that type.
	 * @param type Type of server to add.
	 * @param stub Remote stub of new server.
	 * @exception RemoteException if there is an RMI problem.
	 */
	TOSServer addServer(String type,TOSServer stub) throws RemoteException
	{
		if (type.equals(TOSServer.PIPE))
		{
			pserver.newServer((TOSPipeServer)stub);
			return pserver;
		}
		else if (type.equals(TOSServer.SYNC))
		{
			sserver.newServer((TOSSyncServer)stub);
			return sserver;
		}
		else if (type.equals(TOSServer.FN))
		{
			fnserver.newServer((TOSFileNameServer)stub);		
			return fnserver;
		}
		else
			return null;
	}
	
	/** Changes the default server to a new entry.
	 * This is an internal service function called by 
	 * <code>registerServer</code>.
	 * @param type Type of server to add.
	 * @param stub Remote stub of new server.
	 * @exception RemoteException if there is an RMI problem.
	 */
	void changeStubs(String type, TOSServer stub) throws RemoteException
	{
		if (type.equals(TOSServer.PIPE))
			pserver = (TOSPipeServer)stub;
		else if (type.equals(TOSServer.SYNC))
			sserver = (TOSSyncServer)stub;
		else if (type.equals(TOSServer.FN))
			fnserver = (TOSFileNameServer)stub;	
	}
		
	/* Process functions */
	
	/** Registers a new process.
	 * Called by a newly running TOS user process.  It adds the description
	 * of the process to the <code>ProcessTable</code>.
	 * @param process Remote stub of the process' <code>Listener</code> thread
	 * @return TOS identifier of the new process.
	 * @exception RemoteException if there is an RMI problem.
	 * @see TOSProcess
	 * @see Listener
	 */
	public int registerProcess(TOSListener process, String classname, String hostname) throws RemoteException	
	{
		ProcessRecord rec = new ProcessRecord(process, classname, hostname);
		synchronized (ProcessTable) {
			ProcessTable.addElement(rec);
		}
		System.out.println("Registered process: id=" + rec.id + ", classname=" + classname + ", hostname=" + hostname);
		return rec.id;
	}
	
	/** Removes an exiting process from the system tables.
	 * Called when a <code>TOSProcess</code> terminates.  The process
	 * is removed from the system tables.
	 * @param process TOS identifier of process.
	 * @exception RemoteException if there is an RMI problem.
	 */
	public void unregisterProcess(int process) throws RemoteException
	{
		synchronized (ProcessTable) {
			System.out.println("Unregistering process: " + process);
			for (int i=0; i<ProcessTable.size(); i++)
			{
				ProcessRecord prec = (ProcessRecord)ProcessTable.elementAt(i);
				if (prec.id==process)
				{
					ProcessTable.removeElement(prec);
					break;
				}
			}
		}
	}
		
	/** Kills a process immediately.
	 * Called from the Kill Process option in the Administrator's 
	 * Command menu.  This will unregister the process before signaling
	 * it to destroy itself.
	 * @param id TOS identifier of process to die.
	 * @exception RemoteException if there is an RMI problem.
	 */
	public void killProcess(int id) throws RemoteException
	{
		synchronized (ProcessTable) {
			for (int i=0; i<ProcessTable.size(); i++)
			{
				ProcessRecord prec = (ProcessRecord)ProcessTable.elementAt(i);
				if (prec.id==id) 
				{
					try { 
						prec.stub.killProcess();
					} catch (RemoteException e) {
						// this usually means a network problem, or proc already dead
					}
					break;
				}
			}
		}
		unregisterProcess(id);
	}	
		/* Disk functions */
	
	/** Registers a new disk.
	 * The new disk's data is added to the disk table and given to the default filename
	 * server for its inclusion.  The disk will already exist in the
	 * <code>DiskTable</code> from when it was actually created.  This function
	 * is called later, when the disk has finished its initialization.
	 * This is the final step in creating a new disk or restarting an existing one.
	 * @param filename Physical file of the disk.
	 * @param servername Server name.
	 * @param stub Remote stub of disk.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NoDiskException if no disk is expected of that name.
	 */
	public void registerDisk(String filename, String servername, TOSDisk stub) throws RemoteException, NoDiskException
	{
		int i;
		synchronized (DiskTable) {
			for (i=0; i<DiskTable.size(); i++)
			{
				DiskRecord fsrec = (DiskRecord)DiskTable.elementAt(i);
				if (fsrec.filename.equals(filename))
				{	
					fsrec.isRunning = true;
					fsrec.servername = servername;
					fsrec.stub = stub;
					System.out.println("File server " + fsrec.servername + " registered.");
					setDiskRecord(fsrec);
					fnserver.addDisk(servername,stub);
					return;
				}
			}
		}
		throw new NoDiskException();
	}
	
	/** Creates a new disk.
	 * This is the first step in the creation of a new disk.  The disk is
	 * added to the <code>DiskTable</code> and the appropriate launcher
	 * is told to actually create the disk.
	 * @param name Name of disk.
	 * @param hostname Host disk will run on.
	 * @param numfiles Maximum number of files disk can hold.
	 * @param blocksize Size of data blocks.
	 * @param numblocks Number of data blocks available.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NoLauncherException if no launcher exists on that host.
	 * @exception IOException if an I/O problem develops during disk creation.
	 * @see Administrator#OnDCreate
	 */
	public void createDisk(String name,String hostname,int numfiles,int blocksize,int numblocks) throws RemoteException, NoLauncherException, IOException
	{
		DiskRecord fsrec = new DiskRecord(name,hostname,numfiles,blocksize,numblocks);
		synchronized (DiskTable) {
			fsrec.pos = DiskTable.size();
			DiskTable.addElement(fsrec);
		}
		commit();
		String NumFiles = " " + (new Integer(numfiles)).toString();
		String BlockSize = " " + (new Integer(blocksize)).toString();
		String NumBlocks = " " + (new Integer(numblocks)).toString();
		//launchServer("Disk " + name + NumFiles + BlockSize + NumBlocks,hostname);
		Host runhost;
		try {
			runhost = getHost(hostname);
		} catch (NotFoundException e) {
			throw new NoLauncherException();
		}

		if (host.equals(runhost))
			createDisk(name,numfiles,blocksize,numblocks);
		else
		{
			LauncherAdmin launcher;
			launcher = (LauncherAdmin)LauncherHostMap.get(hostname);
			if (launcher==null)
				throw new NoLauncherException("Launcher not found on remote host");
			launcher.createDisk(name,numfiles,blocksize,numblocks);
		}
	}
	
	/** Variable used to ensure that new disk initialization-file names are unique */
	int initval = 0;
	
	/** Returns the unique name of a disk initialization file.
	 * Unlike other TOS server processes, a single launcher can create
	 * more than one disks, conceivably at the same time.  Thus, it labels
	 * each initialization file it creates in successive order; Disk1.dat, 
	 * Disk2.dat etc., to a preset maximum (currently 128).
	 * @return name of next initialization file
	 * @exception IOException if the maximum has been reached.
	 */
	String nextDiskInitName() throws IOException
	{
		if (initval>Disk.MAX_DISKS)
			throw new IOException ("This launcher has the maximum number of disks.");
		return "Disk" + String.valueOf(initval++) + Server.Extension;
	}
	
	/** Creates a new disk.
	 * Writes out the disk parameters to its initialization file and
	 * calls <code>Runtime.exec()</code> to start the new process.
	 * @param name Name of new disk.
	 * @param numfiles Maximum number of files.
	 * @param blocksize Size of data blocks.
	 * @param numblocks Number of data blocks.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception IOException if the maximum number of disks has been launched 
	 * <b>or</b> an I/O problem occurs when writing the initialization file or starting
	 * the new process.
	 */
	public void createDisk(String name,int numfiles,int blocksize,int numblocks) throws RemoteException, IOException
	{
		try {
			String filename;
			filename = nextDiskInitName();
			File file = new File(filename);
			FileWriter writer = new FileWriter(file);
			String outstr = name+" "+String.valueOf(portnum)+" "+String.valueOf(numfiles)+" "+String.valueOf(blocksize)+" "+String.valueOf(numblocks);
			char[] outchr = outstr.toCharArray();
			writer.write(outchr,0,outchr.length);
			writer.close();
			Process process = Runtime.getRuntime().exec("Disk");													
		} catch (IOException e) {
			System.out.println("Unable to launch disk: exception " +e);
			
		}
	
	}
	
	/** Restarts a previously running disk.
	 * Looks up the disk's host and instructs the correct launcher
	 * to start it using <code>launchServer</code>
	 * @param servername Name of disk.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NotFoundException is there is no disk of that name.
	 * @exception NoLauncherException if there is no launcher at that location.
	 * @exception IOException if an error occurs starting the disk.
	 */
	public void startDisk(String servername) throws RemoteException, NotFoundException, NoLauncherException, IOException
	{
		String hostname = getDiskHost(servername);
		Host runhost;
		try {
			runhost = getHost(hostname);
		} catch (NotFoundException e) {
			throw new NoLauncherException();
		}

		if (host.equals(runhost))
			launchDisk(servername);
		else
		{
			LauncherAdmin launcher;
			launcher = (LauncherAdmin)LauncherHostMap.get(hostname);
			if (launcher==null)
				throw new NoLauncherException("Launcher not found on remote host");
			launcher.launchDisk(servername);
		}
	}
	
	/** Launches a disk on the launcher's host.
	 * <p>This function is to be called by the <code>startDisk</code>
	 * function.
	 * @param servername Disk to be launched.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NotFoundException is there is no disk of that name.
	 * @exception NoLauncherException if there is no launcher at that location.
	 * @exception IOException if an error occurs starting the disk.
	 */
	public void launchDisk(String servername) throws RemoteException
	{	
		try {
			String filename;
			filename = nextDiskInitName();
			File file = new File(filename);
			FileWriter writer = new FileWriter(file);
			String outstr = servername+" "+String.valueOf(portnum);
			char[] outchr = outstr.toCharArray();
			writer.write(outchr,0,outchr.length);
			writer.close();
			Process process = Runtime.getRuntime().exec("Disk");													
		} catch (IOException e) {
			System.out.println("Unable to launch disk: exception " +e);
			
		}
	}		
	
	/** Stops a running disk.
	 * Signals the disk process to terminate and marks it in the table as 
	 * not running.
	 * @param servername Name of disk.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NotFoundException is there is no disk of that name.
	 */
	public void stopDisk(String servername) throws RemoteException, NotFoundException
	{
		DiskRecord fsrec = getDiskRecord(servername);
		TOSDisk fs = fsrec.stub;
		try {
			fs.terminate();
		} catch (UnmarshalException e) {
			// do nothing - this is normal when terminating remote server
		}
		fsrec.isRunning = false;
		setDiskRecord(fsrec);
	}
	
	/** Removes a disk from the system.
	 * Unmounts the disk, stops it, and removes its entry from the
	 * <code>DiskTable</code>.
 	 * @param servername Name of disk.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NotFoundException is there is no disk of that name.
	 */
	public void removeDisk(String servername) throws RemoteException, NotFoundException
	{
		fnserver.unmount(servername);
		DiskRecord fsrec = getDiskRecord(servername);
		if (fsrec.isRunning)
			stopDisk(servername);
		int i;
		synchronized (DiskTable) {
			DiskTable.removeElement(fsrec);
			for (i=fsrec.pos; i<DiskTable.size(); i++)
			{
				fsrec = (DiskRecord)DiskTable.elementAt(i);
				fsrec.pos--;
			}
		}
	}
	
	/** Identifies the host a given disk is running on.
 	 * @param servername Name of disk.
	 * @exception RemoteException if there is an RMI problem.
	 * @exception NotFoundException is there is no disk of that name.
	 */
	public String getDiskHost(String servername) throws RemoteException, NotFoundException
	{
		DiskRecord fsrec = getDiskRecord(servername);
		return fsrec.hostname;
	}
	
	/** Replaces an entry in the <code>DiskTable</code>.
	 * @param fsrec Entry to replace.
	 */
	void setDiskRecord(DiskRecord fsrec)
	{
		synchronized (DiskTable) {
			DiskTable.removeElementAt(fsrec.pos);
			DiskTable.insertElementAt(fsrec,fsrec.pos);
		}
	}
							 
	/** Retrieves an entry in the <code>DiskTable</code>.
 	 * @param servername Name of disk.
	 * @exception NotFoundException is there is no disk of that name.
	 */
	DiskRecord getDiskRecord(String servername) throws NotFoundException
	{
		int i;
		for (i=0; i<DiskTable.size(); i++)
		{
			DiskRecord fsrec = (DiskRecord)DiskTable.elementAt(i);
			fsrec.pos = i;
			if (fsrec.servername.equals(servername))
				return fsrec;
		}
		throw new NotFoundException();
	}
		
	
	
	/* Retrieve and commit functions */
	
	/** Saves the internal state of the launcher.
	 * This places only the 
	 * <code>DiskTable</code>to disk.  This is the only information
	 * the launcher contains that is persistent.
	 */
	void commit()
	{
		try {
			synchronized (datafile) {
				File file = new File(datafile);
				FileOutputStream ofstream = new FileOutputStream(file);
				ObjectOutputStream ostream = new ObjectOutputStream(ofstream);
				synchronized (DiskTable) {
					//ostream.writeBoolean(hasFN);
					ostream.writeObject(DiskTable);
				}
				ostream.close();
				ofstream.close();
			}
		} catch (IOException e) {
		}

	}
	
	/** Retrieves the state of the launcher's last invocation.
	 * This retrieves only the 
	 * <code>DiskTable</code> from disk.  This is the only information
	 * the launcher contains that is persistent.
	 */
	void retrieve()
	{
		try {
			synchronized (datafile) {
				File file = new File(datafile);
				FileInputStream ifstream = new FileInputStream(file);
				ObjectInputStream istream = new ObjectInputStream(ifstream);
				synchronized (DiskTable) {
		//			hasFN = istream.readBoolean();
					DiskTable = (Vector)istream.readObject();
				}
				istream.close();
				ifstream.close();
			}
		} catch (FileNotFoundException e) {
			System.out.println("File " + datafile + " not found: assuming first-ever TOS run.");
		} catch (Exception e) {
			System.out.println("WARNING: Error reading disk configuration information from " + datafile + " " + e.toString());
			
		}
	}
	
	/** Terminates all the launcher's child processes before the launcher terminates.
	 * @exception Throwable if an exception or error is generated by <code>Server.finalize()</code>.
	 */
	public void finalize() throws Throwable
	{
		super.finalize();
	}
					
}

