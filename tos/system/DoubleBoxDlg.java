package tos.system;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import com.sun.java.swing.*;

import javax.swing.*;

/** This class supplies a dialog box that has two combo
 * boxes, an OK and Close button.  The contents of 
 * the bottom combo box are changed when the top selection
 * is changed.
 * 
 * Implements the <code>ItemListener</code> interface for
 * events indicating the change in the top combo box.
 */

class DoubleBoxDlg extends AdminDlg implements ItemListener
{
	/** Label of the first combo box. */
	JLabel toplabel;
	
	/** The first combo box. */
	JComboBox topbox;
	
	/** Label of the second combo box. */
	JLabel midlabel;
	
	/** The second combo box. */
	JComboBox midbox;
	
	/** Blank panel between the two rows. */
	JPanel midblank;
	
	/** Final selection in the first box. */
	String topselection;
	
	/** Final selection in the second box.  */
	String midselection;
	
	/** Hashtable of first-box objects to a vectors of second-box objects */
	Hashtable midlist;
	
	/** Constructor.
	 * Draws this class' components after the parent dialog
	 * has been drawn.  The labels are right-aligned, each label
	 * and combo box occupying a two-column grid pattern.
	 * @param frame Parent frame.
	 * @param title Text of dialog title.
	 * @param func Text of OK button.
	 * @param toplist Set of items in first combo box.
	 * @param toplist Set of items in second combo box, mapped to items in first.
	 * @param topstr Label of first combo box.
	 * @param midstr Label of second combo box.
	 */
	DoubleBoxDlg(Frame frame, String title, String func,
					Vector toplist, Hashtable midlist, 
					String topstr, String midstr)
	{
		super(frame,title,func);
		toplabel = new JLabel(topstr);
		topbox = new JComboBox(toplist);
		topbox.setEditable(false);
		midlabel = new JLabel(midstr);
		midbox = new JComboBox();
		midbox.setEditable(false);
		midblank = new JPanel();
		toplabel.setHorizontalAlignment(SwingConstants.RIGHT);
		midlabel.setHorizontalAlignment(SwingConstants.RIGHT);
		constrain(toplabel,0,0,1,1);
		constrain(topbox,1,0,1,1);
		constrain(midblank,0,1,2,1);
		constrain(midlabel,0,2,1,1);
		constrain(midbox,1,2,1,1);
		topbox.addItemListener(this);
		this.midlist = midlist;
	}
	
	/** Called when the OK button is pressed.
	 * Places the selections of the combo boxes into 
	 * their proper variables.
	 */
	void OnOK()
	{
		topselection = (String)topbox.getSelectedItem();
		midselection = (String)midbox.getSelectedItem();
		super.OnOK();
	}
	
	/* Called when the item's top state is changed.
	 * Loads a new set of data into the second combo box, using the 
	 * vector mapped to by the item in the first box.
	 */
	public void itemStateChanged(ItemEvent e) 
	{
		String topsel = (String)e.getItem();
		Vector newvec = (Vector)midlist.get(topsel);
		midbox.removeAllItems();
		for (int i=0; i<newvec.size(); i++)
			midbox.addItem(newvec.elementAt(i));
	}

		
}
