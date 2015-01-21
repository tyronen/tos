//

//
// TOSFile
//
//
package tos.api;

import java.rmi.*;
import java.io.*;
import tos.system.*;

/** The class representing a TOS file.
 * <p>This class is a key component of the TOS file system.  
 * It contains functions that, although executed within the user application,
 * are nonetheless central to handling files.
 * <p>To use a file, an application must instantiation a 
 * <code>TOSFile</code> object, then call its <code>open</code> method.
 * Read and write primitives are provided, based on a sequential operation.
 * <p>Files may be opened in read, write, or append mode.  
 */

public class TOSFile
{
	/** Character used to delimit disk name from path name.	 */
	public static String servermark = ":";

	/** TOS file separator symbol. */
	public static String separator = "/";
	
	/** TOS current directory symbol. */
	public static String curdir = ".";
	
	/** TOS parent directory symbol.	 */	
	public static String parentdir = "..";
	
	/** Character delimiting entries in a directory file.	 */
	public static char dirdelim = '/';
	
	/** Location within the current block. */
	protected int curbyte = 0; 
	
	/** Contents of the current block. */
	protected byte[] curblock; 
	
	/** Size of blocks in this disk. */
	protected int blocksize; 
	
	/**  Number of the current data block. */
	protected int blocknum;
	
	/** Position within the index block.	 */
	protected int pos; 
	
	/** Current index block. */
	protected IndexBlock iblock;
	
	/** Inode of the file. */
	protected Inode inode;
	
	/** Remote stub of the TOS disk containing the file. */
	protected TOSDisk Disk;
	
	/** Inode number of the file. */
	protected int filenum; 
	
	/** Filename server to which object is connected.	 */
	protected TOSFileNameServer nameserver;
	
	/** Set to <code>true</code> if the file is open. */
	protected boolean isOpen = false;
	
	/** Mode under which the file is open. */
	protected String mode = "";

	/** Constructor.
	 * <p>The constructor merely obtains a stub to a filename server.
	 * @param launcher Launcher from which to obtain the stub.
	 * @exception RemoteException if an RMI error occurs.
	 */
	public TOSFile(TOSLauncher launcher) throws RemoteException
	{
		nameserver = launcher.getFileNameServer();
	}

	/** Creates a new directory.
	 * <p>A new, empty directory is created with the absolute name passed in.
	 * The directory is created by using the <code>open()</code> function to 
	 * create a file which is then marked as a directory and closed.  Any 
	 * exceptions thrown are from the <code>open</code> and <code>close</code>
	 * methods.
	 * @param name Absolute name of the new directory.
	 * @see open
	 * @see close
	 */
	public void mkdir(String name) throws NotFoundException, 
										  InvalidPasswordException, 
										  InvalidModeException, 
										  TOSFileException
	{
		open(name,"w","");
		inode.isDirectory = true;
		close();
	}
	
	/** Returns the path of the file name on the disk itself, and sets the
	 * size of the <code>curblock</code> array.
	 * <p>The functon obtains the file location and path from the filename
	 * server, then contacts the disk to determine the size of its data 
	 * blocks.  The disk prefix is stripped from the name returned, which 
	 * is solely the path on that disk, relative to the disk's root.
	 * @param name Pathname in the global namespace.
	 * @return Name of the file on the disk.
	 * @exception NotFoundException if an error occurred when contacting the 
	 *                              filename server or disk.
	 */
	String getLocalName(String name) throws NotFoundException
	{
		// Convert the name into the internal servername:pathname format
		String filename;
		try {
			filename = nameserver.resolveFileName(name);
		} catch (Exception e) {
			throw new NotFoundException();
		}
		int loc = filename.indexOf(servermark);
		String servername = filename.substring(0,loc);
		try {
			Disk = nameserver.getDisk(servername);
			blocksize = Disk.getBlockSize();		
			curblock = new byte[blocksize];

		} catch (Exception e) {
			throw new NotFoundException();
		}
		String pathname = filename.substring(loc+servermark.length()
											 +TOSFile.separator.length());
		return pathname;
	}

