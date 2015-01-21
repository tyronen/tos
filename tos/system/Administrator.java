//Administrator.java
package tos.system;

import tos.api.NotFoundException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.registry.Registry;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/** This class implements the TOS Administrator, a GUI application that
 * provides elaborate facilities for system administrators to view system
 * information and add and remove system components.
 * <p>
 * The Administrator may connect to any launcher in a TOS system and still
 * obtain a complete, up-to-date, copy of system tables.  
 * <p>
 * You must connect to a launcher before being able to use any of the
 * Administrator's facilities.  You may exit at any time either from the 
 * Exit option of the File menu or by the host-dependent window close 
 * mechanism of your windowing system.
 * <p>
 * The main window contains a table which can display any of six different 
 * views: Host, Launcher, Process, Server, Pipe, and Sync Object.  They 
 * are all automatically updated when their corresponding menu item is
 * selected, or when Refresh is selected from the View menu.
 * <p>
 * In the interest of saving the author's weary fingers, the individual JFC
 * components of the Administrator do not have <i>javadoc</i> comments, but
 * their names should be self-explanatory.  Similarly, each function with the 
 * prefix <code>On</code> is called after the corresponding menu option is selected.
 */

class Administrator extends JFrame implements ActionListener
{
	/** Horizontal position of upper left corner. */
	static int x = 100;
	
	/** Vertical position of upper left corner. */
	static int y = 100;
	
	/** Set to <code>true</code> if connected to a launcher. */
	boolean isConnected = false;
	
	/** The launcher this administrator is connected to. */
	LauncherAdmin TL;
	
	JScrollPane scrollPane = new JScrollPane();
	JMenuBar menubar = new JMenuBar();
	
	JMenu connectionMenu = new JMenu("Connection");
	JMenuItem connect = new JMenuItem("Connect");
	JMenuItem disconnect = new JMenuItem("Disconnect");
	JMenuItem quit = new JMenuItem("Quit");

	JMenu viewMenu = new JMenu("View");
	JMenuItem host = new JMenuItem("Hosts");
	JMenuItem launcher = new JMenuItem("Launchers");
	JMenuItem server = new JMenuItem("Servers");
	JMenuItem process = new JMenuItem("Processes");
	JMenuItem disk = new JMenuItem("Disks");
	JMenuItem sync = new JMenuItem("Sync objects");
	JMenuItem pipe = new JMenuItem("Pipes");
	JMenuItem refresh = new JMenuItem("Refresh");
		
	JMenu commandMenu = new JMenu("Command");
	JMenuItem screate = new JMenuItem("Create Server");
	JMenuItem sremove = new JMenuItem("Remove Server");
	JMenuItem kill = new JMenuItem("Kill Process");
	JMenuItem release = new JMenuItem("Release Sync Object");
	JMenuItem oremove = new JMenuItem("Remove Object");
	
	JMenu diskMenu = new JMenu("Disk");
	JMenuItem dcreate = new JMenuItem("Create");
	JMenuItem mount = new JMenuItem("Mount");
	JMenuItem unmount = new JMenuItem("Unmount");
	JMenuItem dstart = new JMenuItem("Start");
	JMenuItem dstop = new JMenuItem("Stop");
	JMenuItem dremove = new JMenuItem("Remove");

	JTable HostView = new JTable();
	JTable LauncherView = new JTable();
	JTable ServerView = new JTable();
	JTable ProcessView = new JTable();
	JTable DiskView = new JTable();
	JTable SyncObjectView = new JTable();
	JTable PipeView = new JTable();

	TOSTableModel activeModel;
	HostTableModel hModel = new HostTableModel();
	LauncherTableModel lModel = new LauncherTableModel();
	ServerTableModel sModel = new ServerTableModel();
	ProcessTableModel pModel = new ProcessTableModel();
	DiskTableModel dModel = new DiskTableModel();
	SyncTableModel syModel = new SyncTableModel();
	PipeTableModel piModel = new PipeTableModel();
	
