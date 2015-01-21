package tos.system;

import java.io.*;

/** This class provides a data structure to encapsulate basic
 * information about a disk process.
 */


class DiskRecord implements Serializable
{
	/** Complete pathname of disk's physical file. */
	String filename;
	
	/** Name of server, that is name of file itself. */
	String servername;
	
	/** Location of disk. */
	String hostname;

	/** Maximum number of files on the disk. */
	int numfiles;
	
	/** Size of data blocks. */
	int blocksize;
	
	/** Number of data blocks.  */
	int numblocks;
	
	/** Position of this record within a <code>DiskTable</code>.
	 * @see Launcher#DiskTable
	 */
	int pos;
	
	/** Set to <code>true</code> if the disk is running. */
	boolean isRunning;
	
	/** Remote stub of this disk.
	 * <p>This data member is <b>not</b> serializable,
	 * since it points to one running instance of a disk.  
	 * The next time the disk runs, it will have a different stub.
	 */
	TOSDisk stub;
	
	/** Creates a new disk record.
	 * @param filename Complete pathname of disk's physical file.
	 * @param hostname Location of disk.
	 * @param numfiles Maximum number of files on the disk.
	 * @param blocksize Size of data blocks.
	 * @param numblocks Number of data blocks.
	 */
	public DiskRecord(String filename, String hostname, int numfiles, int blocksize, int numblocks)
	{
		this.filename = filename;
		this.hostname = hostname;
		this.servername = "";
		this.numfiles = numfiles;
		this.blocksize = blocksize;
		this.numblocks = numblocks;
		this.isRunning = false;
	}
	
	
	/** Retrieves the object in from an input stream.
	 * <p> This function overrides the default for all 
	 * <code>Serializable</code> objects so the <code>stub</code>
	 * member is not serialized.
	 */
	private void readObject(ObjectInputStream in) throws IOException
	{
		 filename	= in.readUTF();
		 hostname	= in.readUTF();
		 servername = in.readUTF();
		 numfiles	= in.readInt();
		 blocksize	= in.readInt();
		 numblocks	= in.readInt();
		 pos		= in.readInt();
		 isRunning	= in.readBoolean();
	}

	/** Writes the object out to an output stream.
	 * <p> This function overrides the default for all 
	 * <code>Serializable</code> objects so the <code>stub</code>
	 * member is not serialized.
	 */
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.writeUTF(filename);
		out.writeUTF(hostname);
		out.writeUTF(servername);
		out.writeInt(numfiles);
		out.writeInt(blocksize);
		out.writeInt(numblocks);
		out.writeInt(pos);
		out.writeBoolean(isRunning);
	}
	
	
}
