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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.gui.FixedJList;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.smart.cmd.MenuWindowAction;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.gui.JEscapeDialog;
import com.servoy.j2db.util.gui.JMenuAlwaysEnabled;
import com.servoy.j2db.util.gui.SortedListModel;

/**
 * The dialog which shows the list of forms
 * 
 * @author jcompagner
 */
public class WindowMenuDialog implements ActionListener
{
	public static final int MAX_ITEMS = 20;

	private WindowDialog windowsDialog;
	private JMenuItem moreWindowsMenuItem;
	private JMenu windowMenu;
	private ButtonGroup group;
	protected HashMap formMenuItems; // formName -> JMenuItem    

	protected SwingFormManager actionListener;
	protected ArrayList inMenu;

	private final ISmartClientApplication application;

	public WindowMenuDialog(ISmartClientApplication application, SwingFormManager actionListener)
	{
		super();
		this.application = application;
		this.actionListener = actionListener;
		formMenuItems = new HashMap();
		inMenu = new ArrayList();
	}

	public JMenu getWindowMenu()
	{
		if (windowMenu == null)
		{
			group = new ButtonGroup();
			windowMenu = new JMenuAlwaysEnabled(new MenuWindowAction(application));
			windowMenu.setEnabled(false);
		}
		return windowMenu;
	}