	/** The Java entry function.
	 * <p>Sets the output stream to a null stream
	 * so that the occasional <i>java.awt</i> error can
	 * be safely ignored.
	 */
	public static void main(String[] args)
	{
		Debug.setLookAndFeel();
		// done to get rid of those annoying exceptions
		System.setErr(new NullPrintStream());
		Administrator mainframe;
		try {
			mainframe = new Administrator();
		} catch (Throwable e) {
			Debug.DisplayException("main",e);
		}
	}

	/** Constructor.
	 * <p>Calls several initialization functions.
	 */
	public Administrator()
	{
		super("TOS Administrator");
		setMainMenuBar();
		setSize(600,400);
		setLocation(x,y);
		addWindowListener(new AdminListener());
		setModels();
		setVisible(true);
	}

	/** Points each of the <code>JTable</code> objects to the correct <code>AbstractTableModel</code> object. */
	void setModels()
	{
		HostView.setModel(hModel);
		LauncherView.setModel(lModel);
		ServerView.setModel(sModel);
		ProcessView.setModel(pModel);
		DiskView.setModel(dModel);
		SyncObjectView.setModel(syModel);
		PipeView.setModel(piModel);
	}

	/** Points each of the <code>AbstractTableModel</code> objects towards the launcher. */
	void setModelLaunchers(LauncherAdmin la)
	{
		hModel.setLauncher(la);
		pModel.setLauncher(la);
		dModel.setLauncher(la);
		lModel.setLauncher(la);
		sModel.setLauncher(la);
		syModel.setLauncher(la);
		piModel.setLauncher(la);
	}
	
	/** Adds a menu item to a menu.
	 * @param item Menu item to add.
	 * @param cmd Text of the item.
	 * @param parent Menu to add it to.
	 */
	void addMenuItem(JMenuItem item, String cmd, JMenu parent)
	{
		item.setActionCommand(cmd);
		item.addActionListener(this);
		parent.add(item);
	}
	
	/** Sets up the main menu bar and menus. */
	void setMainMenuBar()
	{

		addMenuItem(connect,"Connect",connectionMenu);
		addMenuItem(disconnect,"Disconnect",connectionMenu);
		addMenuItem(quit,"Quit",connectionMenu);
		menubar.add(connectionMenu);

		addMenuItem(refresh,"Refresh",viewMenu);
		viewMenu.addSeparator();
		addMenuItem(host,"Host",viewMenu);
		addMenuItem(launcher,"Launcher",viewMenu);
		addMenuItem(server,"Server",viewMenu);
		addMenuItem(process,"Process",viewMenu);
		addMenuItem(disk,"Disk",viewMenu);
		addMenuItem(sync,TOSServer.SYNC,viewMenu);
		addMenuItem(pipe,TOSServer.PIPE,viewMenu);
		
		addMenuItem(screate,"SCreate",commandMenu);
		addMenuItem(sremove,"SRemove",commandMenu);
		addMenuItem(kill,"Kill",commandMenu);
		addMenuItem(release,"Release",commandMenu);
		addMenuItem(oremove,"ORemove",commandMenu);

		addMenuItem(dcreate,"DCreate",diskMenu);
		addMenuItem(mount,"Mount",diskMenu);
		addMenuItem(unmount,"Unmount",diskMenu);
		addMenuItem(dstart,"DStart",diskMenu);
		addMenuItem(dstop,"DStop",diskMenu);
		addMenuItem(dremove,"DRemove",diskMenu);
				
		disconnect.setEnabled(false);
		getRootPane().setJMenuBar(menubar);		
		
	}

	/** Inner class for listening to window events. */
	protected class AdminListener extends WindowAdapter
	{
		/** Exits the program when the window is closed. */
		public void windowClosing(WindowEvent evt)
		{
			System.exit(0);
		}
	}

