package tos.api;

import java.io.*;

/** Encapsulates reading data from a TOSFile and placing it
 * into a <code>java.io.Writer</code> object. */

public class TOSFileWriter
{
	/** TOS file to read from.	 */
	TOSFile file;
	
	/** Writer to write to.	 */
	Writer writer;
	
	/** Creates a new object with the given TOS file and writer.
	 * Both must have already been opened.
	 * @param file TOS file to read from.
	 * @param writer Writer to write to.
	 */
	public TOSFileWriter(TOSFile file,Writer writer)
	{
		this.file = file;
		this.writer = writer;
	}

	/** Reads a character from the TOS file and writes to the writer.
	 * <b>Warning:</b> This code will not work correctly unless the default 
	 * character encoding scheme is ASCII.
	 * @exception TOSFileNotOpenException if the TOSFile is not open.
	 * @exception InvalidModeException if the TOSFile is not opened for reading.
	 * @exception IOException if an I/O error occurs in either the reading or writing.
	 * @exception TOSFileException if an exception is thrown by the TOSFile object.
	*/
	public void readFile() throws TOSFileNotOpenException, InvalidModeException, IOException, TOSFileException
	{
		if (!file.isOpen)
			throw new TOSFileNotOpenException();
		char mychar;
		while (true)
		{
			try {
				mychar = file.readChar();
			} catch (EOFException e) {
				break;
			}
			writer.write(mychar);
			// Carry out the ASCII CR/LF conversion
			// This code will cause problems on non-ASCII platforms!!!
			if (mychar==13)
				writer.write(10);
		}
	}
}
