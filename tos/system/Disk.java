//
//
// Disk
//
//
package tos.system;

import java.io.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import com.sun.java.swing.*;
import tos.api.*;

/** This class represents the TOS disk object.  It is 
 * started from launchers and will continue to run until
 * either it or its parent launcher is explicitly terminated.
 * <p>The disk contains the following sections:
 * <ul>
 * <li> Superblock - contains information about the disk.
 * <li> Free list - contains a list of every data block's status.
 * <li> Inode list - stores information about each file.
 * <li> Data blocks - hold file data and index information.
 * </ul>
 * <p>Each section is of a fixed size, which is set at the time
 * each disk is created.  Once set, these sizes cannot be changed.
 * <p>The services disks provide are based on blocks rather 
 * than files.  The disk will retrieve a data block to a caller and
 * write a block upon a caller's request.  The question of determining
 * which blocks are part of which files is the caller's responsibility.
 */

class Disk extends RemoteServer implements TOSDisk
{
	/** The free list blocks	 */
	protected FreeList freelist;
	
	/** The RandomAccessFile */
	protected FileStore file;
	
	/** The root node */
	protected Inode root;
	
	/** A memory copy of the superblock */
	protected Superblock superblock;

	/** The launcher on the same host. */
	protected TOSLauncher launcher;
	
	/** Name of the init file.	 */
	static String initname = "Disk";
	
	/** Maximum disks per launcher.	 */
	static int MAX_DISKS = 128;

	/** Maximum size of a mount string.	 */
	static int MOUNT_POINT_SIZE = 512;

	/** File name. */
	protected String servername;
	
	/** Full host path name of file. */
	protected String filename;
		
	/** Obtains the disk's argument list.
	 * <p>When the launcher starts new disks, it places their initialization
	 * data in a file of the name diskx.dat, where x is a number incremented
	 * each time a disk is launched.  As soon as the data is obtained from
	 * the .dat file, it is deleted.
	 * <p>The first two arguments represent the name of the disk and the 
	 * TCP/IP port number it will listen on.  New disks being created have 
	 * an additional three arguments representing the number of files, 
	 * the data block size, and the number of data blocks.
	 * @return Argument list.
	 */	
	static String[] getArgs()
	{
		String fsargs[];
		try {
			SecurityManager smgr = System.getSecurityManager();
			int initval = 0;
			File file;
			while (true)
			{
				file = new File(initname+initval+Server.Extension);
				try {
					if (file.canRead())
						break;
					else
					{
						initval++;
						// to stop infinite loops, we place a limit
						// of 128 disks per server
						if (initval>MAX_DISKS)
							System.exit(1);
					}
				} catch (SecurityException e) {
					initval++;
					// to stop infinite loops, we place a limit
					// of 128 disks per server
					if (initval>MAX_DISKS)
						System.exit(1);

				}
			}
			FileReader reader = new FileReader(file);
			int length = 80;
			int readlen;
			char[] line = new char[length];
			readlen = reader.read(line,0,length);
			reader.close();
			file.delete();
			StringTokenizer strtok = new StringTokenizer(new String(line,0,readlen));
			fsargs = new String[strtok.countTokens()];
			int count = 0;
			while (strtok.hasMoreTokens())
				fsargs[count++] = strtok.nextToken();
		} catch (Throwable e) {
			//Debug.ErrorMessage("File server unable to start","Error reading initalizaton file.");
			Debug.DisplayException("Server: getArgs",e);
			return null;
		}
		return fsargs;
	}
		
	
	/** The main function.
	 * <p>Calls <code>getArgs()</code> to obtain the initialization data
	 * and then calls the appropriate constructor.
	 * @param args Standard Java parameter, ignored in this class.
	 */
	public static void main(String args[]) 
	{
		try {
			Debug.setLookAndFeel();
			System.runFinalizersOnExit(true);
			String fsargs[] = getArgs();
			String filename = fsargs[0];
			int launchport = Integer.parseInt(fsargs[1]);
			Disk fs;
			System.setSecurityManager(new RMISecurityManager());
			if (fsargs.length==2) // Existing disk
			{
				fs = new Disk(filename,launchport);
			}
			else if (fsargs.length==5) // New disk
			{
				int numfiles = (new Integer(fsargs[2])).intValue();
				int blocksize = (new Integer(fsargs[3])).intValue();
				int numblocks = (new Integer(fsargs[4])).intValue();
				fs = new Disk(filename,launchport,
							  numfiles,blocksize,numblocks);
			}
			else
			{
				return;
			}
		} catch (Throwable e) {
			Debug.DisplayException("Disk",e);
		}
	}
	
