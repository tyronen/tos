//
//
// Superblock
//
//
package tos.system;

import java.io.*;

/** The superblock contains information about a disk as a whole.
 * <p>On disk, the superblock stores only four pieces of information:
 * <ul>
 * <li><code>numfiles</code> - The maximum number of files;
 * <li><code>blocksize</code> - The size of each data block;
 * <li><code>numblocks</code> - The number of data blocks;
 * <li><code>mountpt</code> - A character array storing the mountpoint, if any.
 * </ul>
 * In memory, auxiliary items are calculated from these.  These are:
 * <ul>
 * <li><code>iblockarraysize</code> - Number of data blocks represented by an index block.
 * <li><code>inodesize</code> - Size of an inode, in bytes.
 * <li><code>freelistsize</code> - Size of the free list, in bytes.
 * <li><code>freeliststart</code> - Location of the beginning of the free list.
 * <li><code>inodestart</code> - Location of the beginning of the inode list.
 * <li><code>datastart</code> - Location of the first data block.
 * <li><code>size</code> - Size of the physical file of the virtual disk.
 * </ul>
 */

class Superblock 
{
	/** Maximum number of files. */
	int numfiles;
	
	/** Size of each data block. */
	int	blocksize;

	/** Number of data blocks. */
	int numblocks;
		
	/** Mount point of the disk. */
	String mountpt = "";
	
	/** Number of data blocks represented by an index block.	 */
	int iblockarraysize;
	
	/** Size of an inode, in bytes. */
	int inodesize;
	
	/**	Size of the free list, in bytes. */
	int freelistsize;
	
	/**	Location of the beginning of the free list. */
	int freeliststart;
	
	/**	Location of the beginning of the inode list. */
	int inodestart;
	
	/**	Location of the first data block. */
	int datastart;
	
	/**	Size of the physical file of the virtual disk. */
	int size;

	/** Size of the superblock on disk, in bytes. */
	static int SUPERBLOCK_SIZE = 12 + 2*Disk.MOUNT_POINT_SIZE; 
	
	public Superblock(int numfiles, int blocksize, int numblocks)
	{
		this.numfiles = numfiles;
		this.blocksize = blocksize;
		this.numblocks = numblocks;
		calculate();
	}
	
	public Superblock()
	{
		return;
	}
	
	void calculate()
	{
		freelistsize = numblocks;
		iblockarraysize = blocksize/4;
		inodesize = Inode.INODE_SIZE;

		/* Structure of the disk file
			Superblock - 12 bytes
			Free list - 1 byte per data block
			Inode list - 1 inode per file
				each inode contains a description of each file
			Remaining index blocks are in the data area
			Data blocks - each of specified blocksize
		*/
		
		freeliststart = Superblock.SUPERBLOCK_SIZE;
		inodestart = freeliststart + freelistsize;
		datastart = inodestart + inodesize*numfiles;
		size = datastart + numblocks*blocksize;
	}
		
	void mount(String newpt)
	{
		mountpt = newpt;
	}
	
	void retrieve(FileStore file) throws IOException
	{
		synchronized (file) {
			long curpos = file.getFilePointer();
			file.seek(0);
			numfiles = file.readInt();
			blocksize = file.readInt();
			numblocks = file.readInt();
			StringBuffer buffer = new StringBuffer(Disk.MOUNT_POINT_SIZE);
			buffer.setLength(Disk.MOUNT_POINT_SIZE);
			for (int i=0; i<Disk.MOUNT_POINT_SIZE; i++)
				buffer.setCharAt(i,file.readChar());
			mountpt = buffer.toString();
			mountpt = mountpt.trim();
			file.seek(curpos);
		}
		calculate();

	}
	
	void commit(FileStore file) throws IOException
	{
		synchronized (file) {
			long curpos = file.getFilePointer();
			file.seek(0);
			file.writeInt(numfiles);
			file.writeInt(blocksize);
			file.writeInt(numblocks);
			StringBuffer buffer = new StringBuffer(mountpt);
			buffer.setLength(Disk.MOUNT_POINT_SIZE);
			for (int i=0; i<Disk.MOUNT_POINT_SIZE; i++)
				file.writeChar(buffer.charAt(i));
			file.seek(curpos);
		}
	}

		
}

