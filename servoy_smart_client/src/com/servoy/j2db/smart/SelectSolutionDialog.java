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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.gui.FixedJList;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.util.gui.JEscapeDialog;

/**
 * Dialog to open a solution
 * 
 * @author jblok
 */
public class SelectSolutionDialog extends JEscapeDialog implements ActionListener, ListSelectionListener
{
	/*
	 * _____________________________________________________________ Declaration of attributes
	 */
	private final JList list;
	private SolutionMetaData retval;
	private final JButton setButton;
	private final IApplication application;

	/*
	 * _____________________________________________________________ Declaration and definition of constructors
	 */
	public SelectSolutionDialog(ISmartClientApplication app)
	{
		super(app.getMainApplicationFrame(), true);
		application = app;
		setTitle(Messages.getString("servoy.selectSolutionDialog.selectSolution")); //$NON-NLS-1$

		getContentPane().setLayout(new BorderLayout());

		setButton = new JButton(Messages.getString("servoy.button.ok")); //$NON-NLS-1$
		setButton.addActionListener(this);
		setButton.setActionCommand("ok"); //$NON-NLS-1$
		setButton.setEnabled(false);

		JButton cancelButton = new JButton(Messages.getString("servoy.button.cancel")); //$NON-NLS-1$
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("cancel"); //$NON-NLS-1$

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(setButton);
		buttonPane.add(Box.createRigidArea(new Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.add(cancelButton);

		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(setButton);

		list = new FixedJList();
		list.setCellRenderer(new DefaultListCellRenderer());
		//list.setBorder(BorderFactory.createEtchedBorder());
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					setButton.doClick();
				}
			}
		});
		list.addListSelectionListener(this);
		JScrollPane listScroller = new JScrollPane(list);
		JPanel borderPanel = new JPanel();
		borderPanel.setLayout(new BorderLayout());
		borderPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		borderPanel.add(listScroller, BorderLayout.CENTER);
		getContentPane().add(borderPanel, BorderLayout.CENTER);

		loadBounds("SelectSolutionDialog"); //$NON-NLS-1$
	}

	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if (application.isInDeveloper())
		{
			application.getRuntimeProperties().put("load.solution.modifiers", new Integer(e.getModifiers())); //$NON-NLS-1$
		}
		if (command.equals("ok")) ok(); //$NON-NLS-1$
		if (command.equals("cancel")) cancel(); //$NON-NLS-1$
	}

	public void valueChanged(ListSelectionEvent e)
	{
		int index = list.getSelectedIndex();
		list.ensureIndexIsVisible(index);
		setButton.setEnabled(list.getModel().getSize() > 0 && !list.isSelectionEmpty());
	}

	@Override
	public void cancel()
	{
		retval = null;
		setVisible(false);
	}

	public void ok()
	{
		if (list.getModel().getSize() > 0 && !list.isSelectionEmpty())
		{
			retval = (SolutionMetaData)list.getSelectedValue();
			if (retval != null)
			{
				application.getSettings().put("lastSolution", retval); //$NON-NLS-1$
			}
		}
		setVisible(false);
	}


	public SolutionMetaData showDialog(SolutionMetaData[] solutions)
	{
		if (solutions == null || solutions.length == 0)
		{
			return null;
		}

		Properties settings = application.getSettings();
		String name = settings.getProperty("lastSolution"); //$NON-NLS-1$
		DefaultListModel dml = new DefaultListModel();

		int selectedIndex = -1;
		for (int i = 0; i < solutions.length; i++)
		{
			if (solutions[i].getName().equals(name))
			{
				selectedIndex = i;
			}
			dml.addElement(solutions[i]);
		}

		list.setModel(dml);

		if (selectedIndex != -1)
		{
			list.setSelectedIndex(selectedIndex);
			list.ensureIndexIsVisible(selectedIndex);
		}
		else
		{
			list.setSelectedIndex(0);
		}
		setVisible(true);

		return retval;
	}
}