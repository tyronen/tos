package tos.system;

import java.awt.*;
import java.util.*;
import com.sun.java.swing.*;

import javax.swing.*;

/** This class supplies a dialog box with a single
 * combo box and OK and Close buttons.  It accepts
 * a <code>Vector</code> parameter to fill the combo 
 * box and reports the selection to the caller.
 */

class SingleBoxDlg extends AdminDlg
{	
	/** Label of the combo box */
	JLabel midlabel;
	
	/** The combo box. */
	JComboBox midbox;
	
	/** Blank panel above the combo box row.	 */
	JPanel midblank;
	
	/** Text of the selected item on dialog exit. */
	String selection;
	
	/** List of items in the combo box.	 */
	Vector list;

	/** Constructor.
	 * Draws this class' components after the parent dialog
	 * has been drawn.  The labels are right-aligned, the two
	 * occupying a two-column grid pattern.
	 * @param frame Parent frame.
	 * @param title Text of dialog title.
	 * @param func Text of OK button.
	 * @param label Label of combo box.
	 * @param list Set of items in combo box for user to choose from.
	 */
	SingleBoxDlg(Frame frame, String title, String func,
				 String label, Vector list)
	{
		super(frame,title,func);
		midlabel = new JLabel(label);
		midlabel.setHorizontalAlignment(SwingConstants.RIGHT);
		midbox = new JComboBox(list);
		midbox.setEditable(false);
		midblank = new JPanel();
		constrain(midblank,0,1,2,1);
		constrain(midlabel,0,2,1,1);
		constrain(midbox,1,2,1,1);	
		this.list = list;
	}
	
	/** Called when the OK button pressed.
	 * Reports the combo box's selected item.
	 */
	void OnOK()
	{
		selection = (String)midbox.getSelectedItem();
		super.OnOK();	
	}
}