	/** Sends menu selection events to the correct function.
	 * @param e Action event.
	 */
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		if (cmd.equals("Connect"))
			OnConnect();
		else if (cmd.equals("Disconnect"))
			OnDisconnect();
		else if (cmd.equals("Quit"))
			OnQuit(); 
		else if (cmd.equals("Host"))
			OnHost();
		else if (cmd.equals("Launcher"))
			OnLauncher();
		else if (cmd.equals("Server"))
			OnServer();
		else if (cmd.equals("Process"))
			OnProcess();
		else if (cmd.equals("Disk"))
			OnDisk();
		else if (cmd.equals(TOSServer.SYNC))
			OnSync();
		else if (cmd.equals(TOSServer.PIPE))
			OnPipe();
		else if (cmd.equals("SCreate"))
			OnSCreate();
		else if (cmd.equals("SRemove"))
			OnSRemove();
		else if (cmd.equals("Kill"))
			OnKill();
		else if (cmd.equals("Release"))
			OnRelease();
		else if (cmd.equals("ORemove"))
			OnORemove();
		else if (cmd.equals("DCreate"))
			OnDCreate();
		else if (cmd.equals("Mount"))
			OnMount();
		else if (cmd.equals("Unmount"))
			OnUnmount();
		else if (cmd.equals("DStart"))
			OnDStart();
		else if (cmd.equals("DStop"))
			OnDStop();
		else if (cmd.equals("DRemove"))
			OnDRemove();
		if (isConnected)
			OnRefresh();
		getRootPane().repaint();
		setVisible(true);
	}
		
	/** Called from the Connection menu option.
	 * <p>
	 */
	void OnConnect()
	{
		String urlstr  = "";
		urlstr = JOptionPane.showInputDialog(null,"Enter the URL of the launcher to connect to.","Enter launcher name",JOptionPane.QUESTION_MESSAGE);
		try {
			if (urlstr.equals(""))
				return; // the user did not enter a URL
		} catch (NullPointerException e) {	
			return; // the user pressed 'Cancel' instead of OK
		}
		Registry registry;
		try {
			TL = (LauncherAdmin)Naming.lookup("rmi://"+urlstr+"/Launcher");
		} catch (Exception e) {
			Debug.ErrorMessage("Could not connect","Unable to connect to launcher.");
			return;
		}
		isConnected = true;
		connect.setEnabled(false);
		disconnect.setEnabled(true);
		setModelLaunchers(TL);
		addMenus();
		getContentPane().add(scrollPane);
		OnHost();
		Debug.InfoMessage("Connection successful","Successfully connected to launcher.");

	}

	/** Adds all the menus (except Connection) to the menu bar. */
	void addMenus()
	{
		menubar.add(viewMenu);
		menubar.add(diskMenu);
		menubar.add(commandMenu);		
		getRootPane().setJMenuBar(menubar);
	}

	/** Called from the Disconnect option of the Connection menu.
	 * <p>
	 */
	void OnDisconnect()
	{
		connect.setEnabled(true);
		disconnect.setEnabled(false);
		getContentPane().remove(scrollPane);
		menubar.remove(commandMenu);
		menubar.remove(diskMenu);
		menubar.remove(viewMenu);
	//	getRootPane().setJMenuBar(menubar);		
		isConnected = false;
	}

	/** Called from the Quit option of the Connection menu.
	 * <p>
	 */
	void OnQuit()
	{
		isConnected = false;
		dispose();
		System.exit(0);
	}
	
	/** Called from the Refresh option of the View menu.
	 * <p>
	 */
	void OnRefresh()
	{
		if (isConnected)
			activeModel.Refresh();		

	}
		
	/** Called from the Host option of the View menu.
	 * <p>
	 */
	void OnHost()
	{
		scrollPane.setViewportView(HostView);
		activeModel = hModel;
	}
	
	/** Called from the Launchers option of the View menu.
	 * <p>
	 */
	void OnLauncher()
	{
		scrollPane.setViewportView(LauncherView);
		activeModel = lModel;
	}
	
	/** Called from the Servers option of the View menu.
	 * <p>
	 */
	void OnServer()
	{
		scrollPane.setViewportView(ServerView);
		activeModel = sModel;		
	}
	
	/** Called from the Process option of the View menu.
	 * <p>
	 */
	void OnProcess()
	{
		scrollPane.setViewportView(ProcessView);
		activeModel = pModel;
	}
	
	/** Called from the Disks option of the View menu.
	 * <p>
	 */
	void OnDisk()
	{
		scrollPane.setViewportView(DiskView);
		activeModel = dModel;
	}
	
	/** Called from the Sync Objects option of the View menu.
	 * <p>
	 */
	void OnSync()
	{
		scrollPane.setViewportView(SyncObjectView);
		activeModel = syModel;
	}
	
	/** Called from the Pipes option of the View menu.
	 * <p>
	 */
	void OnPipe()
	{
		scrollPane.setViewportView(PipeView);
		activeModel = piModel;
	}
		
	/** Returns a list of servers of type <code>type</code> not found in the launcher's tables.
	 * <p>
	 * Obtains from the launcher the table for the given server type.  All the 
	 * elements in the <code>locationlist</code> vector that are <b>not</b> 
	 * in the launcher's table are returned.  Called by <code>OnSCreate</code>
	 * @return list of servers not in the location list.
	 * @param type Type of server.
	 * @param locationlist List to search from.
	 * @see OnSCreate
	 */
	Vector listWithout(String type,Vector locationlist) throws RemoteException
	{
		Vector with = TL.getTable(type);
		Vector without = new Vector();
		
		/* prepare list of host-locs without servers */
		for (int i=0; i<locationlist.size(); i++)
		{
			String location = (String)locationlist.elementAt(i);
			if (!with.contains(location))
				without.addElement(location);
		}
		return without;

	}
	
	/** Returns a vector of locations of every launcher in the system.
	 * <p>Calls the launcher's <code>getObjectTable</code> function to 
	 * obtain the mapping of locations to launcher stubs, and extracts from 
	 * it a vector containing the locations only.  Called from <code>OnSCreate</code>.
	 * @return Vector containing all launcher locations.
	 * @see tos.system.Launcher#getObjectTable
	 * @see OnSCreate
	 */
	Vector getLocationList() throws RemoteException
	{
		Hashtable launcherlist = ((TOSServer)TL).getObjectTable();
		Enumeration enumeration = launcherlist.keys();
		Vector locationlist = new Vector();
		while (enumeration.hasMoreElements())
		{
			String location = (String)enumeration.nextElement();
			locationlist.addElement(location);
		}
		return locationlist;
	}	
	
	/** Called from the Create Server option of the Command menu.
	 * <p>
	 */
	void OnSCreate()
	{
		try {
			Vector typelist = new Vector();
			typelist.addElement(TOSServer.FN);
			typelist.addElement(TOSServer.PIPE);
			typelist.addElement(TOSServer.SYNC);
		
			Hashtable existing = new Hashtable();
			/* get list of host-locs that have launchers */
			Vector locationlist = getLocationList();
			existing.put(TOSServer.FN,listWithout(TOSServer.FN,locationlist));
			existing.put(TOSServer.PIPE,listWithout(TOSServer.PIPE,locationlist));
			existing.put(TOSServer.SYNC,listWithout(TOSServer.SYNC,locationlist));
		
			DoubleBoxDlg dlg = new DoubleBoxDlg(this,"Create Server","Create",typelist,existing,"Server Type: ","Location: ");
			dlg.setVisible(true);
			if (dlg.retval!=AdminDlg.OK)
				return;
			String type = dlg.topselection+"Server";
			String dest = dlg.midselection;
			TL.launchServer(type,dest);
		} catch (Exception e) {
			Debug.DisplayException("SCreate",e);
		}
	}
	
	/** Called from the Remove Server option of the Command menu.
	 * <p>
	 */
	void OnSRemove()
	{
		try {
			Vector typelist = new Vector();
			typelist.addElement("Launcher");
			typelist.addElement(TOSServer.FN);
			typelist.addElement(TOSServer.PIPE);
			typelist.addElement(TOSServer.SYNC);
		
			Hashtable existing = new Hashtable();
			existing.put("Launcher",getLocationList());
			existing.put(TOSServer.FN,TL.getTable(TOSServer.FN));
			existing.put(TOSServer.PIPE,TL.getTable(TOSServer.PIPE));
			existing.put(TOSServer.SYNC,TL.getTable(TOSServer.SYNC));
		
			DoubleBoxDlg dlg = new DoubleBoxDlg(this,"Remove Server","Remove",typelist,existing,"Server Type: ","Location: ");
			dlg.setVisible(true);
			if (dlg.retval!=AdminDlg.OK)
				return;
			String type = dlg.topselection;
			String dest = dlg.midselection;
			if (type.equals("Launcher"))
			{
				if (!dest.equals(TL.getLocation()))
					TL.terminateLauncher(dest);
				else
				{
					int retval = JOptionPane.showConfirmDialog(this,
						"This is the launcher to which this Administrator is connected.\n"+
						"Proceed with termination?","Confirm launcher termination",JOptionPane.YES_NO_OPTION);
					if (retval==JOptionPane.YES_OPTION)
					{
						try {
							TL.terminate();
						} catch (UnmarshalException e) {
							// this is normal when killing a remote process - ignore
						}
						OnDisconnect();
					}
				}
			}
			else
			{
				try {
					TL.terminateServer(type,dest);
				} catch (NotFoundException e) {
					Debug.ErrorMessage("Not found","The server could not be found.");
					Debug.DisplayException("SRemove",e);
				}
			}
		} catch (Exception e) {
			Debug.DisplayException("SRemove",e);
		}
	
	}
		
	/** Called from the Kill Process option of the Command menu.
	 * <p>
	 */
	void OnKill()
	{
		try {
			Vector ptable = TL.getTable("Process");
			Vector idlist = new Vector();
			String sep = " on host ";
			for	(int i=0; i<ptable.size(); i++)
			{
				ProcessRecord rec = (ProcessRecord)ptable.elementAt(i);
				idlist.addElement(String.valueOf(rec.id));
			}
			SingleBoxDlg dlg = new SingleBoxDlg(this,"Kill Process","Kill","Process: ",idlist);
			dlg.setVisible(true);
			if (dlg.retval!=AdminDlg.OK)
				return;
			int victim = Integer.parseInt(dlg.selection);
			TL.killProcess(victim);
		} catch (Exception e) {
			Debug.DisplayException("OnKill",e);
		}
		
	}
	
	/** Called from the Release Object option of the Command menu.
	 * <p>
	 */
	void OnRelease()
	{
		try {
			// obtain a list of all sync objects
			TOSSyncServer sserver = TL.getSyncServer();
			Hashtable synctable = sserver.getObjectTable();
		
			Vector typelist = getSyncNames();
			
			Hashtable objList = getSyncList(synctable);
		
			DoubleBoxDlg dlg = new DoubleBoxDlg(this,"Release Sync Object","Release",typelist,objList,"Type: ","Object: ");
			dlg.setVisible(true);
			if (dlg.retval!=AdminDlg.OK)
				return;
			String type = dlg.topselection;
			String name = dlg.midselection;
			SyncRecord rec = getSyncRecord(synctable,type,name);
			TOSSyncServer stub = (TOSSyncServer)synctable.get(rec);
			stub.Release(rec.name,rec.type);
		} catch (NotFoundException e) {
			Debug.ErrorMessage("Object not found","Unable to find object in servers.");
		} catch (RemoteException e) {
			Debug.DisplayException("Release",e);
			//Debug.ErrorMessage("Error","Error releasing object.");
		}
	}

	/** Returns a vector containing the three strings "Mutex", "Semaphore" and "Signal".
	 * @return vector of the three strings.
	 */
	Vector getSyncNames()
	{
		Vector typelist = new Vector();
		typelist.addElement("Mutex");
		typelist.addElement("Semaphore");
		typelist.addElement("Signal");
		return typelist;
	}		
	
	/** Returns a hashtable containing sync object types as keys and vectors containing lists of objects as values.
	 * <p>Taking as input a hashtable mapping objects to <code>TOSSyncServer</code> 
	 * stubs, extracts from them three vectors, one for each object type.
	 * These three vectors are made the values of a hashtable, with the respective
	 * type as the key.
	 * <p>Called from <code>OnORemove</code> and <code>OnORelease</code>.
	 * @return a hashtable with keys "Mutex", "Semaphore", and "Signal", and a vector of object names as values.
	 * @param synctable Hashtable having <code>SyncRecords</code> as keys.
	 * @see OnORelease
	 * @see OnORemove
	 */
	Hashtable getSyncList(Hashtable synctable)
	{
		Vector mutexList = new Vector();
		Vector semList = new Vector();
		Vector sigList = new Vector();
		Enumeration enumeration = synctable.keys();
		while (enumeration.hasMoreElements())
		{
			SyncRecord rec = (SyncRecord)enumeration.nextElement();
			if (rec.type==TOSSyncServer.MUTEX)
				mutexList.addElement(rec.name);
			else if (rec.type==TOSSyncServer.SEMAPHORE)
				semList.addElement(rec.name);
			else if (rec.type==TOSSyncServer.SIGNAL)
				sigList.addElement(rec.name);
		}
		Hashtable objList = new Hashtable();
		objList.put("Mutex",mutexList);
		objList.put("Semaphore",semList);
		objList.put("Signal",sigList);
		return objList;
	}
	
	/** Returns a list of pipes extracted from a pipe-stub hashtable.
	 * <p>Taking as input a hashtable mapping pipes to <code>TOSPipeServer</code> 
	 * stubs, extracts from them a vector of the <code>InternalPipe</code> objects.
	 * <p>Called from <code>OnORemove</code>.
	 * @return the vector of pipes.
	 * @param pipetable Hashtable having <code>InternalPipes</code> as keys.
	 * @see OnORemove
	 */
	Vector getPipeList(Hashtable pipetable)
	{
		Vector pipelist = new Vector();
		Enumeration enumeration = pipetable.keys();
		while (enumeration.hasMoreElements())
		{
			InternalPipe rec = (InternalPipe)enumeration.nextElement();
			pipelist.addElement(rec.name);
		}
		return pipelist;
	}
		
	
	/** Called from the Remove Object menu option of the Command menu.
	 * <p>
	 */
	void OnORemove()
	{
		try {
			TOSSyncServer sserver = TL.getSyncServer();
			TOSPipeServer pserver = TL.getPipeServer();
			Hashtable synctable = sserver.getObjectTable();
			Hashtable pipetable = pserver.getObjectTable();
		
			Enumeration enumeration = synctable.keys();
			
			Vector typelist = getSyncNames();
			typelist.addElement("Pipe");

			Hashtable objList = getSyncList(synctable);
			Vector pipelist = getPipeList(pipetable);
			objList.put("Pipe",pipelist);	
		
			DoubleBoxDlg dlg = new DoubleBoxDlg(this,"Remove Object","Remove",typelist,objList,"Type: ","Object: ");
			dlg.setVisible(true);
			if (dlg.retval!=AdminDlg.OK)
				return;
			if (dlg.topselection.equals("Pipe"))
			{
				InternalPipe rec = getInternalPipe(pipetable,dlg.midselection);
				TOSPipeServer stub = (TOSPipeServer)pipetable.get(rec);
				stub.destroy(rec);
			}
			else // a sync object
			{
				SyncRecord rec = getSyncRecord(synctable,dlg.topselection,dlg.midselection);
				TOSSyncServer stub = (TOSSyncServer)synctable.get(rec);
				stub.destroyObject(rec);
			}
		} catch (NotFoundException e) {
			Debug.ErrorMessage("Object not found","Unable to find object in servers.");
		} catch (RemoteException e) {
			Debug.DisplayException("ORemove",e);
			//Debug.ErrorMessage("Error","Error removing object.");
		}
	}
	
	/** Returns the <code>InternalPipe</code> object of the given name from the given hashtable.
	 * <p>Called from <code>OnORemove</code>.
	 * @return <code>InternalPipe</code> object.
	 * @param pipetable Hashtable with <code>InternalPipe</code> keys.
	 * @param name Name of pipe to extract.
	 * @see OnORemove
	 */
	InternalPipe getInternalPipe(Hashtable pipetable, String name) throws NotFoundException
	{
		Enumeration enumeration = pipetable.keys();
		while (enumeration.hasMoreElements())
		{
			InternalPipe rec = (InternalPipe)enumeration.nextElement();
			if (rec.name.equals(name))
			{
				return rec;
			}
		}
		throw new NotFoundException();
	}
	
	/** Returns the <code>SyncRecord</code> object of the given name and type from the given hashtable.
	 * <p>Called from <code>OnORemove</code> and <code>OnORelease</code>.
	 * @return <code>SyncRecord</code> object.
	 * @param synctable Hashtable with <code>SyncRecord</code> keys.
	 * @param type Type of sync object to extract.
	 * @param destname Name of object.
	 * @see OnORemove
	 * @see OnORelease
	 */
	SyncRecord getSyncRecord(Hashtable synctable, String type, String destname) throws NotFoundException
	{
		Enumeration enumeration = synctable.keys();
		int destid = SyncServer.strToNum(type);
		while (enumeration.hasMoreElements())
		{
			SyncRecord rec = (SyncRecord)enumeration.nextElement();
			if (destname.equals(rec.name) && destid==rec.type)	
			{
				return rec;
			}
		}
		throw new NotFoundException();
	}

	/** Called from the Create Disk menu option of the Disk menu.
	 * <p>
	 */
	void OnDCreate()
	{
		DiskLaunchDlg dlg = new DiskLaunchDlg(this);
		dlg.setVisible(true);
		if (!dlg.isOK)
			return;
		try {
			TL.createDisk(dlg.name,dlg.hostname,dlg.numfiles,dlg.blocksize,dlg.numblocks);
		} catch (Exception e) {
			String msg;
			if (e.getMessage().equals(""))
				msg = e.toString();
			else
				msg = e.getMessage();
			Debug.DisplayException("DCreate",e);
			e.printStackTrace();
		}
	}
	
	/** Called from the Mount menu option of the Disk menu.
	 * <p>
	 */
	void OnMount()
	{
		Vector serverlist = dModel.getServerList();
		MountDlg dlg = new MountDlg(this,serverlist);	
		dlg.setVisible(true);
		if (dlg.retval == MountDlg.MOUNT)
		{
			try {
				TL.getFileNameServer().mount(dlg.servername,dlg.newmountpt);
			} catch (Exception e) {
				Debug.ErrorMessage("Error communicating with launcher.","Error");
			}
		}
	}
	
	/** Called from the Unmount menu option of the Disk menu.
	 * <p>
	 */
	void OnUnmount()
	{
		Vector serverlist = dModel.getServerList();
		SingleBoxDlg dlg = new SingleBoxDlg(this,"Unmount Disk","Unmount","Disk: ",serverlist);
		dlg.setVisible(true);
		if (dlg.retval == AdminDlg.OK)
		{
			try {
				TL.getFileNameServer().unmount(dlg.selection);
			} catch (Exception e) {
				Debug.DisplayException("Unmount",e);
			}
		}
	}
	
	/** Called from the Start menu option of the Disk menu.
	 * <p>
	 */
	void OnDStart()
	{
		Vector serverlist = dModel.getFilteredServerList(false);
		SingleBoxDlg dlg = new SingleBoxDlg(this,"Start Disk","Start","Disks: ",serverlist);
		dlg.setVisible(true);
		if (dlg.retval==AdminDlg.OK)
		{
			try {
				TL.startDisk(dlg.selection);
			} catch (Exception e) {
				Debug.DisplayException("DStart",e);
			}
		}

	}
	
	/** Called from the Stop menu option of the Disk menu.
	 * <p>
	 */
	void OnDStop()
	{
		Vector serverlist = dModel.getFilteredServerList(true);
		SingleBoxDlg dlg = new SingleBoxDlg(this,"Stop Disk","Stop","Disks: ",serverlist);
		dlg.setVisible(true);
		if (dlg.retval==AdminDlg.OK)
		{
			try {
				TL.stopDisk(dlg.selection);
			} catch (Exception e) {
				Debug.DisplayException("DStop",e);
			}
		}

	}
	
	/** Called from the Remove menu option of the Disk menu.
	 */
	void OnDRemove()
	{
		Vector serverlist = dModel.getServerList();
		SingleBoxDlg dlg = new SingleBoxDlg(this,"Remove Disk","Remove","Disks: ",serverlist);
		dlg.setVisible(true);
		if (dlg.retval==AdminDlg.OK)
		{
			try {
				TL.removeDisk(dlg.selection);
			} catch (Exception e) {
				Debug.DisplayException("DRemove",e);
			}
		}
	}
	
	
}
