package tos.api;

import java.io.*;

/** Encapsulates reading in from an input stream and placing
 * the contents into a TOS file.
 */

public class TOSFileInputStream
{	
	/** TOS file to write to.	 */
	TOSFile file;
	
	/** Input stream to read from. */
	InputStream istream;

	/** Creates a new object with a given TOS file and input stream.
	 * Both must have already been opened.
	 * @param tosfile TOS file.
	 * @param stream Input stream.
	 */
	public TOSFileInputStream(TOSFile tosfile, InputStream stream)
	{
		file = tosfile;
		istream = stream;
	}
	
	/** Reads a byte from the input stream and writes to the TOS file.
	 * @exception TOSFileNotOpenException if the TOSFile is not open
	 * @exception InvalidModeException if the TOSFile is not opened for writing.
	 * @exception IOException if an I/O error occurs in either the reading or writing.
	 * @exception TOSFileException if an exception is thrown by the TOSFile object.
	 */
	public void writeFile() throws TOSFileNotOpenException, InvalidModeException, IOException, TOSFileException, DiskFullException
	{
		if (!file.isOpen)
			throw new TOSFileNotOpenException();
		int myint = 0;
		byte mybyte;
		while (true)//(istream.available() > 0)
		{
			myint = istream.read();
			if (myint==-1)
				break;
			mybyte = (byte)myint;
			file.write(mybyte);
		}
	}

}
