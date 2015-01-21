//Console.java
package tos.system;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.net.*;
import tos.api.*;

/** This class is run to operate the TOS console, a command-line
 * utility that allows users to navigate the file system, manage
 * files, and compile and run Java classes.
 * 
 * <b>Note:</b> Currently the console does not operate on the Macintosh.
 * This is because the Apple MRJ SDK does not yet support the use of the
 * <code>System.in</code> input stream for non-GUI Java applications.  Future
 * releases of the MRJ SDK may change this.
 * 
 * The console class is responsible for keeping track of the user's
 * working directory and maintaining a connection to the correct
 * disk.  Its connection to a launcher, established by the user, is only
 * used to obtain remote references to TOS disks.
 * 
 * The console may run from any machine on the network, not just those
 * that happen to be running launchers, as long as one launcher and any
 * disks referenced are on a host that can be reached via a TCP/IP link
 * from the machine running the console.
 *
 * A complete description of the operation of the console may be found in
 * the <a href="../Userguide.html">TOS User's Guide</a>.
 */

public class Console
{
	/** The maximum number of arguments in a single command. */
	static int MAX_ARGS = 40;
	
	/* Remote stub of the current disk. */
	protected TOSDisk fs;
	
	/* Remote stub of the current launcher. */
	protected TOSLauncher launcher;
	
	/* Remote stub of the current filename server. */
	protected TOSFileNameServer nameserver;
	
	/** The working directory the user sees. */
	protected String workdir = "";
	
	/** The actual directory relative to the mount point on the disk. */
	protected String realdir = ""; 
	
	/** The name of the current disk. */
	protected String server = ""; 
	
	/** Set to <code>true</code> if connected to a launcher.	 */
	protected boolean isConnected = false; 
	
	/** A convenience reader to <code>System.in</code> */
	protected InputStreamReader stdin = new InputStreamReader(System.in);
		
	/** The registered compiler on the host. */
	protected String compiler = "";

	/** The Java entry function.
	 * @param args No arguments are examined.
	 */
	public static final void main(String[] args)
	{
		Console con = new Console();
	}
	
	/** Starts the command-line loop.
	 * The constructor runs an infinite loop where it prints the command prompt,
	 * reads in a line, strips off the CR/LF characters, and passes it
	 * to <code>InterpretCommand</code>.
	 */
	public Console()
	{
		while (true)
		{
			System.out.print("TOS:" + workdir +"> ");
			String cmdstr = readline();
			try {
				System.out.println(InterpretCommand(cmdstr));
			} catch (Exception e) {
				System.out.println("Error interpreting command.");
			}
		}
	}
	
	/** Reads in a line from the standard input.
	 * @return The read-in line, stripped of CR/LF characters.
	 */
	String readline()
	{
		char[] cmd = new char[80];
		int numread;
		try {
			numread = stdin.read(cmd,0,80);
		} catch (IOException e) {
			return "Error reading from standard input.";
		}
		// Platforms sometimes add CR/LF characters to be removed
		int CR = 13;
		int LF = 10;
		int subt = 0;
		if (cmd[numread-1]==CR || cmd[numread-1]==LF)
			subt++;
		if (cmd[numread-2]==CR || cmd[numread-2]==LF)
			subt++;
		
		return new String(cmd,0,numread-subt);
	}

		
	
	/** Parses the command line.
	 * Uses a <code>StringTokenizer</code> object to 
	 * parse the command line into a <code>String</code> array.
	 * @param commandline The command line as a single <code>String</code>.
	 * @exception Exception any exception thrown while parsing.
	 */
	String InterpretCommand(String commandline) throws Exception
	{
		StringTokenizer parser = new StringTokenizer(commandline);
		String command;
		Vector argvec = new Vector(MAX_ARGS);
		while (true)
		{
			try {
				argvec.addElement(parser.nextToken());
			} catch (NoSuchElementException e) {
				break;
			}
		}
		argvec.trimToSize();
		if (argvec.size()<1)
			return "No command entered.";
		String args[] = new String[argvec.size()];
		argvec.copyInto(args);
		return HandleCommand(args);
	}

