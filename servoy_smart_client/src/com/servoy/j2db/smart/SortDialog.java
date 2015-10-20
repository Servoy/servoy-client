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
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.property.SortEditor;
import com.servoy.j2db.util.gui.JEscapeDialog;

public class SortDialog extends JEscapeDialog implements ActionListener
{
	private IApplication application;
	private SortEditor sortEditor;
	private boolean OK;

	public SortDialog(Dialog parent, IApplication b)
	{
		this(parent, true, b);
	}

	public SortDialog(Frame parent, IApplication b)
	{
		this(parent, true, b);
	}

	public SortDialog(Dialog parent, boolean modal, IApplication b)
	{
		super(parent, modal);
		init(b);
	}

	public SortDialog(Frame parent, boolean modal, IApplication b)
	{
		super(parent, modal);
		init(b);
	}

	private void init(IApplication b)
	{
		application = b;
		setTitle(Messages.getString("servoy.sortDialog.sortTitle")); //$NON-NLS-1$

		//buttons
		final JButton setButton = new JButton(Messages.getString("servoy.sortDialog.sortTitle")); //$NON-NLS-1$
		setButton.addActionListener(this);
		setButton.setActionCommand("ok"); //$NON-NLS-1$

		final JButton cancelButton = new JButton(Messages.getString("servoy.button.cancel")); //$NON-NLS-1$
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("cancel"); //$NON-NLS-1$

		JButton copyButton = new JButton(Messages.getString("servoy.button.copy")); //$NON-NLS-1$
		copyButton.addActionListener(this);
		copyButton.setActionCommand("copy"); //$NON-NLS-1$

		getRootPane().setDefaultButton(setButton);

		//Lay out the buttons from left to right.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(copyButton);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(setButton);
		buttonPane.add(Box.createRigidArea(new Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.add(cancelButton);

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		sortEditor = new SortEditor();
		JPanel sortPane = new JPanel(new BorderLayout());
		sortPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		sortPane.add(sortEditor.getCustomEditor(), BorderLayout.CENTER);

		contentPane.add(sortPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.SOUTH);

		loadBounds("SortDialog"); //$NON-NLS-1$
		setMinimumSize(new Dimension(900, 250));
		setButton.requestFocus();
	}

	public List<SortColumn> showDialog(ITable t, List<SortColumn> sortColumns)
	{
		sortEditor.init(application, t, sortColumns);
		setVisible(true);
		if (OK)
		{
			return sortEditor.getData();
		}
		else
		{
			return null;
		}
	}

	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();
		if (command.equals("ok")) //$NON-NLS-1$
		ok();
		else if (command.equals("cancel")) //$NON-NLS-1$
		cancel();
		else if (command.equals("copy")) //$NON-NLS-1$
		copy();
	}

	private void copy()
	{
		List<SortColumn> list = sortEditor.getData();
		WebStart.setClipboardContent(FoundSetManager.getSortColumnsAsString(list));
	}

	private void ok()
	{
		OK = true;
		setVisible(false);
	}

	@Override
	public void cancel()
	{
		OK = false;
		this.setVisible(false);
	}
}