	protected JRadioButtonMenuItem addForm(Form form)
	{
		if (windowMenu == null)
		{
			// headless client? addForm to menu doesn't make any sense for a headless client
			return null;
		}
		JRadioButtonMenuItem mi = (JRadioButtonMenuItem)formMenuItems.get(form.getUUID());
		if (mi != null) return mi;

		if (form.getShowInMenu() || actionListener.getShowFormsAllInWindowMenu())
		{
			if (windowMenu.getMenuComponentCount() > MAX_ITEMS)
			{
				removeOldest();
				if (moreWindowsMenuItem == null)
				{
					removeOldest();
					moreWindowsMenuItem = new JMenuItem(Messages.getString("servoy.windowMenuDialog.more")); //$NON-NLS-1$
					moreWindowsMenuItem.addActionListener(this);
					windowMenu.insertSeparator(0);
					windowMenu.insert(moreWindowsMenuItem, 0);

					if ("true".equals(application.getSettings().getProperty("windowdialog_state", "false"))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					{
						if (windowsDialog == null)
						{
							windowsDialog = new WindowDialog();
						}
						windowsDialog.showDialog();

					}
				}
			}
			mi = new JRadioButtonMenuItem(form.getName());
			if (form.getShowInMenu() && actionListener.getShowFormsAllInWindowMenu())
			{
				mi.setIcon(application.loadImage("showinmenuform.gif")); //$NON-NLS-1$
			}
			else
			{
				mi.setIcon(application.loadImage("empty.gif")); //$NON-NLS-1$
			}
			mi.addActionListener(actionListener);
			formMenuItems.put(form.getUUID(), mi);

			insertMenuItem(form, mi);

			if (windowsDialog != null)
			{
				windowsDialog.listModel.add(mi);
			}

			group.add(mi);
			return mi;
		}
		return null;
	}

	private void insertMenuItem(Form form, JRadioButtonMenuItem mi)
	{
		mi.updateUI(); // make sure that font or other l&f settings are applied. 
		int count = windowMenu.getMenuComponentCount();
		if (count == 0)
		{
			inMenu.add(mi);
			windowMenu.setEnabled(true);
			windowMenu.add(mi);
		}
		else
		{
			int i = 0;
			if (windowMenu.getMenuComponent(0) == moreWindowsMenuItem)
			{
				i = 2;
			}

			for (; i < count; i++)
			{
				Component comp = windowMenu.getMenuComponent(i);
				if (comp instanceof JMenuItem)
				{
					JMenuItem item = (JMenuItem)comp;
					if (item.getText().compareToIgnoreCase(form.getName()) > 0)
					{
						break;
					}
				}
				else
				{
					break;
				}
			}
			inMenu.add(mi);
			windowMenu.insert(mi, i);
		}
	}

	private void removeOldest()
	{
		JRadioButtonMenuItem item = (JRadioButtonMenuItem)inMenu.remove(0);
		while (item.isSelected())
		{
			inMenu.add(item);
			item = (JRadioButtonMenuItem)inMenu.remove(0);
		}
		windowMenu.remove(item);
	}

	public void destroy()
	{
		if (windowMenu != null)
		{
			windowMenu.removeAll();
			windowMenu.setEnabled(false);
		}
		formMenuItems = new HashMap();
		group = new ButtonGroup();
		inMenu = new ArrayList();

		if (windowsDialog != null)
		{
			windowsDialog.dispose();
			windowsDialog = null;
		}
		moreWindowsMenuItem = null;
	}

	/**
	 * @param f
	 */
	public void selectForm(Form f)
	{
		JRadioButtonMenuItem mi = (JRadioButtonMenuItem)formMenuItems.get(f.getUUID());
		if (mi == null) return;

		if (mi.getParent() == null)
		{
			removeOldest();
			insertMenuItem(f, mi);
		}
		else
		{
			if (inMenu.remove(mi))
			{
				inMenu.add(mi);
			}
		}

		if (mi != null && !mi.isSelected()) mi.setSelected(true);
	}

	/**
	 * @param form
	 */
	public void formChanged(final Form form)
	{
		final JMenuItem mi = (JMenuItem)formMenuItems.get(form.getUUID());
		if (mi != null)
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					String name = form.getName();

					if (windowsDialog != null)
					{
						windowsDialog.listModel.remove(mi);
					}

					mi.setText(name);
					mi.setActionCommand(name);
					if (form.getShowInMenu() && actionListener.getShowFormsAllInWindowMenu())
					{
						mi.setIcon(application.loadImage("showinmenuform.gif")); //$NON-NLS-1$
					}
					else
					{
						mi.setIcon(application.loadImage("empty.gif")); //$NON-NLS-1$
					}

					if (windowsDialog != null)
					{
						windowsDialog.listModel.add(mi);
					}
				}
			});
		}
	}

	/**
	 * @param form
	 */
	public void removeForm(Form form)
	{
		JMenuItem menuItem = (JMenuItem)formMenuItems.get(form.getUUID());
		formMenuItems.remove(form.getUUID());
		if (menuItem != null)
		{
			Container m = menuItem.getParent();
			if (m != null)
			{
				m.remove(menuItem);
			}
			if (windowsDialog != null)
			{
				windowsDialog.listModel.remove(menuItem);
			}
		}
	}

	/**
	 * @param form
	 */
	public void refreshFrom(Form form)
	{
		JMenuItem menuItem = (JMenuItem)formMenuItems.get(form.getUUID());
		if (menuItem != null)
		{
			menuItem.setText(form.getName());
			if (form.getShowInMenu() && actionListener.getShowFormsAllInWindowMenu())
			{
				menuItem.setIcon(application.loadImage("showinmenuform.gif")); //$NON-NLS-1$
			}
			else
			{
				menuItem.setIcon(application.loadImage("empty.gif")); //$NON-NLS-1$
			}
		}
	}


	/**
	 * 
	 */
	public void clearList()
	{
		if (windowsDialog != null)
		{
			windowsDialog.listModel.removeAll();
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == moreWindowsMenuItem)
		{
			if (windowsDialog == null)
			{
				windowsDialog = new WindowDialog();
			}
			windowsDialog.showDialog();
		}
	}


	private class WindowDialog extends JEscapeDialog implements KeyListener, MouseListener
	{
		protected FixedJList list;
		protected SortedListModel listModel;

		public WindowDialog()
		{
			super(application.getMainApplicationFrame(), Messages.getString("servoy.windowMenuDialog.chooseForm"), false); //$NON-NLS-1$
			setName("windowdialog"); //$NON-NLS-1$
			getContentPane().setLayout(new BorderLayout());

			listModel = new SortedListModel(new MenuItemComparator(), formMenuItems.values());

			list = new FixedJList();
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			list.addMouseListener(this);
			list.addKeyListener(this);
			list.setCellStringValue(new MenuItemCellString());
			list.setCellRenderer(new MenuItemRenderer());
			list.setModel(listModel);

			getContentPane().add(new JScrollPane(list), BorderLayout.CENTER);

			if (!Settings.getInstance().loadBounds(this))
			{
				this.setLocationRelativeTo(application.getMainApplicationFrame());
				this.setSize(new Dimension(200, 300));
			}
			application.registerWindow("windowdialog", this); //$NON-NLS-1$
		}

		public void showDialog()
		{
			FormController fc = ((FormManager)application.getFormManager()).getCurrentMainShowingFormController();
			JMenuItem menuItem = (JMenuItem)formMenuItems.get(fc.getForm().getUUID());
			if (menuItem != null) list.setSelectedValue(menuItem, true);
			setVisible(true);
		}

		private void openForm()
		{
			JMenuItem item = (JMenuItem)list.getSelectedValue();
			actionListener.actionPerformed(new ActionEvent(item, ActionEvent.ACTION_PERFORMED, item.getActionCommand()));
			//setVisible(false);
		}

		@Override
		protected void cancel()
		{
			setVisible(false);
		}

		/*
		 * @see java.awt.Dialog#dispose()
		 */
		@Override
		public void dispose()
		{
			if (isVisible())
			{
				setVisible(false);
				application.getSettings().setProperty("windowdialog_state", "true"); //$NON-NLS-1$//$NON-NLS-2$
			}
			else
			{
				application.getSettings().setProperty("windowdialog_state", "false"); //$NON-NLS-1$//$NON-NLS-2$
			}
			super.dispose();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.util.JEscapeDialog#setVisible(boolean)
		 */
		@Override
		public void setVisible(boolean b)
		{
			if (!b)
			{
				Settings.getInstance().saveBounds(this);
			}
			super.setVisible(b);
		}

		public void keyTyped(KeyEvent e)
		{
		}

		public void keyPressed(KeyEvent e)
		{
		}

		public void keyReleased(KeyEvent e)
		{
			if (e.getKeyCode() == KeyEvent.VK_ENTER)
			{
				openForm();
			}
		}


		public void mouseReleased(MouseEvent e)
		{
			if (e.getClickCount() == 2)
			{
				openForm();
			}
		}

		public void mouseClicked(MouseEvent e)
		{
		}

		public void mousePressed(MouseEvent e)
		{
		}

		public void mouseEntered(MouseEvent e)
		{
		}

		public void mouseExited(MouseEvent e)
		{
		}
	}

	private static class MenuItemRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			Icon icon = null;
			if (value != null && value instanceof JMenuItem)
			{
				icon = ((JMenuItem)value).getIcon();
				value = ((JMenuItem)value).getText();
			}
			Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (comp instanceof JLabel)
			{
				((JLabel)comp).setIcon(icon);
			}
			return comp;
		}
	}

	private static class MenuItemComparator implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			return ((JMenuItem)o1).getText().compareToIgnoreCase(((JMenuItem)o2).getText());
		}
	}
	private static class MenuItemCellString implements FixedJList.CellStringValue
	{
		public String getValue(Object value)
		{
			if (value != null && value instanceof JMenuItem) return ((JMenuItem)value).getText();
			else if (value != null) return value.toString();
			return ""; //$NON-NLS-1$
		}

	}
}