	/** Sends the command and its arguments to the correct function.
	 * If the console is not connected to a launcher, only the 
	 * <i>connect</i> and <i>exit</i> commands are accepted.
	 * Once connected, any command may be entered.  Some commands
	 * have two names, one based on its MS-DOS equivalent, the other
	 * on UNIX.
	 * @param args Array representing the command and its arguments.
	 */
	String HandleCommand(String[] args)
	{
		String command = args[0];
		if (command.equals("exit"))
			return Exit(args);
		else if (command.equals("connect"))
			return Connect(args);
		
		if (!isConnected)
			return "Not connected to a launcher.";
		if (command.equals("cd"))
			return Cd(args);
		else if (command.equals("chmod"))
			return Chmod(args);
		else if (command.equals("copy") || command.equals("cp"))
			return Copy(args);
		else if (command.equals("delete") || command.equals("del") || command.equals("rm"))
			return Delete(args);
		else if (command.equals("dir") || command.equals("ls"))
			return Dir(args);
		else if (command.equals("disconnect"))
			return Disconnect(args);
		else if (command.equals("edit"))
			return Edit(args);
		else if (command.equals("exec"))
			return Exec(args);
		else if (command.equals("export"))
			return Export(args);
		else if (command.equals("import"))
			return Import(args);
		else if (command.equals("javac"))
			return Javac(args);
		else if (command.equals("mkdir") || command.equals("md"))
			return Mkdir(args);
		else if (command.equals("move") || command.equals("mv"))
			return Move(args);
		else if (command.equals("regcomp"))
			return Regcomp(args);
		else if (command.equals("rmdir"))
			return Rmdir(args);
		else if (command.equals("run"))
			return Run(args);
		else
			return ("Invalid command");

	}

	/** Converts a relative filename into an absolute filename.
	 * <p> This is a fairly complex function, because much of the 
	 * code that is used to navigate the file system is contained 
	 * only here.
	 * <p> The function starts by dealing quickly with destinations
	 * consisting entirely of the root (<code>/</code>), present 
	 * (<code>.</code>), and parent (<code>..</code>) directories.
	 * <p> Other destinations fall into three categories.  They can
	 * either be absolute (eg. <i>/dir1/dir2/file</i>), forward
	 * relative (eg. <i>dir1/dir2/file</i>), or backward relative
	 * (with one or more <i>../</i> sections).  The strategy followed is
	 * to first convert all three to the absolute form.  For absolute 
	 * names this is trivial; for forward relative names it is a 
	 * simple matter of appending the destination to the current working
	 * directory.  
	 * <p>Backward relative names must be parsed backwards as many times
	 * as there are <code>..</code> symbols.  Afterwards, they can be
	 * treated as forward relative.
	 * @param relName Relative file name to convert.
	 * @return Absolute name of the file, in the global namespace.
	 */
	String absoluteName(String relName)
	{
		// absolute case '/'
		if (relName.substring(0,TOSFile.separator.length()).equals(TOSFile.separator))
			return relName;	
		// just plain '.'
		else if (relName.equals(TOSFile.curdir))  
			return this.workdir;
		// just plain '..'
		else if (relName.equals(TOSFile.parentdir)) 
		{
			int lastsep = this.workdir.lastIndexOf(TOSFile.separator);
			if (lastsep==0) // we're at root
				return TOSFile.separator;
			else
				return this.workdir.substring(0,lastsep);
		}
		else
		{
			// Eliminate superfluous ./ references
			int curlength = TOSFile.curdir.length()+TOSFile.separator.length();
			if (relName.length()>=curlength)
				if (relName.substring(0,curlength).equals(TOSFile.curdir+TOSFile.separator))
					relName = relName.substring(curlength);

			// If it uses ../, do the backward motion
			String absName = this.workdir;
			// parlength = length of ../ prefix = 3
			int parlength = TOSFile.parentdir.length()+TOSFile.separator.length();
			int lastpos;
			while (relName.length()>=parlength && relName.substring(0,parlength).equals(TOSFile.parentdir+TOSFile.separator))
			{
				if (absName.equals(TOSFile.separator))
					return ("Invalid pathname"); // means we are already in root, cannot go back
				lastpos = absName.lastIndexOf(TOSFile.separator);
				if (lastpos==0)
					absName = TOSFile.separator; // the root directory
				else
					absName = absName.substring(0,lastpos);
				relName = relName.substring(parlength);
			}
			// Now go forward
			if (absName.equals(TOSFile.separator))
				// / --> /newdir
				absName = absName + relName;
			else
				// /olddir = /olddir/newdir
				absName = absName + TOSFile.separator + relName;			
			return absName;
		}
	}
	
	
	