	/** Extracts the name of a physical disk file from a complete 
	 * pathname on the host.
	 * @param Host file name, either absolute or relative.
	 * @return relative host file name.
	 */
	static String resolveServerName(String filename)
	{
		int pos = filename.lastIndexOf(File.separator);
		String sname;
		if (pos>=0)
			sname = filename.substring(pos+1);
		else
			sname = filename;
		return sname;
	}
	
	/** Constructor called on creation of a new disk.
	 * @param filename Disk name.
	 * @param launchport TCP/IP port to listen on.
	 * @param numfiles Maximum number of files in the new disk.
	 * @param blocksize Size of data blocks in the new disk.
	 * @param numblocks Number of data blocks in the new disk.
	 * @exception RemoteException if an RMI error occurs.
	 */
	public Disk(String filename, int launchport, int numfiles, 
				int blocksize, int numblocks) throws RemoteException
	{
		super();
		try {
			startup(filename,launchport);
			superblock = new Superblock(numfiles, blocksize, numblocks);
			file = new FileStore(filename,superblock);
			superblock.commit(file);
			freelist = new FreeList(superblock,file);
			freelist.initialize();
			freelist.commit();
			initializeInodes();
			root = createFile(0,"/");
		} catch (Exception e) {
			Debug.ErrorMessage("Disk error","Error on disk startup");
		}
	}
	
	/** Constructor called when an existing disk is restarted.
	 * @param filename Disk name.
	 * @param launchport TCP/IP port to listen on.
	 * @exception RemoteException if an RMI error occurs.	 
	 */
	public Disk(String filename, int launchport) throws RemoteException
	{
		super();
		try {
			startup(filename,launchport);
			file = new FileStore(filename);				
			superblock = new Superblock();
			superblock.retrieve(file);
			freelist = new FreeList(superblock,file);
			freelist.retrieve();
			TOSFileNameServer fn = launcher.getFileNameServer();
			fn.mount(servername,superblock.mountpt);
		} catch (Exception e) {
			Debug.ErrorMessage("File server creation error",
							   "Could not open file for server " 
							   + servername + ".  Server not created.");
			Debug.DisplayException("File server creation error",e);
		}
	}

	/** Performs RMI setup of the disk.
	 * <p>This method is called by both constructors.  It exports a remote
	 * stub, binds itself to the system registry, and registers itself with 
	 * the launcher.
	 * @param filename Disk name.
	 * @param launchport TCP/IP port to listen on.
	 * @exception Exception if an error occurs.
	 */
	void startup(String filename, int launchport) throws Exception
	{
		servername = resolveServerName(filename);
		setStreams("Disk_"+servername);		
		TOSDisk stub = (TOSDisk)UnicastRemoteObject.exportObject(this);
		Registry registry = LocateRegistry.getRegistry(launchport);
		registry.bind("FS"+servername,this);
		this.servername = servername;
		this.filename = filename;
		launcher = (TOSLauncher)registry.lookup("Launcher");
		launcher.registerDisk(filename,servername,stub);
	}
	
	/** Initializes the inode list with empty inodes.
	 * Called by the new-disk constructor.
	 * @exception IOException if an I/O error occurs.
	 */
	void initializeInodes() throws IOException
	{
		int i;
		Inode node;
		for (i=0; i<superblock.numfiles; i++)
		{
			node = new Inode(i);
			node.commit(file,superblock);
		}
	}

