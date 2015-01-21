package tos.system;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import com.sun.java.swing.*;
import tos.api.*;

import javax.swing.*;

/** This dialog is used to allow users to enter in the mount point
 * to which a disk is to be mapped.  The mount point must be an
 * absolute file name.
 */

class MountDlg extends ConstrainDlg implements ActionListener
{
	/** Shorthand for the Mount button. */
	static int MOUNT = 1;
	
	/** Shorthand for the Cancel button. */
	static int CANCEL = 2;
	
	/* Indicates whether the user pressed Mount or Cancel. */
	int retval;
	
	/** The name of the disk. */
	String servername;
	
	/** The new mount point.*/
	String newmountpt;
	
	/** Combo box allowing the user to select a disk.*/
	JComboBox mountbox;
	
	/** Text field allowing the user to enter a mount point. */
	JTextField mountfield = new JTextField();
	
	/** Label of the combo box. */
	JLabel boxlabel = new JLabel("Disk: ");
	
	/** Label of the text field.	 */
	JLabel fieldlabel = new JLabel("Mount point: ");
	
	/** Blank panel between rows.  */
	JPanel blankpanel = new JPanel();
	
	/** Panel for the buttons to be laid out on. */
	JPanel butpanel = new JPanel();
	
	/** The Mount button. */
	JButton mount = new JButton("Mount");
	
	/** The Cancel button. */
	JButton cancel = new JButton("Cancel");
	
	/** Draws the components onto the dialog.
	 * @param frame Parent frame
	 * @param serverlist List of disks.
	 */
	public MountDlg(Frame frame, Vector serverlist)
	{
		super(frame,"Mount Disk",true);
		setSize(300,200);
		setLocation(Administrator.x+150,Administrator.y+100);
		mountbox = new JComboBox(serverlist);
		cpane.setLayout(layout);
		constrain(boxlabel,0,0,1,1);
		constrain(mountbox,1,0,1,1);
		constrain(fieldlabel,0,1,1,1);
		constrain(mountfield,1,1,1,1);
		constrain(blankpanel,0,2,2,1);
		butpanel.setLayout(new GridLayout(1,3));
		butpanel.add(mount);
		butpanel.add(new JPanel());
		butpanel.add(cancel);
		constrain(butpanel,0,3,2,1);
		mount.setActionCommand("Mount");
		cancel.setActionCommand("Cancel");
		mount.addActionListener(this);
		cancel.addActionListener(this);
		
	}
	
	/** Handler for AWT action events.
	 * The only AWT events that will actually appear are
	 * those generated from a button press.
	 * @param e Event received.
	 */
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		if (cmd.equals("Mount"))
			OnMount();
		else if (cmd.equals("Cancel"))
			OnCancel();
	}
	
	/** Called when the user presses the Mount button.
	 * Retrieves the values from the entry components, and ensures that
	 * the mount point is an absolute path name.  The path name itself
	 * is <b>not</b> verified at this point.
	 */
	void OnMount()
	{
		retval = MOUNT;
		servername = (String)mountbox.getSelectedItem();
		newmountpt = mountfield.getText();
		if (!newmountpt.substring(0,TOSFile.separator.length()).equals(TOSFile.separator))
		{
			JOptionPane.showMessageDialog(this,"Mount point must be absolute path name.","Bad format",JOptionPane.ERROR_MESSAGE);
			return;
		}
		dispose();

	}
	
	/** Exits the dialog when the user presses the Cancel button. */
	void OnCancel()
	{
		retval = CANCEL;
		cancel.removeActionListener(this);
		dispose();
	}
}
