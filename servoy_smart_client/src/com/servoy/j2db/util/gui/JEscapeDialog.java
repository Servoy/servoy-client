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
package com.servoy.j2db.util.gui;


import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.UIUtils;

public abstract class JEscapeDialog extends JDialog
{
	public JEscapeDialog()
	{
		this((Frame)null, false);
	}

	public JEscapeDialog(Frame owner)
	{
		this(owner, false);
	}

	public JEscapeDialog(Frame owner, boolean modal)
	{
		this(owner, null, modal);
	}

	public JEscapeDialog(Frame owner, String title)
	{
		this(owner, title, false);
	}

	public JEscapeDialog(Frame owner, String title, boolean modal)
	{
		super(owner, title, modal);
	}

	public JEscapeDialog(Dialog owner)
	{
		this(owner, false);
	}

	public JEscapeDialog(Dialog owner, boolean modal)
	{
		this(owner, null, modal);
	}

	public JEscapeDialog(Dialog owner, String title)
	{
		this(owner, title, false);
	}

	public JEscapeDialog(Dialog owner, String title, boolean modal)
	{
		super(owner, title, modal);
	}

	@Override
	protected JRootPane createRootPane()
	{
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				cancel();//setVisible(false);
			}
		});

		ActionListener actionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent actionEvent)
			{
				cancel();//setVisible(false);
			}
		};
		JRootPane rootPane = new JRootPane();
		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		rootPane.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		return rootPane;
	}

	protected abstract void cancel();

	protected void loadBounds(String name)
	{
		boolean defaultPositioning = true;
		this.setName(name);
		if (name != null)
		{
			if (Settings.getInstance().loadBounds(this))
			{
				defaultPositioning = false;
			}
		}
		if (defaultPositioning)
		{
			pack();
			Window owner = getOwner();
			if (owner != null && owner.isShowing()) setLocationRelativeTo(owner);
			else setBounds(UIUtils.getCenteredBoundsOn(owner.getBounds(), getWidth(), getHeight()));
		}
	}

	private boolean isModalLocationInvalid()
	{
		if (isModal())
		{
			// bounds were loaded from properties file; if this dialog is modal, we must make sure
			// that if will be initially shown inside the owner's bounds
			Window owner = getOwner();
			if (owner != null)
			{
				Rectangle ownerBounds = owner.getBounds(); // bounds for owner window
				Rectangle bounds = getBounds(); // the restored bounds for this window
				Rectangle intersection = ownerBounds.intersection(bounds);

				if (intersection.isEmpty())
				{
					// this means that the dialog is not on top of it's owner => NOK (may be on some other screen even)
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void setVisible(boolean b)
	{
		if (b)
		{
			// all dialogs must be on screen and if they are modal, they must be on top of the owner window
			if (!UIUtils.isOnScreen(getBounds()) || isModalLocationInvalid())
			{
				setLocationRelativeTo(getOwner());
			}
		}
		else
		{
			Settings.getInstance().saveBounds(this);
		}
		super.setVisible(b);
	}
}
