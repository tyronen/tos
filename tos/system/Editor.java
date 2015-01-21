package tos.system;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import com.sun.java.swing.*;
import tos.api.*;

import javax.swing.*;

/** The TOS editor is a very simple GUI editor to allow easy
 * creation and editing of text files from within the TOS console
 * environment.  For more information, see the 
 * <a href="../Userguide.html">User's Guide</a>.
 */


class Editor extends JFrame implements ActionListener
{
	/** JFC text editor component. */
	JEditorPane edit = new JEditorPane();
	
	/** Menu bar.  */
	JMenuBar menubar = new JMenuBar();
	
	/** File menu. */
	JMenu fileMenu = new JMenu("File");

	/** Save menu item. */
	JMenuItem save = new JMenuItem("Save");

	/** Quit menu item. */
	JMenuItem quit = new JMenuItem("Quit");
	
	/** Launcher to connect to. */
	TOSLauncher launcher;
	
	/** Name of TOS file being edited. */
	String filename;
	
	/** Password of that file. */
	String password;
	
	/** Set to <code>true</code> if the file has been saved. */
	boolean isSaved = false;
	
	/** Constructor.
	 * <p>The constructor sets up the global instance data, then 
	 * reads the TOS file into the display buffer.
	 * @param filename Name of TOS file being edited.
	 * @param password Password of that file. 
	 * @param launcher Launcher to connect to. 
	 */
	public Editor(String filename, String password, TOSLauncher launcher)
	{
		super("TOS Editor");
		Debug.setLookAndFeel();
		this.launcher = launcher;
		this.filename = filename;
		this.password = password;
		save.setActionCommand("Save");
		quit.setActionCommand("Quit");
		fileMenu.add(save);
		fileMenu.add(quit);
		menubar.add(fileMenu);
		getRootPane().setJMenuBar(menubar);
		edit.setContentType("text/plain");
		getContentPane().add(edit);
		char[] filetext;
		int i = 0;
		int size = 0;
		String text;
		try {
			TOSFile file = new TOSFile(launcher);
			file.open(filename,"r",password);
			size = file.getSize();
			filetext = new char[size+1];
			try {
				for (i=0; i<size; i++)
					filetext[i] = file.readChar();
			} catch (EOFException e) {

			}
			file.close();
			text = new String(filetext);
		} catch (NotFoundException e) {
			// no problem, just create a new file
			text = new String("");
		} catch (Exception e) {
			Debug.ErrorMessage("Error","Error opening file.");
			return;
		}
		System.setErr(new NullPrintStream());
		edit.setText(text);
		save.addActionListener(this);
		quit.addActionListener(this);
		addWindowListener(new AdminListener(this));
		addKeyListener(new EditKeyListener(this));
		setSize(480,320);
		setLocation(100,100);
		setVisible(true);
	}
	
	/** Inner class to handle window events. */
	protected class AdminListener extends WindowAdapter
	{
		Editor editor;
		public AdminListener(Editor editor)
		{
			super();
			this.editor = editor;
		}
		
		public void windowClosing(WindowEvent evt)
		{
			editor.OnQuit();
		}
	}
	
	/** Inner class to handle key events.
	 * <p>Pressing any key marks the file as not having
	 * been saved.
	 */
	protected class EditKeyListener extends KeyAdapter
	{
		Editor editor;
		EditKeyListener(Editor editor) 
		{ 
			super();
			this.editor = editor;
		}
	
		void KeyPressed(KeyEvent e) 
		{ 
			editor.isSaved = false;
		}	
	}

	/** Action event handler.
	 * @param e Action event.
	 */
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		if (cmd.equals("Save"))
			OnSave();
		else if (cmd.equals("Quit"))
			OnQuit();
	}
	
	/** Saves the file.	 */
	void OnSave()
	{
		String edittext = edit.getText();
		try {
			TOSFile file = new TOSFile(launcher);
			file.open(filename,"w",password);
			file.writeString(edittext);
			file.close();
			isSaved = true;
		} catch (Exception e) {
			Debug.ErrorMessage("Error",e.toString());
		}
	}
	
	/** Quits the editor, prompting to save first if unsaved. */
	void OnQuit()
	{
		int retval;
		if (!isSaved)
		{
			retval = JOptionPane.showConfirmDialog(this,"Save before quitting?");
			if (retval == JOptionPane.YES_OPTION)
				OnSave();
			else if (retval == JOptionPane.CANCEL_OPTION)
				return;
		}
		isSaved = true;
		disableEvents(AWTEvent.MOUSE_EVENT_MASK);
		dispose();
		System.setErr(System.out);
	}
				

}