	/** Opens a file by name with the given mode and password.
	 * <p>This function first checks to ensure that the mode passed in 
	 * is legitimate. It then calls <code>getLocalName()</code> to translate
	 * the file's name.  This is followed by a call to 
	 * <code>locateFile()</code> to obtain the file's inode.  The file's 
	 * permissions are checked and exceptions are thrown if the correct 
	 * password is not supplied when needed.  The first  index and data 
	 * blocks are read into memory and the position variables are initialized.
	 * <p>
	 * @param name Name of the file, in global namespace.
	 * @param mode Mode of the file - must be "r", "w", or "a"
	 * @param password File's password, set to "" if there is no password.
	 * @exception NotFoundException if the file or the path of its parent 
	 *                              (when creating a new file) could not 
	 *                              be found.
	 * @exception InvalidModeException if an invalid mode was supplied.
	 * @exception InvalidPasswordException if the password was incorrect.
	 * @exception TOSFileException if another error occurred.
	 */
	public void open(String name, String mode, String password) throws NotFoundException, 
																	   InvalidPasswordException, 
																	   InvalidModeException, 
																	   TOSFileException
	{
		if (!mode.equals("r") && !mode.equals("w") && !mode.equals("a") 
			&& !mode.equals("md"))
			throw new InvalidModeException();
		
		// get basic info
		String pathname = getLocalName(name);
		this.mode = new String(mode);
		try {
			inode = locateFile(pathname);
		} catch (Exception e) {
			throw new NotFoundException();
		}
		this.mode = new String(mode);
		
		if (mode.equals("r"))
			if (inode.canRead = false && !inode.Password.equals(password))
				throw new InvalidPasswordException();
		else
			if (inode.canWrite = false && !inode.Password.equals(password))
				throw new InvalidPasswordException();

		filenum = inode.Number;
		if (mode.equals("w"))
		{
			pos = 0;
			curbyte = 0;
			inode.size = 0;
			// this size should be set by blocksize!
		}		
		
		if (mode.equals("a"))
		{
			goToSpot(inode.lastindexblock,inode.lastindexentry,
					 inode.lastdataentry+1);
		}
		else // w OR r
		{
			iblock = inode.iblock;
			blocknum = iblock.BlocksUsed[0];
		}

		if (mode.equals("r"))
		{
			curbyte = 0;
			pos = -1;
			try {
				readBlock();
			} catch (Exception e) {
				
				throw new TOSFileException();
			}
		}
		isOpen = true;

	}
	
	/** Returns the size of the file.
	 * @return The file's size.
	 */
	public int getSize()
	{
		return inode.size;
	}

	/** Returns the inode of the file passed in.
	 * <p>This function is the key component of the navigation of TOS disks.
	 * Its presence within this class means that this work can be carried out
	 * largely within user applications, greatly lightening the demands made
	 * on the disks.
	 * <p>If a file does not exist, a call to <code>createFile</code> is made
	 * to create it.
	 * <p>The function works by first obtaining the disk's root file, then 
	 * calling <code>locateInDir</code> on the first directory within the
	 * file's path name.  This sequence is repeated for each directory in the
	 * path until the file itself is found.
	 * <p>If the file is not found but its path does exist, the file is created.
	 * @param pathname Name of file.
	 * @return inode of the file, obtained from the disk.
	 * @exception NotFoundException if the file could not be found.
	 * @exception FilesFullException if a new file cannot be created because 
	 *                               the maximum number already exist.
	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs.
	 * @exception DiskFullException if a new file cannot be created because
	 *                              the disk is full.
	 * @exception TOSFileException if another error occurs.
	 */
	Inode locateFile(String pathname) throws NotFoundException, 
											 FilesFullException, 
											 RemoteException, 
											 IOException,
											 DiskFullException,
											 TOSFileException
	{
		int nextnum = 0;
		String dirname = "";
		int slash;
		String oldmode = mode;
		mode = "r"; // temporary while reading directories
		while (true)
		{
			try {
				inode = Disk.getNode(nextnum);
			} catch (Exception e) {
				throw (new NotFoundException());
			}
			if (dirname.equals(pathname))
				break;
			slash = pathname.indexOf('/');
			if (slash<0)
				dirname = pathname;
			else
				dirname = pathname.substring(0,slash);
			pathname = pathname.substring(slash+1);
			try {
				nextnum = locateInDir(dirname);
			} catch (EOFException e) {
				mode = oldmode;
				if (pathname.indexOf(TOSFile.separator)>=0)
					throw e; // subdirectory must be created with mkdir
				else
					return createFile(inode,pathname);
			}
		}// while (!dirname.equals(pathname));
		return inode;
	}

