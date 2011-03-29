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
package com.servoy.j2db.util.toolbar;


import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.servoy.j2db.Messages;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.FixedToggleButtonModel;
import com.servoy.j2db.util.IProvideButtonModel;
import com.servoy.j2db.util.gui.ActionCheckBoxMenuItem;

/**
 * ToolbarPanel is a container for the <code>Toolbar</code> components. The
 * serialized state is used to represent a single configuration of the toolbar.
 */
public class ToolbarPanel extends EnablePanel implements IToolbarPanel
{

	/** All toolbars which are represented in ToolbarPool too. */
	private final WeakHashMap<String, ToolbarConstraints> allToolbars;
	/** List of visible toolbar rows. */
	private final List<ToolbarRow> toolbarRows;
	/** All invisible toolbars (visibility==false || tb.isCorrect==false). */
	private final Map<ToolbarConstraints, Integer> invisibleToolbars;
	private final Map<String, Toolbar> toolbars;
	/**
	 * Toolbars which was described in DOM Document, but which aren't represented
	 * in ToolbarPool. For example ComponentPalette and first start of IDE.
	 */
	/** Cached preferred width. */
	private int prefWidth;

	/** toolbar layout manager for this configuration */
	private final ToolbarLayout toolbarLayout;
	/** toolbar drag and drop listener */
	private final ToolbarDnDListener toolbarListener;

	private final Map<String, ToolbarAction> actions;
	private JPopupMenu popup;
	private JMenu menu;

	private final int iMaxWidth;
	private final MouseAdapter madapter;

	/**
	 * Create new ToolbarPanel
	 */
	public ToolbarPanel(int iWidth)
	{
		super();
		iMaxWidth = iWidth;
		toolbarLayout = new ToolbarLayout(this);
		setLayout(toolbarLayout);
		allToolbars = new WeakHashMap<String, ToolbarConstraints>();
		actions = new HashMap<String, ToolbarAction>();
		toolbarRows = new ArrayList<ToolbarRow>();
		invisibleToolbars = new HashMap<ToolbarConstraints, Integer>();
		toolbars = new HashMap<String, Toolbar>();
		toolbarListener = new ToolbarDnDListener(this);
		madapter = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent me)
			{
				maybeShowPopup(me);
			}