	/** Implements the <i>cd</i> command.
	 * <p>Once an absolute name of the destination has been arrived 
	 * at, it must be checked for validity.  The function tries to open
	 * the directory for reading (since directories are only files without
	 * permissions).  
	 * <p>If this succeeds, the next step is to obtain
	 * a remote stub to the correct disk.  The filename server is 
	 * contacted and the path relative to the disk's root obtained.
	 * <p>Finally, the disk's remote stub is obtained using the
	 * URL-based <code>Naming</code> class.
	 * @param args Argument list.
	 */
	String Cd(String args[])
	{
		if (args.length!=2)
			return ("Usage: cd pathname");
		/* 3 types of addresses:  
			- absolute /dir1/dir2/...
			- forwards relative dir1/dir2 or ./dir1/dir2
			- backwards relative ../ ...  ../dir1/dir2
		*/
		String realdir = this.realdir;
		String newpath = absoluteName(args[1]);

		// Now we have a new working directory.  Validate it.
		if (newpath.equals(TOSFile.separator))
		{
			realdir = newpath;
			// We operate on the principle that reads/writes are
			// not permitted in the root directory
		}
		else 
		{
			TOSFile check;
			try {
				check = new TOSFile(launcher);
				check.open(newpath,"r","");
			} catch (NotFoundException e) {
				return "No such directory.";
			} catch (Exception f) {
				f.printStackTrace();
				return "Unknown error.";
			}
			if (!check.isDirectory())
				return "Not a directory.";
			try {
				check.close();
			} catch (Exception g) {
				// just let it go
			}

			try {
				realdir = nameserver.resolveFileName(newpath);
			} catch (NoDiskException e) {
				return "Pathname does not map to a mounted Disk.";
			} catch (RemoteException e) {
				return "Network connection error.";
			}
			int mark = realdir.indexOf(TOSFile.servermark);
			String newserver = realdir.substring(0,mark);
			String servpath  = realdir.substring(mark+TOSFile.servermark.length());
			if (!server.equals(newserver))
			{
				try {
					String hostname = launcher.getDiskHost(newserver);
					String mystring = "rmi://"+hostname+"/FS"+newserver;
					fs = (TOSDisk)Naming.lookup(mystring);
				} catch (Exception e) {
					return "Unable to connect to Disk.";
				}
				server = newserver;
			}
			realdir = realdir.substring(server.length()+TOSFile.servermark.length());
		}
		
		this.realdir = realdir;
		this.workdir = newpath;
		return "";
	}

	/** Implements the <i>chmod</i> command.
	 * <p>After validating all arguments, the function simply calls
	 * the <code>TOSFile.open</code> function.
	 * @param args Argument list.
	 * @see TOSFile#open
	 */
	String Chmod(String args[])
	{
		if (args.length!=4)
			return ("Usage: chmod filename permissions password");
		byte permissions;
		try {
			permissions = (byte)Integer.parseInt(args[2]);
		} catch (Exception e) {
			return "Invalid permissions.";
		}
		boolean canRead = ((permissions & 4)==1);
		boolean canWrite = ((permissions & 2)==1);
		boolean canExecute = ((permissions & 1)==1);
		String password = args[3];
		String tosfilename = args[1];
		TOSFile tosfile;
		try {
			tosfile = new TOSFile(launcher);
			openFile(tosfile,tosfilename,"r",password);
			tosfile.chmod(password,canRead,canWrite,canExecute);
		} catch (NotFoundException e) {
			return "File not found.";
		} catch (InvalidPasswordException e) {
			return "Invalid password";
		} catch (InvalidModeException e) {
		    return ("File is not marked as protected.");
		} catch (Exception e) {
			  return "Could not access file.";
		}
		try {
			tosfile.close();
		} catch (TOSFileException e) {
			return "Error closing file.";
		}
		return "File permission changed.";
	}
	