	/** Locates a file within a directory.
	 * <p>This method traverses the directory using the 
	 * <code>getNextDirEntry()</code> function until the file is either
	 * found or the end of file is reached.
	 * @param dirname Directory to search in.
	 * @return Inode number of the file
	 * @exception EOFException if the directory does not contain the file.
	 * @exception TOSFileException if there is an error in traversal.
	 */
	int locateInDir(String dirname) throws EOFException, TOSFileException
	{
		boolean isname = true;
		int nextnum;
		String name = new String("");
		char inchar = 0;
		StringBuffer buffer = new StringBuffer(255);
	
		iblock = inode.iblock;
		blocknum = iblock.BlocksUsed[0];
		pos = -1;
		curbyte = 0;

		readBlock();
		do 
		{
			name = getNextDirEntry();
			String temp = getNextDirEntry();
			try {
				nextnum = (new Integer(temp)).intValue();
			} catch (NumberFormatException e) {
				throw new TOSFileException();
			}
		} while (!name.equals(dirname)); 
		return nextnum;
	}
	

	/** Returns the next entry in a directory file.
	 * <p>The algorithm used here is simple enough; keep reading 
	 * characters until the delimiter character is found, then repeat.  
	 * The first set of characters will be the name of a file, the second
	 * is the file's inode number.
	 * @return next entry.
	 * @exception EOFException if the end of the directory file is reached.
	 * @exception TOSFileException if an unexplained error occurs.
	 */
	String getNextDirEntry() throws EOFException, TOSFileException
	{
		StringBuffer buffer = new StringBuffer(0);
		char inchar = 0;
		while (true)
		{
			try {
				inchar = readChar();
			} catch (InvalidModeException e) {
				// doesn't happen
			}
			
			// The structure of a directory file is:
			// entry1/inode1/entry2/inode2....etc.
			if (inchar==dirdelim)
			{
				String temp = buffer.toString();
				return temp;
			}
			else
				buffer.append(inchar);
		} // while-true loop
	
	}
	
	/** Creates a new TOS file in the given location with the given name.
	 * <p>This function assumes it is already pointed at the directory where
	 * the new file is to be located; it places a new entry there for this file
	 * and then calls the TOS disk to create the file.
	 * @param parent Inode of directory where file is to be
	 * @return Inode of the new file.
	 * @exception NotFoundException if the file could not be found.
	 * @exception FilesFullException if a new file cannot be created because 
	 *                               the maximum number already exist.
	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs.
	 * @exception DiskFullException if a new file cannot be created because
	 *                              the disk is full.
	 * @exception TOSFileException if another error occurs.
	 */
	Inode createFile(Inode parent,String filename) throws NotFoundException, 
														  FilesFullException, 
														  RemoteException, 
														  TOSFileException, 
														  IOException, 
														  DiskFullException	
	{		
		if (!mode.equals("w") && !mode.equals("md"))
			throw new NotFoundException();
		// get next number
		int newnum = Disk.newInode();
		// write new directory entry - must go to end of directory for this
		String purename = filename.substring(filename.lastIndexOf(TOSFile.separator) 
											 + TOSFile.separator.length());
		char[] newname = purename.toCharArray();
		int i;
		try {
			for (i=0; i<newname.length; i++)
				writeChar(newname[i]);
			writeChar(dirdelim);
			char[] newnumstr = String.valueOf(newnum).toCharArray();
			for (i=0; i<newnumstr.length; i++)
				writeChar(newnumstr[i]);
			writeChar(dirdelim);
			close();
			// return new node
		} catch (InvalidModeException e) {
			// can't happen - screened for already
			return null;
		}
		return Disk.createFile(newnum,filename);
	}

	/** Marks a file as protected.
	 * @param password New password of the file.
	 */
	public void protectFile(String password)
	{
		inode.Password = password;
		inode.isProtected = true;
	}
	