			@Override
			public void mouseReleased(MouseEvent me)
			{
				maybeShowPopup(me);
			}
		};
		addMouseListener(madapter);
		setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
	}

	public void clear()
	{
		List<String> names = new ArrayList<String>(toolbars.keySet());
		for (String name : names)
		{
			removeToolBar(name);
		}
	}

	public Toolbar createToolbar(String name, String displayName)
	{
		return createToolbar(name, displayName, -1);
	}

	public Toolbar createToolbar(String name, String displayName, int wantedRow)
	{
		Toolbar tb = new Toolbar(name, displayName, true);
		addToolbar(tb, wantedRow);
		return tb;
	}

	public void addToolbar(Toolbar tb, int wantedRow)
	{

		tb.setDnDListener(toolbarListener);
		ToolbarConstraints tc;
		String name;
		ToolbarRow newRow = null;

		name = tb.getName();
		tc = allToolbars.get(name);
		if (tc == null)
		{
			if (wantedRow != -1)
			{
				while (wantedRow >= toolbarRows.size())
				{
					createLastRow();
				}
				newRow = toolbarRows.get(wantedRow);
			}
			else
			{
				for (int i = 0; i < toolbarRows.size(); i++)
				{
					ToolbarRow row = toolbarRows.get(i);
					int rowwidth = row.getPrefWidth();
					int iTbSize = tb.getPreferredSize().width;
//					int count = row.toolbarCount();
					if ( /* count < 2 && */(rowwidth + iTbSize) < iMaxWidth)
					{
						newRow = row;
						break;
					}
				}
			}
			/* If there is no toolbar constraints description defined yet ... */
			if (newRow == null) newRow = createLastRow();

			/* ... there is created a new constraints. */
			tc = new ToolbarConstraints(this, name, null, Boolean.TRUE);

			addToolbar(newRow, tc);
		}
		add(tb, tc);
		revalidateWindow();
		actions.put(tb.getName(), new ToolbarAction(this, tc, tb));
		toolbars.put(tb.getName(), tb);
		tb.addMouseListener(madapter);
	}

	public Toolbar getToolBar(String name)
	{
		return toolbars.get(name);
	}

	public int getToolBarRow(String name)
	{
		ToolbarConstraints tc = getToolbarConstraints(name);
		if (tc.isVisible()) return tc.rowIndex();
		return -1;
	}

	public int getToolbarRowIndex(String name)
	{
		for (int i = 0; i < toolbarRows.size(); i++)
		{
			ToolbarRow row = toolbarRows.get(i);
			int index = row.indexOf(name);
			if (index != -1) return index;
		}
		return -1;
	}

	public void removeToolBar(String name)
	{
		removeToolbarEx(name);
	}

	private void maybeShowPopup(MouseEvent e)
	{
		if (popup == null) popup = createMenus();
		if (e.isPopupTrigger())
		{
			popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	/**
	 * Add toolbar to list of all toolbars. If specified toolbar constraints
	 * represents visible component it is added to specified toolbar row.
	 * Otherwise toolbar constraints is added to invisible toolbars.
	 * 
	 * @param row
	 *           toolbar row of new toolbar is part
	 * @param tc
	 *           added toolbar represented by ToolbarConstraints
	 */
	void addToolbar(ToolbarRow row, ToolbarConstraints tc)
	{
		if (tc == null) return;

		if (tc.isVisible()) row.addToolbar(tc);
		else
		{
			int rI;
			if (row == null) rI = toolbarRows.size();
			else rI = toolbarRows.indexOf(row);
			invisibleToolbars.put(tc, new Integer(rI));
		}
		allToolbars.put(tc.getName(), tc);
	}

	/**
	 * Remove toolbar from list of all toolbars. This could mean that toolbar is
	 * represented only in DOM document.
	 * 
	 * @param name
	 *           name of removed toolbar
	 */
	ToolbarConstraints removeToolbarEx(String name)
	{
		ToolbarConstraints tc = allToolbars.remove(name);
		if (tc != null)
		{
			removeVisible(tc);

			popup = null;
			actions.remove(name);
			Toolbar tb = toolbars.remove(name);
			if (tb != null) tb.setVisible(false);
			invisibleToolbars.remove(name);

			if (tc.destroy()) checkToolbarRows();
		}
		return tc;
	}

	/**
	 * Add toolbar row as last row.
	 * 
	 * @param row
	 *           added toolbar row
	 */
	void addRow(ToolbarRow row)
	{
		addRow(row, toolbarRows.size());
	}

	/**
	 * Add toolbar row to specific index.
	 * 
	 * @param row
	 *           added toolbar row
	 * @param index
	 *           specified index of toolbar position
	 */
	void addRow(ToolbarRow row, int index)
	{
		/* It is important to recompute row neighbourhood. */
		ToolbarRow prev = null;
		ToolbarRow next = null;
		if (index > 0 && index - 1 < toolbarRows.size())
		{
			prev = toolbarRows.get(index - 1);
		}
		if (index >= 0 && index < toolbarRows.size())
		{
			next = toolbarRows.get(index);
		}

		if (prev != null) prev.setNextRow(row);
		row.setPrevRow(prev);
		row.setNextRow(next);
		if (next != null) next.setPrevRow(row);

		toolbarRows.add(index, row);
		updateBounds(row);
		this.setVisible(true);
	}

	/**
	 * Remove toolbar row from list of all rows.
	 * 
	 * @param row
	 *           removed toolbar row
	 */
	void removeRow(ToolbarRow row)
	{
		/* It is important to recompute row neighbournhood. */
		ToolbarRow prev = row.getPrevRow();
		ToolbarRow next = row.getNextRow();
		if (prev != null)
		{
			prev.setNextRow(next);
		}
		if (next != null)
		{
			next.setPrevRow(prev);
		}

		toolbarRows.remove(row);
		this.setVisible(toolbarRows.size() != 0);
		updateBounds(next);
		revalidateWindow();
	}

	/**
	 * Update toolbar row cached bounds.
	 * 
	 * @param toolbarRow
	 *           updated toolbarRow
	 */
	void updateBounds(ToolbarRow toolbarRow)
	{
		ToolbarRow row = toolbarRow;
		while (row != null)
		{
			row.updateBounds();
			row = row.getNextRow();
		}
	}

	/**
	 * @param row
	 *           specified toolbar row
	 * @return index of toolbar row
	 */
	int rowIndex(ToolbarRow row)
	{
		return toolbarRows.indexOf(row);
	}

	/**
	 * Updates cached preferred width of toolbar configuration.
	 */
	void updatePrefWidth()
	{
		Iterator<ToolbarRow> it = toolbarRows.iterator();
		prefWidth = 0;
		while (it.hasNext())
		{
			prefWidth = Math.max(prefWidth, it.next().getPrefWidth());
		}
	}

	/**
	 * @return configuration preferred width
	 */
	int getPrefWidth()
	{
		return prefWidth;
	}

	/**
	 * @return configuration preferred height. If there is no row, preferred
	 *         height is 25% of BASIC_HEIGHT.
	 */
	int getPrefHeight()
	{
		double rowCount = getRowCount();
		if (rowCount == 0) rowCount = 0.25;
		return (ToolbarLayout.VGAP + (int)((ToolbarLayout.VGAP + Toolbar.BASIC_HEIGHT) * rowCount));
	}

	/**
	 * Checks toolbar rows. If there is some empty row it is removed.
	 */
	void checkToolbarRows()
	{
		Object[] rows = toolbarRows.toArray();
		ToolbarRow row;

		for (int i = rows.length - 1; i >= 0; i--)
		{
			row = (ToolbarRow)rows[i];
			if (row.isEmpty()) removeRow(row);
		}
	}

	/**
	 * @return number of rows.
	 */
	int getRowCount()
	{
		return toolbarRows.size();
	}

	/**
	 * @param name toolbar constraints name
	 * @return toolbar constraints of specified name
	 */
	public ToolbarConstraints getToolbarConstraints(String name)
	{
		return allToolbars.get(name);
	}

	/**
	 * Checks toolbars constraints if there is some of specific name. If isn't
	 * then is created new toolbar constraints. Otherwise is old toolbar
	 * constraints confronted with new values (position, visibility).
	 * 
	 * @param name of checked toolbar
	 * @param position of toolbar
	 * @param visible visibility of toolbar
	 * @return toolbar constraints for specified toolbar name
	 */
	ToolbarConstraints checkToolbarConstraints(String name, Integer position, Boolean visible)
	{
		ToolbarConstraints tc = allToolbars.get(name);
		if (tc == null) tc = new ToolbarConstraints(this, name, position, visible);
		else tc.checkNextPosition(position, visible);
		return tc;
	}

	/**
	 * Revalidates toolbar pool window. It is important for change height when
	 * number of rows is changed.
	 */
	void revalidateWindow()
	{
		// replan to AWT thread, if needed (because of direct swing calls)
		if (SwingUtilities.isEventDispatchThread())
		{
			doRevalidateWindow();
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					doRevalidateWindow();
				}
			});
		}
	}

	/** 
	 * Performs revalidating work 
	 */
	private void doRevalidateWindow()
	{
		revalidate();
		java.awt.Window w = SwingUtilities.windowForComponent(this);
		if (w != null)
		{
			w.validate();
		}
	}

	/**
	 * Removes toolbar from visible toolbars.
	 * @param tc  specified toolbar
	 */
	void removeVisible(ToolbarConstraints tc)
	{
		invisibleToolbars.put(tc, new Integer(tc.rowIndex()));
		if (tc.destroy()) checkToolbarRows();
		tc.setVisible(false);

		this.setVisible(toolbarRows.size() != 0);
	}

	/**
	 * Adds toolbar from list of invisible to visible toolbars.
	 * @param tc specified toolbar
	 */
	void addInvisible(ToolbarConstraints tc)
	{
		int rC = toolbarRows.size();
		int pos = (invisibleToolbars.remove(tc)).intValue();
		tc.setVisible(true);
		for (int i = pos; i < pos + tc.getRowCount(); i++)
		{
			getRow(i).addToolbar(tc, tc.getPosition());
		}

		this.setVisible(true);
		if (rC != toolbarRows.size()) revalidateWindow();

	}

	/**
	 * @param rI ndex of required row
	 * @return toolbar row of specified index. If rI is out of bounds then new row is created.
	 */
	ToolbarRow getRow(int rI)
	{
		ToolbarRow row;
		int s = toolbarRows.size();
		if (rI < 0)
		{
			row = new ToolbarRow(this);
			addRow(row, 0);
		}
		else if (rI >= s)
		{
			row = new ToolbarRow(this);
			addRow(row);
		}
		else
		{
			row = toolbarRows.get(rI);
		}
		return row;
	}

	/**
	 * @return toolbar row at last row position.
	 */
	ToolbarRow createLastRow()
	{
		return getRow(toolbarRows.size());
	}

	/**
	 * Popup menu that should be displayed when the users presses right mouse
	 * button on the panel. This menu can contain contains list of possible
	 * configurations, additional actions, etc.
	 * 
	 * @return popup menu to be displayed
	 */
	private JPopupMenu createMenus()
	{
		menu = new JMenu(Messages.getString("servoy.toolbars.text")); //$NON-NLS-1$
		menu.setIcon(new EmptyIcon());
		popup = new JPopupMenu();

		// generate list of available actions
		Object[] col = actions.values().toArray();
		Arrays.sort(col);

		for (Object element : col)
		{
			ToolbarAction tba = (ToolbarAction)element;

			ActionCheckBoxMenuItem mi = new ActionCheckBoxMenuItem(tba);
			//			mi.setState(tba.isSelected());
			popup.add(mi);

			ActionCheckBoxMenuItem mi2 = new ActionCheckBoxMenuItem(tba);
			menu.add(mi2);
		}
		return popup;
	}

	private static class EmptyIcon implements Icon
	{
		public int getIconHeight()
		{
			return 16;
		}

		public int getIconWidth()
		{
			return 16;
		}

		public void paintIcon(Component c, Graphics g, int x, int y)
		{
		}
	}

	/**
	 * Popup menu that should be displayed when the users presses right mouse
	 * button on the panel. This menu can contain contains list of possible
	 * configurations, additional actions, etc.
	 * 
	 * @return popup menu to be displayed
	 */
	public JMenu getMenu()
	{
		if (menu == null) createMenus();
		return menu;
	}

	public Map<String, ToolbarAction> getActions()
	{
		return actions;
	}

	public void setToolbarVisible(String name, boolean visible)
	{
		ToolbarAction action = actions.get(name);
		if (action != null)
		{
			if ((action.isSelected() && !visible) || (!action.isSelected() && visible))
			{
				action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "do")); //$NON-NLS-1$
			}
		}
	}

	private static class ToolbarAction extends AbstractAction implements IProvideButtonModel, Comparable<ToolbarAction>
	{
		private final ToolbarPanel tp;
		private final ToolbarConstraints tc;
		private final Toolbar tb;
		private final FixedToggleButtonModel model;

		ToolbarAction(ToolbarPanel tp, ToolbarConstraints tc, Toolbar tb)
		{
			super(tb.getDisplayName());
			this.tp = tp;
			this.tb = tb;
			this.tc = tc;
			model = new FixedToggleButtonModel();
			model.setSelected(tb.isVisible());
		}

		public void actionPerformed(ActionEvent ae)
		{
			boolean wasVisible = tc.isVisible();
			if (wasVisible)
			{
				tp.removeVisible(tc);
				tb.setVisible(false);
			}
			else
			{
				tp.addInvisible(tc);
				tb.setVisible(true);
			}
			model.setSelected(tb.isVisible());
		}

		public String getName()
		{
			return tb.getName();
		}

		boolean isSelected()
		{
			return tb.isVisible();
		}

		public ButtonModel getModel()
		{
			return model;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(ToolbarAction toolbarAction)
		{
			return getName().compareToIgnoreCase(toolbarAction.getName());
		}
	}

	/*
	 * @see com.servoy.j2db.util.toolbar.IToolbarPanel#getCurrentToolBars()
	 */
	public String[] getToolBarNames()
	{
		return toolbars.keySet().toArray(new String[toolbars.size()]);
	}
}