	/** Implements the <i>connect</i> command.
	 * <p> Uses <code>Naming</code> methods to find the 
	 * launcher and gets its default filename server.
	 * Finally the command <i>cd /</i> is run.
	 * @param args Argument list.
	 */
	String Connect(String args[])
	{
		if (args.length!=2)
			return ("Usage: connect url");
		if (isConnected)
			return "Cannot reconnect while connected.";
		String url = "rmi://" + args[1] + "/Launcher";
		try {
			launcher = (TOSLauncher)Naming.lookup(url);
			nameserver = launcher.getFileNameServer();
		} catch (java.rmi.UnknownHostException e) {
			return ("Host not found.");
		} catch (MalformedURLException e) {
			return ("Incorrectly formatted URL.");
		} catch (NotBoundException e) {
			return ("No launcher found on that host");
		} catch (RemoteException e) {
			return ("Unable to connect to remote host.");
		}
		isConnected = true;
		System.out.println("Connected to launcher.");
		try {
			return InterpretCommand("cd " + TOSFile.separator);
		} catch (Exception e) {
			return e.toString();
		}
	}

	String[] getPasswords()
	{
		String retval[] = new String[2];
		System.out.print("Enter source-file password (blank if none): ");
		retval[0] = readline();
		System.out.print("Enter destination-file password (blank if none): ");
		retval[1] = readline();
		return retval;
	}

	
	/** Implements the <i>copy</i> or <i>cp</i> command.
	 * <p>The main functionality of copying is in the
	 * <code>TOSFile.copyFile</code> function.
	 * @param args Argument list.
	 * @see TOSFile#copyFile
	 */
	String Copy(String args[])
	{
		if (args.length!=3)
			return ("Usage: copy|cp source-file destination-file");
		String source = absoluteName(args[1]);
		String dest = absoluteName(args[2]);
		if (source.equals(dest))
			return "Cannot copy file onto itself.";
		return doCopy(source,dest,getPasswords());
	}
	
	String doCopy(String source, String dest, String[] passwords)
	{
		TOSFile srcfile;
		TOSFile destfile;
		try {
			srcfile = new TOSFile(launcher);
			destfile = new TOSFile(launcher);
			srcfile.open(source,"r",passwords[0]);
			destfile.open(dest,"w",passwords[1]);
			srcfile.copyFile(destfile);
			srcfile.close();
			destfile.close();
		} catch (Exception e) {
			
			return "Error copying file";
		}

		return ("File copied.");
	}
		

	/** Implements the <i>delete</i> command, also known as <i>del</i> or <i>rm</i>.
	 * <p>The main functionality of deleting is in the
	 * <code>TOSFile.delete</code> function.
	 * @param args Argument list.
	 * @see TOSFile#delete
	 */
	String Delete(String args[])
	{
		if (args.length!=2 && args.length!=3)
			return ("Usage: delete filename [password]");
		String filename = absoluteName(args[1]);
		String password;
		if (args.length==2)
			password = "";
		else
			password = args[2];
		TOSFile file;
		try {
			file = new TOSFile(launcher);
			file.delete(filename,password);
		} catch (RemoteException e) {
			return "Unable to connect to Disk.";
		} catch (TOSFileException e) {
			  return "Unable to delete file.";
		} catch (NotFoundException e) {
			  return "No such file.";
		} catch (tos.api.InvalidPasswordException e) {
			  return "Invalid password.";
		} catch (InvalidDirectoryException e) {
			  return "Use the rmdir command for directories.";
		}
		return "File deleted.";
	}