	/** Sets or changes a file's permissions.
	 * @param password Password of the file.
	 * @param canRead Read permission.
	 * @param canWrite Write permission.
	 * @param canExecute Execute permission.
	 * @exception InvalidModeException if the file is not marked as protected.
	 * @exception InvalidPasswordException if the password is incorrect.
	 */
	public void chmod(String password,boolean canRead, boolean canWrite, 
					  boolean canExecute) throws InvalidModeException, 
												 InvalidPasswordException
	{
		if (!inode.isProtected)
			throw new InvalidModeException();
		if (!password.equals(inode.Password))
			throw new InvalidPasswordException();
		inode.canRead = canRead;
		inode.canWrite = canWrite;
		inode.canExecute = canExecute;
	}
	
	
	/** Copies a file.
	 * <p>The file is copied to the destination.  The current implementation
	 * is not buffered; a possible extension to the system is a buffered
	 * version of this function.
	 * @param dest File to copy to.
	 * @exception TOSFileNotOpenException if the file is not open.
	 * @exception InvalidModeException if the file is not opened for reading,
	 *                                 or the destination for writing.
	 * @exception IOException if an I/O error occurs.
	 * @exception DiskFullException if the disk is full.
	 * @exception TOSFileException if an error occurs.
	 */
	public void copyFile(TOSFile dest) throws TOSFileNotOpenException, 
											  InvalidModeException, 
											  IOException, 
											  TOSFileException, 
											  DiskFullException
	{
		if (!isOpen || !dest.isOpen)
			throw new TOSFileNotOpenException();
		byte mybyte;
		while (true)
		{
			try {
				mybyte = read();
			} catch (EOFException e) {
				break;
			}
			dest.write(mybyte);
		}
		
	}

	/** Move to a given location in a TOS file.
	 * @param niblocknum Index block to move to.
	 * @param npos Index of data block to move to.
	 * @param ncurbyte Position within the data block to move to.
	 * @exception TOSFileException if an error occurs.
	 */
	void goToSpot(int niblocknum, int npos, int ncurbyte) 
		throws TOSFileException
	{
		String oldmode = mode;
		pos = npos - 1;
		try {
			iblock = Disk.retrieveIndexBlock(niblocknum);
			mode = "r";
			readBlock();
			mode = oldmode;
		} catch (Exception e) {
			throw new TOSFileException();
		}
		curbyte = ncurbyte;
	}
	
	/** Deletes a file.
	 * <p>A fairly elaborate sequence of steps is needed to delete a file.
	 * The data and index blocks on the disk must be freed and the inode marked
	 * as unused.  Most of this code, however, is located on the disk.  This method
	 * calls the disk and afterward contents itself with removing the deleted file's 
	 * entry in the parent directory.
	 * @param filename File to delete.
	 * @param password Password.
	 * @exception TOSFileException if an unknown error occurs.
	 * @exception RemoteException if an RMI error occurs.
	 * @exception NotFoundException if the file cannot be found.
	 * @exception InvalidPasswordException if the password is invalid.
	 * @exception InvalidDirectoryException if the parent directory is 
	 *                                      corrupted.
	 */
	public void delete(String filename, String password) throws TOSFileException, RemoteException, NotFoundException, InvalidPasswordException, InvalidDirectoryException
	{
		try {
			open(filename,"w",password);
		} catch (InvalidModeException e) {
			// impossible when hard-coded
		}
	//	if (inode.isDirectory)
	//		throw new InvalidDirectoryException();
		while (true)
		{
			if (pos==0)
				Disk.freeSpace(iblock.blocknum);
			Disk.freeSpace(blocknum);
			try {
				readBlock();
			} catch (EOFException e) {
				break;
			}
		}
		Disk.removeFile(inode);	
		// remove directory entry
		int loc = filename.lastIndexOf(TOSFile.separator);
		String parent = filename.substring(0,loc);
		String child = filename.substring(loc+TOSFile.separator.length());
		try {
			open(parent,"r","");
		} catch (InvalidModeException e) {
			System.out.println("Middle: invalid mode exception");
		}
		String entry = "";
		String temp;
		int val;
		int wpos, wcurbyte, wiblocknum;
		// locate the entry to delete
		while (true)
		{
			wpos = pos;
			wcurbyte = curbyte;
			wiblocknum = iblock.blocknum;
			try {
				entry = getNextDirEntry();
				temp = getNextDirEntry();
			} catch (EOFException e) {
				break;
			}
			if (child.equals(entry))
				break;
		}
		// read in values to move forward
		StringBuffer buffer = new StringBuffer(0);
		char inchar = 0;
		while (true)
		{
			try {
				inchar = readChar();
			} catch (InvalidModeException e) {
				System.out.println("Readchar invalid mode");
			} catch (EOFException e) {
				break;
			}
			buffer.append(inchar);
		}
		close();
		String bufstring = buffer.toString();
		try {
			open(parent,"a","");
		} catch (InvalidModeException e) {

		}
		
		// return to the correct state
		goToSpot(wiblocknum,wpos,wcurbyte);
		
		try {
			writeString(bufstring);
		} catch (InvalidModeException e) {

		} catch (DiskFullException e) {
			 // we will ignore this 
			 // - can't happen when file size is being reduced
		}
		close();
	}
	
