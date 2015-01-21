//TOSTableModel.java
package tos.system;

import javax.swing.table.AbstractTableModel;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/** TOSTableModel is an abstract class whose subclasses provide an 
 * easy-to-use encapsulation of facilities of the JFC <i>AbstractTableModel</i>
 * class.
 * 
 * See the JFC/Swing documentation on how the JTable class interacts
 * with AbstractTableModel-derived classes to display information in
 * tabular form.
 * 
 */

abstract class TOSTableModel extends AbstractTableModel
{
	/** Headers for each column. */
	String columnNames[];
	
	/** Two-dimensional array of data. 	*/
	String data[][];
	
	/** Vector, each entry in which corresponds to a table row */
	Vector datavec;	
	
	/** Launcher from which data is obtained */
	LauncherAdmin mTL;

	/** Default constructor. 
	 *  The constructor only calls the superclass.
	 */
	public TOSTableModel()
	{
		super();
	}
	
	/** Connects table model to launcher
	 * @param TL Remote stub of launcher
	 */
	void setLauncher(LauncherAdmin TL)
	{
		mTL = TL;
	}
	
	/** Update the information displayed in the table. */
	void Refresh()
	{
		try {
			getTable();
		} catch (RemoteException e) {
			Debug.DisplayException("Refresh err",e);
		}
	}
	
	/** Subclasses implement this method to display data items.	 */
	abstract void getTable() throws RemoteException;
	
	/** Called to display a column header.
	 * @param Zero-based column index.
	 * @returns the title of the column.
	 */
	public String getColumnName(int column)
	{
		return columnNames[column];
	}
	
	/** Returns the number of rows in the table.  
	 * This function is required by JFC.
	 * @return the number of rows.
	 */
	public int getRowCount()
	{
		return datavec.size();
	}
			
	/** Returns the number of columns in the table.
	 * This function is required by JFC.
	 * @return the number of columns.
	 */
	public int getColumnCount()
	{
		return columnNames.length;
	}
	
	/** Returns the data to be displayed at the selected grid box.
	 * @param row Row number.
	 * @param column Column number
	 * @return data to be shown in the table.
	 */
	public Object getValueAt(int row, int column)
	{
		return null;
	}
}

/** This model is used for the Administrator's Host view.
 * 
 * Column 0 displays the host's name.
 * Column 1 displays the host's native operating system.
 * Column 2 displays the host's IP address.
 * 
 * The source of the data is the <code>HostTable</code> in the launcher.
 */
class HostTableModel extends TOSTableModel
{
	/** Constructor.
	 * The constructor sets the column headers.
	 */
	public HostTableModel()
	{
		super();
		columnNames = new String[3];
		columnNames[0] = "Name";
		columnNames[1] = "Operating System";
		columnNames[2] = "IP address";
	}
			
	/** Returns the data to be displayed at the selected grid box.
	 * @param row Row number.
	 * @param column Column number
	 * @return data to be shown in the table.
	 */
	public Object getValueAt(int row, int column)
	{
		Host curhost =(Host)datavec.elementAt(row);
		if (column==0)
			return curhost.hostname;
		else if (column==1)
			return curhost.osname;
		else if (column==2)
			return curhost.address.getHostAddress();
		else
			return null;
	}
	
	/** Gets the table from the launcher. */
	void getTable() throws RemoteException
	{
		datavec = mTL.getTable("Host");
	}

}

/** This model is used for the Administrator's Launcher view.
 * 
 * Column 0 displays the name of the launcher's host.
 * Column 1 displays the port the launcher is listening on.
 * 
 * The source of the data is the <code>LauncherTable</code> in the launcher.
 */
class LauncherTableModel extends TOSTableModel
{
	public LauncherTableModel()
	{
		super();
		columnNames = new String[2];
		columnNames[0] = "Host";
		columnNames[1] = "Port";
	}
	
	/** Returns the data to be displayed at the selected grid box.
	 * @param row Row number.
	 * @param column Column number
	 * @return data to be shown in the table.
	 */
	public Object getValueAt(int row, int column)
	{
		ServerRecord srec = (ServerRecord)datavec.elementAt(row);
		if (column==0)
			return srec.hostname;
		else if (column==1)
			return String.valueOf(srec.portnum);
		else
			return null;
	}
	