	/** Terminates the disk.
	 * The disk is unbound from the registry and the physical
	 * file is closed.
	 * @exception RemoteException if an RMI error occurs.
	 */
	public void terminate() throws RemoteException
	{
		try {
			Naming.unbind("FS" + servername);
			file.close();
		} catch (Exception e) {
			Debug.ErrorMessage("Error on disk termination",e.toString());
		}
		/* A possible extension here would be to close all open files. */
		System.exit(0);  
	}
	
	/** Allocates new space to the given inode.  
	 * If that inode's block is full, allocate a new index block.
	 * @param iblock Index block where new data block is to be placed.
	 * @return int Number of the new data block.
 	 * @exception RemoteException if an RMI error occurs.
 	 * @exception IOException if an I/O error occurs.
 	 * @exception DiskFullException if the disk is full.
	 */
	public int newDataBlock(IndexBlock iblock) throws RemoteException, 
													  IOException,
													  DiskFullException
	{
		int blocknum;
		try {
			blocknum = freelist.allocateSpace();
		} catch (EmptyStackException e) {
			throw new DiskFullException();
		}
		try {
			iblock.insert(blocknum);
		} catch (TOSFileException e) {
			Debug.ErrorMessage("newdatablock",e.toString());
			return -1;
		}
		initializeBlock(blocknum);
		return blocknum;
	}
	
	/** Clears the contents of a data block and writes it to disk.
	 * Called by the <code>newDataBlock</code> function.
	 * @param blocknum Number of block to initialize.
	 * @exception IOException if an I/O error occurs.
	 */
	void initializeBlock(int blocknum) throws IOException
	{
		byte[] tempblock = new byte[superblock.blocksize];
		int i;
		for (i=0; i<superblock.blocksize; i++)
			tempblock[i] = 0;
		try {
			commitDataBlock(blocknum,tempblock);
		} catch (RemoteException e) {
			// won't happen when called locally
		}
	}

	
	/** Returns a block to the free list.
	 * @param blocknum Block to free.
	 * @exception RemoteException if an RMI error occurs.
	 */
	public void freeSpace(int blocknum) throws RemoteException
	{
		try {
			freelist.freeSpace(blocknum);
		} catch (IOException e) {
			// brush it off for now....have to deal later
		}
	}

	/** Returns the root node.
	 * @return Inode object representing the root.
	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs.
	 */
	public Inode getRootNode() throws RemoteException, IOException
	{
		return getNode(0);
	}

	/** Creates a new file.  The first index block and data block are 
	 * allocated and assigned to the new inode.
	 * @param filenum Inode number of new file.
	 * @param filename Name of new file.
	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs.
	 * @exception DiskFullException if the disk is full.
	 */
	public Inode createFile(int filenum, String filename) 
		throws RemoteException, IOException, DiskFullException
	{
		int iblocknum;
		synchronized (freelist) {
			iblocknum = freelist.allocateSpace();
		}
		IndexBlock iblock = new IndexBlock(iblocknum,
										   superblock.iblockarraysize);
		try {
			newDataBlock(iblock);
		} catch (RemoteException e) {
			// this one won't happen when executed locally
		}
		Inode inode = new Inode(filenum, filename, iblock);
		inode.commit(file,superblock);
		commitIndexBlock(iblock);
		return inode;
	}
	
	/** Removes a file.
	 * This simply marks the inode as being unused.  This funciton does not
	 * actually free the file's blocks.
	 * @param inode Inode of the file to be removed.
	 * @exception RemoteException if an RMI error occurs.
	 */
	public void removeFile(Inode inode) throws RemoteException
	{
		inode.isUsed = false;
		try {
			inode.commit(file,superblock);
		} catch (IOException e) {
			// brush off
		}
	}
	
	/** Returns the given inode.
	 * @param num Number of inode to return.
	 * @return the inode.
	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs.
     */	
	public Inode getNode(int num) throws RemoteException, IOException
	{
		Inode retnode;
		retnode = new Inode(num,file,superblock);
		return retnode;
	}

