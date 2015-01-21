//FileStore.java
package tos.system;

import java.io.*;

/** This class implemented the physical storage of a TOS virtual disk.
 * <p>The <code>java.io.RandomAccessFile</code> contains most of the 
 * functionality needed by TOS.  The main difference between this class
 * and its parent is that the functions here are declared as 
 * <code>synchronized</code> for thread-safe operation.
 * <p><i>Some of the documentation for this class is extracted
 * from the Java Development Kit, version 1.1.7.  Copyright 1995-98 by Sun Microsystems Inc.
 * Used by permission.</i>
 */

class FileStore extends RandomAccessFile
{
	/** The default extension of a TOS disk file.	 */
	static String ext = ".tos";
	
	/** The disk's superblock. */
	Superblock superblock;	
	
	/** Constructor used to create a new file.
	 * @param name Name of disk.
	 * @param superblock Superblock of new disk.
	 * @exception IOException if an I/O problem occurs.
	 */ 
	FileStore(String name, Superblock superblock) throws IOException
	{
		super(name + ext,"rw");
		this.superblock = superblock;
	}
	
	/** Constructor used to reopen an existing file.
	 * <p>The only difference between this constructor and the other
	 * is that this one attempts to retrieve the superblock from the 
	 * file.
	 * @param name Name of disk.
	 * @exception IOException if an I/O problem occurs.
	 */ 
	FileStore(String name) throws IOException
	{
		super(name + ext,"rw");
		superblock = new Superblock();
		superblock.retrieve(this);
	}
	
	/** Moves the file pointer to the beginning of an inode.
	 * @param inode Number of inode to go to.
 	 * @exception IOException if an I/O problem occurs.
	 */
	synchronized void goToNode(int inode) throws IOException
	{
		seek(superblock.inodestart + inode*superblock.inodesize);
	}
	
	/** Moves the file pointer to the beginning of a data block.
	 * @param block Number of block to go to.
 	 * @exception IOException if an I/O problem occurs.
	 */
	synchronized void goToBlock(int block) throws IOException
	{
		seek(superblock.datastart + superblock.blocksize*block);
	}
	
    /**
     * Returns the current offset in this file. 
     *
     * @return     the offset from the beginning of the file, in bytes,
     *             at which the next read or write occurs.
     * @exception  IOException  if an I/O error occurs.
     */
	public synchronized long getFilePointer() throws IOException
	{
		return super.getFilePointer();
	}

    /** Reads a byte of data from this file.      * This method blocks if no 
     * input is yet available. 
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             file is reached.
     * @exception  IOException  if an I/O error occurs.
     */
	public synchronized int read() throws IOException
	{
		return super.read();
	}

    /**
     * Writes the specified byte to this file. 
     *
     * @param      b   the <code>byte</code> to be written.
     * @exception  IOException  if an I/O error occurs.
     */
	public synchronized void write(int b) throws IOException
	{
		super.write(b);
	}

    /**
     * Sets the file-pointer offset, measured from the beginning of this 
     * file, at which the next read or write occurs.  The offset may be 
     * set beyond the end of the file. Setting the offset beyond the end 
     * of the file does not change the file length.  The file length will 
     * change only by writing after the offset has been set beyond the end 
     * of the file. 
     *
     * @param      pos   the offset position, measured in bytes from the 
     *                   beginning of the file, at which to set the file 
     *                   pointer.
     * @exception  IOException  if an I/O error occurs.
     */
	public synchronized void seek(long pos) throws IOException
	{
		super.seek(pos);
	}

    /**
     * Skips exactly <code>n</code> bytes of input. 
     * <p>
     * This method blocks until all the bytes are skipped, the end of 
     * the stream is detected, or an exception is thrown. 
     *
     * @param      n   the number of bytes to be skipped.
     * @return     the number of bytes skipped, which is always <code>n</code>.
     * @exception  EOFException  if this file reaches the end before skipping
     *               all the bytes.
     * @exception  IOException  if an I/O error occurs.
     */
	public synchronized int skipBytes(int n) throws IOException
	{
		return super.skipBytes(n);
	}
}