	/** Implements the <i>dir</i> command.
	 * @param args Argument list.
	 */
	String Dir(String args[])
	{
		if (args.length!=1 && args.length!=2)
			return ("Usage: dir|ls [dirname]");
		String dirname;
		if (args.length==1)
			dirname = workdir;
		else
			dirname = absoluteName(args[1]);
		TOSFile dir;
		try {
			dir = new TOSFile(launcher);
			return dir.dir(dirname);
		} catch (InvalidDirectoryException f) {
			f.printStackTrace();
			return dirname + " is not a directory.";
		} catch (Exception g) {
			g.printStackTrace();
			return "Error opening directory.";
		}
	}
	
	/** Implements the <i>disconnect</i> command.
	 * <p>This is carried out merely by setting  
	 * the boolean <code>isConnected</code> to <code>false</code>.
	 * The launcher connection is not touched until <code>connect</code>
	 * is called again.
	 * @param args Argument list.
	 */
	String Disconnect(String args[])
	{
		if (args.length!=1)
			return ("The disconnect command takes no parameters.");
		isConnected = false;
		workdir = "";
		return ("Disconnected.");

	}
	
	/** Implements the <i>edit</i> command.
	 * <p>Invokes the TOS editor.  The editor handles the 
	 * matter of determining if the filename and password
	 * entered are valid.
	 * @param args Argument list.
	 */
	String Edit(String args[])
	{
		if (args.length!=2 && args.length!=3)
			return ("Usage: edit filename [password]");
		String tosfilename = absoluteName(args[1]);
		String password = "";
		if (args.length==3)
			password = args[2];
		Editor editor = new Editor(tosfilename,password,launcher);
		return "Editor launched.";		
	}
	
	/** Implements the <i>exec</i> command.
	 * <p>The function works by reading in the file's contents
	 * as a series of lines and passing them to <code>InterpretCommand</code> 
	 * one by one.
	 * @param args Argument list.
	 */
	String Exec(String args[])
	{
		// Syntax for scripts will be:
		// each statement on a separate line
		// there will be no control-of-flow facilities or arguments
		
		if (args.length!=2 && args.length!=3)
			return ("Usage: exec scriptname [password]");
		String tosfilename = args[1];
		if (!tosfilename.endsWith(".scr"))
			tosfilename = tosfilename + ".scr";
		String password;
		if (args.length==3)
			password = args[2];
		else
			password = "";
		TOSFile tosfile;
		try {
			tosfile = new TOSFile(launcher);
			System.out.println("opening file: "+tosfilename);
			openFile(tosfile,tosfilename,"r",password);
		} catch (InvalidPasswordException e) {
			return "Invalid password.";
		} catch (NotFoundException e) {
			return "No such script.";
		} catch (Exception e) {
			 return "Could not open file.";
		}
		String cmd;
		String resp;
		while (true)
		{
			try {
				cmd = tosfile.readln();
				resp = InterpretCommand(cmd);
				System.out.println(resp);
			} catch (EOFException e) {
				break;
			} catch (InvalidModeException e) {
				  // won't happen - we checked already
			} catch (Exception e) {
				return e.getMessage();
			}
		}
		return "Script finished.";
	}

	/** Implements the <i>exit</i> command.
	 * @param args Argument list.
	 */
	String Exit(String args[])
	{
		if (args.length!=1)
			return ("The exit command takes no parameters.");
		System.exit(0);
		return ("");

	}

