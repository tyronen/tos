package tos.api;

import java.io.*;

/** Encapsulates writing a TOS file to a <code>java.io.OutputStream</code> object.*/

public class TOSFileOutputStream
{
	/** TOS file to read from.	 */
	TOSFile file;
	
	/** OutputStream to write to. */
	OutputStream ostream;

	/** Creates the object with the given TOS file and output stream.
	 * Both the TOS file and the stream must already be open.
	 * @param tosfile TOS file to read from.
	 * @param stream Output stream to write to.
	 */
	public TOSFileOutputStream(TOSFile tosfile, OutputStream stream)
	{
		file = tosfile;
		ostream = stream;
	}
	
	/** Reads a byte from the TOS file and writes it to the output stream.
 	 * @exception TOSFileNotOpenException if the TOSFile is not open
	 * @exception InvalidModeException if the TOSFile is not opened for reading.
	 * @exception IOException if an I/O error occurs in either the reading or writing.
	 * @exception TOSFileException if an exception is thrown by the TOSFile object.
	*/
	public void readFile() throws TOSFileNotOpenException, InvalidModeException, IOException, TOSFileException
	{
		if (!file.isOpen)
			throw new TOSFileNotOpenException();
		byte mybyte;
		while (true)
		{
			try {
				mybyte = file.read();
			} catch (EOFException e) {
				break;
			}
			ostream.write(mybyte);
		}
		
	}
	
}
