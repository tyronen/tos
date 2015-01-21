//ProcessRecord.java
package tos.system;

import java.io.*;
import java.security.*;
import java.util.*;

/** This class provides a data structure to encapsulate basic
 * information about a running process.
 * 
 * Only the <code>Launcher</code> class currently makes use of 
 * this class.
 * 
 * @see Launcher
 */

class ProcessRecord implements Serializable
{
	/** Remote stub of the process' listener. */
	TOSListener stub;
	
	/** Java class being run by the process. */
	String classname;
	
	/** Host on which the process is running. */
	String hostname;
	
	/** TOS process identifier of the process.  */
	int id;
	
	/** A random number generator for new process IDs.
	 * The <code>java.security.SecureRandom</code> class could
	 * be used here instead, but it will slow down
	 * the registration of new processes considerably.
	 */
	static Random rnd = new Random();
	
	/** This version is meant for use by the Launcher
	 */
	ProcessRecord(TOSListener lstub, String classnm, String host)
	{
		stub = lstub;
		classname = classnm;
		hostname = host;
		byte[] mybytes = new byte[2];
		// Convert the byte array to a 32-bit long
		id = 0;
		for (int i=0; i<mybytes.length; i++)
		{
			mybytes[i] = (byte)rnd.nextInt();
			id += Math.abs(((int)mybytes[i])<<(8*(mybytes.length-1-i)));
		}
	}
	
	boolean equals(ProcessRecord other)
	{
		return (id == other.id);
	}
}
