//
//
// FreeList
//
//
package tos.system;

import java.util.*;
import java.io.*;

/** This class encapsulates the list of free data blocks, maintained
 * both on disk and in memory.
 * <p>The on-disk version consists of a fixed-size array of bytes, 
 * each byte set to 1 if it is free and 0 if it is used.  When a 
 * virtual disk is started, all locations in the array with values 
 * set to 0 are placed in a <code>java.util.Stack</code> object.  
 * Allocations and deallocations of space are made from this stack.
 * <p>For maximum reliability, every change to the free
 * list from an allocation or deallocation is written to disk 
 * at once.
 */

class FreeList
{
	/** Used blocks.	 */
	static byte USED = 0;
	
	/** Free blocks. */
	static byte FREE = 1;
	
	/** Stack containing free blocks. */
	protected Stack stack = new Stack();
	
	/** Number of available data blocks.	 */
	int numblocks; 
	
	/** Location of the free list in the disk file.	 */
	int freeliststart;
	
	/** Disk file.	 */
	FileStore file;

	/** Constructor.
	 * @param superblock Superblock of the disk.
	 * @param file Physical file of the disk.
	 */
	public FreeList(Superblock superblock, FileStore file)
	{
		numblocks = superblock.numblocks;
		freeliststart = superblock.freeliststart;
		this.file = file;
	}

	/** Returns <code>true</code> if there are no more free blocks.
	 * @return <code>true</code> if there are no more free blocks.
	 */
	boolean empty()
	{
		return stack.empty();
	}

	/** Places all blocks in the stack.
	 * <p>Called only when a disk is being created, this function
	 * prepares the stack for use by placing every block on the free list.
	 */
	void initialize()
	{
		int i;
		synchronized (stack) {
			for (i=numblocks-1; i>=0; i--)
				stack.push(new Integer(i));
		}
	}
	
	/** Retrieves the free list from disk.
	 * <p>Traverses the physical disk file in the free list section.  
	 * There is one byte for every data block.  Those bytes with values
	 * set to FREE are pushed into the stack.
	 * @exception IOException if there is an I/O error.
	 */
	void retrieve() throws IOException
	{
		int i;
		byte blockused;
		synchronized (file) {
			long oldpos = file.getFilePointer();
			file.seek(freeliststart);
			for (i=0; i<numblocks; i++)
			{
				try {
				blockused = file.readByte();
				} catch (IOException e) {
					throw e;
				}
				if (blockused==FREE)
					synchronized (stack) {
						stack.push(new Integer(i));
					}
			}
			file.seek(oldpos);
		}
	}
	
	/** Writes the entire free list to disk.
	 * <p>One byte is written to the disk file for every data block.
	 * @exception IOException if there is an I/O error.
	 */
	void commit() throws IOException
	{
		synchronized (file) {
			long oldpos = file.getFilePointer();
			file.seek(freeliststart);
			int i;
			byte blockused;
			for (i=0; i<numblocks; i++)
			{
				boolean isthere;
				synchronized (stack) {
					isthere = stack.contains(new Integer(i));
				}
				if (isthere)
					blockused = FREE;
				else
					blockused = USED;
				file.writeByte(blockused);
			}
			file.seek(oldpos);
		}
	}
	
	/** Allocates a new data block.
	 * <p>The function obtains the number of a free block from the stack
	 * and writes that block's entry on the on-disk free list as used.
	 * @return Number of the new block.
	 * @exception IOException if there is an I/O error.
	 */
	int allocateSpace() throws IOException
	{
		int newblock;
		synchronized (stack) {
			newblock = ((Integer)stack.pop()).intValue();
		}
		synchronized (file) {
			long oldpos = file.getFilePointer();
			file.seek(freeliststart+newblock);
			file.writeByte(USED);
			file.seek(oldpos);
		}
		return newblock;
	}

	/** Returns a data block to the free list.
	 * <p>The function pushes the data block on the stack and writes its
	 * on-disk free list entry as free.
	 * @exception IOException if there is an I/O error.
	 */
	void freeSpace(int oldblock) throws IOException
	{
		long oldpos = file.getFilePointer();
		stack.push(new Integer(oldblock));
		file.seek(freeliststart+oldblock);
		file.writeByte(FREE);
		file.seek(oldpos);
	}

}