	/* Obtains the <code>LauncherTable</code> from the launcher. */
	void getTable() throws RemoteException
	{
		datavec = mTL.getTable("Launcher");
	}
}

/** This service class is used to display data in the 
 * ServerTableModel.  It contains a remote stub to 
 * a server and its location.
 */
class STableRecord {
	TOSServer stub; 
	String location;
	STableRecord(TOSServer stub)
	{
		this.stub = stub;
		try {
			this.location = stub.getLocation();
		} catch (RemoteException e) {
			Debug.ErrorMessage("Server not found","One of the expected servers could not be found.");
			this.location = "Unknown";
		}
	}
}

/** This model is used for the Administrator's Server view.
 * 
 * Column 0 displays the server's type.
 * Column 1 displays the server's location.
 * 
 * All the filename servers are listed first, followed
 * by the pipe servers, then the sync servers.
 * 
 * The source of the data is the <code>serverTable</code> in
 * each of the launcher's default filename server, sync server,
 * and pipe server.
 */
class ServerTableModel extends TOSTableModel
{
	/** Row position of the first filename server. */
	int firstfilename = 0;
	
	/** Row position of the first pipe server.  */
	int firstpipe;
	
	/** Row position of the first sync server.  */
	int firstsync;
	
	public ServerTableModel()
	{
		super();
		columnNames = new String[2];
		columnNames[0] = "Type";
		columnNames[1] = "Location";
	}
	
	/** Returns the data to be displayed at the selected grid box.
	 * @param row Row number.
	 * @param column Column number
	 * @return data to be shown in the table.
	 */
	public Object getValueAt(int row, int column)
	{
		if (column==0)
		{
			if (row<firstpipe)
				return TOSServer.FN;
			else if (row>=firstpipe && row<firstsync)
				return TOSServer.PIPE;
			else
				return TOSServer.SYNC;
		}
		else if (column==1)
		{
			STableRecord rec = (STableRecord)datavec.elementAt(row);
			return rec.location;
		}
		else
			return null;
		
	}
	
	/** Gets the tables from the servers.
	 * Obtains stubs to each of the three servers, 
	 * then obtains from each their list and concatenates
	 * all three into one large vector, noting the 
	 * position of each of the three types. 
	 */
	void getTable()
	{
		try {
			int i;
			Vector table;
			datavec = new Vector();
			TOSFileNameServer fnserver = mTL.getFileNameServer();
			table = fnserver.getServerTable();
			for (i=0; i<table.size(); i++)
			{
				TOSFileNameServer temp = (TOSFileNameServer)table.elementAt(i);
				datavec.addElement(new STableRecord(temp));
			}
			firstpipe = table.size();
			TOSPipeServer pserver = mTL.getPipeServer();
			table = pserver.getServerTable();
			for (i=0; i<table.size(); i++)
			{
				TOSPipeServer temp = (TOSPipeServer)table.elementAt(i);
				datavec.addElement(new STableRecord(temp));
			}
			firstsync = firstpipe + table.size();
			TOSSyncServer sserver = mTL.getSyncServer();
			table = sserver.getServerTable();
			for (i=0; i<table.size(); i++)
			{
				TOSSyncServer temp = (TOSSyncServer)table.elementAt(i);
				datavec.addElement(new STableRecord(temp));
			}				
		} catch (RemoteException e) {
			Debug.ErrorMessage("Connection error","Error connecting to server.");
		}
	}
}

/** This model is used for the Administrator's Process view.
 * 
 * Column 0 displays the process' TOS identifier.
 * Column 1 displays the process' class name.
 * Column 2 displays the process' host name.
 * 
 * The source of the data is the <code>ProcessTable</code> in the launcher.
 */
class ProcessTableModel extends TOSTableModel
{

	public ProcessTableModel()
	{
		super();
		columnNames = new String[3];
		columnNames[0] = "Id";
		columnNames[1] = "Name";
		columnNames[2] = "Host";
	}

