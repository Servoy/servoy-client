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
package com.servoy.j2db.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.servoy.j2db.smart.J2DBClient;

public class MultiSelectionPanel extends JPanel implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private boolean hasUpDownButtons;

	private JList sourceList;

	private JList targetList;

	private DefaultListModel sourceModel = new DefaultListModel();

	private DefaultListModel targetModel = new DefaultListModel();

	private IndexedObject[] entryList;

	public MultiSelectionPanel(Object[] entries, boolean[] isSelected, int[] targetListIndices, String message,
			String label1, String label2, boolean hasUpDownButtons)
	{
		super();

		this.hasUpDownButtons = hasUpDownButtons;

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder());

		final JButton downButton = new JButton("Down"); //$NON-NLS-1$
		Dimension minimumSize = downButton.getPreferredSize();// new
																				// Dimension(100,20);

		final JButton addButton = new JButton(" >> "); //$NON-NLS-1$
		addButton.addActionListener(this);
		addButton.setActionCommand("add"); //$NON-NLS-1$
		addButton.setPreferredSize(minimumSize);
		addButton.setMinimumSize(minimumSize);
		addButton.setMaximumSize(minimumSize);

		final JButton removeButton = new JButton(" << "); //$NON-NLS-1$
		removeButton.addActionListener(this);
		removeButton.setActionCommand("remove"); //$NON-NLS-1$
		removeButton.setPreferredSize(minimumSize);
		removeButton.setMinimumSize(minimumSize);
		removeButton.setMaximumSize(minimumSize);

		JPanel movePane = new JPanel();
		movePane.setLayout(new BoxLayout(movePane, BoxLayout.Y_AXIS));
		movePane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		movePane.setMaximumSize(new Dimension(150, 200));
		movePane.add(Box.createVerticalGlue());
		movePane.add(addButton);
		movePane.add(Box.createRigidArea(new Dimension(0, J2DBClient.BUTTON_SPACING)));
		movePane.add(removeButton);
		movePane.add(Box.createVerticalGlue());

		JPanel sourcePane = new JPanel();
		sourcePane.setLayout(new BorderLayout());
		if (label1 != null)
		{
			JLabel sourceLabel = new JLabel(label1);
			sourceLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
			sourcePane.add(sourceLabel, BorderLayout.NORTH);
		}

		JPanel targetPane = new JPanel();
		targetPane.setLayout(new BorderLayout());
		if (label2 != null)
		{
			JLabel targetLabel = new JLabel(label2);
			targetLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
			targetPane.add(targetLabel, BorderLayout.NORTH);
		}

		sourceList = new JList(sourceModel);
		JScrollPane sourceScrollPane = new JScrollPane(sourceList);
		sourceScrollPane.setPreferredSize(new Dimension(200, 150));
		sourcePane.add(sourceScrollPane, BorderLayout.CENTER);
		sourceList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					if (MultiSelectionPanel.this.hasUpDownButtons)
						add(); // order matters in target
					else move(sourceList, targetList); // keep standard order
				}
			}
		});

		targetList = new JList(targetModel);
		JScrollPane targetScrollPane = new JScrollPane(targetList);
		targetScrollPane.setPreferredSize(new Dimension(200, 150));
		targetPane.add(targetScrollPane, BorderLayout.CENTER);
		targetList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					move(targetList, sourceList);
				}
			}
		});

		JPanel listPane = new JPanel(new BorderLayout());
		listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.X_AXIS));

		listPane.add(sourcePane);
		listPane.add(movePane);
		listPane.add(targetPane);

		if (hasUpDownButtons)
		{
			JPanel upDownPane = new JPanel();
			upDownPane.setLayout(new BoxLayout(upDownPane, BoxLayout.Y_AXIS));
			upDownPane.setMaximumSize(new Dimension(150, 200));

			JButton upButton = new JButton("Up"); //$NON-NLS-1$
			upButton.addActionListener(this);
			upButton.setActionCommand("up"); //$NON-NLS-1$
			upButton.setPreferredSize(minimumSize);
			upButton.setMinimumSize(minimumSize);
			upButton.setMaximumSize(minimumSize);
			upButton.setMnemonic(KeyEvent.VK_U);

			upDownPane.add(Box.createVerticalGlue());
			upDownPane.add(upButton);

			upDownPane.add(Box.createRigidArea(new Dimension(0, J2DBClient.BUTTON_SPACING)));

			downButton.addActionListener(this);
			downButton.setActionCommand("down"); //$NON-NLS-1$
			downButton.setPreferredSize(minimumSize);
			downButton.setMinimumSize(minimumSize);
			downButton.setMaximumSize(minimumSize);
			downButton.setMnemonic(KeyEvent.VK_D);

			upDownPane.add(downButton);
			upDownPane.add(Box.createVerticalGlue());

			upDownPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

			listPane.add(upDownPane);
		}

		if (message != null)
		{
			JPanel messagePane = new JPanel();
			messagePane.setLayout(new BoxLayout(messagePane, BoxLayout.X_AXIS));
			messagePane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
			messagePane.add(new JLabel(message));
			messagePane.add(Box.createHorizontalGlue());
			add(messagePane, BorderLayout.NORTH);
		}

		add(listPane, BorderLayout.CENTER);

		setData(entries, isSelected, targetListIndices);
	}

	public void setData(Object[] entries, boolean[] isSelected, int[] targetListIndices)
	{
		sourceModel.clear();
		targetModel.clear();

		entryList = new IndexedObject[entries.length];
		for (int i = 0; i < entries.length; i++)
		{
			entryList[i] = new IndexedObject(i, entries[i]);
		}

		targetListIndices = targetListIndices == null ? new int[0] : ((int[]) targetListIndices.clone());
		if (!hasUpDownButtons)
			Arrays.sort(targetListIndices);
		entries = (Object[]) entries.clone();
		for (int i = 0; i < targetListIndices.length; i++)
		{
			int index = targetListIndices[i];
			targetModel.addElement(entryList[index]);
			if (isSelected != null && isSelected[index])
				targetList.addSelectionInterval(i, i);
			entries[index] = null;
		}

		for (int i = 0, j = 0; i < entries.length; i++)
		{
			if (entries[i] != null)
			{
				sourceModel.addElement(entryList[i]);
				if (isSelected != null && isSelected[i])
					sourceList.addSelectionInterval(j, j);
				j++;
			}
		}
	}

	private void add()
	{
		int targetLength = targetModel.getSize();
		int[] selected = sourceList.getSelectedIndices();
		if (selected.length > 0)
		{
			Arrays.sort(selected);
			sourceList.clearSelection();
			targetList.clearSelection();
			int j = 0;
			for (int i = 0; i < selected.length; i++)
			{
				Object object = sourceModel.remove(selected[i] - j++);
				targetModel.addElement(object);
			}
			targetList.addSelectionInterval(targetLength, targetLength + selected.length - 1);
		}
	}

	private void move(JList sourceList, JList targetList)
	{
		DefaultListModel sourceModel = (DefaultListModel) sourceList.getModel();
		DefaultListModel targetModel = (DefaultListModel) targetList.getModel();

		int[] selected = sourceList.getSelectedIndices();
		if (selected.length > 0)
		{
			sourceList.clearSelection();
			int[] targetIndices = new int[targetModel.size() + selected.length];
			int k = 0;
			Enumeration enumeration = targetModel.elements();
			while (enumeration.hasMoreElements())
			{
				IndexedObject object = (IndexedObject) enumeration.nextElement();
				targetIndices[k++] = object.getIndex();
			}
			for (int i = selected.length - 1; i >= 0; i--)
			{
				IndexedObject object = (IndexedObject) sourceModel.remove(selected[i]);
				targetIndices[k++] = selected[i] = object.getIndex();
			}
			Arrays.sort(targetIndices);
			Arrays.sort(selected);
			k = 0;
			targetModel.clear();
			for (int i = 0; i < targetIndices.length; i++)
			{
				targetModel.addElement(entryList[targetIndices[i]]);
				if (k < selected.length && selected[k] == targetIndices[k])
				{
					selected[k++] = i;
				}
			}
			targetList.setSelectedIndices(selected);
		}
	}

	private void up()
	{
		int[] selected = targetList.getSelectedIndices();
		if (selected.length > 0)
		{
			Arrays.sort(selected);
			if (selected[0] > 0)
			{
				for (int i = 0; i < selected.length; i++)
				{
					Object object = targetModel.remove(selected[i]);
					selected[i]--;
					targetModel.add(selected[i], object);
				}
				targetList.setSelectedIndices(selected);
			}
		}
	}

	private void down()
	{
		int[] selected = targetList.getSelectedIndices();
		if (selected.length > 0)
		{
			Arrays.sort(selected);
			if (selected[selected.length - 1] < targetModel.size() - 1)
			{
				for (int i = selected.length - 1; i >= 0; i--)
				{
					Object object = targetModel.remove(selected[i]);
					selected[i]++;
					targetModel.add(selected[i], object);
				}
				targetList.setSelectedIndices(selected);
			}
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if ("add".equals(command)) //$NON-NLS-1$
		{
			if (hasUpDownButtons)
				add(); // order matters in target
			else move(sourceList, targetList); // keep standard order
		}
		else if ("remove".equals(command)) //$NON-NLS-1$
			move(targetList, sourceList);
		else if ("up".equals(command)) //$NON-NLS-1$
			up();
		else if ("down".equals(command)) //$NON-NLS-1$
			down();
	}

	public int[] getSelectedValues()
	{
		int[] selection = new int[targetModel.size()];
		Enumeration elements = targetModel.elements();
		int i = 0;
		while (elements.hasMoreElements())
		{
			IndexedObject element = (IndexedObject) elements.nextElement();
			selection[i++] = element.getIndex();
		}
		return selection;
	}

}
