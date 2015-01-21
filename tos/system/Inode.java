//
//
// Inode
//
//
package tos.system;

import java.io.*;
import java.util.*;
import tos.api.*;

/** This class provides functions for manipulating TOS inodes.
 * <p>When a TOS disk is created, the entire inode list is written to 
 * disk with zero values throughout.  The <code>isUsed</code> field is
 * is set to <code>false</code> throughout at the beginning.  
 * <p>When an inode is allocated to a file, its fields are filled in.  Some
 * of them are dependent on data found within the disk's superblock, such as 
 * the actual size of an index block.
 * <p>Individual inodes on disk are of a fixed size.  String fields within an
 * inode are padded with null characters to a fixed length to maintain this.  
 * Date objects are converted to <code>long</code> values before being
 * committed to the physical file.
 */


public class Inode implements Serializable
{
	/** Number of the inode.	 */
	public int Number;
	
	/** Whether there is a file using this inode. */
	boolean isUsed;
	
	/** Whether this file is a directory. */
	public boolean isDirectory = false;
	
	/** Whether this file is protected.	 */
	public boolean isProtected = false;
	
	/** Whether the file can be read without a password. */
	public boolean canRead = true;
	
	/** Whether the file can be written without a password. */
	public boolean canWrite = true;
	
	/** Whether the file can be executed without a password. */
	public boolean canExecute = true;
	
	/** Name of file. */
	public String Filename;
	
	/** Password of file.	 */
	public String Password;
	
	/** Timestamp of file's creation. */
	Date Created;
	
	/** Timestamp of file's last modification. */
	Date Modified;
	
	/** Size of file. */
	public int size;

	/** First index block for the file.	 */
	public int firstindexblock;
	
	/** Last index block for the file.	 */
	public int lastindexblock;
		
	/** Last entry in the last index block.	 */
	public int lastindexentry;
	
	/** Last byte used in the last data block. 	 */	
	public int lastdataentry;
	
	/** Index block currently being used. */
	public IndexBlock iblock;
	
	/** Maximum length of an inode string. */
	static int MAX_LENGTH = 255;
	
	/** Total size of the inode's names. */
	static int STRING_SIZE = 4 + 2*MAX_LENGTH;
	
	/** Size of a Java <code>Date</code> object. */
	static int DATE_SIZE = 8;
	
	/** Total size of an inode.	 */
	static int INODE_SIZE = 4 + 1 + 1 + 1 + 1 + 1 + 1 + STRING_SIZE + STRING_SIZE + DATE_SIZE + DATE_SIZE + 4 + 4 + 4 + 4 + 4;	

	/** Constructor used only at Disk startup to create blank inodes.
	 * @param num Number of inode.
	 * @see tos.system.Disk#initializeInodes
	 */
	public Inode(int num)
	{
		Number = num;
		isUsed = false;
		Filename = "";
		Password = "";
		Created = new Date();
		Modified = new Date();
		size = 0;
		firstindexblock = 0;
		lastindexentry = 0;
		lastindexblock = 0;
		lastdataentry = -1;
	}
		
	/** Constructor is used to read an inode from a file.
	 * @param num Number of inode.
	 * @param file Physical disk file.
	 * @param superblock Superblock of disk.
	 */
	public Inode(int num,FileStore file,Superblock superblock) throws IOException
	{
		Number = num;
		synchronized(file) {
			file.goToNode(Number);
			retrieve(file,superblock.iblockarraysize);
			iblock = new IndexBlock(firstindexblock,superblock.iblockarraysize);
			file.seek(superblock.datastart + superblock.blocksize * firstindexblock);
			iblock.read(file);
		}
	}
	
	/** Constructor used when a new file is created.
	 * @param num Number of inode.
	 * @param filename Name of file.
	 * @param niblock First index block for new file.
	 */
	public Inode(int num, String filename, IndexBlock niblock)
	{
		Number = num;
		if (num==0) // root must be a directory
			isDirectory = true;
		Filename = filename;
		Created = new Date();
		Modified = new Date();
		size = 0;
		Password = "";
		iblock = niblock;
		isUsed = true;
		lastindexentry = 0;
		firstindexblock = iblock.blocknum;
		lastindexblock = iblock.blocknum;
		lastdataentry = -1;

	}

	/** Determines if two inode objects are equal.
	 * <p>Two inodes are considered equal if their <code>Number</code> 
	 * fields are equal.
	 * @param other Other inode.
	 * @return <code>true</code> if they are equal, <code>false</code> otherwise.
	 */
	boolean equals(Inode other)
	{
		return (this.Number==other.Number);
	}
	