	/** Returns the data to be displayed at the selected grid box.
	 * @param row Row number.
	 * @param column Column number
	 * @return data to be shown in the table.
	 */
	public Object getValueAt(int row, int column)
	{
		ProcessRecord rec = (ProcessRecord)datavec.elementAt(row);
		if (column==0)
			return new Integer(rec.id);
		else if (column==1)
			return rec.classname;
		else if (column==2)
			return rec.hostname;
		else
			return null;
		
	}
	
	/** Gets the process table from the launcher. */
	void getTable() throws RemoteException
	{
		datavec = mTL.getTable("Process");
	}
}

/** This service class is used to display data in the 
 * DiskTableModel.  It contains a DiskRecord and its
 * mountpoint drawn from the filename server.
 */
class DTableRecord
{
	DiskRecord record;
	String mountpoint;
	
	/** Creates a new <code>DTableRecord</code> from a <code>DiskRecord</code>.
	 */
	DTableRecord(DiskRecord record)
	{
		this.record = record;
		this.mountpoint = "";
	}
}

/** This model is used for the Administrator's Disk view.
 * 
 * Column 0 displays the disk's name.
 * Column 1 displays the disk's host name.
 * Column 2 displays the disk's mount table.
 * Column 3 displays the disk's maximum number of files.
 * Column 4 displays the disk's data block size
 * Column 5 displays the disk's number of data blocks.
 * 
 * The source of the data is the <code>DiskTable</code> in the launcher
 * and the <code>MountTable</code> in the launcher's default 
 * filename server.
 */
class DiskTableModel extends TOSTableModel
{
	public DiskTableModel()
	{
		super();
		columnNames = new String[7];
		columnNames[0] = "Name";
		columnNames[1] = "Host";
		columnNames[2] = "Mount point";
		columnNames[3] = "Files";
		columnNames[4] = "Block size";
		columnNames[5] = "Blocks";
		columnNames[6] = "Running";
	}
	
	/** Returns the data to be displayed at the selected grid box.
	 * @param row Row number.
	 * @param column Column number
	 * @return data to be shown in the table.
	 */
	public Object getValueAt(int row, int column)
	{
		DTableRecord fsrec = (DTableRecord)datavec.elementAt(row);
		if (column==0)
			if (fsrec.record.servername.equals(""))
				return "Unregistered";
			else
				return fsrec.record.servername;
		else if (column==1)
			return fsrec.record.hostname;
		else if (column==2)
			if (fsrec.mountpoint.equals(""))
				return "Unmounted";
			else
				return fsrec.mountpoint;
		else if (column==3)
			return new Integer(fsrec.record.numfiles);
		else if (column==4)
			return new Integer(fsrec.record.blocksize);
		else if (column==5)
			return new Integer(fsrec.record.numblocks);
		else if (column==6)
			return (fsrec.record.isRunning) ? "Yes" : "No";
		else
			return null;
	}
	
	/** Gets the data from the launcher and filename server.
	 * First the vector <code>datavec</code> is filled with copies
	 * of the <code>DiskRecord</code> objects obtained from the 
	 * disk table in the launcher.  The default filename server's
	 * mount table is used to obtain the mount points.
	 *
	 *<b>Note:</b> This only displays mount points for disks
	 * mounted at this launcher's default filename server.
	 */
	void getTable() throws RemoteException
	{
		datavec = new Vector();
		Vector tempvec = mTL.getTable("Disk");
		for (int j=0; j<tempvec.size(); j++)
			datavec.addElement(new DTableRecord((DiskRecord)tempvec.elementAt(j)));

		TOSFileNameServer server = mTL.getFileNameServer();
		Hashtable mtable = server.getMountTable();
		Enumeration enumeration = mtable.keys();
		while (enumeration.hasMoreElements())
		{
			String mountpt = (String)enumeration.nextElement();
			String sname = (String)mtable.get(mountpt);
			for (int i=0; i<datavec.size(); i++)
			{
				DTableRecord rec = (DTableRecord)datavec.elementAt(i);
				if (rec.record.servername.equals(sname))
					rec.mountpoint = mountpt;
			}
		}
	}
	
	/** Obtains a list of disks from the list of disk records in memory
	 * @return list of disks.
	 */
	Vector getServerList()
	{
		int i;
		Vector serverlist = new Vector();
		for (i=0; i<datavec.size(); i++)
		{
			DiskRecord fsrec = ((DTableRecord)datavec.elementAt(i)).record;
			serverlist.addElement(fsrec.servername);
		}
		return serverlist;
	}
	
