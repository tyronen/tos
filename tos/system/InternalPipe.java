package tos.system;

import java.io.*;

/** This class provides a data structure to encapsulate basic
 * information about a pipe, and to perform simple operations on 
 * pipes.
 * 
 * The pipe is considered to be attached to two processes, which
 * carry internal monikers called "left" and "right" (imaginative, no?).  
 * The process that originally creates the pipe is considered the "left"
 * process; the next process to connect to it is called "right".  If one
 * side of the pipe is left dangling, the next process to connect to it
 * receives the moniker that is available.  If <i>both</i> sides have
 * had their process disconnect, the pipe is considered to be abandoned
 * and its server will destroy it.
 * 
 * These monikers are communicated to the client, whose <code>Pipe</code>
 * object passes it at every server request.
 * 
 * Therefore, if a client thread passes a <code>Pipe</code> object to 
 * another thread or process, both will be sharing the same side of the 
 * pipe.  Care must be taken, since the data inside the pipe can only be 
 * read once.  Data written into the pipe will be read in the order it was 
 * written.
 * 
 * The pipe is implemented by having four objects, two of class 
 * <code>java.io.PipedInputStream</code> and two of class 
 * <code>java.io.PipedOutputStream</code>.
 */

class InternalPipe implements Serializable
{
	/** Integer representing the left side	 */
	static int LEFTHANDLE = 0;
	
	/** Integer representing the right side	 */
	static int RIGHTHANDLE = 1;
	
	/** Integer representing that both sides are taken.	 */
	static int UNAVAILABLE = -1;
	
	/** Name of pipe. */
	String name;

	/** Location of server. */
	String location;

	/** Set to <code>true</code> if this is the left side. */
	boolean isLeft = false;
	
	/** Set to <code>true</code> if this is the right side. */
	boolean isRight = false;
	
	/** Set to <code>true</code> if no process has ever used this pipe.	 */
	boolean neverUsed = true;

	/** Stream containing data to be read by the left. */
	PipedInputStream  leftfromright;
	
	/** Stream for data written by the left. */
	PipedOutputStream lefttoright;
	
	/** Stream for data to be read by the right. */
	PipedInputStream  rightfromleft;
	
	/** Stream for data written by the right. */
	PipedOutputStream righttoleft;

	/** Creates a new object.
	 * @param name Name of pipe.
	 * @location Location of pipe server.
	 * @exception IOException if an I/O problem occurs creating the streams.
	 */
	InternalPipe(String name, String location) throws IOException
	{
		this.name = name;
		this.location = location;
		leftfromright	= new PipedInputStream();
		lefttoright		= new PipedOutputStream();
		rightfromleft	= new PipedInputStream(lefttoright);
		righttoleft		= new PipedOutputStream(leftfromright);
	}
	
	/** Called when a client process creates or attempts to connect to a pipe.
	 * This will assign either the left side or the right side to the 
	 * calling process, whichever is free.  If both are taken, the process
	 * is told the pipe is unavailable.  
	 * Once the right side has been assigned, <code>neverUsed</code> is 
	 * set to <code>false</code>.  The next time both sides are 
	 * unassigned, the pipe will be destroyed.
	 * @returns A handle indicating whether the pipe is left or right.
	 */
	synchronized int open()
	{
		if (!isLeft)
		{
			isLeft = true;
			return LEFTHANDLE;
		}
		else if (!isRight)
		{
			neverUsed = false;
			isRight = true;
			return RIGHTHANDLE;
		}
		else
			return UNAVAILABLE;
	}
	
	/** Reads data from a pipe.
	 * This will read the specified number of bytes from either the
	 * left or right sides, depending on what handle the client has 
	 * passed in.
	 * @param handle The side of the pipe.
	 * @param count Maximum number of bytes to read.
	 * @return Array containing data read.
	 * @exception IOException if nothing could be read, or if a runtime Java
	 * exception occurred.
	 */
	byte[] read(int handle, int count) throws IOException
	{
		byte[] buffer = new byte[count];
		int num = 1;
		if (handle == LEFTHANDLE)
			synchronized (leftfromright) {
				num = leftfromright.read(buffer,0,count);
			}
		else if (handle == RIGHTHANDLE)
			synchronized (rightfromleft) {
				num = rightfromleft.read(buffer,0,count);
			}
		//return num;
		byte[] outbuf;
		if (num>0)
		{
			outbuf = new byte[num];
			System.arraycopy(buffer,0,outbuf,0,num);
			return outbuf;
		}
		else
			throw new EOFException();
	}
	
	/** Writes data into a pipe.
	 * This will write the specified byte arrat into either the
	 * left or right sides, depending on what handle the client has 
	 * passed in.
	 * @param handle The side of the pipe.
	 * @param buffer The bytes to be written.
	 * @param count Number of bytes in the buffer.
	 * @exception IOException if nothing could be read, or if a runtime Java
	 * exception occurred.
	 */
	void write(int handle, byte[] buffer, int count) throws IOException
	{
		if (handle == LEFTHANDLE)
			synchronized (lefttoright) {
				lefttoright.write(buffer,0,count);
			}
		else if (handle == RIGHTHANDLE)
			synchronized (righttoleft) {
				righttoleft.write(buffer,0,count);
			}
	}

	/** Closes the pipe.
	 * The side being closed is marked as unused. 
	 * If the pipe is to be removed, all streams are closed.  
	 * @param handle Side of pipe passed in by client.
	 */
	void close(int handle)
	{
		if (handle == LEFTHANDLE)
			isLeft = false;
		else if (handle == RIGHTHANDLE)
			isRight = false;
		if (unused())
		{
			try {
				righttoleft.close();
				rightfromleft.close();
				lefttoright.close();
				leftfromright.close();
			} catch (IOException e) {
				// we ignore this exception - not the end of the world
				// if a pipe isn't closed properly
			}
		}
		
	}
	
	/** Indicates whether a pipe is no longer needed.
	 * @return <code>true</code> if this pipe has been used
	 * before, but has no left or right sides.
	 */
	boolean unused()
	{
		return (!neverUsed && !isLeft && !isRight);
	}
	
	/** Equality operator.
	 * @param other Another <code>InternalPipe</code>.
	 * @return <code>true</code> if the other pipe has the same name.
	 */
	boolean equals(InternalPipe other)
	{
		return (name.equals(other.name));
	}

	/** Function required to make this class <code>Serializable</code>.
	 * The stream objects in this class are not serializable.  To get around
	 * this, this function is defined.  Only the name, location, and the 
	 * three <code>boolean</code> variables are written.  UTF encoding is
	 * used for the strings.
	 */
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.writeUTF(name);
		out.writeBoolean(isLeft);
		out.writeBoolean(isRight);
		out.writeBoolean(neverUsed);
		out.writeUTF(location);
	}
 
	/** Function required to make this class <code>Serializable</code>.
	 * The stream objects in this class are not serializable.  To get around
	 * this, this function is defined.  Only the name, location, and the 
	 * three <code>boolean</code> variables are read.  UTF encoding is
	 * used for the strings.
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		name = in.readUTF();
		isLeft = in.readBoolean();
		isRight = in.readBoolean();
		neverUsed = in.readBoolean();
		location = in.readUTF();

	}
 

	
}
