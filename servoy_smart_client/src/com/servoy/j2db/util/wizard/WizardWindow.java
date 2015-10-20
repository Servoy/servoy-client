/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.j2db.util.wizard;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import com.servoy.j2db.Messages;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SwingHelper;
import com.servoy.j2db.util.gui.FixedCardLayout;
import com.servoy.j2db.util.gui.JDK131ProgressBar;
import com.servoy.j2db.util.gui.JEscapeDialog;

/**
 * Wizard swing display window
 * 
 * @author jblok
 */
public abstract class WizardWindow implements IWizard
{
	private static final String DIALOG_NAME = "WizardWindowDialog";

	private int INITIAL_WIDTH = 800;
	private int INITIAL_HEIGHT = 600;
	public static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10); //SansSerif //$NON-NLS-1$

	private JLabel statusLabel;
	private JDK131ProgressBar statusProgessBar;
	private String title;

	// The GUI fields
	private HighlightJButton prevButton; // The previous button
	private HighlightJButton nextButton; // The next button
	private HighlightJButton quitButton; // The quit button

	private JPanel panelsContainer;
	private FixedCardLayout rolodex;

	protected IWizardState state;

	//needed for use in dialog and frame
	protected Window window;
	protected JRootPane rootPane;
	protected Container contentPane;

	public WizardWindow()
	{
		this(new Dimension(800, 600));
	}

	public WizardWindow(Dimension size)
	{
		INITIAL_WIDTH = size.width;
		INITIAL_HEIGHT = size.height;
		createState();
	}

	// Shows the frame
	public void showFrame(String title, Image windowIcon) throws Exception
	{
		this.title = title;
		window = new JFrame();
		((JFrame)window).setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		if (windowIcon != null) ((JFrame)window).setIconImage(windowIcon);
		((JFrame)window).setTitle(title);
		rootPane = ((JFrame)window).getRootPane();
		contentPane = ((JFrame)window).getContentPane();
		init();
		SwingHelper.centerFrame(window);
//        ((JFrame)window).setResizable(false);
		window.setVisible(true);
	}

	// Shows the frame
	public void showDialog(String title, Dialog owner) throws Exception
	{
		window = new JEscapeDialog(owner, title, true)
		{
			@Override
			protected void cancel()
			{
				exit();
			}
		};
		window.setName(DIALOG_NAME);
		showDialogEx(owner);
	}

	// Shows the frame
	public void showDialog(String title, Frame owner) throws Exception
	{
		window = new JEscapeDialog(owner, title, true)
		{
			@Override
			protected void cancel()
			{
				exit();
			}
		};
		window.setName(DIALOG_NAME);
		showDialogEx(owner);
	}

	private void showDialogEx(Window owner) throws Exception
	{
		((JDialog)window).setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		rootPane = ((JDialog)window).getRootPane();
		contentPane = ((JDialog)window).getContentPane();
		init();
		((JDialog)window).setLocationRelativeTo(owner);
//        ((JDialog)window).setResizable(false);
		window.setVisible(true);
	}

	private void init() throws Exception
	{
		// Builds the GUI
		createPanels();

		// Prepares the glass pane to block gui interaction when needed
		Component glassPane = rootPane.getGlassPane();
		glassPane.addMouseListener(new MouseAdapter()
		{
		});
		glassPane.addMouseMotionListener(new MouseMotionAdapter()
		{
		});
		glassPane.addKeyListener(new KeyAdapter()
		{
		});

		// We set the layout & prepare the constraint object
		contentPane.setLayout(new BorderLayout());

		// We add the panels container
		panelsContainer = new JPanel();
		rolodex = new FixedCardLayout();
		panelsContainer.setLayout(rolodex);
		Enumeration e = state.getAllPanels();
		while (e.hasMoreElements())
		{
			JPanel panel = (JPanel)e.nextElement();
			panelsContainer.add(panel, panel.getName());
			//rolodex.addLayoutComponent(panel,panel.getName());//mag niet?
//System.out.println("added "+panel.getName());
		}
//		JScrollPane scroller = new JScrollPane(panelsContainer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

//        scroller.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(panelsContainer, BorderLayout.CENTER); //scroller

		// We add the navigation buttons & labels

		NavigationHandler navHandler = new NavigationHandler();

		prevButton = new HighlightJButton(Messages.getString("servoy.button.prev"), //$NON-NLS-1$
			getImageIcon("stepback.gif"), //$NON-NLS-1$
			Color.white);
		prevButton.addActionListener(navHandler);

		nextButton = new HighlightJButton(Messages.getString("servoy.button.next"), //$NON-NLS-1$
			getImageIcon("stepforward.gif"), //$NON-NLS-1$
			Color.white);
		nextButton.addActionListener(navHandler);

		quitButton = new HighlightJButton(Messages.getString("servoy.button.quit"), //$NON-NLS-1$
			getImageIcon("stop.gif"), //$NON-NLS-1$
			Color.white);
		quitButton.addActionListener(navHandler);

		JPanel down = new JPanel();
		down.setLayout(new BorderLayout());
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(prevButton);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(nextButton);
		buttonPane.add(Box.createRigidArea(new Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.add(quitButton);
		down.add(buttonPane, BorderLayout.CENTER);
		down.add(createStatusPanel(), BorderLayout.SOUTH);

		contentPane.add(down, BorderLayout.SOUTH);

		//show first
		switchPanel(true);
//		String panelName = state.getNextPanel().getName();
//		rolodex.show(panelsContainer,panelName);
//		state.handleButtons();
		rootPane.setDefaultButton(nextButton);

		window.pack();
		window.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);

		// Sets the window events handler
		window.addWindowListener(new WindowHandler(this));

	}

	/**
	 * The first panel must be called 'start'
	 */
	protected abstract void createPanels() throws Exception;

	/**
	 * creates the defaultWizardState
	 */
	protected void createState()
	{
		if (state == null)
		{
			state = new WizardState(this);
		}
	}

	public IWizardState getState()
	{
		return state;
	}

	protected void addPanel(IWizardPanel panel)
	{
		state.addNewPanel(panel);
	}

	public abstract ImageIcon getImageIcon(String name);

	public Window getMainApplicationWindow()
	{
		return window;
	}

	// Switches the current panel
	public void switchPanel(boolean next)
	{
		if (next && !state.canProgress())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		panelsContainer.setVisible(false);
		IWizardPanel panel;
		if (next)
		{
			panel = state.getNextPanel();
		}
		else
		{
			panel = state.getPreviousPanel();
		}

		rolodex.show(panelsContainer, panel.getName());

		if (showPanelNameInTitle)
		{
			String name = panel.getName().toLowerCase();
			((JFrame)window).setTitle(title + " - (" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		state.handleButtons();

		panelsContainer.setVisible(true);

		if (panel != null)
		{
			Runnable runnable = panel.needsToRunFirst(next);
			if (runnable != null)
			{
				//TODO: make thread pool?
				Thread th = new Thread(runnable, "runner"); //$NON-NLS-1$
				th.setPriority(3);//below normal, UI is more important here.
				th.start();
			}
		}
	}

	public boolean showPanelNameInTitle = false;

	public void showPanelNameInTitle(boolean b)//use full for debugging
	{
		showPanelNameInTitle = b;
	}

	// Makes a clean closing
	private void exit()
	{
		if (state.isFinished())
		{
			window.setVisible(false);
			realExit();
			window.dispose();
		}
		else
		{
			int res = showCancelDialog();
			if (res == JOptionPane.YES_OPTION)
			{
				window.setVisible(false);
				realExit();
				window.dispose();
			}
		}
	}

	/**
	 * return of JOptionPane.YES_OPTION will couse call to realExit and delete window
	 * 
	 * @return int
	 */
	protected int showCancelDialog()
	{
		int res = JOptionPane.showConfirmDialog(window, Messages.getString("servoy.wizardwindow.quiting.message"), //$NON-NLS-1$
			Messages.getString("servoy.wizardwindow.quiting.title"), //$NON-NLS-1$
			JOptionPane.YES_NO_OPTION);
		return res;
	}

	protected abstract void realExit();

	// Blocks GUI interaction
	// always call this method with a finally block on releaseGUI!
	public void blockGUI(String reason)
	{
		semiBlockGUI(reason);
		setGlassPaneEnable();
	}

	private void setGlassPaneEnable()
	{
		rootPane.getGlassPane().setVisible(true);
		rootPane.getGlassPane().setEnabled(true);
	}

	private void setGlassPaneDisable()
	{

		rootPane.getGlassPane().setEnabled(false);
		rootPane.getGlassPane().setVisible(false);
	}

	/**
	 * Always call this method with a finally block in setInRelease it set the cursors on wait and sets the indeterminate property of progres bar on true; The
	 * difference from blockGUI is that it doesnt set the glasspane on the window;
	 */
	public void semiBlockGUI(String reason)
	{
		statusLabel.setText(reason);
		// "Your report has been assigned an internal review ID of 1079568, which is NOT visible on the Sun Developer Network (SDN)."
		// NullPointerException in Sun VM - the synchronized block is a workaround
		synchronized (statusProgessBar.getTreeLock())
		{
			statusProgessBar.setIndeterminate(true);
		}
		window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}


	/**
	 * it set the cursors on normal and sets the indeterminate property of progres bar on false;
	 */
	public void semiReleaseGUI()
	{
		window.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		// "Your report has been assigned an internal review ID of 1079568, which is NOT visible on the Sun Developer Network (SDN)."
		// NullPointerException in Sun VM - the synchronized block is a workaround
		synchronized (statusProgessBar.getTreeLock())
		{
			statusProgessBar.setIndeterminate(false);
		}
		statusLabel.setText(Messages.getString("servoy.general.status.ready")); //$NON-NLS-1$
	}


	// Releases GUI interaction
	public void releaseGUI()
	{
		setGlassPaneDisable();
		semiReleaseGUI();

	}

	// Locks the 'previous' button
	public void lockPrevButton()
	{
		prevButton.setEnabled(false);
	}

	// Locks the 'next' button
	public void lockNextButton()
	{
		nextButton.setEnabled(false);
	}

	// Unlocks the 'previous' button
	public void unlockPrevButton()
	{
		prevButton.setEnabled(true);
	}

	// Unlocks the 'next' button
	public void unlockNextButton()
	{
		nextButton.setEnabled(true);
	}

	public void lockFinishButton()
	{
		quitButton.setText(Messages.getString("servoy.button.cancel")); //$NON-NLS-1$
	}

	public void unlockFinishButton()
	{
		quitButton.setText(Messages.getString("servoy.button.finish")); //$NON-NLS-1$
	}

	//.....................................................................
	// Some event handler classes

	// Handles the events from the navigation bar elements
	class NavigationHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();
			if (source == prevButton)
			{
				switchPanel(false);
			}
			else if (source == nextButton)
			{
				switchPanel(true);
			}
			else if (source == quitButton)
			{
				exit();
			}
		}
	}

	public void doNext()
	{
		switchPanel(true);
	}

	// The window events handler
	private static class WindowHandler extends WindowAdapter
	{
		private final WizardWindow ww;

		public WindowHandler(WizardWindow ww)
		{
			this.ww = ww;
		}

		@Override
		public void windowClosing(WindowEvent e)
		{
			ww.exit();
		}
	}

	protected JPanel createStatusPanel()
	{
		Color darkShadow = UIManager.getColor("controlShadow"); //$NON-NLS-1$
		Color lightShadow = UIManager.getColor("controlLtHighlight"); //$NON-NLS-1$

		JPanel status = new JPanel();
		Border border = BorderFactory.createBevelBorder(BevelBorder.LOWERED, lightShadow, status.getBackground(), darkShadow, status.getBackground());

		//set the status
		statusLabel = new JLabel();
//		statusLabel.setFont(smallFont);
		statusLabel.setText(Messages.getString("servoy.general.status.ready")); //$NON-NLS-1$
		statusLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		statusLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		statusLabel.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(0, 2, 0, 0)));//BorderFactory.createEtchedBorder());
		statusLabel.setMinimumSize(new Dimension(100, 18));
		statusLabel.setPreferredSize(new Dimension(4000, 18));

		statusProgessBar = new JDK131ProgressBar();
		statusProgessBar.setMaximumSize(new Dimension(100, 18));
		statusProgessBar.setPreferredSize(new Dimension(100, 18));
		statusProgessBar.setMinimumSize(new Dimension(100, 18));

		statusProgessBar.setBorder(border);//BorderFactory.createEtchedBorder());
		statusProgessBar.setStringPainted(false);
		statusProgessBar.setAlignmentX(Component.RIGHT_ALIGNMENT);
		statusProgessBar.setAlignmentY(Component.BOTTOM_ALIGNMENT);

		status.setLayout(new BoxLayout(status, BoxLayout.X_AXIS));
		status.add(statusLabel);
		status.add(statusProgessBar);
		status.setBorder(BorderFactory.createEmptyBorder());
		return status;
	}

	public void reportError(Component parentComponent, String msg, Exception ex)
	{
		Debug.error(msg);
		Debug.error(ex);
	}

	public void reportError(String msg, Exception ex)
	{
		Debug.error(msg);
		Debug.error(ex);
	}

	/**
	 * @param string
	 * @param currentWindow
	 * @throws Exception 
	 */
	public void showWindow(String title, Window w) throws Exception
	{
		if (w instanceof Dialog)
		{
			showDialog(title, (Dialog)w);
		}
		showDialog(title, (Frame)w);
	}
}
