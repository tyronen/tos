package tos.api;

import java.io.*;

/** This class encapsulates the transfer of data <i>from</i> a 
 * <code>java.io.Reader</code> object <i>to</i> a TOSFile.
 */

public class TOSFileReader
{	
	/** TOSFile to write into.	 */
	TOSFile file;
	
	/** Reader to read from. */
	Reader reader;

	/** Constructor.
	 * Creates a TOSFileReader with the given TOSFile and reader.
	 * Both must have been opened before passing it to this method.
	 * @param tosfile TOSFile to write to.
	 * @param Reader Reader to read from.
	 */
	public TOSFileReader(TOSFile tosfile, Reader reader)
	{
		file = tosfile;
		this.reader = reader;
	}
	
	/** Reads a character from the reader and writes into the TOS file.
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
		char mychar;
		while (true)//(reader.available() > 0)
		{
			myint = reader.read();
			if (myint==-1)
				break;
			mychar = (char)myint;
			file.writeChar(mychar);
		}

	}

}