	/** Returns <code>true</code> if the file is a directory.
	 * @return <code>true</code> if the file is a directory.
	 */
	public boolean isDirectory()
	{
		return inode.isDirectory;
	}
	
	/** List the contents of a directory.
	 * <p>This function is all that is needed to list directory contents - 
	 * <code>open</code> need not be called.  It will list every entry in 
	 * the file, concantenated into a single string delimited with the newline
	 * character.
	 * @param dirname Directory to list.
	 * @return List of directory contents.
	 * @exception InvalidDirectoryException if this file is not a directory.
	 * @exception TOSFileException if an unknown file error occurs.
	 * @exception NotFoundException if the directory cannot be found.
	 */
	public String dir(String dirname) throws InvalidDirectoryException, 
											 TOSFileException, 
											 NotFoundException
	{
		String output = "";
		try {
			output = nameserver.getDiskName(dirname);
		} catch (RemoteException e) {
			// brush it off
		}
	
		try {
			open(dirname,"r","");
		} catch (InvalidPasswordException e) {
			// brush it off, doesn't happen
		} catch (InvalidModeException e) {
			  // also can't happen
		} catch (NotFoundException e) {//(NoDiskException e) {
			if (output.equals(""))
				throw new InvalidDirectoryException();
			else
				return output;
		}
		if (!inode.isDirectory)
			throw new InvalidDirectoryException();
		String numval;
		while (true)
		{
			try {
				output = output + getNextDirEntry() + "\n";
				numval = getNextDirEntry();
			} catch (EOFException e) {
				return output;
			} catch (Exception f) {
				return "Unable to list directory contents.";
			}
		}
	}
	
	/** Reads the specified number of bytes into a buffer.
	 * <p>Calls the <code>read()</code> method repeatedly until 
	 * the buffer is filled.
	 * @param buffer Byte array in which to place data.
	 * @param length Maximum number of bytes to read.
	 * @return Number of bytes actually read.
	 * @exception InvalidModeException if file is not open for reading.
	 * @exception TOSFileException if an unknown error occurs.
	 * @exception EOFException if the end of file is reached.
	 */
	public int read(byte[] buffer, int length) throws InvalidModeException, 
													  TOSFileException, 
													  EOFException
	{
		if (!mode.equals("r"))
			throw new InvalidModeException();
		int tot = 0;
		while (length>0)
		{
			buffer[tot++] = read();
			length--;
		}
		return tot-length-1; // the number of bytes actually read
	}
		
	/** Reads a single 2-byte character.
	 * @return Character read in.
	 * @exception InvalidModeException if file is not open for reading.
	 * @exception TOSFileException if an unknown error occurs.
	 * @exception EOFException if the end of file is reached.
	 */
	public char readChar() throws InvalidModeException, 
								  TOSFileException, 
								  EOFException
	{
		byte[] buffer = new byte[2];
		read(buffer,2);
		char outchar = (char)((char)(buffer[0]<<8) + (char)(buffer[1]));
		return outchar;
	}

