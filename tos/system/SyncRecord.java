package tos.system;

import java.io.*;
import java.util.*;

/** This class provides a data structure to encapsulate basic
 * information about a sync object.
 */

class SyncRecord implements Serializable
{
	/** Name of the sync object. */
	String name;
	
	/** Type of the object.	 */
	int type;
	
	/** Count of the object (always -1 for signals)	 */
	int count = 0;
	
	/** Maximum of the count (always 1 for mutexes)	 */
	int max;
	
	/** Queue of waiting threads */
	Vector queue = new Vector();
		
	/** The process of the first thread in the queue. */
	String firstproc;
	
	/** The first thread in the queue. */
	String firstthread;
	
	/** The object's location. 
	 * The server itself does not use this field; it is used
	 * by the administrator.
	 */
	String location; 
	
	/** Creates a new record.
	 * @param name Name of the sync object.
	 * @param type Type of the object.
	 * @param max Maximum of the count.
	 * @param location The object's location. 
	 */
	SyncRecord(String name, int type, int max, String location) 
	{ 
		this.name = name; 
		this.type = type;
		this.max = max;
		this.location = location;
		
	}
		
	/** Considers two records equal if their names and types are equal.
	 * @param other Other <code>SyncRecord.</code>
	 * @return <code>true</code> if the <code>name</code> and <code>type</code>
	 * fields are equal.
	 */
	boolean equals(SyncRecord other) 
	{ 
		return (name.equals(other.name) && type==other.type); 
	}
	
	/** Fills in the <code>firstproc</code> and <code>firstthread</code> fields.
	 * This function assumes the process and thread names have been 
	 * concatenated together and that the first charater in the thread 
	 * name is non-numeric.  For this reason, applications that name
	 * their threads are strongly advised not to use all-numeric
	 * thread names.
	 */
	void keepFirst()
	{
		String elem = (String)queue.firstElement();
		int pos = 0;
		while (Character.isDigit(elem.charAt(pos++)));
		firstproc = elem.substring(0,pos-1);
		firstthread = elem.substring(pos-1);
	}

	/** Add a new thread to the queue.
	 * Called by the server's <code>Wait()</code> function.
	 * @param id String containing the waiting thread's process identifier and name.
	 */
	public void addElement(String id)
	{
		queue.addElement(id);
		if (queue.size()==1)
			keepFirst();
	}
	
	/** Remove a thread from the queue.
	 * Called by the server's <code>Release()</code> function.
	 * @param id String containing the waiting thread's process identifier and name.
	 */
	public void removeElement(String id)
	{
		queue.removeElement(id);
		if (queue.size()>0)
			keepFirst();
		else
		{
			firstproc = "";
			firstthread = "";
		}
	}
			
			
}