	/** Update the on-disk copy of an inode.  The Modified field is set to
	 * the current date.
	 * @param inode Inode to update.
 	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs.
	 */
	public void updateNode(Inode inode) throws RemoteException, IOException
	{
		inode.Modified = new Date();
		inode.commit(file,superblock);	
	}

	/** Allocates a new inode number.
	 * @return the new number
 	 * @exception RemoteException if an RMI error occurs.
 	 * @exception FilesFullException if there are no more inodes available.
	 * @exception IOException if an I/O error occurs.
	 */
	public int newInode() 
		throws RemoteException, FilesFullException, IOException
	{
		// we do a vertical scan of the physical file, looking at each inode
		// in the file, until we find a one marked unused, allocate it
		synchronized (file) {
			file.seek(superblock.inodestart);
			int num;
			boolean isUsed;
			while (file.getFilePointer() < superblock.datastart)
			{
				num = file.readInt();
				isUsed = file.readBoolean();
				if (!isUsed)
					return num;
				file.skipBytes(superblock.inodesize - (4 + 1));
			}
			throw new FilesFullException();
		}
	}
	
	/** Creates a new index block.
	 * @param filenum Inode number of the file.
	 * @param parent Last index block in the file.
	 * @return the new index block.
	 * @exception IOException if an I/O error occurs.
	 * @exception DiskFullException if the disk is full.
	 */
	public IndexBlock newIndexBlock(int filenum, IndexBlock parent) 
		throws IOException, DiskFullException
	{
		int blocknum;
		try {
			synchronized (freelist) {
				blocknum = freelist.allocateSpace();
			} 
		} catch (EmptyStackException e) {
			throw new DiskFullException();
		}
		IndexBlock niblock = new IndexBlock(blocknum,
											superblock.iblockarraysize);
		parent.setChild(blocknum);
		return niblock; 
	}

	/** Retrieves an index block from the disk file.
	 * @param iblocknum Number of block to retrieve.
 	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs. 
	 */
	public IndexBlock retrieveIndexBlock(int iblocknum) 
		throws RemoteException, IOException
	{
		IndexBlock iblock = new IndexBlock(iblocknum,
										   superblock.iblockarraysize);
		synchronized (file) {
			file.seek(superblock.datastart+iblocknum*superblock.blocksize);
			iblock.read(file);
		}
		return iblock;
	}

	/** Writes an index block to disk.
	 * @param iblock Index block to write.
 	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs. 
	 */
	public void commitIndexBlock(IndexBlock iblock) 
		throws RemoteException, IOException
	{
		synchronized(file) {
			file.seek(superblock.datastart+
					  iblock.blocknum*superblock.blocksize);
			iblock.write(file);
		}
		return;
	}

	/** Retrieves a data block from disk.
	 * @param blocknum Number of block to retrieve.
 	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs. 
	 */
	public byte[] retrieveDataBlock(int blocknum) 
		throws RemoteException, IOException
	{
		byte[] block = new byte[superblock.blocksize];
		synchronized (file) {
			file.seek(superblock.datastart + blocknum * superblock.blocksize);
			try {
				file.read(block,0,superblock.blocksize);
			} catch (NullPointerException e) {
				Debug.ErrorMessage("retrieve",e.getMessage());
			}
		}
		return block;
	}

	/** Writes a data block to disk.
	 * @param blocknum Number of block to write.
	 * @param block Content of data block.
 	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs. 
	 */
	public void commitDataBlock(int blocknum, byte[] block) 
		throws RemoteException, IOException
	{
		synchronized (file)
		{
			file.seek(superblock.datastart+blocknum*superblock.blocksize);
			file.write(block);
		}

	}
	
	/** Writes a mount point to the superblock.
	 * @param mountpt New mount point.
	 * @exception RemoteException if an RMI problem occurs.	 
	 */
	public void mount(String mountpt) throws RemoteException, IOException
	{
		superblock.mount(mountpt);
		superblock.commit(file);
	}
	
	/** Returns the size of a data block in this disk.
	 * @return the size of a data block in this disk.
	 * @exception RemoteException if an RMI problem occurs.	 
	 */
	public int getBlockSize() throws RemoteException
	{
		return superblock.blocksize;
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