	/** Reads in a line of text.	 
	 * <p>This function assumes that there is a newline character after 
	 * every line of text, including the last line.  If used to read in 
	 * an entire text file, the last line of text should have a trailing
	 * newline character.
	 * @return a string containing the next line of text.
	 * @exception InvalidModeException if file is not open for reading.
	 * @exception TOSFileException if an unknown error occurs.
	 * @exception EOFException if the end of file is reached.
	 */
	public String readln() throws InvalidModeException, 
								  EOFException, 
								  TOSFileException
	{
		// THIS ALGORITHM MEANS THAT TEXT FILES
		// MUST HAVE A TRAILING CARRIAGE RETURN
		if (!mode.equals("r"))
			throw new InvalidModeException();
		char[] buffer = new char[128];
		int pos = 0;
		char prevchar = 0; 
		char curchar = 0;
		while (true)
		{
			curchar = readChar();
			if (curchar==10 && prevchar==13) // EOL marker
				break;
			else
				buffer[pos++] = prevchar;
		}
		return new String(buffer);
	}
	
	/** Returns <code>true</code> if the file is at its end.
	 * @return <code>true</code> if the file is at its end.
	 */
	boolean isEOF()
	{
		return (iblock.blocknum==inode.lastindexblock &&
				pos==inode.lastindexentry &&
				curbyte==inode.lastdataentry+1) 
				||
			    (inode.lastdataentry==-1);
	}
	
	/** Reads in a single byte from the file.
	 * <p>Since the entire data block has already been loaded into memory,
	 * all this function has to do is return the current byte and increment 
	 * the pointer.  If the end of the data block is reached, the next one is
	 * loaded with a call to <code>readBlock</code>
	 * @exception InvalidModeException if file is not open for reading.
	 * @exception TOSFileException if an unknown error occurs.
	 * @exception EOFException if the end of file is reached.
	 */
	public byte read() throws InvalidModeException, 
							  TOSFileException,
							  EOFException	
	{
		if (!mode.equals("r"))
			throw new InvalidModeException();
		if (curbyte==blocksize)
		{
			readBlock();
			curbyte = 0;
		}
		if (isEOF())
		{
			throw new EOFException();
		}
		else
		{
			return curblock[curbyte++];
		}
	}

	/** Obtains the next index block from the disk if needed.
	 * <p>This method will only contact the disk if the current
	 * index block is really at its end; otherwise, it will do nothing.
	 * @exception TOSFileException if an I/O or RMI or other error occurs
	 *                             when contacting the TOS disk.
	 */
	void nextIndexBlock() throws TOSFileException 
	{
		if (iblock.isAtEnd(pos))
		{
			try {
				iblock = Disk.retrieveIndexBlock(iblock.BlocksUsed[pos]);
			} catch (Exception e) {
				throw new TOSFileException();
			}
			pos = 0;
		}
	}
	
	/** Obtains the next data block from the disk.
	 * Calls <code>nextIndexBlock</code> to obtain the next
	 * index block if needed.
	 * @exception EOFException if end of file is reached.
	 * @exception TOSFileException if there is an error contacting the disk.
	 */
	void readBlock() throws EOFException, TOSFileException
	{
		nextIndexBlock();
		blocknum = iblock.BlocksUsed[++pos]; 
		if (blocknum==IndexBlock.UNUSED)
		{
			throw new EOFException();
		}
		try {
			curblock = Disk.retrieveDataBlock(blocknum);
		} catch (Exception e) {
			throw new TOSFileException();
		}
	}

	/** Writes a buffer to a TOS file.
	 * <p>Calls the <code>write()</code> method repeatedly until 
	 * the buffer is filled.
	 * @param buffer Byte array containing data.
	 * @param length Number of bytes to write.
	 * @exception InvalidModeException if file is not open for writing.
	 * @exception TOSFileException if an error occurs communicating 
	 *                             with the disk.
	 * @exception DiskFullException if the end of file is reached.
	 */

	public void write(byte[] buffer, int length) throws InvalidModeException, 
														TOSFileException, 
														DiskFullException
	{
		if (!mode.equals("w") && !mode.equals("a"))
			throw new InvalidModeException();
		int tot = 0;
		while (length>0)
		{
			write(buffer[tot]);
			tot++;
		}
	}

