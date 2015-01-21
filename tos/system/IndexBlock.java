//
//
// IndexBlock
//
//
package tos.system;

import java.io.*;
import tos.api.*;

/** This class represents data blocks that hold parts of a file's 
 * index of data blocks rather than the data itself.
 * <p>The inode of each TOS file contains the number of the first
 * index block.  Each index block contains an array containing, in order
 * the numbers of the data blocks being used by the TOS file.  The last 
 * entry in the array points to the location of the next index block
 * for this TOS file.  Thus, index blocks can form a linked list that 
 * can be traversed sequentially by a process using the TOS file.
 * <p>Index blocks, like data blocks, are of a size fixed by the 
 * Administrator when creating the TOS disk.  Thus, the number of data block
 * entries in an index block will be equal to the number of bytes in 
 * a block DIV 4, since there are four bytes in a Java integer.
 * <p>A console or user application will have an instance of this class
 * open for every open TOS file using the <a href="../system/TOSFile.html">TOSFile</a>
 * class.  It uses the member variables and functions here during its
 * traversal of the TOS file.
 * <p>The <a href="../system/TOSFile.html">TOSFile</a> is responsible
 * for ensuring that index blocks are allocated as needed.  
 */

public class IndexBlock implements Serializable
{
	/** Special constant used for unneeded entries in an index block.	 */
	public static char UNUSED = 0;

	/** Array where each value is the address of a data block. */
	public int BlocksUsed[]; 
	
	/** Current location of the caller in the array. */
	int curblock = 0;
	
	/** Block number of this index block.	 */
	public int blocknum;
	
	/** <code>True</code> if there is another index block following this one.	 */
	boolean UsingChild = false;
	
	/** Size of the <code>BlocksUsed</code array. */
	int size;
	
	/** Constructor.
	 * @param blocknum Block number of the index block.
	 * @param size Number of data blocks to be listed in a single index block.
	 */
	public IndexBlock(int blocknum,int size)
	{
		BlocksUsed = new int[size];
		this.size = size;
		this.blocknum = blocknum;
		int i;
		for (i=0; i<size; i++)
			BlocksUsed[i] = IndexBlock.UNUSED;
	}

	/** Sets the last entry in the array to point to the next index block.
	 * @param childloc Address of child index block.
	 */
	void setChild(int childloc)
	{
		BlocksUsed[size-1] = childloc;
		curblock = size-1;
		UsingChild = true;
	}

	/** Returns true if 
	 */
	public boolean isAtEnd(int pos)
	{
		return (pos==(size-1));
	}
		
	void insert(int dblocknum) throws TOSFileException
	{
		if (curblock<=size-2)
		{
			BlocksUsed[curblock] = dblocknum;
			curblock++;
		}
		else
		{
			UsingChild = true;
			throw (new TOSFileException("The index block is full."));
		}
	}
	
	void write(FileStore file) throws IOException
	{
		int i;
		synchronized (file) {
			for (i=0; i<size; i++)
				file.writeInt(BlocksUsed[i]);
		}
	}
	
	void read(FileStore file) throws IOException
	{
		int i;
		synchronized (file) {
			for (i=0; i<size; i++)
				BlocksUsed[i] = file.readInt();
		}
	}
}

