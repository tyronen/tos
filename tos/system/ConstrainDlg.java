package tos.system;

import java.awt.*;

import javax.swing.*;

/** Abstract class encapsulating the third-party <code>constrain</code> function.
 * This class is inherited by all dialog classes in the TOS GUI, to enable
 * them to easily make use of the AWT <code>GridBagLayout</code> class.
 */

abstract class ConstrainDlg extends JDialog
{
	/** Dialog layout. */
	GridBagLayout layout = new GridBagLayout();
	
	/** Auxiliary structure used by the layout. */
	GridBagConstraints con = new GridBagConstraints();
	
	/** ContentPane of the dialog.  
	 * JFC requires components be added to the content pane, not the
	 * dialog itself.
	 */
	Container cpane = getContentPane();

	/** Constructor, wraps <code>JDialog</code> constructor.
	 */
	public ConstrainDlg(Frame frame, String title, boolean isModal)
	{
		super(frame,title,isModal);
	}
	
	/** A simple-to-use method for placing components onto a <code>GridBagLayout</code>. 
	 * 
	 * The component is placed in column <code>x</code>, row <code>y</code>
	 * and overlaps other boxes in the grid to take up a total size of 
	 * <code>width</code> by <code>height</code> boxes.
	 * 
	 * This function adapted from code obtained from
	 * http://www.cdt.luth.se/~mattias/java102/jws/examples/layout/LayoutEx_5.java
	 * Copyright (c) 1996 Centre for Distance-spanning Technology/CDT,
	 * All Rights Reserved.
	 * 
	 * @param comp Component to add.
	 * @param x Column.
	 * @param y Row.
	 * @param width Number of columns.
	 * @param height Number of rows.
	 */
	
	void constrain(Component comp, int x, int y, int width, int height)
	{
		con.gridx = x; 
		con.gridy = y;
		con.gridwidth = width; 
		con.gridheight = height;
		con.fill = GridBagConstraints.BOTH;
	    layout.setConstraints(comp, con);
	    cpane.add(comp);
	}

}
