/**
 * SequenceMatrix represents datasets in a matrix of sequences versus genes.
 * This gives it an edge against TaxonDNA, which really sees datasets as
 * a sequence-to-polypeptide map. SequenceMatrix can therefore *really*
 * import in GenBank and Nexus formats (it's import will, I suppose, be
 * something to behold). God only knows if it will 'live' within TaxonDNA
 * forever: one supposes that it'll eventually break free, but while a
 * lot of the code (DNA.*, for a big instance) is living together, it
 * probably makes a lot of sense to keep them joined at the hip.
 *
 * SequenceMatrix will, hopefully, be a much SMALLER program than TaxonDNA.
 * We will also experiment with Swing, just to see how that goes (and because
 * JTable would save work like nobody's business).
 *
 */

/*
 *
 *  SequenceMatrix
 *  Copyright (C) 2006 Gaurav Vaidya
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *  
 */

package com.ggvaidya.TaxonDNA.SequenceMatrix;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;	// for drag-n-drop
import java.awt.dnd.*;		// drag-n-drop
import java.io.*;
import java.util.*;

import javax.swing.*;		// "Come, thou Tortoise, when?"

import com.ggvaidya.TaxonDNA.Common.*;
import com.ggvaidya.TaxonDNA.DNA.*;
import com.ggvaidya.TaxonDNA.DNA.formats.*;
import com.ggvaidya.TaxonDNA.UI.*;

public class SequenceMatrix implements WindowListener, ActionListener, ItemListener, DropTargetListener, MouseListener {
	// The following variables create and track our AWT interface
	private Frame		mainFrame 		= new Frame();
	private JTable		mainTable		= null;	

	// Other components of SequenceMatrix
	private DataStore	dataStore		= new DataStore(this);
	private Preferences	prefs			= new Preferences(this);
	private Taxonsets	taxonSets		= new Taxonsets(this);
	private FileManager	fileMan			= new FileManager(this);
	private Exporter	exporter		= new Exporter(this);

//
//	1.	ENTRYPOINT. The entrypoint is where the entire SequenceMatrix system starts up.
//		As usual, you can just create a new SequenceMatrix to 'do everything'.
//
	/**
	 * SequenceMatrix's entry point. Our arguments are the files we must open
	 * initially.
	 *
	 * This is slighly redundant, but it allows us to "create" SequenceMatrices
	 * from other programs without much fuss.
	 */
	public static void main(String[] args) {
		Vector files	=	new Vector();

		for(int x = 0; x < args.length; x++) {
			files.add(new File(args[x]));	
		}

		new SequenceMatrix( files );
	}
	
//
// 	2.	CONSTRUCTORS.
//
	/**
	 * Creates a new SequenceMatrix object. We create a new toplevel.
	 * Be warned that we do NOT spawn a thread: if TaxonDNA wants
	 * to create a SequenceMatrix for whatever reason, it will have
	 * to do this IN a thread - otherwise when TaxonDNA exits,
	 * we will automatically exit as well.
	 *
	 * @param files A vector of files to load in.
	 */
	public SequenceMatrix(Vector files) {
		createUI();			// create our user interface

		// now load up all the files
		Iterator i = files.iterator();
		while(i.hasNext()) {
			File f = (File) i.next();

			fileMan.addFile(f);
		}

		resetFrameTitle();
	}

//
// 	3.	EVENT PROCESSING. Handles stuff which happens to the main frame, mostly.
// 		This involves menu, window and drop listening.
//
	/**
	 * Handles ActionEvents for the file menu and help menu.
	 */
	public void actionPerformed(ActionEvent evt) {
		String cmd = evt.getActionCommand();

		//
		// File -> New. Just close the present file. 
		// 
		if(cmd.equals("Clear all"))
			dataStore.clear();

		//
		// File -> Open. Tries to load the file specified.
		// The current file is closed before anything 
		// further is done.
		//
		if(cmd.equals("Add sequences"))
			fileMan.addFile();

		//
		// File -> Exit. Calls our exit() way out.
		//
		if(cmd.equals("Exit"))
			exit();	

		// Export -> Export table as tab delimited. Should be fairly
		// easy: we could just query the table and spit it out!
		if(cmd.equals("Export table as tab-delimited"))
			fileMan.exportTableAsTabDelimited();

		//
		// Export -> Export as NEXUS. Since TaxonDNA doesn't understand
		// complex sequences, we do all of these manually. Thankfully,
		// there's not a lot that needs to be done.
		//
		// We basically just 'export' this on to the MatrixModel,
		// who knows all about such things.
		//
		if(cmd.equals("Export sequences as NEXUS"))
			fileMan.exportAsNexus();

		//
		// Export -> Export as TNT
		// Exports the current dataset in TNT.
		//
		if(cmd.equals("Export sequences as TNT"))
			fileMan.exportAsTNT();

		//
		// Settings -> Preferences. Allows you to change how SequenceMatrix
		// works.
		//
		if(cmd.equals("Preferences"))
			prefs.go();

		//
		// Settings -> Taxonsets. Activate the Taxonsets system!
		if(cmd.equals("Taxonsets"))
			taxonSets.go();

		//
		// Help -> About. We should put something
		// up here, once we get proper documentation
		// working in the Help -> * menu.
		//
		if(cmd.equals("About SequenceMatrix")) {
			String copyrightNotice = new String(
					"You may cite this program as follows:\n\t" + getCitation() + "\n---\n" +
					getName() + ", Copyright (C) 2006 Gaurav Vaidya. \nSequenceMatrix comes with ABSOLUTELY NO WARRANTY. This is free software, and you are welcome to redistribute it under certain conditions; check the COPYING file you should have recieved along with this package.\n\n");
					
			MessageBox mb = new MessageBox(mainFrame, "About this program", copyrightNotice 
					+ "Written by Gaurav Vaidya\nIf I had time to put something interesting here, there'd be something in the help menu too. All apologies.\n\n"
					+ "This program was written with Vim (http://vim.org) with occasional help from Eclipse (http://eclipse.org/). Compilation was handled by Ant (http://ant.apache.org/)." 
			);
			mb.showMessageBox();
		}		
	}