	/** Writes a text string to a file.
	 * Each character in the file is stored in its 2-byte Unicode format.
	 * @param str String to write.
	 * @exception InvalidModeException if file is not open for writing.
	 * @exception TOSFileException if an error occurs communicating 
	 *                             with the disk.
	 * @exception DiskFullException if the end of file is reached.
	 */
	public void writeString(String str) throws InvalidModeException, 
											   TOSFileException, 
											   DiskFullException
	{
		char[] charr = str.toCharArray();
		int i;
		for (i=0; i<charr.length; i++)
			writeChar(charr[i]);
	}
	
	/** Writes a single character to a file, in Unicode format.
	 * @param nch Character to write.
 	 * @exception InvalidModeException if file is not open for writing.
	 * @exception TOSFileException if an error occurs communicating 
	 *                             with the disk.
	 * @exception DiskFullException if the end of file is reached.
	 */
	public void writeChar(char nch) throws InvalidModeException, 
										   TOSFileException, 
										   DiskFullException
	{
		byte byte1 = (byte)(nch>>8);
		byte byte2 = (byte)((nch<<8)>>8);
		write(byte1);
		write(byte2);
	}
	
	/** Writes a single byte to a file.
	 * @param nb Byte to write.
 	 * @exception InvalidModeException if file is not open for writing.
	 * @exception TOSFileException if an error occurs communicating 
	 *                             with the disk.
	 * @exception DiskFullException if the end of file is reached.
	 */
	public void write(byte nb) throws InvalidModeException, 
									  TOSFileException, 
									  DiskFullException
	{
		if (!mode.equals("w") && !mode.equals("a"))
			throw new InvalidModeException();
		if (curbyte==blocksize)
		{
			writeBlock();
			curbyte = 0;
		}
		try {
			curblock[curbyte++] = nb;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new TOSFileException();
		}
		inode.size++;
	}

	/** Writes a data block to disk.  If the current index block 
	 * is full, it is also written to disk and a new one obtained.
	 * @exception TOSFileException if an error occurs communicating 
	 *                             with the disk.
	 * @exception DiskFullException if the end of file is reached.
	 */
	void writeBlock() throws TOSFileException, DiskFullException
	{			
		try {
			Disk.commitDataBlock(blocknum,curblock);
			if (iblock.isAtEnd(pos))
			{
				// must allocate space for a new index block
				Disk.commitIndexBlock(iblock);
				iblock = Disk.newIndexBlock(filenum,iblock);
				pos = 0;
			}
			blocknum = iblock.BlocksUsed[++pos];
			curbyte = 0;
			if (blocknum==IndexBlock.UNUSED)
			{
				// must allocate space for new data block
				// Note that this is done one step BEFORE needed
				blocknum = Disk.newDataBlock(iblock);
			}

		} catch (Exception e) {
			throw new TOSFileException();
		}

	}

	/** Closes a file.
	 * <p>There is no special character marking the end of file,
	 * as is the case in some other operating systems.  Instead, the 
	 * inodes store the number of the last index block, last written entry
	 * within that index block, and last byte written within the last 
	 * data block.  These are written to disk when the file is closed.
	 * <p>A problem with this arrangement is if an application crashes while
	 * writing a file, there will be no end of file written, which could
	 * render the file unusable.
	 * @exception TOSFileException if an error occurs communicating 
	 *                             with the disk.
	 */
	public void close() throws TOSFileException
	{
		// write the EOF character
		if (mode.equals("w") || mode.equals("a"))
		{
			iblock.BlocksUsed[pos+1] = IndexBlock.UNUSED;
			inode.lastindexblock = iblock.blocknum;
			inode.lastindexentry = pos;
			inode.lastdataentry = curbyte - 1;
		
			try {
				Disk.commitDataBlock(blocknum,curblock);
				Disk.commitIndexBlock(iblock);
				Disk.updateNode(inode);
			} catch (Exception e) {
				throw new TOSFileException();
			}
		}
		isOpen = false;
	}
	
	/** Calls <code>close()</code> before an object is discarded.
	 * <p>Applications are expected to close their files explicitly 
	 * after they are finished using with them.  This function should
	 * not be relied upon to close files in all situations.
	 * @exception Throwable if an exception is thrown by the superclass.
	 */
	public void finalize() throws Throwable
	{
		try {
			if (isOpen)
				close();
		} catch (Throwable e) {
		}
		super.finalize();
	}

}

