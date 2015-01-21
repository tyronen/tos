package tos.system;

import java.io.*;
import java.rmi.*;

/** This class provides a data structure to encapsulate basic
 * information about any server process.
 */

class ServerRecord implements Serializable
{
	/** The server's remote stub. */
	Remote stub;
	
	/** The type of server. */
	String type;
	
	/** The name of the host the server is running on.  */
	String hostname;
	
	/** The port number the server is listening on.	 */
	int portnum;
	
	/** Creates a new server record.
	 * @param stub The server's remote stub. 
	 * @param type The type of server.
	 * @param hostname The name of the host the server is running on.
	 * @param portnum The port number the server is listening on.
	 */
	ServerRecord(Remote stub, String type, String hostname, int portnum)
	{
		this.stub = stub;
		this.type = type;
		this.hostname = hostname;
		this.portnum = portnum;
	}
}
