package tos.system;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import com.sun.java.swing.*;
import tos.api.*;

import javax.swing.*;

/** Many of the dialogs displayed in the TOS Administrator have
 * fundamentally the same functionality.  This abstract class
 * encapsulates these.
 * 
 * AdminDlg dialogs are laid out using a <code>GridBagLayout</code> with 
 * four rows.  On the fourth row there are two buttons, an <i>OK</i> 
 * button whose text is determined at instantiation time, and a 
 * <i>Close</i> button whose intent is to exit the dialog 
 * without performing any action.
 * 
 * The class also implements the <code>ActionListener</code> interface so 
 * that it can easily handle button-pressing or other dialog-generated events.
 * 
 * This fixes the size of the dialog at 300 x 200 pixels, a size
 * optimized for display on 800 x 600 displays.  The dialog is placed 150
 * pixels to the right of and 150 down from the top left corner of its parent 
 * window, the Administrator main frame window.
 * 
 * This class, like all TOS GUI components, is implemented using the
 * JFC/Swing library.  See the Swing documentation for more information.
 */

abstract class AdminDlg extends ConstrainDlg implements ActionListener
{
	/** Shorthand for pressing the OK button.	 */
	static int OK = 1;
	
	/** Shorthand for pressing the Close button. */
	static int CLOSE = 0;
	
	/** Value pressed by the user to exit the dialog.	 */
	int retval;
	
	/** Bottom blank panel, just above the bottom row */
	JPanel botblank;
	
	/** Panel to hold the buttons.	 */
	JPanel butpanel;
	
	/** Blank panel to provide space between the buttons. */
	JPanel blank;
	
	/** OK button. */
	JButton ok;
	
	/** Close button. */
	JButton close;
		
	/** Constructor.
	 * This draws the dialog but does not display it.  The blank 
	 * space in the middle will be further drawn upon by subclasses.
	 * @param frame Parent frame.
	 * @param title Title of dialog.
	 * @param func Text of OK button.
	 */
	public AdminDlg(Frame frame, String title, String func)
	{
		super(frame,title,true);
		setSize(300,200);
		setLocation(Administrator.x+150,Administrator.y+100);
		cpane.setLayout(layout);
		botblank = new JPanel();
		butpanel = new JPanel();
		butpanel.setLayout(new GridLayout(1,3));
		ok = new JButton(func);
		close = new JButton("Close");
		blank = new JPanel();
		constrain(botblank,0,3,2,1);
		butpanel.add(ok);
		butpanel.add(blank);
		butpanel.add(close);
		constrain(butpanel,0,4,2,1);
		ok.setActionCommand("OK");
		close.setActionCommand("Close");
		ok.addActionListener(this);
		close.addActionListener(this);
	}
	

	
	/** Handler for AWT action events.
	 * The only AWT events that will actually appear are
	 * those generated from a button press.
	 * @param e Event received.
	 */
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		if (cmd.equals("OK"))
			OnOK();
		else if (cmd.equals("Close"))
			OnClose();
		dispose();
	}
	
	/** Called when the OK button pressed. 
	 * Sets <code>retval</code> accordingly.
	 */
	void OnOK()
	{
		retval = AdminDlg.OK;
	}
	
	/** Called when the Close button pressed. 
	 * Sets <code>retval</code> accordingly.
	 */
	void OnClose()
	{
		retval = AdminDlg.CLOSE;
	}


	
}