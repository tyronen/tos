package tos.system;

import java.io.*;
import java.rmi.*;
import tos.api.*;

/** Provides the remote interface of a TOS disk.
 */

public interface TOSDisk extends Remote
{
	/** Returns a block to the free list.
	 * @param blocknum Block to free.
	 * @exception RemoteException if an RMI error occurs.
	 */
	 void freeSpace(int blocknum) throws RemoteException; // called by TOSFile

	/** Allocates new space to the given inode.  
	 * @param iblock Index block where new data block is to be placed.
	 * @return int Number of the new data block.
 	 * @exception RemoteException if an RMI error occurs.
 	 * @exception IOException if an I/O error occurs.
 	 * @exception DiskFullException if the disk is full.
	 */
	int newDataBlock(IndexBlock iblock) throws RemoteException, 
											   IOException, 
											   DiskFullException;
	 
	/** Retrieves a data block from disk.
	 * @param blocknum Number of block to retrieve.
 	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs. 
	 */
	 byte[] retrieveDataBlock(int blocknum) throws RemoteException, 
												   IOException;
	 
	/** Retrieves an index block from the disk file.
	 * @param iblocknum Number of block to retrieve.
 	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs. 
	 */
	 IndexBlock retrieveIndexBlock(int iblocknum) throws RemoteException, 
														 IOException;
	 
	/** Writes an index block to disk.
	 * @param iblock Index block to write.
 	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs. 
	 */
	 void commitIndexBlock(IndexBlock iblock) throws RemoteException, 
													 IOException;
	 
	/** Creates a new index block.
	 * @param filenum Inode number of the file.
	 * @param parent Last index block in the file.
	 * @return the new index block.
	 * @exception IOException if an I/O error occurs.
	 * @exception DiskFullException if the disk is full.
	 */
	 IndexBlock newIndexBlock(int filenum,IndexBlock iblock) throws RemoteException, 
																	IOException,
																	DiskFullException;
	/** Returns the root node.
	 * @return Inode object representing the root.
	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs.
	 */ 
	 Inode getRootNode() throws RemoteException, IOException;
	 
	/** Writes a data block to disk.
	 * @param blocknum Number of block to write.
	 * @param block Content of data block.
 	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs. 
	 */
	 void commitDataBlock(int blocknum, byte[] block) throws RemoteException, 
															 IOException;
	 
	/** Returns the given inode.
	 * @param num Number of inode to return.
	 * @return the inode.
	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs.
     */	
	 Inode getNode(int num) throws RemoteException, IOException;
	 
	/** Update the on-disk copy of an inode.  The Modified field is set to
	 * the current date.
	 * @param inode Inode to update.
 	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs.
	 */
	 void updateNode(Inode inode) throws RemoteException, IOException;
	 
	/** Terminates the disk.
	 * The disk is unbound from the registry and the physical
	 * file is closed.
	 * @exception RemoteException if an RMI error occurs.
	 */
	 void terminate() throws RemoteException;	
	 
	/** Allocates a new inode number.
	 * @return the new number
 	 * @exception RemoteException if an RMI error occurs.
 	 * @exception FilesFullException if there are no more inodes available.
	 * @exception IOException if an I/O error occurs.
	 */
	 int newInode() throws RemoteException, FilesFullException, IOException;
	 
	/** Returns the size of a data block in this disk.
	 * @return the size of a data block in this disk.
	 * @exception RemoteException if an RMI problem occurs.	 
	 */
	 int getBlockSize() throws RemoteException;
	 
	/** Creates a new file.  The first index block and data block are 
	 * allocated and assigned to the new inode.
	 * @param filenum Inode number of new file.
	 * @param filename Name of new file.
	 * @exception RemoteException if an RMI error occurs.
	 * @exception IOException if an I/O error occurs.
	 * @exception DiskFullException if the disk is full.
	 */
	Inode createFile(int filenum, String filename) throws RemoteException, 
														   IOException, 
														   DiskFullException;
	/** Removes a file.
	 * This simply marks the inode as being unused.  This funciton does not
	 * actually free the file's blocks.
	 * @param inode Inode of the file to be removed.
	 * @exception RemoteException if an RMI error occurs.
	 */
	 void removeFile(Inode inode) throws RemoteException;
	
	 /** Writes a mount point to the superblock.
	 * @param mountpt New mount point.
	 * @exception RemoteException if an RMI problem occurs.	 
	 */
	void mount(String mountpt) throws RemoteException, IOException;


}