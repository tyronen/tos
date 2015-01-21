package tos.system;

import java.io.*;
import com.sun.java.swing.*;

import javax.swing.*;

/** This class is a utility class for displaying error messages
 * without having to deal with the intricacies of the 
 * com.sun.java.swing. JOptionPane class and its static 
 * methods.
 * 
 * Messages are displayed in a message box for Windows and Macintosh systems.
 * For UNIX systems, however, they are written to a log file, since UNIX
 * sessions are often run out of a telnet connection with no GUI display 
 * facilities.
 */
class Debug
{
	
	/** Set to <code>true</code> if error output is to be shown 
	 * in GUI format.
	 */
	static boolean UseGUIDisplay = true;
	
    /** This method, intended to be run in the <code>main()</code>
     * function of a running server process, sets the Swing pluggable
     * look and feel to one matching the host environment.  If the
     * environment is not one of UNIX, Macintosh, or Windows, the
     * Motif environment is used.
     * 
     */
	static void setLookAndFeel()
	{
		try {
			String os = System.getProperty("os.name");
			String lf;
			if (os.indexOf("Win")!=-1)
				lf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
			else if (os.indexOf("Mac")!=-1)
				lf = "com.sun.java.swing.plaf.mac.MacLookAndFeel";
			else
			{
				lf = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
				UseGUIDisplay = false;
				return;
			}
			UIManager.setLookAndFeel(lf);
		} catch (NullPointerException e) {
			// we just use the default			
		} catch (Exception e) {
			
			// we use the default
		}
		
	}

	
    /** Displays a simple message box with an information symbol.
     * 
     * @param message Message to be shown in the body of the message box.
     * @param title Title of the message box.
     */
	static void InfoMessage(String title, String message)
	{
		if (UseGUIDisplay)
			JOptionPane.showMessageDialog(null,message,title,JOptionPane.INFORMATION_MESSAGE);
		else
			System.out.println(title + ":" + message);
	}
	
    /** Displays a simple message box with an error symbol (a white
     * X in a red circle)
     * 
     * @param message Message to display in the box.
     * @param title Title of the box.
     */
	static void ErrorMessage(String title, String message)
	{
		if (UseGUIDisplay)
			JOptionPane.showMessageDialog(null,message,title,JOptionPane.ERROR_MESSAGE);
		else
			System.out.println(title + ":" + message);
	}

    /** Displays a complete stack dump of a thrown exception or error.
     * 
     * @param title Title of the message box.
     * @param e Exception or error to parse.
     */
	static void DisplayException(String title,Throwable e)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw,true);
		e.printStackTrace(pw);
		ErrorMessage(title,sw.toString());
	}

}
