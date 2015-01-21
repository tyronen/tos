package tos.system;

/** This is a print stream that throws away all 
 * input it receives.  Useful for hiding
 * the System.err or System.out streams.
 */

import java.io.*;

/** An inner class used to implement the NullPrintStream.
 */
class NullStream extends OutputStream
{
	/** Overrides the <code>OutputStream.write</code> function to do nothing.
	 */
	public void write(int b) throws IOException
	{
		// do nothing!  haha	
	}
}

public class NullPrintStream extends PrintStream
{
	public NullPrintStream()
	{
		super(new NullStream());
	}
}
