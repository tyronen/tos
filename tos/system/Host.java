package tos.system;

import java.io.*;
import java.net.*;

/** This class encapsulates a data structure about a host, including
 * its IP address and name.
 */

class Host implements Serializable
{
	/** The host's address. */
	InetAddress address;
	
	/** The host's native operating system. */
	String osname;
	
	/** The host's network name. */
	String hostname;
	
	/** Creates an object using the local host's information.
	 * The hostname, address, and OSname are obtained from 
	 * the static <code>System.getProperty()</code> and 
	 * <code>InetAddress</code> functions.
	 * */
	public Host()
	{
		hostname = "localhost";
		try {
			address = InetAddress.getLocalHost();
			hostname = address.getHostName();
		} catch (UnknownHostException e) {
			address = null;
		}
		try {
			osname = System.getProperty("os.name");
		} catch (SecurityException e) {
			osname = "Unknown";
		}
	} 
	
	/** Creates a host object using the given address and OS name.
	 * @param addr The host's address.
	 * @param os The host's native operating system.
	 */
	public Host(InetAddress addr, String os)
	{
		address = addr;
		hostname = addr.getHostName();
		osname = os;
	}

	/** Uses address equality to determine host equality.
	 * @param other Another host.
	 * @return <code>true</code> if the two hosts have the same address.
	 */
	public boolean equals(Host other)
	{
		return (this.address.equals(other.address));
	}


}