	/** Implements the <i>export</i> command.
	 * <p>The challenge of this function is that it must 
	 * handle both text and binary files.  Java provides
	 * this functionality via the <code>OutputStreamWriter</code>
	 * classes.
	 * <p>User programs may carry out similar operation using
	 * the TOS file stream classes.
	 * @param args Argument list.
	 * @see TOSFileOutputStream
	 * @see TOSFileWriter
	 */
	String Export(String args[])
	{
		if (args.length!=4 && args.length!=5)
			return ("Usage: export TOS-filename host-filename filemode [password]");
		String hostfilename = args[2];
		String tosfilename = args[1];
		String filemode = args[3];
		if (!filemode.equals("text") && !filemode.equals("binary"))
			return "Must indicate text or binary file.";
		String password = "";
		if (args.length == 5)
			password = args[4];
		File hostfile = new File(hostfilename);
		String cantdoit = "Unable to write to host file";
		try {
			if (hostfile.exists())
				if (!hostfile.canWrite())
					return cantdoit;	
		} catch (SecurityException e) {
			return cantdoit;
		}
		TOSFile tosfile;
		try {
			tosfile = new TOSFile(launcher);
			openFile(tosfile,tosfilename,"r",password);
		} catch (Exception e) {
			  if (e instanceof NotFoundException)
				 return "TOSFile not found.";
			  else if (e instanceof InvalidPasswordException)
				  return "TOSFile not found.";
			  								 
			  return "Error opening TOS file.";
		}
		try {
			FileOutputStream ostream = new FileOutputStream(hostfile);
			if (filemode.equals("binary"))
			{
				TOSFileOutputStream tostream = new TOSFileOutputStream(tosfile,ostream);
				tostream.readFile();
			}
			else // text
			{
				OutputStreamWriter writer = new OutputStreamWriter(ostream);
				TOSFileWriter twriter = new TOSFileWriter(tosfile,writer);
				twriter.readFile();
				writer.close();
			}
			ostream.close();
			tosfile.close();
		} catch (Exception e) {
			
			return "Error exporting file.";
		}
		return ("File exported successfully.");
	}

	/** Implements the <i>import</i> command.
	 * <p>User programs may carry out similar operation using
	 * the TOS file stream classes.
	 * @param args Argument list.
	 * @see TOSFileInputStream
	 * @see TOSFileReader
	 */
	String Import(String args[])
	{
		if (args.length!=4 && args.length !=5)
			return ("Usage: import host-filename TOS-filename filemode [password]");
		String hostfilename = args[1];
		String tosfilename = args[2];
		String filemode = args[3];
		if (!filemode.equals("text") && !filemode.equals("binary"))
			return "Must indicate text or binary file.";
		String password = "";
		if (args.length == 5)
			password = args[4];
		File hostfile = new File(hostfilename);
		FileInputStream istream;
		String cantdoit = "Unable to read host file";
		try {
			if (!hostfile.canRead())
				return cantdoit;
			istream = new FileInputStream(hostfile);
		} catch (Exception e) {
			return cantdoit;
		}
		TOSFile tosfile;
		try {
			tosfile = new TOSFile(launcher);
			openFile(tosfile,tosfilename,"w",password);
		} catch (InvalidPasswordException e) {
			return "Invalid password.";
		} catch (Exception e) {
		    return "Error opening TOS file.";
		}
		try {
			if (filemode.equals("binary"))
			{
				TOSFileInputStream tistream = new TOSFileInputStream(tosfile,istream);
				tistream.writeFile();
			}
			else // text
			{
				InputStreamReader reader = new InputStreamReader(istream);
				TOSFileReader treader = new TOSFileReader(tosfile,reader);			
				treader.writeFile();
				reader.close();
			}
			istream.close();
			tosfile.close();


		} catch (Exception e) {
			
			return "Error importing file.";
		}
		return ("File imported successfully.");

	}

	/** Implements the <i>javac</i> command.
	 * <p>The function starts the compiler in a separate process,
	 * but it connects the System.in and System.out streams
	 * to those of the process.  It also starts a separate thread
	 * in the private <code>printerr</code> class which reads in
	 * from the process' error stream and writes it to System.err.
	 * @param args Argument list.
	 */
	String Javac(String args[])
	{
		if (compiler.equals(""))
			return "No compiler registered.  Use the regcomp command.";
		if (args.length<2)
			return ("Usage: javac classname [compiler-options]");
		String classname = args[1];
		if (!classname.endsWith(".java"))
			classname = classname + ".java";
		int lastsep = classname.lastIndexOf(TOSFile.separator);
		String barename = classname.substring(lastsep+1);
		String exargs[] = {"Export",classname,barename,"text"};
		Export(exargs);
		String cmd = compiler + " " + classname;
		if (args.length>2)
		{
			for (int i=2; i<args.length; i++)
				cmd = cmd + " " + args[i];
		}
		try {
			runProcess(cmd);
		} catch (IOException e) {
			return "Unable to run compiler.";
		}
		barename = barename.substring(0,barename.length()-5);
		String imparray[] = {"Import",barename+".class",barename+".class","binary"};
		Import(imparray);
		File javafile = new File(barename+".java");
		javafile.delete();
		File classfile = new File(barename+".class");
		classfile.delete();
		return "Compiler execution completed.";
	}
	
	
	/** Class to implement thread handling the display of the forked process' error stream on the screen.  */
	class printerr implements Runnable
	{
		/** Character reader of the input stream */
		InputStreamReader reader;
		