	/** Obtains a list of those disks actually running from the disk record list in memory.
	 * @return list of disks.
	 */
	Vector getFilteredServerList(boolean isRunning)
	{
		int i;
		Vector serverlist = new Vector();
		for (i=0; i<datavec.size(); i++)
		{
			DiskRecord fsrec = ((DTableRecord)datavec.elementAt(i)).record;
			if (fsrec.isRunning == isRunning)
				serverlist.addElement(fsrec.servername);
		}
		return serverlist;
	}
}

/** This model is used for the Administrator's Sync Object view.
 * 
 * Column 0 displays the object's location.
 * Column 1 displays the object's type.
 * Column 2 displays the object's name.
 * Column 3 displays the identifier of the process who has the first thread waiting on the object.
 * Column 4 displays the name of the first thread waiting on the object.
 * 
 * The source of the data is the <code>objectTable</code>
 * in the launcher's default sync server.
 */
class SyncTableModel extends TOSTableModel 
{
	
	public SyncTableModel()
	{
		super();
		columnNames = new String[5];
		columnNames[0] = "Location";
		columnNames[1] = "Type";
		columnNames[2] = "Name";
		columnNames[3] = "First Process";
		columnNames[4] = "First Thread";
	}
	
	/** Returns the data to be displayed at the selected grid box.
	 * @param row Row number.
	 * @param column Column number
	 * @return data to be shown in the table.
	 */
	public Object getValueAt(int row, int column)
	{
		SyncRecord rec = (SyncRecord)datavec.elementAt(row);
		if (column==0)
			return String.valueOf(rec.location);
		else if (column==1)
		{
			if (rec.type==TOSSyncServer.MUTEX)
				return "Mutex";
			else if (rec.type==TOSSyncServer.SEMAPHORE)
				return "Semaphore";
			else
				return "Signal";
		}
		else if (column==2)
			return rec.name;
		else if (column==3)
			return rec.firstproc;
		else if (column==4)
			return rec.firstthread;
		else
			return null;
	}
	
	/** Obtains the data from the sync server.
	 * The sync server's object table is converted
	 * into a vector of its keys, which are of type
	 * <code>SyncRecord</code>.
	 */
	void getTable() throws RemoteException
	{
		TOSSyncServer sync = mTL.getSyncServer();
		datavec = new Vector();
		Hashtable qtable = sync.getObjectTable();
		Enumeration enumeration = qtable.keys();
		while (enumeration.hasMoreElements())
		{
			SyncRecord rec = (SyncRecord)enumeration.nextElement();
			datavec.addElement(rec);
		}
	}
}

/** This model is used for the Administrator's Pipe view.
 * 
 * Column 0 displays the pipe's location.
 * Column 1 displays the pipe's name.
 * 
 * The source of the data is the <code>objectTable</code> 
 * in the launcher's default pipe server.
 */
class PipeTableModel extends TOSTableModel
{
	public PipeTableModel()
	{
		columnNames = new String[2];
		columnNames[0] = "Location";
		columnNames[1] = "Name";
	}
	
	/** Returns the data to be displayed at the selected grid box.
	 * @param row Row number.
	 * @param column Column number
	 * @return data to be shown in the table.
	 */
	public Object getValueAt(int row, int column)
	{	
		InternalPipe rec = (InternalPipe)datavec.elementAt(row);
		if (column==0)
			return rec.location;
		else if (column==1)
			return rec.name;
		else
			return null;
	}

	/** Obtains the data from the pipe server.
	 * The pipe server's object table is converted
	 * into a vector of its keys, which are of type
	 * <code>InternalPipe</code>.
	 */
	void getTable() throws RemoteException
	{
		TOSPipeServer pserver = mTL.getPipeServer();
		Hashtable ptable = pserver.getObjectTable();
		datavec = new Vector();
		Enumeration enumeration = ptable.keys();
		while (enumeration.hasMoreElements())
		{
			InternalPipe rec = (InternalPipe)enumeration.nextElement();
			datavec.addElement(rec);
		}

	}
}