	/** Determines if an inode's number is equal to the given number.
	 * @param othernum Number to compare with.
	 * @return <code>true</code> if they are equal, <code>false</code> otherwise.
	 */
	boolean equals(int othernum)
	{
		return (this.Number==othernum);
	}

	/** Determines if an inode's filename is equal to the given string.
	 * @param othername String to compare with.
	 * @return <code>true</code> if they are equal, <code>false</code> otherwise.
	 */
	boolean equals(String othername)
	{
		return (this.Filename.equals(othername));
	}

	/** Adds another data block to the inode.
	 * <p>This function merely calls its counterpart in the 
	 * <code>IndexBlock</code> class.
	 * @param blocknum Number of block to add.
	 * @exception TOSFileException if an error occurs.
	 */
	public void insert(int blocknum) throws TOSFileException
	{
		iblock.insert(blocknum);
	}

	/** Reads a string from a physical disk file.
	 * @param file TOS disk file.
	 * @return string read in.
	 * @exception IOException if an I/O error occurs.
	 */
	String readString(FileStore file) throws IOException
	{
		synchronized (file) {
			int len = file.readInt();
			char[] strarray = new char[len];
			int i;
			for (i=0; i<len; i++)
				strarray[i] = file.readChar();
			String str = new String(strarray);
			file.skipBytes(2*(MAX_LENGTH-len));
			return str;
		}
	}
	
	/** Reads a date from a physical disk file.
	 * @param file TOS disk file.
	 * @return date read in.
	 * @exception IOException if an I/O error occurs.
	 */
	Date readDate(FileStore file) throws IOException
	{
		long intdate;
		synchronized (file) {
			intdate = file.readLong();
		}
		Date date = new Date(intdate);
		return date;
	}
	
	/** Retrieves an inode from the physical disk file.
	 * @param file TOS physical disk file.
	 * @param iblockarraysize Number of entries in an index block.
	 * @exception IOException if an I/O error occurs.
	 */
	void retrieve(FileStore file,int iblockarraysize) throws IOException
	{
		synchronized (file) {
			long start = file.getFilePointer();
			Number = file.readInt();
			isUsed = file.readBoolean();
			isDirectory = file.readBoolean();
			isProtected = file.readBoolean();
			canRead = file.readBoolean();
			canWrite = file.readBoolean();
			canExecute = file.readBoolean();
			Filename = readString(file);
			Password = readString(file);
			Created = readDate(file);
			Modified = readDate(file);
			size = file.readInt();
			firstindexblock = file.readInt();
			lastindexentry = file.readInt();
			lastindexblock = file.readInt();
			lastdataentry = file.readInt();
		}
	}

	/** Writes a string to a physical disk file.
	 * @param file TOS disk file.
	 * @param str String to write.
	 * @exception IOException if an I/O error occurs.
	 */
	void writeString(FileStore file, String str) throws IOException
	{
		int i;
		int len = str.length();
		file.writeInt(len);
		file.writeChars(str);
		for (i=len; i<MAX_LENGTH; i++)
			file.writeChar(0);
	}
	
	/** Writes a date to a physical disk file.
	 * @param file TOS disk file.
	 * @param date Date to write.
	 * @exception IOException if an I/O error occurs.
	 */
	void writeDate(FileStore file, Date date) throws IOException
	{
		file.writeLong(date.getTime());
	}
	
	
	/** Writes an inode to the physical disk file.
	 * @param file TOS disk file to write to.
	 * @param superblock Superblock of the disk.
	 * @exception IOException if an I/O error occurs.
	 */
	void commit(FileStore file,Superblock superblock) throws IOException
	{
		synchronized(file) {
			long oldpos = file.getFilePointer();
			file.goToNode(Number);
			long start = file.getFilePointer();
			file.writeInt(Number);
			file.writeBoolean(isUsed);
			file.writeBoolean(isDirectory);
			file.writeBoolean(isProtected);
			file.writeBoolean(canRead);
			file.writeBoolean(canWrite);
			file.writeBoolean(canExecute);
			writeString(file,Filename);
			writeString(file,Password);
			writeDate(file,Created);
			writeDate(file,Modified);
			file.writeInt(size);
			file.writeInt(firstindexblock);
			file.writeInt(lastindexentry);
			file.writeInt(lastindexblock);
			file.writeInt(lastdataentry);
			file.seek(oldpos);
		}
	}
	

}

