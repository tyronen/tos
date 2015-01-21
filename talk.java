/*

here's the basic idea.

Proc 1 establishes a pipe
Proc 2 connects to the pipe.
Each process has a loop

pipe.read(instr);
System.out.println(instr);
System.in.readln(outstr);
pipe.write(outstr);
otherside.release();
myside.wait();

pipe negotiation

first guy creates pipe, if he can create he makes
signal 1

if he can't, he makes signal 2


*/
import java.io.*;
import tos.api.*;

public class talk extends TOSProcess
{
	protected InputStreamReader stdin;
	
	public static void main(String[] args)
	{
		talk mytalk = new talk(args);
	}
	
	public talk(Object[] parameterList)
	{
		super(parameterList);
	}
	
	public void init(Object[] parameterList)
	{
		try {
			stdin = new InputStreamReader(System.in);
			Signal sig1, sig2;
			Pipe pipe = newPipe("pipe");
			byte[] buffer = new byte[80];
			try {
				pipe.create();
				sig1 = openSignal("sig1");
				sig2 = openSignal("sig2");
				System.out.print("> ");
				String nextln = readline();
				buffer = nextln.getBytes();
				pipe.write(buffer,buffer.length);
			} catch (PipeExistsException e) {
				pipe.connect();
				sig1 = openSignal("sig2");
				sig2 = openSignal("sig1");
			}
			while (true)
			{
				sig1.Release();
				sig2.Wait();
				buffer = pipe.read(80);
				System.out.println(new String(buffer));
				System.out.print("> ");
				String nextln = readline();
				buffer = nextln.getBytes();
				pipe.write(buffer,buffer.length);
			}	
		} catch (Throwable e) {
			System.out.println(e);
			System.out.println("Exiting...");
		}
	}
	
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

}