		/** Constructor.
		 * @param input stream to read from.
		 */
		printerr(InputStream istr)
		{
			reader = new InputStreamReader(istr);
			
		}
		
		/** Runs the thread.
		 * The thread continually reads a character from the reader
		 * and writes it to System.err.
		 */
		public void run()
		{
			char mychar;
			while (true)
			{
				try {
					mychar = (char)reader.read();
					System.err.write(mychar);
				} catch (IOException e) {
					System.err.println("Error reading from standard error");
				}
			}
		}
	}
	
	/** Runs a process synchronously.
	 * The process is run by attaching its stdin and stdout to those
	 * of the console's.  Stderr is handled by using an instance
	 * of the <code>printerr</code> class.
	 * @param cmd Command line of new process.
	 * @exception IOException if any of the streams could not be handled.
	 */
	void runProcess(String cmd) throws IOException
	{
		Process proc = Runtime.getRuntime().exec(cmd);

		InputStream oldi = System.in;
		PrintStream oldo = System.out;
		System.setIn(proc.getInputStream());
		System.setOut(new PrintStream(proc.getOutputStream()));
		Thread errthread = new Thread(new printerr(proc.getErrorStream()));
		errthread.start();
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			System.out.println("Error - forked process could not complete.");
		}
		errthread.stop();
		System.setIn(oldi);
		System.setOut(oldo);
	}		
			
	/** Implements the <i>mkdir</i> or <i>md</i> command.
	 * @param args Argument list.
	 */
	String Mkdir(String args[])
	{
		if (args.length!=2)
			return ("Usage: mkdir|md dirname");
		String dirname = args[1];
		if (dirname.indexOf(TOSFile.separator)>=0)
			return "File separator character not permitted in directory names.";
		TOSFile newdir;
		try {
			newdir = new TOSFile(launcher);
			newdir.mkdir(absoluteName(dirname));		
		} catch (Exception e) {
			
			return e.toString();
		}
		return "Directory created.";
	}

	/** Implements the <i>move</i> or <i>mv</i> command.
	 * @param args Argument list.
	 */
	String Move(String args[])
	{
		if (args.length!=3)
			return ("Usage: move|mv source-file destination-file");
		String source = absoluteName(args[1]);
		String dest = absoluteName(args[2]);
		if (source.equals(dest))
			return "Cannot move file onto itself.";
		String[] passwords = getPasswords();
		String copyout = doCopy(source,dest,passwords);
		if (!copyout.equals("File copied."))
			return copyout;
		String delargs[] = new String[3];
		delargs[0] = "Delete";
		delargs[1] = args[1];
		delargs[2] = passwords[0];
		String delout = Delete(delargs);
		if (!delout.equals("File deleted."))
			return delout;
		return "File moved.";
	}
	
	/** Implements the <i>protect</i> command.
	 * <p> This function only performs validation; like
	 * most console functions, <code>TOSFile</code> contains
	 * most of the functionality.
	 * @param args Argument list.
	 * @see TOSFile#protect
	 */
	String Protect(String args[])
	{
		if (args.length!=2 && args.length!=3)
			return ("Usage: protect filename [oldpassword]");
		String oldpassword = "";
		if (args.length == 3)
			oldpassword = args[2];
		int numread = 0;
		System.out.print("Enter password: ");
		char[] cmd = new char[255];
		try {
			numread = stdin.read(cmd,0,80);
		} catch (IOException e) {
			System.out.println("Error reading from standard input.");
		}
		String password = new String(cmd,0,numread-2);
		if (password.indexOf(' ')!=-1)
			return "Space character not permitted in passwords.";
		TOSFile file;
		String filename = args[1];
		try {
			file = new TOSFile(launcher);
			openFile(file,filename,"r",oldpassword);
			file.protectFile(password);
			file.close();
		} catch (Exception e) {
			return "Error protecting file.";
		}
		return "Protection has been assigned.";
	}

	/** Implements the <i>regcomp</i> command.
	 * @param args Argument list.
	 */
	String Regcomp(String args[])
	{
		if (args.length!=2)
			return ("Usage: regcomp appname");
		compiler = args[1];
		return "Registered compiler is " + compiler;
	}

	/** Implements the <i>rmdir</i> command.
	 * Uses the <code>dir</code> command to ensure that the
	 * directory is empty; if it is, the directory is removed.
	 * @param args Argument list.
	 */
	String Rmdir(String args[])
	{
		if (args.length!=2 && args.length!=3)
			return ("Usage: rmdir dirname [password]");
		String dirname = absoluteName(args[1]);
		String password = "";
		if (args.length==3)
			password = args[2];
		try {
			TOSFile dir = new TOSFile(launcher);
			dir.open(dirname,"r",password);
			if (!dir.isDirectory())
				return dirname + " is not a directory.";
			dir.close();
			String contents = dir.dir(dirname);
			if (!contents.equals(""))
				return "Directory " + dirname + " is not empty.";
			dir.delete(dirname,password);
			return ("Directory deleted.");
		} catch (NotFoundException e) {
			  return "Directory not found.";
		} catch (Exception e) {
			return "Error deleting directory.";
		} 
	}

	/** Implements the <i>run</i> command.
	 * <p>This function is only operable on Windows and 
	 * UNIX systems, as it makes use of host-dependent 
	 * environment variables to invoke the Java interpreter.
	 * @param args Argument list.
	 */
	String Run(String args[])
	{
		String classname = args[1];
		String noext;
	    if (!classname.endsWith(".class"))
			classname = classname + ".class";
		
		int pos = classname.lastIndexOf(TOSFile.separator);
		String barename = classname.substring(pos+1);
		
		//check for background operator
		boolean isBack = false;
		int arglen = args.length;
		if (args[arglen-1].equals("&"))
		{
			isBack = true;
			arglen--;
		}
		// export the class file
		String[] exportargs = new String[4];
		exportargs[0] = "Export";
		exportargs[1] = classname;
		exportargs[2] = barename;
		exportargs[3] = "binary";
		Export(exportargs);
		String arglist = "";
		for (int i=2; i<arglen; i++)
			arglist = arglist + " " + args[i];

		// remove the .class extension
		barename = barename.substring(0,barename.length()-6);
		
		// THIS PART IS NOT MACINTOSH COMPATIBLE!!
		// WHO CARES - CONSOLE DOESN'T WORK ON MAC ANYWAY
	
		String jcmd;
		String os = System.getProperty("os.name");
		if (os.indexOf("Win")!=-1)
			jcmd = "%JAVA%";
		else // WILL ONLY WORK ON UNIX!
			jcmd = "$JAVA";
		
		try {
			System.out.println(jcmd + " " + barename + arglist);
			if (!isBack)
				runProcess(jcmd + " " + barename + arglist);
			else
				Runtime.getRuntime().exec(jcmd + " " + barename + arglist);
			} catch (IOException e) {
			e.printStackTrace();
			return "Unable to run class.";
		}
			
		File classfile = new File(classname);
		classfile.delete();
		return "Class " +  barename + " has finished executing.";
		
	}

	/** Opens a file.
	 * <p> This is a convenience function that converts a relative name
	 * to absolute before opening a file.
	 * @param file TOS file to open.
	 * @param tosfilename Name of the file.
	 * @param mode Mode to open with.
	 * @param password Password.
	 * @exception NotFoundException if the file or server cannot be found.
	 * @exception InvalidPasswordException if the wrong password is supplied.
	 * @exception InvalidModeException if the wrong mode is supplied.
	 * @exception TOSFileException for other errors.
	 */
	protected void openFile
	(TOSFile file, String tosfilename, String mode, String password) 
	throws NotFoundException, InvalidPasswordException, InvalidModeException, TOSFileException
	{
		String fullfilename = absoluteName(tosfilename);
		file.open(fullfilename, mode, password);
	}

}
