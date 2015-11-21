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
package com.servoy.j2db.property;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.Debug;

/**
 * Make it possible to set a dataprovider on element
 *
 * @author jblok
 */
public class DataProviderEditor extends JPanel implements IOptimizedPropertyEditor, ItemListener
{
	private boolean showRelatedOptionsOnly = false;
	private boolean showColumnsOnly = false;
	private String value = null;
	protected IApplication application;
	protected JList list;
	protected JComboBox relationsComboBox;

	public DataProviderEditor()
	{
		this(false);
	}

	public DataProviderEditor(boolean showToolTips)
	{
		// main part of the dialog
		list = new JList();
		list.setCellRenderer(new PropertyListCellRenderer(showToolTips)
		{

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof IPersist && component instanceof JLabel)
				{
					JLabel label = (JLabel)component;
					Solution sol = (Solution)((IPersist)value).getRootObject();
					if (application.getSolution() != sol)
					{
						label.setText(label.getText() + " (" + sol.getName() + ")"); //$NON-NLS-1$//$NON-NLS-2$
					}
				}
				else if (value instanceof Column && component instanceof JLabel)
				{
					((JLabel)component).setText(((Column)value).getTitle());
				}
				return component;
			}

		});
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					fireActionEvent();
				}
			}
		});

		JScrollPane listScroller = new JScrollPane(list);
		listScroller.setPreferredSize(new Dimension(300, 300));
		// //XXX: Must do the following, too, or else the scroller thinks
		// //XXX: it's taller than it is:
		// listScroller.setMinimumSize(new Dimension(250, 80));
		// listScroller.setAlignmentX(LEFT_ALIGNMENT);

		// Create a container so that we can add a title around
		// the scroll pane. Can't add a title directly to the
		// scroll pane because its background would be white.
		// Lay out the label and scroll pane from top to button.
		JPanel listPane = new JPanel();
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
		relationsComboBox = new JComboBox();
		// base = db.getBase();

		// JLabel label = new JLabel("hello");
		// listPane.add(comboBox);
		listPane.add(Box.createRigidArea(new Dimension(0, J2DBClient.BUTTON_SPACING)));
		listPane.add(listScroller);
		list.setModel(new DefaultListModel());
		// listPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

		// Lay out the buttons from left to right.
		/*
		 * JPanel buttonPane = new JPanel(); buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		 * buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); buttonPane.add(Box.createHorizontalGlue()); buttonPane.add(setButton);
		 * buttonPane.add(Box.createRigidArea(new Dimension(10, 0))); buttonPane.add(cancelButton);
		 */
		// Put everything together, using the content pane's BorderLayout.
		// Container contentPane = getContentPane();
		// JPanel borderPanel = new JPanel();
		setLayout(new BorderLayout());
		// borderPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(relationsComboBox, BorderLayout.NORTH);
		add(listPane, BorderLayout.CENTER);
		// borderPanel.add(buttonPane, BorderLayout.SOUTH);
		// add(borderPanel, BorderLayout.CENTER);
		// loadBounds("SelectFieldDialog");
		// createItemsInBox();
		relationsComboBox.addItemListener(this);
		// setButton.requestFocus();
		setName("DataProviderEditor");
	}

	public Relation getRelation()
	{
		Object item = relationsComboBox.getSelectedItem();
		if (item instanceof Relation)
		{
			return (Relation)item;
		}
		return null;
	}

	protected Table definedTable;

	protected void fillRelationsComboBox(Relation[] relations) throws Exception
	{
		boolean relationsAdded = false;
		String item = null;

		Table table = null;
		if (definedTable == null)
		{
			FormManager fm = (FormManager)application.getFormManager();
			FormController fc = fm.getCurrentMainShowingFormController();
			if (fc != null)
			{
				Form form = fc.getForm();
				table = form.getTable();
			}
		}
		else
		{
			table = definedTable;
			// definedTable = null;//clear!
		}
		if (relationsComboBox.getItemCount() > 0) relationsComboBox.removeAllItems();
		Iterator it = application.getFlattenedSolution().getRelations(table, true, true);
		while (it.hasNext())
		{
			Relation rel = (Relation)it.next();
			if (!showSortableOnly || (showSortableOnly && rel.isUsableInSort()))
			{
				relationsComboBox.addItem(rel);
				relationsAdded = true;
			}
		}
		if (!showRelatedOptionsOnly)
		{
			String tname = ""; //$NON-NLS-1$
			if (table != null) tname = table.getName();
			item = "DataProviders for " + tname;
			if (relationsComboBox.getModel().getSize() > 0)
			{
				relationsComboBox.insertItemAt(item, 0);
			}
			else
			{
				relationsComboBox.addItem(item);
			}
		}
		if (relations == null)
		{
			if (item == null)
			{
				if (relationsComboBox.getModel().getSize() != 0) relationsComboBox.setSelectedIndex(0);
			}
			else
			{
				relationsComboBox.setSelectedItem(item);
			}
		}
		else
		{
			relationsComboBox.setSelectedItem(relations[0]);
		}
		relationsComboBox.setEnabled(relationsAdded && !showRelatedOptionsOnly);
	}

	public void setValue(Object v)
	{
		value = (String)v;

		FormManager fm = (FormManager)application.getFormManager();
		FormController fc = fm.getCurrentMainShowingFormController();
		if (fc != null)
		{
			Form form = fc.getForm();
			if (form != null)
			{
				try
				{
					IDataProvider dp = application.getFlattenedSolution().getDataproviderLookup(null, form).getDataProvider(value);
					showDataEx(dp);
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}
		}
	}

	public void showDataEx(IDataProvider dp) throws Exception
	{
		relationsComboBox.removeItemListener(this);

		if (dp instanceof ColumnWrapper)
		{
			fillRelationsComboBox(((ColumnWrapper)dp).getRelations());
		}
		else
		{
			fillRelationsComboBox(relation == null ? null : new Relation[] { relation });
		}
		relationsComboBox.addItemListener(this);

		list.setModel(new DefaultListModel());
		fillDataProviderList();
		if (dp instanceof ColumnWrapper)
		{
			dp = ((ColumnWrapper)dp).getColumn();
		}
		if (dp != null) list.setSelectedValue(dp, true);
	}

	private Relation relation;

	public void setRelation(Relation r) throws Exception
	{
		relation = r;
	}

	public void itemStateChanged(ItemEvent e) // for relations Combobox
	{
		fillDataProviderList();
	}

	protected void fillDataProviderList()
	{
		try
		{
			ITable table = null;
			if (definedTable == null)
			{
				FormManager fm = (FormManager)application.getFormManager();
				FormController fc = fm.getCurrentMainShowingFormController();
				if (fc != null)
				{
					Form form = fc.getForm();
					table = form.getTable();
				}
			}
			else
			{
				if (!showRelatedOptionsOnly) table = definedTable;
			}

			DefaultListModel model = (DefaultListModel)list.getModel();
			model.removeAllElements();

			if (showNoneOption) model.addElement("-none-");
			if (!showColumnsOnly) model.addElement("*columns");
			Object o = relationsComboBox.getSelectedItem();
			if (o != null)
			{
				if (o instanceof String)
				{
					// table = form.getTable();
				}
				else
				{
					table = ((Relation)o).getForeignTable();
				}
				if (table != null)
				{
					Iterator<Column> it = table.getColumnsSortedByName();
					while (it.hasNext())
					{
						IColumn c = it.next();

						ColumnInfo ci = c.getColumnInfo();
						if (ci != null && ci.isExcluded())
						{
							continue;
						}

						if (hideMediaColumns)
						{
							// use dataprovider type as defined by column converter
							ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, c, application);
							if (componentFormat.dpType == IColumnTypes.MEDIA)
							{
								continue;
							}
						}
						model.addElement(c);
					}
				}
			}

			FlattenedSolution s = application.getFlattenedSolution();
			if (table != null && !showColumnsOnly)
			{
				Iterator it = s.getScriptCalculations(table, true);
				if (it.hasNext())
				{
					model.addElement("*calculations");
				}
				while (it.hasNext())
				{
					ScriptCalculation sc = (ScriptCalculation)it.next();
					for (int i = 0; i < model.size(); i++)
					{
						Object obj = model.elementAt(i);
						if (obj instanceof IDataProvider)
						{
							IDataProvider dp = (IDataProvider)obj;
							if (dp.getDataProviderID().equals(sc.getDataProviderID()))
							{
								model.remove(i);// remove the column from the list if use by
								// stored calc
								break;
							}
						}

					}
					model.addElement(sc);
				}
				Iterator it2 = s.getScriptVariables(true);
				if (it2.hasNext())
				{
					model.addElement("*globals");
				}
				while (it2.hasNext())
				{
					model.addElement(it2.next());
				}
				Iterator it3 = s.getAggregateVariables(table, true);
				if (it3.hasNext())
				{
					model.addElement("*aggregates");
				}
				while (it3.hasNext())
				{
					model.addElement(it3.next());
				}
			}
			if (table != null && showColumnsOnly && showSortableOnly)
			{
				Iterator it3 = s.getAggregateVariables(table, true);
				while (it3.hasNext())
				{
					model.addElement(it3.next());
				}
			}
			if (showGlobalsOption && showColumnsOnly)
			{
				Iterator it2 = s.getScriptVariables(true);
				if (it2.hasNext())
				{
					model.addElement("*globals");
				}
				while (it2.hasNext())
				{
					model.addElement(it2.next());
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
	}

	public String getJavaInitializationString()
	{
		return null;
	}

	public String[] getTags()
	{
		return null;
	}

	public boolean isPaintable()
	{
		return false;
	}

	public void paintValue(Graphics gfx, java.awt.Rectangle box)
	{
	}

	public Object getValue()
	{
		Object retval = null;

		Object[] selections = list.getSelectedValues();
		IDataProvider[] array = new IDataProvider[selections.length];
		for (int i = 0; i < selections.length; i++)
		{
			Object o = selections[i];
			if (o instanceof IDataProvider)
			{
				Object item = relationsComboBox.getSelectedItem();
				if (item instanceof Relation && o instanceof IColumn)
				{
					array[i] = new ColumnWrapper((IColumn)o, new Relation[] { ((Relation)item) });
				}
				else
				{
					array[i] = (IDataProvider)o;
				}
			}
		}

		if (selections.length == 1)
		{
			retval = array[0];
		}
		else
		{
			retval = array;
		}

		if (returnValueAsString)// if used to setDataProviderID
		{
			if (array != null && array.length != 0 && array[0] != null)
			{
				retval = array[0].getDataProviderID();
			}
			else
			{
				return null;
			}
		}
		relation = null;
		return retval;
	}

	public void setAsText(String text)
	{
		value = text;
	}

	public String getAsText()
	{
		if (value != null)
		{
			return value;
		}

		/*
		 * Form form = application.getFormManager().getCurrentMainShowingForm(); if (form != null && value != null) { try { IDataProvider dp =
		 * form.getDataProvider(value); if (dp != null) { return dp.getDisplayName(); } } catch(Exception ex) { Debug.error(ex); } }
		 */
		return "<unknown>";
	}

	public Component getCustomEditor()
	{
		return this;
	}

	public boolean supportsCustomEditor()
	{
		return true;
	}

	public void init(IApplication app)
	{
		application = app;
	}

	public void init(IClientPluginAccess access)
	{
		application = ((ClientPluginAccessProvider)access).getApplication();
	}

	private final ArrayList elist = new ArrayList();

	public void addActionListener(ActionListener l)
	{
		elist.add(l);
	}

	public void removeActionListener(ActionListener l)
	{
		elist.remove(l);
	}

	private void fireActionEvent()
	{
		ActionEvent ae = null;
		Iterator iter = elist.iterator();
		while (iter.hasNext())
		{
			ActionListener listener = (ActionListener)iter.next();
			// Lazily create the event:
			if (ae == null) ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "OK");
			listener.actionPerformed(ae);
		}
	}

	public void setAllowMultipleSelections(boolean b)
	{
		if (b)
		{
			list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		}
		else
		{
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
	}

	private boolean showSortableOnly = false;

	public void setShowSortableOnly(boolean b)
	{
		showSortableOnly = b;
	}

	public void setShowRelatedOnly(boolean b)
	{
		showRelatedOptionsOnly = b;
	}

	public void setShowColumnsOnly(boolean b)
	{
		showColumnsOnly = b;
	}

	public void setRelatedEnabled(boolean b)
	{
		relationsComboBox.setEnabled(b);
	}

	private boolean showNoneOption = true;

	public void dontShowNoneOption()
	{
		showNoneOption = false;
		// fillDataProviderList();
	}

	/**
	 * Sets the definedTable.
	 *
	 * @param definedTable The definedTable to set
	 */
	public void setDefinedTable(ITable definedTable)
	{
		this.definedTable = (Table)definedTable;
	}

	private boolean returnValueAsString = true;

	/**
	 * Sets the returnValueAsString.
	 *
	 * @param returnValueAsString The returnValueAsString to set
	 */
	public void setReturnValueAsString(boolean returnValueAsString)
	{
		this.returnValueAsString = returnValueAsString;
	}

	public void prepareForVisible(IApplication app, boolean b)
	{
		// ignore
	}

	/**
	 * @param b
	 */
	public void setShowGlobals(boolean b)
	{
		showGlobalsOption = b;
	}

	private boolean showGlobalsOption = false;

	private boolean hideMediaColumns = false;

	public void setHideMediaColumns(boolean b)
	{
		hideMediaColumns = b;
	}
}
