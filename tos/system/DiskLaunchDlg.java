//DiskLaunchDlg.java
package tos.system;

import java.awt.*;
import java.awt.event.*;
import com.sun.java.swing.*;

import javax.swing.*;

/** This dialog, which does not fit the two <code>AdminDlg</code>-based
 * templates, is used to enter the parameters for creating a new disk.
 */

public class DiskLaunchDlg extends ConstrainDlg implements ActionListener
{
	/** Set to <code>true</code> if the user has pressed the OK button. */
	boolean isOK = false;
	
	/** Name of new disk. */
	String name;
	
	/** Name of host of new disk. */
	String hostname;
	
	/** Maximum number of files. */
	int numfiles;
	
	/** Data block size. */
	int blocksize;
	
	/** Number of data blocks.  */
	int numblocks;
	
	/** Text field to enter disk name.	 */
	JTextField namebox = new JTextField();
	
	/** Text field to enter host name. */
	JTextField hostnamebox = new JTextField();
	
	/** Text field to enter number of files. */
	JTextField numfilesbox = new JTextField();
	
	/** Text field to enter block size. */
	JTextField blocksizebox = new JTextField();
	
	/** Text field to enter number of blocks. */
	JTextField numblocksbox = new JTextField();
	
	/** Label for name.	 */
	JLabel namelbl = new JLabel("Name: ",SwingConstants.RIGHT);

	/** Label for host.	 */
	JLabel hostlbl = new JLabel("Host: ",SwingConstants.RIGHT);
	
	/** Label for number of files.	 */
	JLabel filelbl = new JLabel("Max. number of files: ",SwingConstants.RIGHT);
	
	/** Label for block size.	 */
	JLabel bloclbl = new JLabel("Data block size: ",SwingConstants.RIGHT);
	
	/** Label for number of blocks.	 */
	JLabel numblbl = new JLabel("No. of data blocks ",SwingConstants.RIGHT);
	
	/** Blank panel to place the buttons on. */
	JPanel botpanel = new JPanel();

	/** OK button. */
	JButton ok = new JButton("OK");
	
	/** Cancel button. */
	JButton cancel = new JButton("Cancel");
	
	/** Constructor.
	 * <p>Draws the components on the screen.  The five text fields and their labels
	 * are placed each immediately below the one preceding it.  The two buttons 
	 * follow along the bottom row.
	 * @param frame Parent frame.
	 */
	public DiskLaunchDlg(Frame frame)
	{
		super(frame,"Create File Server",true);
		setSize(300,200);
		setLocation(Administrator.x+150,Administrator.y+100);
		cpane.setLayout(layout);
		constrain(namelbl,0,0,1,1);
		constrain(namebox,1,0,1,1);
		constrain(hostlbl,0,1,1,1);
		constrain(hostnamebox,1,1,1,1);
		constrain(filelbl,0,2,1,1);
		constrain(numfilesbox,1,2,1,1);
		constrain(bloclbl,0,3,1,1);
		constrain(blocksizebox,1,3,1,1);
		constrain(numblbl,0,4,1,1);
		constrain(numblocksbox,1,4,1,1);
		constrain(new JPanel(),0,5,2,1);
		botpanel.setLayout(new GridLayout(1,3));
		botpanel.add(ok);
		botpanel.add(new JPanel());
		botpanel.add(cancel);
		constrain(botpanel,0,6,2,1);
		ok.setActionCommand("OK");
		cancel.setActionCommand("Cancel");
		ok.addActionListener(this);
		cancel.addActionListener(this);
	}
	
	/** Handles action events.
	 * <p>If the OK button is pressed, the values of the variables are 
	 * updated, and checked to see if the numbers are in fact numeric
	 * text.  If Cancel is pressed, the dialog exits without saving 
	 * its contents.
	 * @param e The action that just occurred.
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals("OK"))
		{
			name = namebox.getText();
			hostname = hostnamebox.getText();
			try 
			{
				numfiles = (new Integer(numfilesbox.getText())).intValue();
				blocksize = (new Integer(blocksizebox.getText())).intValue();
				numblocks = (new Integer(numblocksbox.getText())).intValue();
			} catch (NumberFormatException f) {
				JOptionPane.showMessageDialog(this,"The number of files, block size, and number of blocks must be numbers.","Bad format",JOptionPane.ERROR_MESSAGE);
				return;
			}
			isOK = true;
			dispose();
		}
		else if (e.getActionCommand().equals("Cancel"))
		{
			dispose();
		}
	
	}
}