	//
	// The following functions are part of the WindowListener stuff.
	// Hook if needed.
	//
	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}	
	/**
	 * If somebody tries to close the window, call our local exit() before closing the Window
	 */
	public void windowClosing(WindowEvent e) {
		exit();
	}

	//
	// The following functions are part of the Drop target listener stuff.
	//
	public void dragEnter(DropTargetDragEvent dtde) {
		if(dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			// A list of files!
			// Yummy!
			dtde.acceptDrag(DnDConstants.ACTION_COPY);
		} else {
			dtde.rejectDrag();
		}
	}
	public void dragExit(DropTargetEvent dte) {}
	public void dragOver(DropTargetDragEvent dtde) {
	}
	public void drop(DropTargetDropEvent dtde) {
		if(dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			// A list of files!
			// Yummy!
			dtde.acceptDrop(DnDConstants.ACTION_COPY);

			Transferable tf = 		dtde.getTransferable();
			java.util.List list = null;
			try {
				list =		(java.util.List) tf.getTransferData(DataFlavor.javaFileListFlavor);
			} catch(UnsupportedFlavorException e) {
				dtde.rejectDrop();
				return;	
			} catch(IOException e) {
				dtde.rejectDrop();
				return;
			}

			Iterator i = list.iterator();
			while(i.hasNext()) {
				File file = 	(File) i.next();

				fileMan.addFile(file);
			}

			dtde.dropComplete(true);
		} else {
			dtde.rejectDrop();
		}
	}
	public void dropActionChanged(DropTargetDragEvent dtde) {}

	private CheckboxMenuItem last_chmi = null;
	/**
	 * An item listener, used for the sort sub-menu
	 */
	public void	itemStateChanged(ItemEvent e) {
		CheckboxMenuItem chmi = (CheckboxMenuItem) e.getSource();

		if(last_chmi != null)
			last_chmi.setState(false);

		String label = chmi.getLabel();
		if(label.equals("By name"))
			dataStore.updateSort(DataStore.SORT_BYNAME);

		if(label.equals("By species epithet"))
			dataStore.updateSort(DataStore.SORT_BYSECONDNAME);
		
		if(label.equals("By number of character sets"))
			dataStore.updateSort(DataStore.SORT_BYCHARSETS);

		if(label.equals("By total length"))
			dataStore.updateSort(DataStore.SORT_BYTOTALLENGTH);

		chmi.setState(true);
		last_chmi = chmi;
	}

	// MouseListener: for listening in on the Table model
	//
	// Don't play around with this: there's are handy 
	// rightClick(MouseEvent) and doubleClick(MouseEvent)
	// events around for your enjoyment.
	//
	private PopupMenu popupMenu = null;
	public void mouseClicked(MouseEvent e) {
		if(e.getSource().equals(mainTable)) {
			// check for right clicks
			if(e.isPopupTrigger())
				rightClick(e);

			// check for double clicks
			if(e.getClickCount() == 2)
				doubleClick(e);
		}
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {} 
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {
		if(e.getSource().equals(mainTable)) {
			if(e.isPopupTrigger())
				rightClick(e);
		}
	}

	/**
	 * Event: somebody right clicked in the mainTable somewhere
	 */
	public void rightClick(MouseEvent e) {
		/*
		popupMenu.show((Component)e.getSource(), e.getX(), e.getY());
		*/
		int col = mainTable.columnAtPoint(e.getPoint());

		if(col == 0)
			return;

		String colName = dataStore.getColumnName(col);

		MessageBox mb = new MessageBox(
				mainFrame,
				"Are you sure?",
				"Are you sure you want to remove column '" + colName + "'? This can't be undone!",
				MessageBox.MB_YESNO);
		if(mb.showMessageBox() == MessageBox.MB_YES) {
			dataStore.deleteColumn(colName);
		}
	}

	/**
	 * Event: somebody double clicked in the mainTable somewhere
	 */
	public void doubleClick(MouseEvent e) {
		int row = mainTable.rowAtPoint(e.getPoint());
		int col = mainTable.columnAtPoint(e.getPoint());

		if(row != -1 && col != -1 && col != 0) {
			// it's, like, valid, dude.
			dataStore.toggleCancelled(
					dataStore.getColumnName(col),
					dataStore.getRowName(row)
			);
		}

	}

//
//	4.	FUNCTIONAL CODE. It does things.
//
//		Mostly, it's used by us to call bits of code to 'do' things: exit SequenceMatrix,
//		exportAsNexus, etc. Public functions can be called by other components to do
//		things, such as poke a sequence straight onto the screen.

	/**
	 * Updates the display. In our case, we just need to call the TableModel
	 * and tell it about this.
	 */
	public void updateDisplay() {
		dataStore.updateDisplay();
	}

	/**
	 * Clears all the sequences away.
	 */
	public void clear() {
		dataStore.clear();
	}

	/**
	 * Creates the user interface of SequenceMatrix.
	 */
	private void createUI() {
		// main frame
		mainFrame = new Frame("SequenceMatrix");
		mainFrame.addWindowListener(this);
		mainFrame.setLayout(new BorderLayout());
		mainFrame.setMenuBar(createMenuBar());
		mainFrame.setBackground(SystemColor.control);

		mainTable = new JTable(dataStore);
		mainTable.addMouseListener(this);
		mainTable.setColumnSelectionAllowed(true);		// why doesn't this work?
		mainTable.getTableHeader().setReorderingAllowed(false);	// don't you dare!
		JScrollPane scrollPane = new JScrollPane(mainTable);
		// i get these dimensions straight from the java docs, so
		// don't blame me and change them if you like, etc.
		//
		// p.s. Commented out, this causes a funky MacOS X GUI bug.
//		mainTable.setPreferredScrollableViewportSize(new Dimension(500, 200));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		mainFrame.add(scrollPane);

		// HACK: MacOS X (or BorderLayout?) places the ScrollPane at (0, 0)
		// 	which on OS X is the upper left corner of the title bar.
		// 	We need it to be at the top of the client area, which ought
		// 	to be stored in getInsets(). So we're going to place
		// 	a blank component on the top of the screen.

		// Start the mainFrame!
		mainFrame.pack();
		mainFrame.setVisible(true);

		// set up the DropTarget
		// Warning: this will need to be changed if table is ever not
		// completely covering our client area
		DropTarget dropTarget = new DropTarget(mainFrame, this);
		dropTarget.setActive(true);
		
		DropTarget dropTargetTable = new DropTarget(mainTable, this);
		dropTargetTable.setActive(true);
	}
	
	/**
	 * Resets the title of the main frame. You don't need to know this: you can safely
	 * unlockSequenceList(sequences) and this will be handled.
	 */
	private void resetFrameTitle() {
		mainFrame.setTitle(getName());
	}

	/**
	 * Creates and returns the main menubar.
	 */
	private MenuBar createMenuBar() {
		Iterator i;
		MenuBar menubar 	=	new MenuBar();
		int count = 0;

		// File menu
		Menu 	file		=	new Menu("File");
		file.add(new MenuItem("Clear all"));
		file.add(new MenuItem("Add sequences"));
//		file.addSeparator();
//		file.add(new MenuItem("Save", new MenuShortcut(KeyEvent.VK_S)));
//		file.add(new MenuItem("Save As", null));
				// new MenuShortcut(KeyEvent.VK_V))); --> Gets in the way of Ctrl-C Ctrl-V
				// new MenuShortcut(KeyEvent.VK_A)) --> deleting the shortcut since it gets in the way of Ctrl-A on Windows
		file.addSeparator();
		file.add(new MenuItem("Exit", new MenuShortcut(KeyEvent.VK_X)));
		file.addActionListener(this);
		menubar.add(file);
		
		// View menu
		Menu	view		=	new Menu("View");
		
		// Sort sub-menu
		Menu 	sort		=	new Menu("Sort all sequences ");

		CheckboxMenuItem chmi = new CheckboxMenuItem("By name", true);
		chmi.addItemListener(this);
		sort.add(chmi);
		last_chmi = chmi;		// primes the clear-the-last-box option
		chmi = new CheckboxMenuItem("By species epithet", false);
		chmi.addItemListener(this);
		sort.add(chmi);
		chmi = new CheckboxMenuItem("By number of character sets", false);
		chmi.addItemListener(this);
		sort.add(chmi);
		chmi = new CheckboxMenuItem("By total length", false);
		chmi.addItemListener(this);
		sort.add(chmi);

		sort.addActionListener(this);
		view.add(sort);

		view.addActionListener(this);
		menubar.add(view);

		// Export menu
		Menu	export 		= 	new Menu("Export");
		export.add(new MenuItem("Export table as tab-delimited"));
		export.add(new MenuItem("Export sequences as NEXUS", new MenuShortcut(KeyEvent.VK_N)));
		export.add(new MenuItem("Export sequences as TNT"));
		export.addActionListener(this);
		menubar.add(export);

		// Settings menu
		Menu 	settings	=	new Menu("Settings");
		settings.add(new MenuItem("Taxonsets"));
		settings.add(new MenuItem("Preferences"));
		settings.addActionListener(this);
		menubar.add(settings);

		// Help menu
		Menu	help		=	new Menu("Help");
		help.add("About SequenceMatrix");
		help.addActionListener(this);
		menubar.add(help);
		menubar.setHelpMenu(help);
		
		// Creates a popup menu for later use
		/*
		popupMenu =  PopupMenu("Matrix options");

		mainFrame.add(popupMenu);
		*/

		return menubar;
	}

	/**
	 *	Clean up anything needing cleanup-ing, then kill this program with all
	 *	the power of System.exit(0).
	 */
	private void exit() {
		clear(); 
		mainFrame.dispose();		// this "closes" this window; whether or not this 
						// terminates the application depends on whether 
						// other stuff is running.
		System.exit(0);			// until we run this command. then it's all dead, all right.

	}

	/**
	 * Returns the name of this program, i.e. "SequenceMatrix" with appropriate versioning information.
	 */
	public String getName() {
		return "TaxonDNA/SequenceMatrix " + Versions.getTaxonDNA();
	}

	/**
	 * Returns the current Frame object.
	 */
	public Frame		getFrame() {
		return mainFrame;
	}
	
	/**
	 * Returns the Preferences object (so you can see what the user wants).
	 */
	public Preferences getPrefs() {
		return prefs;
	}

	/**
	 * Returns the Exporter.
	 */
	public Exporter getExporter() {
		return exporter;
	}

	/**
	 * Returns the SequenceGrid object, if anyone wants it.
	 */
	public DataStore getDataStore() {
		return dataStore;
	}

	/**
	 * Returns the Taxonsets component.
	 */
	public Taxonsets getTaxonsets() {
		return taxonSets;
	}
	
	/**
	 * Returns the citation used by SequenceMatrix.
	 */
	public String getCitation() {
		return "Meier, R., Kwong, S., Vaidya, G., Ng, Peter K. L. DNA Barcoding and Taxonomy in Diptera: a Tale of High Intraspecific Variability and Low Identification Success. Systematic Biology, 55: 715-728.";
	}
}
