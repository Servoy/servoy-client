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
package com.servoy.j2db.smart;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.FormWindow;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.ui.IWindowVisibleChangeListener;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Text;

/**
 * Class to show a Servoy form inside a JFrame.
 * @author acostescu
 */
public class FormFrame extends JFrame implements FormWindow
{

	private static final long serialVersionUID = 1L;

	private IApplication application;
	private IMainContainer mainContainer;

	public FormFrame(IApplication app, String windowName)
	{
		super();
		init(app, windowName);
	}

	protected void init(final IApplication app, String windowName)
	{
		setName(windowName);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		application = app;
		getContentPane().setLayout(new BorderLayout());

		InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = getRootPane().getActionMap();
		ICmdManager cm = app.getCmdManager();

		Action[] actions = new Action[12];
		actions[0] = cm.getRegisteredAction("cmdnewrecord"); //$NON-NLS-1$
		actions[1] = cm.getRegisteredAction("cmdduplicaterecord"); //$NON-NLS-1$
		actions[2] = cm.getRegisteredAction("cmddeleterecord"); //$NON-NLS-1$
		actions[3] = cm.getRegisteredAction("cmddeleteallrecord"); //$NON-NLS-1$
		actions[4] = cm.getRegisteredAction("cmdfindmode"); //$NON-NLS-1$
		actions[5] = cm.getRegisteredAction("cmdfindall"); //$NON-NLS-1$
		actions[6] = cm.getRegisteredAction("cmdomitrecord"); //$NON-NLS-1$
		actions[7] = cm.getRegisteredAction("cmdshowomitrecords"); //$NON-NLS-1$
		actions[8] = cm.getRegisteredAction("cmdrevertrecords"); //$NON-NLS-1$
		actions[9] = cm.getRegisteredAction("cmdsort"); //$NON-NLS-1$
		actions[10] = cm.getRegisteredAction("cmdnextrecord"); //$NON-NLS-1$
		actions[11] = cm.getRegisteredAction("cmdprevrecord"); //$NON-NLS-1$

		for (Action element : actions)
		{
			im.put((KeyStroke)element.getValue(Action.ACCELERATOR_KEY), element.getValue(Action.NAME));
			am.put(element.getValue(Action.NAME), element);
		}

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				cancel();
			}
		});

		addWindowFocusListener(new WindowAdapter()
		{
			@Override
			public void windowGainedFocus(WindowEvent e)
			{
				setAsCurrentContainer();
			}
		});
	}

	public void cancel()
	{
		if (mainContainer != null)
		{
			if (mainContainer.getController() != null)
			{
				mainContainer.getController().stopUIEditing(true);
			}
			application.getRuntimeWindowManager().closeFormInWindow(mainContainer.getContainerName(), true);
		}
	}

	@Override
	protected JRootPane createRootPane()
	{
		// when in full screen, ESC should close the window
		JRootPane rp = super.createRootPane();
		ActionListener actionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent actionEvent)
			{
				GraphicsDevice device = getGraphicsConfiguration().getDevice();
				if (device.getFullScreenWindow() == FormFrame.this)
				{
					cancel();
				}
			}
		};
		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		rp.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		return rp;
	}

	public void setMainContainer(IMainContainer container)
	{
		mainContainer = container;
		getContentPane().add((Component)container, BorderLayout.CENTER);
	}

	public IMainContainer getMainContainer()
	{
		return mainContainer;
	}

	/**
	 * @see java.awt.Dialog#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(String rawTitle)
	{
		FormController fp = mainContainer.getController();
		String title = rawTitle;
		if (title == null) title = fp.getForm().getTitleText();
		if (title == null) title = fp.getName();
		title = application.getI18NMessageIfPrefixed(title);

		if (title != null)
		{
			String name2 = Text.processTags(title, fp.getTagResolver());
			if (name2 != null) title = name2;
		}
		super.setTitle(title);
	}

	/**
	 * Set yourself as the current container.
	 */
	public void setAsCurrentContainer()
	{
		((FormManager)application.getFormManager()).setCurrentContainer(mainContainer, getName());
	}

	public void closeWindow()
	{
		GraphicsDevice device = getGraphicsConfiguration().getDevice();
		if (device.getFullScreenWindow() == this)
		{
			device.setFullScreenWindow(null); // reset full-screen window when window closes
		}
		storeBounds();
		setVisible(false);
		IFormManager ifm = application.getFormManager();
		if (ifm instanceof FormManager)
		{
			((FormManager)ifm).setCurrentContainer(null, null);
		}
	}

	public boolean restoreBounds()
	{
		if (isResizable()) return Settings.getInstance().loadBounds(this, application.getSolutionName());
		return Settings.getInstance().loadLocation(this, application.getSolutionName());

	}

	public void storeBounds()
	{
		if (application.getRuntimeWindowManager().getWindow(this.getName()).getStoreBounds())
		{
			if (isResizable()) Settings.getInstance().saveBounds(this, application.getSolutionName());
			else Settings.getInstance().saveLocation(this, application.getSolutionName());
		}
		else Settings.getInstance().deleteBounds(this.getName(), application.getSolutionName());
	}

	public void setFullScreen()
	{
		GraphicsDevice device = getGraphicsConfiguration().getDevice();
		if (device.getFullScreenWindow() != this)
		{
			dispose();
			setUndecorated(true);
			device.setFullScreenWindow(this); // sets visible and blocks when modal
		}
	}

	// For future implementation of case 286968 change
//	public void loadPersistedBounds()
//	{
//		boolean defaultPositioning = true;
//		String name = getName();
//		if (name != null)
//		{
//			if (Settings.getInstance().loadBounds(this))
//			{
//				defaultPositioning = false;
//			}
//		}
//		if (defaultPositioning)
//		{
//			pack();
//			setLocationRelativeTo(getOwner());
//		}
//	}
//
//	public void setPersistBounds(boolean persistBounds)
//	{
//		this.persistBounds = persistBounds;
//	}
//
//	@Override
//	public void setVisible(boolean b)
//	{
//		if (!b && persistBounds)
//		{
//			Settings.getInstance().saveBounds(this);
//		}
//		super.setVisible(b);
//	}

	@Override
	public void setVisible(boolean b)
	{
		for (IWindowVisibleChangeListener l : visibleChangeListeners.toArray(new IWindowVisibleChangeListener[visibleChangeListeners.size()]))
			l.beforeVisibleChange(this, b);

		super.setVisible(b);
	}


	private final ArrayList<IWindowVisibleChangeListener> visibleChangeListeners = new ArrayList<IWindowVisibleChangeListener>();

	public void addWindowVisibleChangeListener(IWindowVisibleChangeListener l)
	{
		if (visibleChangeListeners.indexOf(l) == -1) visibleChangeListeners.add(l);
	}

	public void removeWindowVisibleChangeListener(IWindowVisibleChangeListener l)
	{
		visibleChangeListeners.remove(l);
	}
}